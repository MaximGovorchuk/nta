package com.maxim.model;

import java.time.Instant;

public class Transaction {
	private final Instant timestamp;
	private final double amount;

	public Transaction(final Instant timestamp, final double amount) {
		this.timestamp = timestamp;
		this.amount = amount;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public double getAmount() {
		return amount;
	}
}
