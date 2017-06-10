package com.maxim.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;

public class TransactionRequestDto {
	@JsonDeserialize(using = UnixTimestampDeserializer.class)
	private Instant timestamp;

	private double amount;

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}
}
