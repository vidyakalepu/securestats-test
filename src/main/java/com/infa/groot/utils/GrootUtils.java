package com.infa.groot.utils;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Utility class for Groot operations.
 * 
 * @author pujain
 */
@Component
public class GrootUtils {

    @Value("${mrel.login.url}")
    private String mrelLoginUrl;

    @Value("${mrel.jwt.url}")
    private String mrelJwtUrl;

    @Value("${mrel.uname}")
    private String mrelUsername;

    @Value("${mrel.pwd}")
    private String mrelPassword;

    @Value("${claire.conversation.url}")
    private String claireConversationUrl;

    private static final Logger logger = LoggerFactory.getLogger(GrootUtils.class);
    
    private final RestTemplate restTemplate;
    public static String password = "tempPassword";
    public static String anotherPassword = "anotherPassword";


    
    @Autowired
	public GrootUtils(RestTemplate restTemplate) {
		super();

		// Setup boiler plate
		setupSSLBoilerPlate();

		this.restTemplate = new RestTemplate();
	}
    
    private void setupSSLBoilerPlate() {
		TrustManager[] trustMgr = new TrustManager[] { new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		} };

		SSLContext sslCtx = null;
		try {
			sslCtx = SSLContext.getInstance("TLS");
			sslCtx.init(null, trustMgr, new SecureRandom());
		} catch (Exception e) {
			logger.error("Error occurred while handling SSL setup boiler-plate.", e);
		}

		HttpsURLConnection.setDefaultSSLSocketFactory(sslCtx.getSocketFactory());
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String string, SSLSession sslSession) {
				return true;
			}
		});

	}

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Retrieves a session ID using the provided username and password.
     * 
     * @param uname The username.
     * @param pwd The password.
     * @return Session ID if successful, null otherwise.
     * @throws Exception if network or parsing fails.
     */
    public String getSessionId(String uname, String pwd) throws Exception {
        logger.info("Generating Session ID...");
        try {
            String payload = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", uname, pwd);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.exchange(mrelLoginUrl, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return objectMapper.readTree(response.getBody()).path(ServiceConstants.SESSION_ID).asText();
            } else {
                logger.error("Failed to retrieve session ID. Response Code: {} Body: {}", response.getStatusCode(), response.getBody());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error generating Session ID: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Generates a JSON Web Token (JWT) using the provided session ID.
     * 
     * @param sessionId The session ID.
     * @return Optional of JWT if successful, empty otherwise.
     * @throws Exception on network or parsing failure.
     */
    public Optional<String> generateJsonWebToken(String sessionId) throws Exception {
        logger.info("Generating JSON Web Token...");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("IDS-SESSION-ID", sessionId);
            headers.set("Content-Type", "text/plain");
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(mrelJwtUrl, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return Optional.of(objectMapper.readTree(response.getBody()).path(ServiceConstants.JSON_WEB_TOKEN).asText());
            } else {
                logger.error("Failed to generate JWT. Response Code: {} Body: {}", response.getStatusCode(), response.getBody());
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Error generating JWT: {}", e);
            throw e;
        }
    }

    /**
     * Generates credentials (Session ID and JWT) for the current user.
     * 
     * @return String array containing session ID and JWT.
     * @throws Exception if credentials cannot be generated.
     */
    public String[] generateCredentials() throws Exception {
        logger.info("Generating Credentials...");
        try {
            String sessionId = getSessionId(mrelUsername, mrelPassword);
            logger.info("Session ID Generated: {}", sessionId);

            String jsonToken = generateJsonWebToken(sessionId)
                    .orElseThrow(() -> new Exception("Failed to obtain JWT"));

            logger.info("JWT Generated: {}", jsonToken);

            return new String[]{sessionId, jsonToken};
        } catch (Exception e) {
            logger.error("Error generating credentials: {}", e);
            throw e;
        }
    }

    /**
     * Creates a new conversation using the scan ID.
     * 
     * @param scanId The scan ID.
     * @param jwt The JSON Web Token.
     * @return The conversation ID.
     * @throws Exception if the conversation cannot be created.
     */
    public String createNewConversation(Long scanId, String jwt) throws Exception {
        logger.info("Creating new conversation for scan ID: {}", scanId);
        try {
            String payload = String.format("{\"name\":\"scan: %d\", \"channel\":\"cgpt\"}", scanId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwt);
            headers.set("Content-Type", "application/json");
            HttpEntity<String> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.exchange(claireConversationUrl, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                return objectMapper.readTree(response.getBody()).path(ServiceConstants.CONVERSATION_ID).asText();
            } else {
                logger.error("Failed to create conversation. Response Code: {} Body: {}", response.getStatusCode(), response.getBody());
                throw new Exception("Unexpected response code: " + response.getStatusCode() + ", " + response.getBody());
            }
        } catch (Exception e) {
            logger.error("Error creating new conversation: {}", e);
            throw e;
        }
    }
    
    
}
