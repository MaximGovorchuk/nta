package com.maxim.service;

import com.maxim.model.Statistic;
import com.maxim.model.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TransactionService.class)
public class TransactionServiceTest {
	private final TransactionService transactionService = new TransactionService();
	private Instant frozenTime;

	@Before
	public void freezeCurrentTime() {
		frozenTime = Instant.now();

		PowerMockito.mockStatic(Instant.class);
		PowerMockito.when(Instant.now()).thenReturn(frozenTime);
	}

	@Test
	public void shouldConsiderInstancesAsOlder() {
		assertTrue(transactionService.isOlderThan60Seconds(frozenTime.minusSeconds(60).minusMillis(1)));
		assertTrue(transactionService.isOlderThan60Seconds(frozenTime.minusSeconds(61)));
		assertTrue(transactionService.isOlderThan60Seconds(frozenTime.minusSeconds(999)));
	}

	@Test
	public void shouldConsiderInstancesAsRecent() {
		assertFalse(transactionService.isOlderThan60Seconds(frozenTime));
		assertFalse(transactionService.isOlderThan60Seconds(frozenTime.minusSeconds(60).plusMillis(1)));
	}

	@Test
	public void shouldReturnInitialTransaction() {
		final double initialAmount = 123.0;
		final Instant initialTimestamp = frozenTime;

		transactionService.recordTransaction(new Transaction(initialTimestamp, initialAmount));

		assertTransactionStatistic(1, initialAmount, initialAmount, initialAmount);
	}

	@Test
	public void shouldAccumulateStatistic() {
		final int quantityOfTransactions = 6;
		for (int i = 1; i <= quantityOfTransactions; i++) {
			transactionService.recordTransaction(new Transaction(frozenTime.plusSeconds(i), i));
		}

		final double expectedSum = 1 + 2 + 3 + 4 + 5 + 6;
		assertTransactionStatistic(quantityOfTransactions, (double) quantityOfTransactions, 1.0, expectedSum);
	}

	@Test
	public void shouldEvictHalfOfStatisticAfter30Seconds() {
		final int quantityOfTransactions = 6;
		for (int i = 1; i <= quantityOfTransactions; i++) {
			transactionService.recordTransaction(new Transaction(frozenTime.plusSeconds(i), i));
		}

		forwardCurrentTime(Duration.ofSeconds(60 + 4));

		final int expectedCount = 3;
		final double expectedSum = 4 + 5 + 6;
		assertTransactionStatistic(expectedCount, (double) quantityOfTransactions, 4.0, expectedSum);
	}

	@Test
	public void shouldEvictAllStatisticAfter60Seconds() {
		final int quantityOfTransactions = 6;
		for (int i = 1; i <= quantityOfTransactions; i++) {
			transactionService.recordTransaction(new Transaction(frozenTime.plusSeconds(i), i));
		}

		// Evict all transactions
		forwardCurrentTime(Duration.ofSeconds(60 + quantityOfTransactions + 1));

		final double expectedZeroValue = 0.0;
		assertTransactionStatistic((long) expectedZeroValue, expectedZeroValue, expectedZeroValue, expectedZeroValue);
	}

	@Test
	public void shouldCombineStatisticAfterEvictionWithNewOne() {
		final int quantityOfTransactions = 6;
		for (int i = 1; i <= quantityOfTransactions; i++) {
			transactionService.recordTransaction(new Transaction(frozenTime.plusSeconds(i), i));
		}

		// Evict half of transactions
		forwardCurrentTime(Duration.ofSeconds(60 + 4));

		final int newTransactionAmount = 7;
		transactionService.recordTransaction(new Transaction(frozenTime, newTransactionAmount));

		final double expectedSum = 4 + 5 + 6 + newTransactionAmount;
		final int expectedCount = 4;
		assertTransactionStatistic(expectedCount, (double) newTransactionAmount, 4.0, expectedSum);
	}

	private void forwardCurrentTime(Duration delta) {
		frozenTime = frozenTime.plus(delta);
		PowerMockito.when(Instant.now()).thenReturn(frozenTime);
		transactionService.removeTransactionsOlderThan60Seconds(); // Manual invoke here, full e2e scenario is converted in AppTest
	}

	// One might argue it's a bad idea to abstract such thing however it does reduces lines of code
	private void assertTransactionStatistic(final long expectedCount, final double expectedMax, final double expectedMin, final double expectedSum) {
		final Statistic statistic = transactionService.getLatestStatistic();

		assertEquals(expectedCount, statistic.getCount());
		assertEquals(expectedMax, statistic.getMax(), 0.0);
		assertEquals(expectedMin, statistic.getMin(), 0.0);
		assertEquals(expectedSum, statistic.getSum(), 0.0);
		final double expectedAvg = expectedCount == 0 ? 0 : expectedSum / expectedCount;
		assertEquals(expectedAvg, statistic.getAvg(), 0.0);
	}
}