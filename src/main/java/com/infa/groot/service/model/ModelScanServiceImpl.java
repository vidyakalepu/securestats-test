package com.infa.groot.service.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infa.groot.repo.GrootScanRecord;
import com.infa.groot.repo.GrootScanRecordRepo;
import com.infa.groot.repo.GrootScanStatus;
import com.infa.groot.utils.GrootScanType;
import com.infa.groot.utils.GrootScanUtils;
import com.infa.groot.utils.ResultHandlerUtil;

/**
 * 
 * @author pujain
 *
 */
@Service
public class ModelScanServiceImpl implements ModelScanService {
    private static final Logger logger = LoggerFactory.getLogger(ModelScanServiceImpl.class);

    @Value("${reasoner.url}")
	private String reasonerUrl;
    
    @Value("${product.help.url}")
	private String productHelpUrl;
    
    @Value("${reasoner.request.body}")
	private String reasonerRequestBody;
    
    @Value("${product.help.request.body}")
	private String productHelpRequestBody;
    
    @Inject
    private GrootScanRecordRepo grootScanRecordRepo;

    @Inject
    private GrootScanUtils grootScanUtils;

    @Inject
    private ResultHandlerUtil resultHandlerUtil;

    private static final int MAX_RETRIES = 3;
    

    private final ConcurrentMap<Long, Boolean> cancellationMap = new ConcurrentHashMap<>();

    @Override
    public void runModelScan(Long scanId) {
        try {
            GrootScanRecord scan = grootScanRecordRepo.findById(scanId).orElseThrow(() -> new RuntimeException("Scan not found"));
            
            GrootScanType scanType = GrootScanType.valueOf(scan.getScanType().toUpperCase());
            String conversationId = scan.getConversationId();
            List<String> promptCategories = Arrays.asList(scan.getPromptCategories().split(","));
            String promptFilePath = scan.getPromptFilePath();

            String url = null;
            String requestBody = null;

            if (GrootScanType.MULTISTEP_REASONER.equals(scanType)) {
                url = reasonerUrl;
                requestBody = reasonerRequestBody;
            } else if (GrootScanType.PRODUCT_HELP.equals(scanType)) {
                url = productHelpUrl;
                requestBody = productHelpRequestBody;
            }

            if (url == null || requestBody == null) {
                logger.error("URL or Request body missing. Failing Scan.");
                grootScanUtils.updateScanStatus(scanId, GrootScanStatus.FAILED, Optional.of("Scan failed due to missing url"));
                return;
            }

            boolean hasSuccessfulPrompt = false;

            for (String promptCategory : promptCategories) {
                if (cancellationMap.getOrDefault(scanId, false)) {
                    logger.info("Scan with ID: {} was canceled.", scanId);
                    grootScanUtils.updateScanStatus(scanId, GrootScanStatus.DELETED, Optional.of("Scan deleted by user"));
                    return;
                }

                String completeFilePath = String.format("%s%s.txt", promptFilePath, promptCategory);
                logger.info("Processing file: {}", completeFilePath);
                List<String> prompts = grootScanUtils.readPrompts(completeFilePath);
                if (prompts == null || prompts.isEmpty()) {
                    logger.error("Failed to read prompts or file is empty for {}", completeFilePath);
                    continue;
                }

                Map<String, String> promptResponseMap = new HashMap<>();
                for (String prompt : prompts) {
                    if (cancellationMap.getOrDefault(scanId, false)) {
                        logger.info("Scan with ID: {} was canceled during prompt processing.", scanId);
                        grootScanUtils.updateScanStatus(scanId, GrootScanStatus.DELETED, Optional.of("Scan deleted by user"));
                        return;
                    }

                    logger.info("Processing prompt: {}", prompt);

                    int attempt = 0;
                    while (attempt < MAX_RETRIES) {
                        String response = makeRequest(prompt, url, requestBody);

                        if (response.startsWith("Error:") || response.startsWith("Exception:")) {
                            attempt++;
                            logger.error("Request failed for prompt {}. Attempt {} of {}", prompt, attempt, MAX_RETRIES);
                            if (attempt >= MAX_RETRIES) {
                                logger.error("Maximum retries reached for prompt {}. Marking scan as failed.", prompt);
                                if (hasSuccessfulPrompt) {
                                    logger.info("Marking scan as partially successful.");
                                    grootScanUtils.updateScanStatus(scanId, GrootScanStatus.PARTIALLY_SUCCESSFUL, Optional.of("Scan finished. Partial results available."));
                                } else {
                                    grootScanUtils.updateScanStatus(scanId, GrootScanStatus.FAILED, Optional.of("Scan failed"));
                                }
                                return;
                            }
                        } else {
                            promptResponseMap.put(prompt, response);
                            hasSuccessfulPrompt = true;
                            break;   
                        }
                    }
                }

                resultHandlerUtil.saveScanResult(promptResponseMap, scanType, scan.getId(), promptCategory, conversationId);
            }

            if (hasSuccessfulPrompt) {
                logger.info("Scan completed with partial success.");
                grootScanUtils.updateScanStatus(scanId, GrootScanStatus.PARTIALLY_SUCCESSFUL, Optional.of("Scan finished. Partial results available"));
            } else {
                logger.info("Scan completed successfully.");
                grootScanUtils.updateScanStatus(scanId, GrootScanStatus.SUCCESSFUL, Optional.of("Scan finished"));
            }

        } catch (Exception e) {
            logger.error("Exception occurred during scan. Marking scan as failed. Exception: {}", e.getMessage());
            grootScanUtils.updateScanStatus(scanId, GrootScanStatus.FAILED, Optional.of(String.format("Scan failed due to exception: {}", e)));
        }
    }

    @Override
    public void cancelScan(Long scanId) {
        logger.info("Cancel request received for scan with ID: {}", scanId);
        cancellationMap.put(scanId, true);
    }

    private String makeRequest(String prompt, String url, String requestBody) {
	    RestTemplate restTemplate = new RestTemplate();
	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	
	    ObjectMapper objectMapper = new ObjectMapper();
	    String updatedRequestBody;
	
	    try {
            Map<String, Object> requestBodyMap = objectMapper.readValue(requestBody, Map.class);
	
	        if (requestBodyMap.containsKey("inputs")) {
	            String inputs = (String) requestBodyMap.get("inputs");
	            inputs = inputs.replace("(prompt)", prompt);
	            requestBodyMap.put("inputs", inputs);
	        }
	
	        updatedRequestBody = objectMapper.writeValueAsString(requestBodyMap);
	        logger.info("Request Body for prompt {}: {}", prompt, updatedRequestBody);
	    } catch (JsonProcessingException e) {
	        logger.error("Error parsing or processing request body for prompt: {}. Error: {}", prompt, e.getMessage());
	        return "Error: Invalid request body.";
	    }
	
	    HttpEntity<String> requestEntity = new HttpEntity<>(updatedRequestBody, headers);
	
	    try {
	        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
	        
	        if (!ObjectUtils.isEmpty(responseEntity) && !ObjectUtils.isEmpty(responseEntity.getBody())) {
	            return responseEntity.getBody();
	        } else {
	            logger.warn("Received null response or empty body for prompt: {}", prompt);
	            return "Error: Empty or null response received.";
	        }
	    } catch (RestClientException e) {
	        logger.error("Error making request for prompt: {}. Error: {}", prompt, e.getMessage());
	        return "Error: " + e.getMessage();
	    }
	}

}
