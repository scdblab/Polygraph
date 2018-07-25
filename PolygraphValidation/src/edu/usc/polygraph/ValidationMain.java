package edu.usc.polygraph;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class ValidationMain {

	public static void main(String[] args) {

		// if(ValidationConstants.countDiscardedWrites){
		// discardedRecs= new HashSet<String>();
		// propertiesWithState=new HashSet<String>();
		// }

		// PropertyConfigurator.configure("log4j.properties");
		Validator.init(args);

		SimpleDateFormat ft = new SimpleDateFormat("'at' hh:mm:ss a");
		long runStartTime = System.currentTimeMillis();
		String startedString = "Staretd " + ft.format(new Date());
		System.out.println(startedString);
		ValidationStats validationStats = new ValidationStats(10000, 100000000L, Validator.application);

		ArrayList<Validator> validators = new ArrayList<Validator>();
		runValidators(validationStats, Validator.clientId, Validator.numClients, Validator.numPartitions, validators);

		long end = System.currentTimeMillis();
		long sumWriteLogsCount = 0, sumStaleCounter = 0, sumReadLogsCount = 0;
		long maxSchedulesAll = 0, fullyDiscardedWritesCountAll = 0, partiallyDiscardedWritesCountAll = 0;
		long sumCDSE = 0;
		long sumReadOver = 0;
		long sumfactorialCount = 0;
		// long
		// maxRecords=0,fullyDiscardedWritesCount=0,partiallyDiscardedWritesCount=0;
		// long avgSchedules=0,avgRecords=0,maxSchedules=0;
		ArrayList<HashMap<Long, Bucket>> bucketsArr = new ArrayList<HashMap<Long, Bucket>>();
		try {

			for (Validator v : validators) {
				System.out.println(v.validatorID + ":" + getDurationStr(Validator.validatorStartTime, v.finishTime));
				sumfactorialCount += v.factorialCount;
				sumCDSE += v.cdseCount;
				sumReadOver += v.readOverLappingCount;
				maxSchedulesAll = Math.max(maxSchedulesAll, v.maxSchedules);

			}
			for (ValidatorData vd : ValidatorData.partitions.values()) {
				if (ValidationParams.COMPUTE_FRESHNESS)
					bucketsArr.add(vd.freshnessBuckets);
				sumWriteLogsCount += vd.writeLogsCount;
				sumReadLogsCount += vd.readLogsCount.get();
				sumStaleCounter += vd.staleCount.get();
				fullyDiscardedWritesCountAll += vd.fullyDiscardedWritesCount;
				partiallyDiscardedWritesCountAll += vd.partiallyDiscardedWritesCount;

			}

		} catch (Exception e) {
			e.printStackTrace(System.out);
		}

		System.out.printf("Total Read count: %d\nTotal Write count: %d\nTotal anomaly count: %d\n", sumReadLogsCount,
				sumWriteLogsCount, sumStaleCounter);
		if (ValidationParams.PRINT_ALL_STATS) {
			System.out.printf("Total CDSE count: %d\nTotal Read Overlapping count: %d\nTotal Factorial count: %d\n",
					sumCDSE, sumReadOver, sumfactorialCount);
		}
		if (ValidationParams.COMPUTE_FRESHNESS) {
			Bucket.getFreshnessBucketsStr(bucketsArr,true);
			Bucket.computeFreshnessConfidence(bucketsArr);
		}
		System.out.println(startedString);

		System.out.println("Finished " + ft.format(new Date()));
		String durationStr = getDurationStr(runStartTime, end);
		System.out.println("Total Duration = " + durationStr);
		 durationStr = getDurationStr(Validator.validatorStartTime, end);
		System.out.println("Validation Duration = " + durationStr);
		System.out.println("GC Time= " + getDurationStr(0, getGarbageCollectionTime()));
		double avgMem = validationStats.getSumMU() / validationStats.getCountMU();
		double maxMem = validationStats.getMemoryUsageMax();

		System.out.println("Average memory usage = " + avgMem + " MB");
		System.out.println("Max memory usage = " + maxMem + " MB");
		System.out.print("Partition ID, Reads, Updates, Anomaly Count");
		if (ValidationParams.PRINT_ALL_STATS) {
			System.out.print(
					",Average Schedules (Max),Average Records (Max),Fully Discarded Writes Count, Partially Discarded Writes Count, Unordered Read Count, Unordered Write Count");
		}

		System.out.println();

		long readLogsCount = 0;
		long writeLogsCount = 0;
		long unorderedR = 0;
		long unorderedW = 0;
		long staleCounter = 0;
		long fullyDiscardedWritesCount = 0;
		long partiallyDiscardedWritesCount = 0;
		for (ValidatorData vd : ValidatorData.partitions.values()) {
			String partitionTimestr = "";
			if (ValidationParams.timePartition) {
				partitionTimestr = String.format(", Reads num:%s , Writes num:%s",
						vd.numReadLogsForPartitions.toString(), vd.numWriteLogsForPartitions.toString());

			}
			readLogsCount = vd.readLogsCount.get();
			writeLogsCount = vd.writeLogsCount;
			staleCounter = vd.staleCount.get();
			unorderedR = vd.unorderedReadsCount;
			unorderedW = vd.unorderedWritesCount;
			fullyDiscardedWritesCount = vd.fullyDiscardedWritesCount;
			partiallyDiscardedWritesCount = vd.partiallyDiscardedWritesCount;
			System.out.printf("%d,%d,%d,%d,%s", vd.myPartition, readLogsCount, writeLogsCount, staleCounter,
					partitionTimestr);
			if (ValidationParams.countDiscardedWrites && ValidationParams.PRINT_ALL_STATS) {
				System.out.print(fullyDiscardedWritesCount + "," + partiallyDiscardedWritesCount + ",");
			}
			System.out.println();
		}

		for (Validator v : validators) {

			// MR1
			long durationM_pullTime = (long) Math.floor(v.pullTime / 60000.0);
			long durationS_pullTime = (v.pullTime % 60000) / 1000;
			long durationSS_pullTime = (v.pullTime % 60000) % 1000;
			String durationStr_pullTime = String.format("%02d:%02d.%03d", durationM_pullTime, durationS_pullTime,
					durationSS_pullTime);
			if (ValidationParams.PRINT_ALL_STATS)
				System.out.printf(",%d,%s", v.pullTime, durationStr_pullTime);

			// long duration = times.get(v.validatorID) - runStartTime;

			long duration2 = v.finishTime - Validator.validatorStartTime;
			long duration = duration2;
			long durationM_FTE = (long) Math.floor(duration / 60000.0);
			long durationS_FTE = (duration % 60000) / 1000;
			long durationSS_FTE = (duration % 60000) % 1000;
			String durationStr_FTE = String.format("%02d:%02d.%03d", durationM_FTE, durationS_FTE, durationSS_FTE);

			long validation = duration - v.pullTime;

			long validationM_FTE = (long) Math.floor(validation / 60000.0);
			long validationS_FTE = (validation % 60000) / 1000;
			long validationSS_FTE = (validation % 60000) % 1000;
			String validationStr_FTE = String.format("%02d:%02d.%03d", validationM_FTE, validationS_FTE,
					validationSS_FTE);
			if (ValidationParams.PRINT_ALL_STATS) {
				System.out.printf(",%d,%s", validation, validationStr_FTE);
				System.out.printf(",%d,%s", duration, durationStr_FTE);
				if (v.measureCount == 0)
					v.measureCount = 1;
				double avgDBSize = v.dbStateSize / v.measureCount;
				double avgSSSize = v.serialSchedSize / v.measureCount;

				System.out.print("," + avgSSSize + "," + avgDBSize + "," + v.totalScheds / v.measureCount + ","
						+ v.uniqueSS / v.measureCount);
				System.out.print("," + unorderedR + "," + unorderedW);
				System.out.println();
			}

		}
		durationStr=String.valueOf((end-Validator.validatorStartTime)/1000);
		System.out.println("Total:Reads,Updates,Anomaly count,duration");
		System.out.printf("%d,%d,%d,%s %n", sumReadLogsCount, sumWriteLogsCount, sumStaleCounter, durationStr);
		String id = String.valueOf(Validator.numValidators);
		if (!ValidationParams.USE_KAFKA) {
			id = Validator.getFileName(Validator.logDir);
		}

		Validator.close();

	}

	private static long getGarbageCollectionTime() {
		long collectionTime = 0;
		for (GarbageCollectorMXBean garbageCollectorMXBean : ManagementFactory.getGarbageCollectorMXBeans()) {
			if (garbageCollectorMXBean.getCollectionTime() > 0)
				collectionTime += garbageCollectorMXBean.getCollectionTime();
		}
		return collectionTime;
	}

	public static String getDurationStr(long runStartTime, long end) {
		long durationM = (long) Math.floor((end - runStartTime) / 60000.0);
		long durationS = ((end - runStartTime) % 60000) / 1000;
		long durationSS = ((end - runStartTime) % 60000) % 1000;
		String durationStr = String.format("%02d:%02d.%03d", durationM, durationS, durationSS);
		return durationStr;
	}

	private static void runValidators(ValidationStats validationStats, int clientId, int numClients, int numPartitions,
			ArrayList<Validator> validators) {
		HashMap<Integer, ArrayList<Integer>> threadsPartitions = new HashMap<>();
		if (Validator.numValidators > Validator.numPartitions) {
			Validator.numValidators = Validator.numPartitions;
			System.out.printf("Adjusting number of validator threads to %d to be equal to the number of partitions %n",
					Validator.numValidators);
		}
		int numThreads = Validator.numValidators / numClients;
		int remainingThreads = Validator.numValidators % numClients;

		if (clientId < remainingThreads)
			numThreads++;
		Validator.myNumThreads = numThreads;
		ArrayList<Integer> myPartitions = new ArrayList<Integer>();
		for (int i = 0; i < numPartitions; i++) {
			int threadId = i % Validator.numValidators;
			if (threadId % numClients == clientId) {
				myPartitions.add(i);

				ArrayList<Integer> partitions = threadsPartitions.get(threadId);
				if (partitions == null) {
					partitions = new ArrayList<>();
					threadsPartitions.put(threadId, partitions);
				}
				partitions.add(i);

			}

		}
		System.out.println("Initializing partitions ... ");
		if (!ValidationParams.timePartition)
			ValidatorData.numThreadsPerPartition = 1;
		initPartitions(myPartitions, Validator.application, Validator.numPartitions);
		System.out.println("Done initializing partitions ");
		int count = ValidatorData.numThreadsPerPartition;
		if (Validator.tempId != -1) {
			count = 1;
		}
		numThreads = numThreads * count;
		Validator.threadsStart = new CountDownLatch(numThreads);
		ExecutorService exec = Executors.newFixedThreadPool(numThreads);
		Set<Future<Integer>> set = new HashSet<Future<Integer>>();
		int counter = 0;
		if (ValidationParams.timePartition)
		System.out.printf(
				"Starting %d Validator threads with %d partitions. Each partition has %d thread(s) with duration %.2f seconds %n",
				numThreads, numPartitions, ValidatorData.numThreadsPerPartition,
				ValidatorData.partitionDur / ValidatorData.SEC_TO_NANO);
		else
			System.out.printf(
					"Starting %d Validator threads with %d partitions. Each partition has %d thread(s) %n",
					numThreads, numPartitions, ValidatorData.numThreadsPerPartition);
		for (int p = 0; p < ValidatorData.numThreadsPerPartition; p++) {
			for (int i = 0; i < Validator.numValidators; i++) {

				if (i % numClients == clientId /* && counter==1 */) {
					if (Validator.tempId == -1 || p == Validator.tempId) {
						Validator validatorThread = new Validator(counter, threadsPartitions.get(i), p);
						validators.add(validatorThread);
						Future<Integer> future = exec.submit(validatorThread);
						set.add(future);
					}
					counter++;
				}
//				counter++;
			}
		}
		validationStats.setValidators(validators);
		validationStats.setStartTime();
		Thread statsThread = new Thread(validationStats);
		statsThread.start();
		HashSet<Integer> threads = new HashSet<Integer>();

		try {
			for (Future<Integer> future : set) {
				int vID = future.get();
				assert !threads.contains(vID) : "Each validator id should be inserted once";
				threads.add(vID);
			}
			exec.shutdown();
			validationStats.terminate();
			statsThread.join();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}

		System.out.printf("Validator threads has finished %n");

	}

	private static void initPartitions(ArrayList<Integer> mypartitions, String topic, int numPartitions) {
		int count = ValidatorData.numThreadsPerPartition;
		int start = 0;
		if (Validator.tempId != -1) {
			start = Validator.tempId;
			count = Validator.tempId + 1;
		}

		for (int j = start; j < count; j++) {
			for (int i = 0; i < mypartitions.size(); i++) {

				ValidatorData vd = new ValidatorData(mypartitions.get(i));
				ValidatorData.partitions.put(mypartitions.get(i) + "-" + j, vd);

			}

		}
	}

}

