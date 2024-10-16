package com.infa.groot.service.model;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 
 * @author pujain
 *
 */
@Service
public class ModelSessionManager {
	
	private Logger logger = LoggerFactory.getLogger(ModelSessionManager.class);

	/**
     * Method to generate a random conversation ID for a MultiStepReasoner scan.
     * The ID will be limited to a maximum of 50 characters.
     *
     * @param scanId the ID of the scan
     * @return a randomly generated conversation ID of max 50 characters
     */
    public String generateConversationId(Long scanId) {
        String conversationId = UUID.randomUUID().toString();
        if (conversationId.length() > 50) {
            conversationId = conversationId.substring(0, 50);
        }
        logger.info("Generated conversation ID: {} for scan ID: {}", conversationId, scanId);
        return conversationId;
    }

}
