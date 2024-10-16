package com.infa.groot.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.google.common.collect.Lists;
import com.infa.groot.dto.GrootResponseDto;
import com.infa.groot.dto.GrootScanDto;
import com.infa.groot.dto.GrootScanResultFilters;
import com.infa.groot.repo.GrootScanRecord;
import com.infa.groot.repo.GrootScanRecordRepo;
import com.infa.groot.repo.GrootScanResult;
import com.infa.groot.repo.GrootScanResultRepo;
import com.infa.groot.repo.GrootScanStatus;
import com.infa.groot.repo.PromptSeverity;
import com.infa.groot.repo.ResponseStatus;
import com.infa.groot.repo.ResultSpecifications;
import com.infa.groot.service.claire.ClaireScanService;
import com.infa.groot.service.model.ModelScanService;
import com.infa.groot.utils.GrootScanType;
import com.infa.groot.utils.GrootScanUtils;
import com.infa.groot.utils.ServiceConstants;

/**
 * 
 * @author pujain
 *
 */
@Service
public class GrootServiceImpl implements GrootService {
	

	@Inject
	private GrootScanRecordRepo grootScanRecordRepo;
	
	@Inject
	private GrootScanResultRepo grootScanResultRepo;
	
	@Inject
	private ClaireScanService claireScanService;
	
	@Inject
	private ModelScanService modelScanService;
	
	@Inject
	private GrootScanUtils grootScanUtils;
	
	
	private Logger logger = LoggerFactory.getLogger(GrootServiceImpl.class);

	public GrootResponseDto saveNewScan(GrootScanDto grootScanDto) {
	    try {
	        logger.info("Accepting new scan with details: {}", grootScanDto);

	        GrootScanRecord scan = new GrootScanRecord(grootScanDto.getProduct(), grootScanDto.getMicroservice(), 
	        		new Date(), String.join(ServiceConstants.COMMA, grootScanDto.getPromptCategories()), 
	        		grootScanDto.getPromptFilePath(), GrootScanStatus.SUBMITTED, grootScanDto.getComment(), grootScanDto.getScanType());
	       
	        GrootScanRecord savedScan = grootScanRecordRepo.save(scan);

	        logger.info("New scan saved with ID: {}", savedScan.getId());
	        
	        GrootResponseDto grootResponseDto = new GrootResponseDto();
	        grootResponseDto.setScanId(savedScan.getId());
	        return grootResponseDto; 

	    } catch (Exception e) {
	        logger.error("Error occurred while saving new scan.", e);
	        return null;
	    }
	}

	public GrootResponseDto getScanStatus(Long scanId) {
	    try {
	        Optional<GrootScanRecord> scan = grootScanRecordRepo.findById(scanId);

	        if (scan.isPresent()) {
	            GrootScanStatus status = scan.get().getStatus();
	            logger.info("Scan status for ID {}: {}", scanId, status);
	      
		        GrootResponseDto grootResponseDto = new GrootResponseDto();
		        grootResponseDto.setScanStatus(status);
		        return grootResponseDto; 
	        } else {
	            logger.warn("Scan with ID {} not found", scanId);
	            return null;
	        }

	    } catch (Exception e) {
	        logger.error("Error occurred while retrieving scan status for ID {}.", scanId, e);
	        return null;
	    }
	}

	public boolean retryScan(Long scanId) {
	    Optional<GrootScanRecord> scan = grootScanRecordRepo.findById(scanId);
	    
	    if (scan.isPresent()) {
	        GrootScanRecord scanRecord = scan.get();
	        if (scanRecord.getStatus().equals(GrootScanStatus.FAILED) || scanRecord.getStatus().equals(GrootScanStatus.PARTIALLY_SUCCESSFUL)) {
		        grootScanUtils.updateScanStatus(scanRecord.getId(), GrootScanStatus.SUBMITTED, Optional.of("Scan retried by user"));
	            logger.info("Retrying scan with ID: {}", scanId);
	            return true;
	        } else {
	            logger.warn("Scan with ID: {} cannot be retried as its current status is: {}", scanId, scanRecord.getStatus());
	        }
	    } else {
	        logger.warn("Scan with ID: {} not found", scanId);
	    }

	    return false;
	}


	public boolean deleteScan(Long scanId) {
	    Optional<GrootScanRecord> scan = grootScanRecordRepo.findById(scanId);
	    
	    if (scan.isPresent()) {
	        GrootScanRecord scanRecord = scan.get();
	        if (scanRecord.getStatus().equals(GrootScanStatus.SCAN_IN_PROGRESS)) {
	            try {
	                if (GrootScanType.CLAIRE.name().equals(scanRecord.getScanType())) {
	                    claireScanService.cancelScan(scanId);
	                } else {
	                    modelScanService.cancelScan(scanId);
	                }
	                logger.info("Cancellation initiated for scan with ID: {}", scanId);
	            } catch (Exception e) {
	                logger.error("Failed to cancel scan with ID: {}.", scanId, e);
	            }
		        grootScanUtils.updateScanStatus(scanRecord.getId(), GrootScanStatus.DELETED, Optional.of("Scan deleted by user"));
	        }
	        logger.info("Deleting scan with ID: {}", scanId);
	        return true;
	    } else {
	        logger.warn("Scan with ID: {} not found", scanId);
	    }

	    return false;
	}