class ValidationStats implements Runnable {
	private Runtime runtime = Runtime.getRuntime();
	private long sumMU = 0, countMU = 0;
	private long memoryUsageMax = -1;
	private long sleepTime = 1000, overflowConstant = 10000;
	private boolean stop = false;
	private long runStartTime;
	private String statsTopic;
	KafkaProducer<String, String> statsProducer;
	private ArrayList<Validator> validators;

	private String getStatsString() {
		long end = System.currentTimeMillis();
		long durationM = (long) Math.floor((end - runStartTime) / 60000.0);
		long durationS = ((end - runStartTime) % 60000) / 1000;
		long durationSS = ((end - runStartTime) % 60000) % 1000;
		// String timeUnit = " miniutes";
		// if (duration == 0) {
		// duration = (end - start) / 1000;
		// timeUnit = " seconds";sudo testdiskbucl
		// }

		String durationStr = String.format("%02d:%02d.%03d", durationM, durationS, durationSS);

		StringBuilder sb = new StringBuilder();
		long readLogsCount = 0;
		long writeLogsCount = 0;
		long staleCounter = 0;
		long maxSchedules = 0;
		long fullyDiscardedWritesCount = 0;
		long partiallyDiscardedWritesCount = 0;
		long avgSchedules = 0;
		ArrayList<HashMap<Long, Bucket>> bucketsArr = new ArrayList<HashMap<Long, Bucket>>();

		for (Validator v : validators) {
			for (ValidatorData vd : v.validatorData) {

				readLogsCount += vd.readLogsCount.get();
				writeLogsCount += vd.writeLogsCount;
				staleCounter += vd.staleCount.get();
				partiallyDiscardedWritesCount += vd.partiallyDiscardedWritesCount;
				fullyDiscardedWritesCount += vd.fullyDiscardedWritesCount;
				bucketsArr.add(vd.freshnessBuckets);

			}
			maxSchedules = Math.max(maxSchedules, v.maxSchedules);

			avgSchedules += v.avgSchedules;
		}
		long avgSched = avgSchedules / validators.size();
		sb.append(Validator.numClients);
		sb.append(ValidationParams.RECORD_ATTRIBUTE_SEPERATOR);
		sb.append(Validator.clientId);
		sb.append(ValidationParams.RECORD_ATTRIBUTE_SEPERATOR);
		sb.append(readLogsCount);
		sb.append(ValidationParams.RECORD_ATTRIBUTE_SEPERATOR);
		sb.append(writeLogsCount);
		sb.append(ValidationParams.RECORD_ATTRIBUTE_SEPERATOR);
		sb.append(staleCounter);
		sb.append(ValidationParams.RECORD_ATTRIBUTE_SEPERATOR);
		sb.append(avgSched);
		sb.append(ValidationParams.RECORD_ATTRIBUTE_SEPERATOR);
		sb.append(maxSchedules);
		sb.append(ValidationParams.RECORD_ATTRIBUTE_SEPERATOR);
		long count = countMU;
		if (count == 0)
			count = 1;
		sb.append((sumMU / count));
		sb.append(ValidationParams.RECORD_ATTRIBUTE_SEPERATOR);
		sb.append(memoryUsageMax);
		sb.append(ValidationParams.RECORD_ATTRIBUTE_SEPERATOR);
		sb.append(partiallyDiscardedWritesCount);
		sb.append(ValidationParams.RECORD_ATTRIBUTE_SEPERATOR);
		sb.append(fullyDiscardedWritesCount);
		sb.append(ValidationParams.RECORD_ATTRIBUTE_SEPERATOR);
		sb.append(durationStr);
		if (ValidationParams.COMPUTE_FRESHNESS) {
			sb.append(ValidationParams.RECORD_ATTRIBUTE_SEPERATOR);
			String bucketsStr=Bucket.getFreshnessBucketsStr(bucketsArr, false);
			sb.append(bucketsStr);
		}
		sb.append(ValidationParams.LINE_SEPERATOR);
		return sb.toString();
	}

