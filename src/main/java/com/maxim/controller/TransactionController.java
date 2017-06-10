package com.maxim.controller;

import com.maxim.dto.Mapper;
import com.maxim.dto.StatisticResponseDto;
import com.maxim.dto.TransactionRequestDto;
import com.maxim.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {
	private final TransactionService transactionService;
	private final Mapper mapper;

	@Autowired
	public TransactionController(TransactionService transactionService, Mapper mapper) {
		this.transactionService = transactionService;
		this.mapper = mapper;
	}

	@RequestMapping(value = "/transactions", method = RequestMethod.POST)
	public ResponseEntity<Void> recordTransaction(@RequestBody final TransactionRequestDto transactionRequestDto) {
		if (transactionService.isOlderThan60Seconds(transactionRequestDto.getTimestamp())) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} else {
			transactionService.recordTransaction(mapper.toTransaction(transactionRequestDto));
			return new ResponseEntity<>(HttpStatus.CREATED);
		}
	}

	@RequestMapping(value = "/statistics", method = RequestMethod.GET)
	public StatisticResponseDto getStatistic() {
		return mapper.toStatisticResponseDto(transactionService.getLatestStatistic());
	}
}