package de.oumaima.servicedesk;

import org.springframework.boot.SpringApplication;

public class TestServiceDeskApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(ServiceDeskApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
