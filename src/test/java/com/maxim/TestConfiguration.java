package com.maxim;

import com.maxim.controller.TransactionController;
import com.maxim.dto.Mapper;
import com.maxim.service.TransactionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableScheduling
@EnableWebMvc
public class TestConfiguration {
	@Bean
	public Mapper statisticMapper() {
		return new Mapper();
	}

	@Bean
	public TransactionService transactionService() {
		return new TransactionService();
	}

	@Bean
	public TransactionController transactionController() {
		return new TransactionController(transactionService(), statisticMapper());
	}
}
