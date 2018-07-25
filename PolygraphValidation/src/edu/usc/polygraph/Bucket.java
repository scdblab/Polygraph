package edu.usc.polygraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

public class Bucket { // multiple ppl access so atomic counter
	public static final int NANO_TO_MILLIS = 1000000;
	long _id;
	public static long freshnessTime = 1300;// Milliseconds
	public static long bucketDuration = 1000;// 18000; ////Milliseconds
	public static long maxBuckets = Long.MAX_VALUE;

	long _startTime;
	double _endTime;
	double _duration;
	AtomicLong numValidReads = null; // read the freshest values
	AtomicLong numStaleReads = null;
	AtomicLong numTotalReads = null; // for this bucket
	double _freshnessProb = 0;

	public double getDuration() {
		return _duration;
	}

	public long getStartTime() {
		return _startTime;
	}

	public double getEndTime() {
		return _endTime;
	}

	public long getNumValidReads() {
		return numValidReads.get();
	}

	public long getNumStaleReads() {
		return numStaleReads.get();
	}

	public long getNumTotalReads() {
		return numTotalReads.get();
	}

	public double getFreshnessProb() {
		if (numTotalReads.get() == 0)
			return 1.0;
		else
			return ((double) getNumValidReads()) / getNumTotalReads();
	}

	public void incValidReads() {
		long v;
		do {

			v = numValidReads.get();
		} while (!numValidReads.compareAndSet(v, v + 1));

		// increase the total number of reads too
		do {
			v = numTotalReads.get();
		} while (!numTotalReads.compareAndSet(v, v + 1));

	}

	public void incStaleReads() {
		long v;
		do {
			v = numStaleReads.get();
		} while (!numStaleReads.compareAndSet(v, v + 1));

		// increase the total number of reads too
		do {
			v = numTotalReads.get();
		} while (!numTotalReads.compareAndSet(v, v + 1));

	}

	Bucket(long id, long start, double end) {

		_id = id;
		_startTime = start;
		_endTime = end;
		_duration = end - start;
		if (numValidReads == null) {
			numValidReads = new AtomicLong();
			numValidReads.set(0);
		}
		if (numStaleReads == null) {
			numStaleReads = new AtomicLong();
			numStaleReads.set(0);
		}
		if (numTotalReads == null) {
			numTotalReads = new AtomicLong();
			numTotalReads.set(0);
		}
	}

	public static double computeFreshnessConfidence(ArrayList<HashMap<Long, Bucket>> buckets) {
		int satisfyingReads = 0, totalReads = 0;
		for (HashMap<Long, Bucket> bucketMap : buckets) {
			for (Bucket bucket : bucketMap.values()) {
				if (bucket.getEndTime() >= freshnessTime) {
					satisfyingReads += bucket.getNumValidReads();
					totalReads += bucket.getNumTotalReads();

				}
			}
		}

		double freshnessConfidence;
		if (totalReads != 0) {
			System.out.printf("Probability of freshness confidence for at most %d ms after last write=%.2f%% %n",
					freshnessTime, (((double) satisfyingReads) / totalReads) * 100);
			freshnessConfidence = (((double) satisfyingReads) / totalReads) * 100;
		} else {
			System.out.printf("Probability of freshness confidence for at most %d ms after last write=0%% %n",
					freshnessTime);

			freshnessConfidence = 0;
		}
		return freshnessConfidence;
	}

	public static String getFreshnessBucketsStr(ArrayList<HashMap<Long, Bucket>> buckets, boolean print) {
		StringBuilder sb = new StringBuilder();
		long allReads = 0;
		long allStale = 0;

		TreeSet<Long> keys = getKeys(buckets);
		for (long i : keys) {
			long bucketReads = 0;
			long bucketStales = 0;

			for (HashMap<Long, Bucket> bucketMap : buckets) {

				Bucket bucket = bucketMap.get(i);
				if (bucket != null) {
					bucketReads += bucket.getNumTotalReads();
					bucketStales += bucket.getNumStaleReads();

				}

			}
			if (bucketReads > 0) {
				double endTime = ((i + 1) * (Bucket.bucketDuration));
				double startTime = i * bucketDuration;
				if (i == Bucket.maxBuckets - 1) {
					endTime = Double.POSITIVE_INFINITY;
				}

				if (print) {
					System.out.println("[" + startTime + ", " + endTime + "]" + " :" + " total reads=" + bucketReads
							+ ", anomalous read=" + bucketStales);
				}
				sb.append(startTime + ":" + endTime + ":" + bucketReads + ":" + bucketStales + ";");
				allReads += bucketReads;
				allStale += bucketStales;
			}

		}
		if (print) {
			System.out.println("total=" + allReads);
			System.out.println("Discard=" + Validator.discardCount);
		}
		sb.append(allReads+":"+allStale);
		return sb.toString();
	}

	private static TreeSet<Long> getKeys(ArrayList<HashMap<Long, Bucket>> buckets) {
		TreeSet<Long> keys = new TreeSet<Long>();
		for (HashMap<Long, Bucket> bucketMap : buckets) {
			for (long key : bucketMap.keySet()) {
				keys.add(key);
			}
		}
		return keys;
	}
}