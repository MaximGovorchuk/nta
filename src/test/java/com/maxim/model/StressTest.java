package com.maxim.model;

import com.maxim.TestConfiguration;
import com.maxim.controller.TransactionController;
import com.maxim.dto.StatisticResponseDto;
import com.maxim.dto.TransactionRequestDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WebAppConfiguration
public class StressTest {
	@Autowired
	private TransactionController transactionController;

	@Test(timeout = 25000L)
	public void shouldBeThreadSafe() throws InterruptedException {
		final int firstBatchInitialTransactionAmountValue = 1;
		final int numberOfTransactions = 10000;
		final int numberOfThreads = 20;
		final ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

		final AtomicInteger nextTransactionAmount = new AtomicInteger(firstBatchInitialTransactionAmountValue);

		final Instant initialSubmitTimestamp = Instant.now().minusSeconds(50);

		// Submit first batch of transactions
		final List<Future<?>> firstBatchTasks = sendBatchOfTransactions(numberOfTransactions, initialSubmitTimestamp, nextTransactionAmount, executorService);

		// Wait till eventual consistency
		final double expectedSumAfterFirstBatch = getSum(firstBatchInitialTransactionAmountValue, numberOfTransactions);
		final double expectedAvgAfterFirstBatch = expectedSumAfterFirstBatch / numberOfTransactions;

		do {
			filterOutCompletedTasks(firstBatchTasks);

			final StatisticResponseDto statistic = transactionController.getStatistic();
			assertThat(statistic.getCount(), lessThanOrEqualTo((long) numberOfTransactions));
			assertThat(statistic.getMin(), equalTo((double) firstBatchInitialTransactionAmountValue));
			assertThat(statistic.getMax(), lessThanOrEqualTo((double) numberOfTransactions));
			assertThat(statistic.getSum(), lessThanOrEqualTo(expectedSumAfterFirstBatch));
			assertThat(statistic.getAvg(), lessThanOrEqualTo(expectedAvgAfterFirstBatch));

		} while (!firstBatchTasks.isEmpty());

		assertTransactionStatistic((long) numberOfTransactions, (double) numberOfTransactions, (double) firstBatchInitialTransactionAmountValue, expectedSumAfterFirstBatch);

		// After waiting eviction of first batch is about to start
		TimeUnit.SECONDS.sleep(10);

		// Submit second batch of transactions
		final int secondBatchInitialTransactionAmountValue = nextTransactionAmount.get();
		final List<Future<?>> secondBatchTasks = sendBatchOfTransactions(numberOfTransactions, Instant.now(), nextTransactionAmount, executorService);
		final double expectedSumAfterSecondBatch = getSum(numberOfTransactions + 1, numberOfTransactions * 2);

		do {
			filterOutCompletedTasks(secondBatchTasks);

			final StatisticResponseDto statistic = transactionController.getStatistic();
			assertThat(statistic.getCount(), lessThanOrEqualTo((long) numberOfTransactions * 2));

			assertThat(statistic.getMin(), lessThanOrEqualTo((double) secondBatchInitialTransactionAmountValue));
			assertThat(statistic.getMin(), greaterThanOrEqualTo((double) firstBatchInitialTransactionAmountValue));

			assertThat(statistic.getMax(), lessThanOrEqualTo((double) nextTransactionAmount.get() - 1));
			assertThat(statistic.getMax(), greaterThanOrEqualTo((double) numberOfTransactions));

			assertThat(statistic.getSum(), lessThanOrEqualTo(expectedSumAfterFirstBatch + expectedSumAfterSecondBatch));

		} while (!secondBatchTasks.isEmpty());

		while (transactionController.getStatistic().getCount() != numberOfTransactions) {
			TimeUnit.SECONDS.sleep(1);
		}

		assertTransactionStatistic((long) numberOfTransactions, (double) nextTransactionAmount.get() - 1, (double) secondBatchInitialTransactionAmountValue, expectedSumAfterSecondBatch);
	}

	private List<Future<?>> sendBatchOfTransactions(int numberOfTransactions, Instant initialSubmitTimestamp, AtomicInteger nextTransactionAmount, ExecutorService executorService) {
		final List<Future<?>> tasks = new ArrayList<>(numberOfTransactions);
		for (long i = 0; i < numberOfTransactions; i++) {
			final Instant transactionTimestamp = initialSubmitTimestamp.plusNanos(i * 300);
			tasks.add(executorService.submit(() -> {
				final TransactionRequestDto transactionRequestDto = new TransactionRequestDto();
				transactionRequestDto.setTimestamp(transactionTimestamp);
				transactionRequestDto.setAmount(nextTransactionAmount.getAndIncrement());
				transactionController.recordTransaction(transactionRequestDto);
			}));
		}
		return tasks;
	}

	private long getSum(long from, long to) {
		return LongStream.range(from, to + 1).sum();
	}

	private void filterOutCompletedTasks(List<Future<?>> tasks) {
		final List<Future<?>> runningTasks = tasks.stream()
				.filter(task -> !task.isDone())
				.collect(Collectors.toList());
		tasks.retainAll(runningTasks);
	}

	// One might argue it's a bad idea to abstract such thing however it does reduces lines of code
	private void assertTransactionStatistic(final long expectedCount, final double expectedMax, final double expectedMin, final double expectedSum) {
		final StatisticResponseDto transactionStatistic = transactionController.getStatistic();

		assertEquals(expectedCount, transactionStatistic.getCount());
		assertEquals(expectedMax, transactionStatistic.getMax(), 0.0);
		assertEquals(expectedMin, transactionStatistic.getMin(), 0.0);
		assertEquals(expectedSum, transactionStatistic.getSum(), 0.0);
		final double expectedAvg = expectedCount == 0 ? 0 : expectedSum / expectedCount;
		assertEquals(expectedAvg, transactionStatistic.getAvg(), 0.0);
	}
}
