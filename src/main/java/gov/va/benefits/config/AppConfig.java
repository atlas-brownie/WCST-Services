package gov.va.benefits.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

/**
 * Component responsible for initializing spring profile based configuration
 * beans...
 * 
 * @author L Antony
 *
 */
@Configuration
public class AppConfig {
	@Bean
	@Profile("local")
	public PropertySourcesPlaceholderConfigurer fetchLocalProperties() {
		PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

		ClassPathResource resource = new ClassPathResource("application-local.yaml");

		configurer.setLocation(resource);
		return configurer;
	}

	@Bean
	@Profile("dev")
	public PropertySourcesPlaceholderConfigurer fetchDevProperties() {
		PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

		ClassPathResource resource = new ClassPathResource("application-dev.yaml");

		configurer.setLocation(resource);
		return configurer;
	}

	@Bean
	@Profile({"qa", "stage"})
	public PropertySourcesPlaceholderConfigurer fetchQAProperties() {
		PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

		ClassPathResource resource = new ClassPathResource("application-qa.yaml");

		configurer.setLocation(resource);
		return configurer;
	}

	@Bean
	@Profile("prod")
	public PropertySourcesPlaceholderConfigurer fetchProductionProperties() {
		PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

		ClassPathResource resource = new ClassPathResource("application-prod.yaml");

		configurer.setLocation(resource);
		return configurer;
	}

}