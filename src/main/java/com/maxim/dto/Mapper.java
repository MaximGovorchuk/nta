package com.maxim.dto;

import com.maxim.model.Statistic;
import com.maxim.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class Mapper {
	public StatisticResponseDto toStatisticResponseDto(final Statistic statistic) {
		return new StatisticResponseDto(statistic.getSum(), statistic.getAvg(), statistic.getMax(), statistic.getMin(), statistic.getCount());
	}

	public Transaction toTransaction(final TransactionRequestDto requestDto) {
		return new Transaction(requestDto.getTimestamp(), requestDto.getAmount());
	}
}
