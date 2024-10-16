package com.infa.groot.service.claire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.infa.groot.utils.GrootUtils;

/**
 * 
 * @author pujain
 *
 */
@Service
@Scope("prototype")
public class ClaireSessionManager {
	private static final Logger logger = LoggerFactory.getLogger(ClaireSessionManager.class);
	
	
	@Autowired
	private GrootUtils grootUtils;
	
	@Value("${session.expiration.time}")
    private int sessionExpirationTime;
	
	 private String sessionId;
     private String jsonToken;
     private long sessionStartTime;

     public void initializeSession() {
    	 try {
             regenerateSession();
         } catch (Exception e) {
             logger.error("Failed to initialize session: {}", e);
             throw new RuntimeException("Could not initialize Claire session.", e);
         }
     }

     public boolean ensureValidSession() {
         if (StringUtils.isEmpty(sessionId) || StringUtils.isEmpty(jsonToken) || isSessionExpired()) {
        	 try {
                 regenerateSession();
                 return true;
             } catch (Exception e) {
            	 logger.error("Session regeneration Failed.", e);
            	 return false;
             
             }
         }
		return true;
     }

     private boolean isSessionExpired() {
         long elapsedTime = (System.currentTimeMillis() - sessionStartTime) / 1000;
         return elapsedTime >= sessionExpirationTime;
     }

     private void regenerateSession() {
         try {
             sessionStartTime = System.currentTimeMillis();
             String[] credentials = grootUtils.generateCredentials();
             sessionId = credentials[0];
             jsonToken = credentials[1];
             logger.info("Session credentials regenerated.");
         } catch (Exception e) {
             logger.error("Error occurred while regenerating credentials. Error: {}", e);
             throw new RuntimeException("Failed to regenerate credentials", e);
         }
     }

     public String getSessionId() {
         return sessionId;
     }

     public String getJsonToken() {
         return jsonToken;
     }

}
