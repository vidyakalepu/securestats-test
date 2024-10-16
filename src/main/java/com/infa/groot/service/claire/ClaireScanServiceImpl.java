package com.infa.groot.service.claire;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.infa.groot.repo.GrootScanRecord;
import com.infa.groot.repo.GrootScanRecordRepo;
import com.infa.groot.repo.GrootScanStatus;
import com.infa.groot.utils.GrootScanType;
import com.infa.groot.utils.GrootScanUtils;
import com.infa.groot.utils.ResultHandlerUtil;
import com.infa.groot.utils.ServiceConstants;

import java.util.Optional; 

/**
 * 
 * @author pujain
 *
 */
@Service
@Scope("prototype")
public class ClaireScanServiceImpl implements ClaireScanService {
    
    @Value("${jar.file.path}")
    private String jarFilePath;

    private static final Logger logger = LoggerFactory.getLogger(ClaireScanServiceImpl.class);

    @Autowired
    private GrootScanRecordRepo grootScanRecordRepo;

    @Inject
    private ResultHandlerUtil resultHandlerUtil;

    @Inject
    private GrootScanUtils grootScanUtils;
    
    @Autowired
    private ClaireSessionManager claireSessionManager;
    
    @Value("${max.retries}")
    private int maxRetries;

    private final ConcurrentMap<Long, Boolean> cancellationMap = new ConcurrentHashMap<>();

    @Override
    public void runClaireScan(Long scanId) {
        int totalPrompts = 0;
        int successfulPrompts = 0;
        try {
        	GrootScanRecord scan = grootScanRecordRepo.findById(scanId)
        		    .orElseThrow(() -> new RuntimeException("Scan not found"));
            String conversationId = scan.getConversationId();
            List<String> promptCategories = Arrays.asList(scan.getPromptCategories().split(ServiceConstants.COMMA));
            String promptFilePath = scan.getPromptFilePath();

            claireSessionManager.initializeSession();

            cancellationMap.put(scanId, true);

            for (String promptCategory : promptCategories) {
                if (!cancellationMap.getOrDefault(scanId, false)) {
                    logger.info("Scan with ID: {} has been canceled. Stopping processing.", scanId);
                    break;
                }

                String completeFilePath = String.format("%s%s%s.txt", promptFilePath,File.separator, promptCategory);
                logger.info("Processing file: {}", completeFilePath);

                List<String> prompts = grootScanUtils.readPrompts(completeFilePath);
                if (ObjectUtils.isEmpty(prompts)) {
                    logger.error("Failed to read prompts or file is empty for {}", completeFilePath);
                    continue;
                }

                totalPrompts += prompts.size();

                Map<String, String> promptResponseMap = new HashMap<>();
                if (!cancellationMap.getOrDefault(scanId, false)) {
                    logger.info("Scan with ID: {} has been canceled. Stopping processing.", scanId);
                    break;
                }

                boolean isFileSuccessful = processPrompts(prompts, promptResponseMap, promptCategory, conversationId, scanId);

                if (isFileSuccessful) {
                    successfulPrompts += promptResponseMap.size();
                    resultHandlerUtil.saveScanResult(promptResponseMap, GrootScanType.CLAIRE, scanId, promptCategory, conversationId);
                }
            }

            if (cancellationMap.getOrDefault(scanId, false)) {
                if (successfulPrompts == totalPrompts && totalPrompts > 0) {
                    grootScanUtils.updateScanStatus(scanId, GrootScanStatus.SUCCESSFUL, Optional.of("Scan finished successfully"));
                } else if (successfulPrompts > 0) {
                    grootScanUtils.updateScanStatus(scanId, GrootScanStatus.PARTIALLY_SUCCESSFUL, Optional.of("Scan finished. Partial results available"));
                } else {
                    grootScanUtils.updateScanStatus(scanId, GrootScanStatus.FAILED, Optional.of("Scan failed"));
                }
            } else {
                grootScanUtils.updateScanStatus(scanId, GrootScanStatus.DELETED, Optional.of("Scan deleted by user"));
            }
        } catch (Exception e) {
            logger.error("Exception occurred during scan, scan could not be completed. Exception: {}", e);
            grootScanUtils.updateScanStatus(scanId, GrootScanStatus.FAILED, Optional.of(String.format("Scan failed due to exception: %s", e)));
        } finally {
            cancellationMap.remove(scanId);
        }
    }

    private boolean processPrompts(List<String> prompts, Map<String, String> promptResponseMap, String promptCategory, String conversationId, Long scanId) {
        boolean promptSuccess = false;

        for (String prompt : prompts) {
            if (!cancellationMap.getOrDefault(scanId, false)) {
                logger.info("Scan with ID: {} has been canceled. Stopping prompt processing.", scanId);
                break;
            }

            logger.info("Processing prompt: {}", prompt);

            String response = retryPromptWithCredentials(prompt, conversationId, scanId);
            if (response != null && !response.startsWith("Error") && !response.startsWith("Exception")) {
                promptResponseMap.put(prompt, response);
                promptSuccess = true;
            } else {
                logger.error("Prompt processing failed for prompt: {}. Marking scan as failed.", prompt);
                return false;
            }
        }

        return promptSuccess;
    }


    private String retryPromptWithCredentials(String prompt, String conversationId, Long scanId) {
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                claireSessionManager.ensureValidSession();

                String response = makeRequest(prompt, claireSessionManager.getJsonToken(), claireSessionManager.getSessionId(), conversationId);
                if (!response.startsWith("javax.websocket.DeploymentException")) {
                    return response;
                }

                logger.error("Request failed due to expired credentials. Attempt {} of {}", attempt + 1, maxRetries);
                attempt++;
            } catch (Exception e) {
                logger.error("Error occurred while retrying prompt {}: {}", prompt, e.getMessage());
                break;
            }
        }

        logger.error("Maximum retry limit reached for prompt: {}. Failing scan.", prompt);
        return null;
    }

    private String makeRequest(String prompt, String jsonToken, String sessionId, String conversationId) {
        try {
            File jarFile = new File(jarFilePath);
            if (!jarFile.exists() || !jarFile.canExecute()) {
                throw new IllegalArgumentException("JAR file not found or not executable at path: " + jarFilePath);
            }

            ProcessBuilder processBuilder = buildProcessBuilder(prompt, jsonToken, sessionId, conversationId);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                StringBuilder errors = new StringBuilder();
                while ((line = errorReader.readLine()) != null) {
                    errors.append(line);
                }
                throw new RuntimeException("Error while executing prompt. Exit code: " + exitCode + ". Errors: " + errors.toString());
            }

            return output.toString();
        } catch (IllegalArgumentException e) {
            logger.error("Critical error: {}", e);
            return null;
        } catch (Exception e) {
            logger.error("Exception occurred while executing prompt {}: {}", prompt, e.getMessage());
            return null;
        }
    }
    
    private ProcessBuilder buildProcessBuilder(String prompt, String jsonToken, String sessionId, String conversationId) {
        return new ProcessBuilder(
            ServiceConstants.JAVA_COMMAND, 
            ServiceConstants.JAR_OPTION, jarFilePath,
            ServiceConstants.SESSION_OPTION, sessionId,
            ServiceConstants.TOKEN_OPTION, jsonToken,
            ServiceConstants.CONVERSATION_OPTION, conversationId,
            ServiceConstants.PROMPT_OPTION, prompt
        );
    }

    @Override
    public void cancelScan(Long scanId) {
        logger.info("Request received to cancel scan with ID: {}", scanId);
        cancellationMap.put(scanId, false);
    }
}
