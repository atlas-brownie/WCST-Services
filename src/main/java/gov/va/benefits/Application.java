package gov.va.benefits;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * Implements the entry-point for Spring-boot micro-services application...
 * 
 * @author L Antony
 *
 */
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		if (StringUtils.isEmpty(System.getenv("spring.profiles.active"))) {
			System.setProperty("spring.profiles.active", "local");
		}

		ApplicationContext ctx = SpringApplication.run(Application.class, args);

		System.out.println("Let's inspect the beans provided by Spring Boot:");

		String[] beanNames = ctx.getBeanDefinitionNames();
		Arrays.sort(beanNames);
		for (String beanName : beanNames) {
			System.out.println(beanName);
		}
	}

}
