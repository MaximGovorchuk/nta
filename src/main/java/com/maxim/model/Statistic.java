package com.maxim.model;

public class Statistic {
	private final double sum;
	private final double max;
	private final double min;
	private final double avg;
	private final long count;

	public Statistic(double sum, double max, double min, double avg, long count) {
		this.sum = sum;
		this.max = max;
		this.min = min;
		this.avg = avg;
		this.count = count;
	}

	public double getSum() {
		return sum;
	}

	public double getAvg() {
		return avg;
	}

	public double getMax() {
		return max;
	}

	public double getMin() {
		return min;
	}

	public long getCount() {
		return count;
	}
}
