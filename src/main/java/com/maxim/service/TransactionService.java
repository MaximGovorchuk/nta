package com.maxim.service;

import com.maxim.model.Transaction;
import com.maxim.model.Statistic;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.PriorityQueue;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class TransactionService {
	private final int EVERY_SECOND = 1000;
	private final Duration SIXTY_SECONDS = Duration.ofSeconds(60);
	private final TreeSet<Transaction> max = new TreeSet<>((o1, o2) -> Double.compare(o2.getAmount(), o1.getAmount()));
	private final TreeSet<Transaction> min = new TreeSet<>((o1, o2) -> Double.compare(o1.getAmount(), o2.getAmount()));
	private final PriorityQueue<Transaction> records = new PriorityQueue<>((o1, o2) -> o1.getTimestamp().compareTo(o2.getTimestamp()));

	private final ReentrantLock reentrantLock = new ReentrantLock();

	private long count;
	private double sum;
	private Statistic latestStatistic = new Statistic(0, 0, 0, 0, 0);

	@Scheduled(fixedRate = EVERY_SECOND)
	public void removeTransactionsOlderThan60Seconds() {
		try {
			reentrantLock.lock();

			Transaction oldestOne;
			while (!records.isEmpty() && isOlderThan60Seconds((oldestOne = records.peek()).getTimestamp())) {
				records.poll();
				count--;
				sum -= oldestOne.getAmount();
				max.remove(oldestOne);
				min.remove(oldestOne);
			}

			double maxAmount = max.isEmpty() ? 0 : max.first().getAmount();
			double minAmount = min.isEmpty() ? 0 : min.first().getAmount();

			latestStatistic = new Statistic(sum, maxAmount, minAmount, getAvg(), count);
		} finally {
			reentrantLock.unlock();
		}
	}

	public void recordTransaction(final Transaction transaction) {
		try {
			reentrantLock.lock();

			count++;
			sum += transaction.getAmount();
			max.add(transaction);
			min.add(transaction);

			records.add(transaction);

			latestStatistic = new Statistic(sum, max.first().getAmount(), min.first().getAmount(), getAvg(), count);
		} finally {
			reentrantLock.unlock();
		}
	}

	public Statistic getLatestStatistic() {
		return latestStatistic;
	}

	public boolean isOlderThan60Seconds(final Instant instant) {
		final Instant beforeSixtySeconds = Instant.now().minus(SIXTY_SECONDS);
		return instant.isBefore(beforeSixtySeconds);
	}

	private double getAvg() {
		return count == 0 ? 0 : sum / count;
	}
}