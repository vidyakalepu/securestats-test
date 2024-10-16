package com.infa.groot.schedulers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.infa.groot.repo.GrootScanRecord;
import com.infa.groot.repo.GrootScanRecordRepo;
import com.infa.groot.repo.GrootScanResultRepo;
import com.infa.groot.repo.GrootScanStatus;
import com.infa.groot.service.claire.ClaireScanService;
import com.infa.groot.service.claire.ClaireSessionManager;
import com.infa.groot.service.model.ModelScanService;
import com.infa.groot.service.model.ModelSessionManager;
import com.infa.groot.utils.GrootScanType;
import com.infa.groot.utils.GrootScanUtils;
import com.infa.groot.utils.GrootUtils;

/**
 * 
 * @author pujain
 *
 */
@Service
public class GrootScanScheduler {
    private Logger logger = LoggerFactory.getLogger(GrootScanScheduler.class);
    
    private final ConcurrentMap<Long, Boolean> activeScans = new ConcurrentHashMap<>();

    @Autowired
    private GrootScanRecordRepo grootScanRecordRepo;

    @Autowired
    private ClaireScanService claireScanService;

    @Autowired
    private ModelScanService modelScanService;

    @Autowired
    private ClaireSessionManager claireSessionManager;

    @Autowired
    private ModelSessionManager modelSessionManager;
    
    @Autowired
    private GrootScanResultRepo grootScanResultRepo;
    
    @Autowired
    private GrootScanUtils grootScanUtils;
    
    @Autowired
    private GrootUtils grootUtils;
    
    @Value("${scheduler.scan.enabled}")
    private boolean isScanSchedulerEnabled;

    @Value("${scheduler.purge.enabled}")
    private boolean isPurgeSchedulerEnabled;


    @Scheduled(cron = "${scan.scheduler}")
	public void processPendingScans() {
	
	    if (!isScanSchedulerEnabled) {
	        logger.info("Scan Scheduler is disabled.");
	        return;
	    }
	
	    logger.info("Scheduled Scan process begins at: {}", LocalDateTime.now());
	    List<GrootScanRecord> pendingScans = grootScanRecordRepo.findByStatus(GrootScanStatus.SUBMITTED);
	
	    for (GrootScanRecord scan : pendingScans) {

	        if (activeScans.putIfAbsent(scan.getId(), true) != null) {
	            continue;
	        }
	
	        logger.info("Processing scan with ID: {}", scan.getId());
	        try {
	            GrootScanType scanType = GrootScanType.valueOf(scan.getScanType().toUpperCase());
	            grootScanUtils.updateScanStatus(scan.getId(), GrootScanStatus.SCAN_IN_PROGRESS, Optional.empty());
	
	            if (scanType == GrootScanType.CLAIRE) {
	                if (!claireSessionManager.ensureValidSession()) {
	                    grootScanUtils.updateScanStatus(scan.getId(), GrootScanStatus.FAILED, Optional.of("Failed to validate session."));
	                    logger.info("Scan with ID: {} failed due to invalid session.", scan.getId());
	                    continue;
	                }
	                scan.setConversationId(grootUtils.createNewConversation(scan.getId(), claireSessionManager.getJsonToken()));
	                grootScanRecordRepo.save(scan);
	
	                claireScanService.runClaireScan(scan.getId());
	
	            } else if (scanType == GrootScanType.PRODUCT_HELP || 
	                       scanType == GrootScanType.MULTISTEP_REASONER || 
	                       scanType == GrootScanType.NL2SQL || 
	                       scanType == GrootScanType.SUMMARIZER) {
	                scan.setConversationId(modelSessionManager.generateConversationId(scan.getId()));
	                grootScanRecordRepo.save(scan);
	
	                modelScanService.runModelScan(scan.getId());
	
	            } else {
	                grootScanUtils.updateScanStatus(scan.getId(), GrootScanStatus.FAILED, Optional.of("Unknown scan type."));
	                logger.warn("Scan with ID: {} failed due to unknown scan type: {}", scan.getId(), scanType);
	                continue;
	            }
	
	        } catch (IllegalArgumentException e) {
	            logger.error("Unknown scan type: {}", scan.getScanType(), e);
	            grootScanUtils.updateScanStatus(scan.getId(), GrootScanStatus.FAILED, Optional.of("Scan failed due to unknown scan type"));
	        } catch (Exception e) {
	            logger.error("Exception occurred while trying to trigger Scan. Exception: {}", e);
	            grootScanUtils.updateScanStatus(scan.getId(), GrootScanStatus.FAILED, Optional.of(String.format("Scan: %d failed due to exception. %s", scan.getId(), e.getMessage())));
	        } finally {
	            activeScans.remove(scan.getId());
	            logger.info("Completed scan with ID: {}", scan.getId());
	        }
	    }
	}

    @Scheduled(cron = "${purge.obsolete.result.scheduler}")
    public void purgeOutdatedScanResults() {
        if (!isPurgeSchedulerEnabled) {
            logger.info("Purge Scheduler is disabled.");
            return;
        }

        logger.info("Scheduled Purge process begins at: {}", LocalDateTime.now());

        try {
            LocalDateTime dateLimitLocal = LocalDateTime.now().minusMonths(1);
            Date dateLimit = Date.from(dateLimitLocal.atZone(ZoneId.systemDefault()).toInstant());

            logger.info("Purging scan results older than: {}", dateLimit);

            int deletedRecordsCount = grootScanResultRepo.purgeOutdatedResults(dateLimit);
            logger.info("Purge process completed successfully. Number of records deleted: {}", deletedRecordsCount);

        } catch (Exception e) {
            logger.error("An error occurred while purging outdated scan results.", e);
        }
    }
}