	public void setValidators(ArrayList<Validator> validators) {
		this.validators = validators;
	}

	public void setStartTime() {
		runStartTime = System.currentTimeMillis();
	}

	ValidationStats(int sleepTime, long overflowConstant, String topic) {
		statsTopic = "STATS_" + topic;

		if (ValidationParams.USE_KAFKA && ValidationParams.PRODUCE_STATS) {
			statsProducer = Validator.initStatsProducer(topic);
		}
		runStartTime = System.currentTimeMillis();
		if (sleepTime <= 1000)
			sleepTime = 1000;
		this.sleepTime = sleepTime;
		if (overflowConstant <= 0)
			overflowConstant = 1;
		this.overflowConstant = overflowConstant;
	}

	public long getSumMU() {
		return sumMU;
	}

	public long getCountMU() {
		return countMU;
	}

	public long getMemoryUsageMax() {
		return memoryUsageMax;
	}

	@Override
	public void run() {
		while (!stop) {
			long currentMU_inBytes = runtime.totalMemory() - runtime.freeMemory();
			long currentMU_MB = currentMU_inBytes / 1048576;
			if (memoryUsageMax < currentMU_MB)
				memoryUsageMax = currentMU_MB;
			sumMU += currentMU_MB;
			countMU++;
			if (countMU >= overflowConstant) {
				if (countMU == 0)
					countMU = 1;
				sumMU = sumMU / countMU;
				countMU = 1;
			}
			if (sumMU < 0) {
				sumMU = 0;
				countMU = 0;
			}
			try {
				Thread.sleep(sleepTime);
				getStatsString();
			} catch (InterruptedException e) {
				e.printStackTrace(System.out);
			}
			if (ValidationParams.USE_KAFKA && ValidationParams.PRODUCE_STATS)
				statsProducer.send(new ProducerRecord<String, String>(statsTopic, 0, null, getStatsString()));
		}

		if (ValidationParams.USE_KAFKA && ValidationParams.PRODUCE_STATS) {

			statsProducer.flush();
			statsProducer.close();
		}
	}

	public void resetCounters() {
		sumMU = 0;
		countMU = 0;
		memoryUsageMax = 0;
	}

	public void terminate() {

		stop = true;
	}

}