	public GrootResponseDto fetchResult(Long scanId, GrootScanResultFilters grootScanResultFilters, Integer page, Integer pageLimit) {
		Optional<GrootScanRecord> scan = grootScanRecordRepo.findById(scanId);
		
		if(scan.isPresent() && (scan.get().getStatus().equals(GrootScanStatus.SUCCESSFUL) || scan.get().getStatus().equals(GrootScanStatus.PARTIALLY_SUCCESSFUL))) {
			GrootScanRecord scanRecord = scan.get();
			
			Specification<GrootScanResult> specs = Specification
					.where(ResultSpecifications.scanRecordIdEquals(scanId))
					.and(ResultSpecifications.conversationIdEquals(scanRecord.getConversationId()))
					.and(ResultSpecifications.promptCategoryLike(grootScanResultFilters.getPromptCategory()))
					.and(ResultSpecifications.severityEquals(grootScanResultFilters.getSeverity()))
					.and(ResultSpecifications.responseStatusEquals(grootScanResultFilters.getResponseStatus()));
			
			if (ObjectUtils.isEmpty(page) || page <= 0) {
				page = 0;
			} else {
				page = page - 1;
			}

			if (ObjectUtils.isEmpty(pageLimit) || pageLimit <= 0) {
				pageLimit = 0;
			}
			
			List<GrootScanResult> scanResult = grootScanResultRepo.findAll(specs, PageRequest.of(page, pageLimit, Sort.by(Direction.ASC, "id"))).getContent();
			
			if(!scanResult.isEmpty()) {
				GrootResponseDto grootResponseDto = new GrootResponseDto();
				grootResponseDto.setScanId(scanId);
		        grootResponseDto.setResults(scanResult);
		        

	            List<Object[]> severityCounts = grootScanResultRepo.fetchFlawCountByScanIds(Arrays.asList(scanId), ResponseStatus.PASSED);
	            
	            if (!severityCounts.isEmpty()) {
	                Object[] row = severityCounts.get(0); 
	                Long lowCount = (Long) row[1];
	                Long mediumCount = (Long) row[2];
	                Long highCount = (Long) row[3];
	                Long criticalCount = (Long) row[4];

	                Map<PromptSeverity, Long> severityCountMap = new HashMap<>();
	                severityCountMap.put(PromptSeverity.LOW, lowCount);
	                severityCountMap.put(PromptSeverity.MEDIUM, mediumCount);
	                severityCountMap.put(PromptSeverity.HIGH, highCount);
	                severityCountMap.put(PromptSeverity.CRITICAL, criticalCount);

	                grootResponseDto.setSeverityCount(severityCountMap);
	            }	        
		        return grootResponseDto;
			} else {
				logger.error("Results for scan: {} cannot be fetched", scanId);
			}
			
		} else {
			logger.warn("Results for Scan: {} are not available", scanId);
		}
		
		return null;
	}

	@Override
	public long fetchResultCount(Long scanId, GrootScanResultFilters grootScanResultFilters) {
			Optional<GrootScanRecord> scan = grootScanRecordRepo.findById(scanId);
			
			if(scan.isPresent() && (scan.get().getStatus().equals(GrootScanStatus.SUCCESSFUL) || scan.get().getStatus().equals(GrootScanStatus.PARTIALLY_SUCCESSFUL))) {
				GrootScanRecord scanRecord = scan.get();
				
				Specification<GrootScanResult> specs = Specification
						.where(ResultSpecifications.scanRecordIdEquals(scanId))
						.and(ResultSpecifications.conversationIdEquals(scanRecord.getConversationId()))
						.and(ResultSpecifications.promptCategoryLike(grootScanResultFilters.getPromptCategory()))
						.and(ResultSpecifications.severityEquals(grootScanResultFilters.getSeverity()))
						.and(ResultSpecifications.responseStatusEquals(grootScanResultFilters.getResponseStatus()));
				
				return grootScanResultRepo.count(specs);			
			} else {
				logger.warn("Results for Scan: {} are not available", scanId);
			}
			return 0;
	}

	@Override
	public Optional<GrootScanRecord> findScanById(Long scanId) {
		return grootScanRecordRepo.findById(scanId);
	}

	@Override
	public List<GrootResponseDto> fetchFlawCount(List<Long> scanIds) {
	    List<GrootResponseDto> result = new ArrayList<>();

	    List<List<Long>> partitionScanIds = Lists.partition(scanIds, 1000);

	    for (List<Long> partition : partitionScanIds) {
	        List<Object[]> severityCounts = grootScanResultRepo.fetchFlawCountByScanIds(partition, ResponseStatus.PASSED);
	        
	        for (Object[] row : severityCounts) {
	            Long scanId = (Long) row[0];
	            Long lowCount = (Long) row[1];
	            Long mediumCount = (Long) row[2];
	            Long highCount = (Long) row[3];
	            Long criticalCount = (Long) row[4];
	            
	            Map<PromptSeverity, Long> severityCountMap = new HashMap<>();
	            severityCountMap.put(PromptSeverity.LOW, lowCount);
	            severityCountMap.put(PromptSeverity.MEDIUM, mediumCount);
	            severityCountMap.put(PromptSeverity.HIGH, highCount);
	            severityCountMap.put(PromptSeverity.CRITICAL, criticalCount);

	            GrootResponseDto responseDto = new GrootResponseDto();
	            responseDto.setScanId(scanId);
	            responseDto.setSeverityCount(severityCountMap);
	            responseDto.setMessage("Severity counts for scan ID: " + scanId);

	            result.add(responseDto);
	        }
	    }
	    return result;
	}


}
