package com.infa.groot.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.infa.groot.repo.GrootScanRecord;
import com.infa.groot.repo.GrootScanRecordRepo;
import com.infa.groot.repo.GrootScanStatus;

/**
 * 
 * @author pujain
 *
 */
@Service
public class GrootScanUtils {

    private static final Logger logger = LoggerFactory.getLogger(GrootScanUtils.class);

    @Inject
    private GrootScanRecordRepo grootScanRecordRepo;

    /**
     * Reads prompts from a file.
     * 
     * @param promptFilePath The path to the prompt file.
     * @return List of prompts.
     */
    public List<String> readPrompts(String promptFilePath) {
        try {
            return Files.readAllLines(Paths.get(promptFilePath));
        } catch (Exception e) {
            logger.error("Error reading prompt file: {}", e);
            return null;
        }
    }

    /**
     * Updates the scan status to failure.
     * 
     * @param scanId The scan ID.
     * @param reason The reason for failure.
     * @param e      The exception that caused the failure.
     */
    public void updateScanStatus(Long scanId, GrootScanStatus status, Optional<String> comment) {
        try {
            GrootScanRecord scan = grootScanRecordRepo.findById(scanId).orElse(null);
            if (scan != null) {
                scan.setLastStatus(scan.getStatus());
                if (status.equals(GrootScanStatus.FAILED) || status.equals(GrootScanStatus.PARTIALLY_SUCCESSFUL)
                        || status.equals(GrootScanStatus.SUCCESSFUL) || status.equals(GrootScanStatus.DELETED)) {
                    scan.setCompletedDate(new Date());
                } else if (status.equals(GrootScanStatus.SCAN_IN_PROGRESS)) {
                    scan.setStartedDate(new Date());
                }
                scan.setStatus(status);
                comment.ifPresent(scan::setComment);
                grootScanRecordRepo.save(scan);
            }
        } catch (Exception e) {
            logger.error("Error updating scan status : {}", e);
        }
    }

}
