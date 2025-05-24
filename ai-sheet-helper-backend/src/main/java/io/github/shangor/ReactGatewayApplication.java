package io.github.shangor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackageClasses = {ReactGatewayApplication.class})

public class ReactGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReactGatewayApplication.class, args);
	}

}
