package com.infa.groot.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infa.groot.dto.GrootResponseDto;
import com.infa.groot.dto.GrootScanDto;
import com.infa.groot.dto.GrootScanResultFilters;
import com.infa.groot.repo.GrootScanRecord;
import com.infa.groot.service.GrootService;

/**
 * 
 * @author pujain
 *
 */
@RestController
@RequestMapping(value = "/api")
public class GrootRestController {
	
	private static Logger logger = LoggerFactory.getLogger(GrootRestController.class);
	
	@Autowired
	private GrootService grootService;
	
	@PostMapping(path = "/scan", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GrootResponseDto> kickStartScan(@RequestBody GrootScanDto grootScanDto) {
		if (ObjectUtils.isEmpty(grootScanDto)|| !grootScanDto.isValid()) {
			logger.warn("Invalid scan request data: {}", grootScanDto);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}

		try {
			GrootResponseDto grootResponseDto = grootService.saveNewScan(grootScanDto);
			if (ObjectUtils.isEmpty(grootResponseDto) || ObjectUtils.isEmpty(grootResponseDto.getScanId())) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
			}
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(grootResponseDto);
		} catch (Exception e) {
			logger.error("Exception occurred while trying to save details for Scan => {}", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@GetMapping(path = "/scan/{scanId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GrootResponseDto> getScanStatus(@PathVariable("scanId") Long scanId) {
		if (ObjectUtils.isEmpty(scanId) || scanId <= 0) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}

		try {
			GrootResponseDto grootResponseDto = grootService.getScanStatus(scanId);
			if (!ObjectUtils.isEmpty(grootResponseDto) && !ObjectUtils.isEmpty(grootResponseDto.getScanStatus())) {
				return ResponseEntity.status(HttpStatus.OK).body(grootResponseDto);
			} else {
				return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
			}
		} catch (Exception e) {
			logger.error("Exception occurred while trying to fetch status for scan {}.", scanId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@PutMapping(path = "/scan/{scanId}")
	public ResponseEntity<String> retryScan(@PathVariable("scanId") Long scanId) {
	    if (ObjectUtils.isEmpty(scanId) || scanId <= 0) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid scan ID");
	    }

	    try {
	        Optional<GrootScanRecord> scanRecord = grootService.findScanById(scanId);  
	        if (scanRecord.isPresent()) {
	        	boolean isRetrySuccessful = grootService.retryScan(scanId);
	        	if (isRetrySuccessful) {
					return ResponseEntity.status(HttpStatus.OK).body("Scan retried successfully");
				} else {
					return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Scan cannot be retried");
				}
	        } else {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Scan not found");
	        }
	    } catch (Exception e) {
	        logger.error("Exception occurred while trying to retry scan {}.", scanId, e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while retrying the scan");
	    }
	}

	
	@DeleteMapping(path = "/scan/{scanId}")
	public ResponseEntity<String> deleteScan(@PathVariable("scanId") Long scanId) {
	    if (ObjectUtils.isEmpty(scanId) || scanId <= 0) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid scan ID");
	    }

	    try {
	        Optional<GrootScanRecord> scanRecord = grootService.findScanById(scanId);
	        if (scanRecord.isPresent()) {
	        	boolean isDeleteSuccessful = grootService.deleteScan(scanId);
				if (isDeleteSuccessful) {
					return ResponseEntity.status(HttpStatus.OK).body("Scan deleted successfully");
				} else {
					return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Scan cannot be deleted");
				}
	        } else {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Scan not found");
	        }
	    } catch (Exception e) {
	        logger.error("Exception occurred while trying to delete scan {}.", scanId, e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deleting the scan");
	    }
	}

	
	@PostMapping(path = "/scan-results/{scanId}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GrootResponseDto> fetchResult(@PathVariable("scanId") Long scanId, @RequestBody GrootScanResultFilters grootScanResultFilters,
			@RequestParam("_page") Integer page, @RequestParam("_limit") Integer pageLimit) {
		if (ObjectUtils.isEmpty(scanId)|| ObjectUtils.isEmpty(page) || ObjectUtils.isEmpty(pageLimit)|| scanId <= 0 || page < 0 || pageLimit <= 0) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}

		try {
			GrootResponseDto grootResponseDto = grootService.fetchResult(scanId, grootScanResultFilters, page, pageLimit);
			if (ObjectUtils.isEmpty(grootResponseDto) || ObjectUtils.isEmpty(grootResponseDto.getResults())) {
				logger.warn("No results found for scanId: {}", scanId);
				return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
			} else {
				return ResponseEntity.status(HttpStatus.OK).body(grootResponseDto);
			}
		} catch (Exception e) {
			logger.error("An unexpected error occurred while fetching results for scan ID: {}.", scanId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@PostMapping(path = "/scan-results/count/{scanId}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Long> fetchResultCount(@PathVariable("scanId") Long scanId, @RequestBody GrootScanResultFilters grootScanResultFilters) {
		if (ObjectUtils.isEmpty(scanId) || scanId <= 0) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}

		try {
			Long resultCount = grootService.fetchResultCount(scanId, grootScanResultFilters);
			if (resultCount == 0) {
				logger.warn("No results found for scanId: {}", scanId);
				return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
			}else {
				return ResponseEntity.status(HttpStatus.OK).body(resultCount);
			}
		} catch (Exception e) {
			logger.error("An unexpected error occurred while fetching result count for scan ID: {}.", scanId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@PostMapping(path = "/scan/flaw-count", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<GrootResponseDto>> getFlawCountForScans(@RequestBody List<Long> scanIds) {
	    if (ObjectUtils.isEmpty(scanIds)) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	    }

	    try {
	        List<GrootResponseDto> flawCountMap = grootService.fetchFlawCount(scanIds);
	        
	        if(ObjectUtils.isEmpty(flawCountMap)) {
	        	logger.warn("Flaw Count could not be fetched for given scan IDs");
				return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	        } else {
		        return ResponseEntity.status(HttpStatus.OK).body(flawCountMap);
	        }
	    } catch (Exception e) {
	        logger.error("An unexpected error occurred while fetching flaw counts for scan IDs: {}.", scanIds, e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	    }
	}
	
	
}
