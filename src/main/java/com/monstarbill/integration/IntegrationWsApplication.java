package com.monstarbill.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import feign.Logger;

@SpringBootApplication
@EnableJpaAuditing
@EnableEurekaClient
@EnableAutoConfiguration
@EnableFeignClients
public class IntegrationWsApplication {

	public static void main(String[] args) {
		SpringApplication.run(IntegrationWsApplication.class, args);
	}

	@Bean
	Logger.Level fiegnLoggerLevel() {
		return Logger.Level.FULL;
	}

}
