package com.infa.groot;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

/**
 * 
 * Profile based config setter file for config-${profile} properties file.
 * 
 * @author pujain
 *
 */
@Configuration
public class ServicePropertyConfig {

	public ServicePropertyConfig() {

	}

	@Configuration
	@PropertySource({ "classpath:config-default.properties" })
	@Profile(FrameworkConstants.DEFAULT_PROFILE)
	static class DefaultConfig {
	}

	@Configuration
	@PropertySource({ "classpath:config-dev.properties" })
	@Profile(FrameworkConstants.DEV_PROFILE)
	static class DevConfig {
	}
	
	@Configuration
	@PropertySource({ "classpath:config-prod.properties" })
	@Profile(FrameworkConstants.PROD_PROFILE)
	static class ProdConfig {
	}

}
