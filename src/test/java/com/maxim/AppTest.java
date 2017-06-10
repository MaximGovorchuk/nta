package com.maxim;

import com.maxim.controller.TransactionController;
import com.maxim.dto.StatisticResponseDto;
import com.maxim.dto.TransactionRequestDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WebAppConfiguration
public class AppTest {
	@Autowired
	private TransactionController transactionController;

	@Test
	public void shouldWorkEndToEnd() throws InterruptedException {
		final double expectedZeroValue = 0.0;
		assertTransactionStatistic(0, expectedZeroValue, expectedZeroValue, expectedZeroValue);

		final double firstTransactionAmount = 1.0;
		final Instant firstTransactionTimestamp = Instant.now().minus(Duration.ofSeconds(58));
		sendTransactionRequest(firstTransactionAmount, firstTransactionTimestamp);

		assertTransactionStatistic(1, firstTransactionAmount, firstTransactionAmount, firstTransactionAmount);

		final double secondTransactionAmount = 2.0;
		sendTransactionRequest(secondTransactionAmount, firstTransactionTimestamp.plusSeconds(2));

		final double expectedSumOfTwoTransactionAmounts = firstTransactionAmount + secondTransactionAmount;
		assertTransactionStatistic(2, secondTransactionAmount, firstTransactionAmount, expectedSumOfTwoTransactionAmounts);

		TimeUnit.SECONDS.sleep(3);

		assertTransactionStatistic(1, secondTransactionAmount, secondTransactionAmount, secondTransactionAmount);

		TimeUnit.SECONDS.sleep(3);

		assertTransactionStatistic(0, expectedZeroValue, expectedZeroValue, expectedZeroValue);
	}

	private ResponseEntity<Void> sendTransactionRequest(final double amount, final Instant timestamp) {
		final TransactionRequestDto transactionRequestDto = new TransactionRequestDto();
		transactionRequestDto.setAmount(amount);
		transactionRequestDto.setTimestamp(timestamp);

		return transactionController.recordTransaction(transactionRequestDto);
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
