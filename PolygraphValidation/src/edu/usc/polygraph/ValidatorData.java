package edu.usc.polygraph;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import edu.usc.polygraph.treemap.Tree;
import edu.usc.polygraph.treemap.TreeType;

public class ValidatorData {
	public Tree<TreeType> commonTree;
	public Map<String, List<DBState>> dbState;// SS
	public Map<String, ScheduleList> currentSS;// SS
	public ArrayList<LogRecord> readWrite;// SS
	public resourceUpdateStat updateLogs;// SS
	public HashMap<String, HashMap<String, ArrayList<LogRecord>>> bucket;// SS
	public Buffer bufferedReads = null;// SS
	public Buffer bufferedWrites = null;// SS
	public Map<String, Map<String, Boolean>> notAllowedList;// SS
	public Map<LogRecord, String> collapsedIntervals;// SS

	public KafkaConsumer<String, String> consumerRead = null;// NoSS
	public KafkaConsumer<String, String> consumerUpdate = null;// NoSS
	public TopicPartition readTopicPartitions;// NoSS
	public TopicPartition updateTopicPartitions;// NoSS
	public LogRecord currentRead = null;// NoSS

	public AtomicLong readLogsCount = new AtomicLong(0);// SS
	public long writeLogsCount = 0;// SS
	public long readStartTime = 0;// SS
	public long readEndTime = 0;// SS
	public AtomicLong staleCount = new AtomicLong(0);// SS
	public int partiallyDiscardedWritesCount = 0;// SS
	public int fullyDiscardedWritesCount = 0;// SS
	public long unorderedReadsCount = 0;// SS
	public long unorderedWritesCount = 0;// SS
	public int myPartition = -1;// NoSS
	ReentrantLock lock;
	public HashMap<Long, Bucket> freshnessBuckets;
	public static HashMap<String, ValidatorData> partitions = new HashMap<>();
	public BufferedReader[] bReaders = null;
	BufferedReader[] updateBReaders;
	LogRecord[] writeRecords;
	public LogRecord[] records = null;
	public int[] index = { -1 };
	private long st0 = 0;
	public double partitionSt = 0;
	public double partitionEt = 0;
	public long partitionCounter = 0;
	public static final long SEC_TO_NANO = 1000000000;
	private static final String READ_FILES = "read";
	private static final String UPDATE_FILES = "update";
	public static double partitionDur = 450 * SEC_TO_NANO;
	public static int numThreadsPerPartition = 8;
	public ArrayList<Long> numReadLogsForPartitions;
	public ArrayList<Long> numWriteLogsForPartitions;
	public HashSet<String> firstRead = null;
	public LinkedList<String> reads;
	public LinkedList<String> writes;
	public static CountDownLatch threadsStart = null;

	public ValidatorData(Integer partition) {

		if (ValidationParams.timePartition)
			firstRead = new HashSet<>();
		if (ValidationParams.COMPUTE_FRESHNESS)
			freshnessBuckets = new HashMap<Long, Bucket>();
		if (ValidationParams.useTreeMap)
			commonTree = new Tree<TreeType>();
		dbState = new ConcurrentHashMap<String, List<DBState>>();
		currentSS = new ConcurrentHashMap<String, ScheduleList>();
		bucket = new HashMap<String, HashMap<String, ArrayList<LogRecord>>>();
		for (String e : ValidationParams.ENTITY_NAMES) {
			HashMap<String, ArrayList<LogRecord>> map = new HashMap<String, ArrayList<LogRecord>>();
			bucket.put(e, map);
		}
		notAllowedList = new ConcurrentHashMap<String, Map<String, Boolean>>();
		collapsedIntervals = new ConcurrentHashMap<>();
		readWrite = new ArrayList<LogRecord>();
		updateLogs = new resourceUpdateStat();
		myPartition = partition;
		bufferedReads = new Buffer(ValidationParams.maxBufferSize, ValidationParams.bufferThreshold);
		bufferedWrites = new Buffer(ValidationParams.maxBufferSize, ValidationParams.bufferThreshold);
		if (ValidationParams.USE_KAFKA)
			initKafkaConsumers(Validator.application, Validator.numPartitions);
		else {
			initFiles();
			getFirstLog();

		}
		if (ValidatorData.numThreadsPerPartition > 1000)
			lock = new ReentrantLock();
		if (ValidationParams.timePartition) {
			numReadLogsForPartitions = new ArrayList<>();
			numWriteLogsForPartitions = new ArrayList<>();

		}

	}

	private void readAll(List<String> reads2, BufferedReader[] bReaders2, LogRecord[] records2, Validator validator) {
		LogRecord log = null;
		do {
			log = Validator.getReadFromFiles(records2, bReaders2, true);
			if (log != null)
				reads2.add(log.getActionName());
			if (reads2.size() % 10000 == 0) {
				System.out.printf("%d:Finished reading %d ...%n", validator.validatorID, reads2.size());
			}

		} while (log != null);

	}

	private void initFiles(String logDir2, BufferedReader[] bReaders, String type) {
		DataInputStream[] dataInStreams = new DataInputStream[ValidationParams.threadCount];
		FileInputStream[] fstreams = new FileInputStream[ValidationParams.threadCount];
		for (int i = 0; i < ValidationParams.threadCount; i++) {
			try {
				int machineid = 0;
				fstreams[i] = new FileInputStream(logDir2 + "/" + type + machineid + "-" + i + ".txt");
				dataInStreams[i] = new DataInputStream(fstreams[i]);
				bReaders[i] = new BufferedReader(new InputStreamReader(dataInStreams[i]));
			} catch (FileNotFoundException e) {
				e.printStackTrace(System.out);
				System.out.println(" Log file not found " + e.getMessage());
			}
		}
	}

	public void closeFiles() {
		try {
			for (int i = 0; i < ValidationParams.threadCount; i++) {

				if (bReaders[i] != null)
					bReaders[i].close();
				if (updateBReaders[i] != null)
					updateBReaders[i].close();
			}
		} catch (IOException e) {
			e.printStackTrace(System.out);
		}
	}

	private void initFiles() {

		bReaders = new BufferedReader[ValidationParams.threadCount];
		String type = READ_FILES;

		initFiles(Validator.logDir, bReaders, type);
		records = new LogRecord[ValidationParams.threadCount];

		updateBReaders = new BufferedReader[ValidationParams.threadCount];
		writeRecords = new LogRecord[ValidationParams.threadCount];
		type = UPDATE_FILES;
		initFiles(Validator.logDir, updateBReaders, type);

	}

	private void getFirstLog() {
		boolean dummy = false;
		if (ValidationParams.timePartition) {
			dummy = true;
		}
		if (!ValidationParams.USE_KAFKA) {
			int index = Validator.getReadFromFilesIndex(records, bReaders, dummy);
			LogRecord r = records[index];
			index = Validator.getReadFromFilesIndex(writeRecords, updateBReaders, dummy);
			LogRecord w = writeRecords[index];
			if (r != null)
				st0 = r.getStartTime();
			if (w != null && w.getStartTime() < st0)
				st0 = w.getStartTime();

		}

	}

	public void lock() {

		if (ValidatorData.numThreadsPerPartition > 1000) {
			try {
				lock.lock();

			} catch (Exception e) {

				e.printStackTrace();
			}
		}
	}

	public void unlock() {

		if (ValidatorData.numThreadsPerPartition > 1000) {
			lock.unlock();
		}
	}

	private void initKafkaConsumers(String topic, int numPartitions) {
		Properties propsArr[] = new Properties[2];
		for (int i = 0; i < propsArr.length; i++) {
			Properties props = new Properties();
			props.put("bootstrap.servers", ValidationParams.KAFKA_HOST);
			props.put("enable.auto.commit", "true");
			props.put("auto.commit.interval.ms", "60000");
			props.put("request.timeout.ms", "500000000");
			props.put("session.timeout.ms", "50000000");
			props.put("connections.max.idle.ms", "50000000");
			// props.put("group.max.session.timeout.ms", "50000000");
			props.put("fetch.min.bytes", ValidationParams.CONSUMER_FETCH_MIN_BYTES);
			props.put("fetch.max.wait.ms", ValidationParams.CONSUMER_FETCH_MAX_WAIT_MS);
			// props.put("group.id", topic +new Random().nextInt());
			props.put("key.deserializer", StringDeserializer.class.getName());
			props.put("value.deserializer", StringDeserializer.class.getName());
			propsArr[i] = props;

		}

		propsArr[0].put("max.partition.fetch.bytes", ValidationParams.READ_MAX_PARTITION_FETCH_BYTES);
		propsArr[0].put("max.poll.records", ValidationParams.READ_MAX_POLL_RECORDS);

		propsArr[1].put("max.poll.records", ValidationParams.UPDATE_MAX_POLL_RECORDS);
		propsArr[1].put("max.partition.fetch.bytes", ValidationParams.UPDATE_MAX_PARTITION_FETCH_BYTES);

		this.readTopicPartitions = new TopicPartition(topic, this.myPartition);

		this.updateTopicPartitions = new TopicPartition(topic, this.myPartition + numPartitions);
		this.consumerRead = new KafkaConsumer<String, String>(propsArr[0]);
		ArrayList<TopicPartition> topicListR = new ArrayList<TopicPartition>();
		topicListR.add(this.readTopicPartitions);
		this.consumerRead.assign(topicListR);
		this.consumerRead.seekToBeginning(topicListR);
		long minR = this.consumerRead.position(this.readTopicPartitions);
		this.bufferedReads.initOffset(minR);

		this.consumerUpdate = new KafkaConsumer<String, String>(propsArr[1]);
		ArrayList<TopicPartition> topicListU = new ArrayList<TopicPartition>();
		topicListU.add(this.updateTopicPartitions);
		this.consumerUpdate.assign(topicListU);
		this.consumerUpdate.seekToBeginning(topicListU);
		long minU = this.consumerUpdate.position(this.updateTopicPartitions);
		this.bufferedWrites.initOffset(minU);
		// System.out.println("first read offset: " + minR + " ,, first
		// update offset: " + minU);

	}

	public ValidatorData() {

	}

	long stTest = 0;

	public void computeNewPartition(int vid, Validator validator, LogRecord currentRead2) {

		if (partitionCounter >= 1) {
			// System.out.printf("%d:Time for partition %d=%s %n", vid, partitionCounter -
			// 1,
			// ValidationMain.getDurationStr(stTest, System.currentTimeMillis()));
		}
		stTest = System.currentTimeMillis();
		partitionSt = st0
				+ (vid * partitionDur + ValidatorData.numThreadsPerPartition * partitionDur * partitionCounter);
		partitionEt = partitionSt + partitionDur - 1;
		// System.out.println("Read to create new partition:"+currentRead2);
		// System.out.println(String.format("%d: counter=%d [%.0f,%.0f], read count=%d",
		// vid, partitionCounter,
		// partitionSt, partitionEt, validator.totalReadLogsCount));
		numReadLogsForPartitions.add(0L);
		numWriteLogsForPartitions.add(0L);
		partitionCounter++;
		bucket.clear();
		for (String e : ValidationParams.ENTITY_NAMES) {
			HashMap<String, ArrayList<LogRecord>> map = new HashMap<String, ArrayList<LogRecord>>();
			bucket.put(e, map);
		}
		dbState.clear();
		collapsedIntervals.clear();
		notAllowedList.clear();
		validator.updateNotAllowed.clear();
		if (ValidationParams.useTreeMap)
			commonTree.clear();
		if (ValidatorData.numThreadsPerPartition == 1) {
			handleOnethread(validator);

		} else {
			if (ValidationParams.readAllFiles) {
				LinkedList<LogRecord> updates = updateLogs.intervalList.intervals;
				handleList(updates);
			} else {
				updateLogs.intervalList.intervals.clear();
			}
			readWrite.clear();

		}
		currentSS.clear();
		firstRead.clear();

		// TODO: check buffer

	}

	private void handleOnethread(Validator validator) {
		if (false) {
			readWrite.clear();
			updateLogs.intervalList.intervals.clear();
			closeFiles();
			initFiles();
		} else {
			HashSet<LogRecord> logsToadd = new HashSet<>();
			for (ScheduleList s : currentSS.values()) {
				for (LogRecord log : s.overlapList) {
					boolean add = checkWrite(log);
					if (add)
						logsToadd.add(log);
				}
			}
			LinkedList<LogRecord> updates = updateLogs.intervalList.intervals;

			handleList(updates);
			handleList(readWrite);

			for (LogRecord log : logsToadd)
				updateLogs.addIntervalSorted(log);
		}

	}

	private void removeDuplicates(ArrayList<LogRecord> intervals) {
		Collections.sort(intervals);

		// remove duplicates
		for (int i = intervals.size() - 1; i > 0; i--) {
			if (intervals.get(i).getStartTime() == intervals.get(i - 1).getStartTime()) {
				if (intervals.get(i).getId().equals(intervals.get(i - 1).getId())) {
					intervals.remove(i);
				}
			}
		}

	}

	private void handleList(List<LogRecord> updates) {
		Iterator<LogRecord> it = updates.iterator();
		while (it.hasNext()) {
			LogRecord log = it.next();
			boolean add = checkWrite(log);
			if (!add)
				it.remove();

		}

	}

	private void handleListSt(List<LogRecord> updates, HashSet<LogRecord> logsToadd) {
		Iterator<LogRecord> it = updates.iterator();
		while (it.hasNext()) {
			LogRecord log = it.next();
			boolean add = checkWrite(log);
			if (add)
				logsToadd.add(log);

		}
		updates.clear();

	}

	public LogRecord checkFirstRead(LogRecord currentRead2, Validator validator) {
		HashMap<String, EntitySpec> generatedEntities = new HashMap<>();
		for (Entity e : currentRead2.getEntities()) {
			for (Property p : e.getProperties()) {
				if (p.getType() != ValidationParams.VALUE_READ)
					continue;
				String pkey = Property.getProprtyKey(e, p);
				String ekey = e.getEntityKey();
				if (firstRead.contains(pkey))
					continue;
				firstRead.add(pkey);
				EntitySpec gentity = generatedEntities.get(ekey);
				if (gentity == null) {
					gentity = new EntitySpec(e.getKey(), e.getName(), null);
					gentity.setPropertiesArrayLis(new ArrayList<>());
					generatedEntities.put(ekey, gentity);

				}
				Property p1 = new Property(p.getName(), p.getValue(), ValidationParams.NEW_VALUE_UPDATE);
				gentity.propertiesArrayLis.add(p1);

			}
		}

		if (!generatedEntities.isEmpty()) {
			Entity[] entities = new Entity[generatedEntities.size()];
			int i = 0;
			for (EntitySpec entity : generatedEntities.values()) {
				entities[i] = entity;
				Property[] props = entities[i].getProperties();
				props = new Property[entity.getPropertiesArrayLis().size()];
				props = entity.getPropertiesArrayLis().toArray(props);
				entities[i].setProperties(props);
				entity.getPropertiesArrayLis().clear();
				entity.setPropertiesArrayLis(null);
				i++;
			}
			return createLogRecordFromRead(entities, currentRead2, validator);
		}
		return null;

	}

	private LogRecord createLogRecordFromRead(Entity[] entities, LogRecord currentRead2, Validator v) {
		String id = "time-" + currentRead2.getId() + "-" + ValidationParams.KEY_SEPERATOR + v.seq;

		v.seq++;
		LogRecord r = new LogRecord(id, currentRead2.getActionName() + "Init", currentRead2.getStartTime(),
				currentRead2.getEndTime(), ValidationParams.UPDATE_RECORD, entities);
		r.setCreatedFromRead(false);
		r.setPartitionID(currentRead2.getPartitionID());
		return r;

	}

	private void addToBucket(LogRecord r) {
		for (Entity e : r.getEntities()) {
			for (Property p : e.getProperties()) {
				String pKey = Property.getProprtyKey(e, p);
				if (!bucket.get(e.name).containsKey(pKey)) {
					ArrayList<LogRecord> temp = new ArrayList<LogRecord>();
					temp.add(r);
					bucket.get(e.name).put(pKey, temp);
				} else {
					ArrayList<LogRecord> b = bucket.get(e.name).get(pKey);
					b.add(r);

				}

			}
		}
	}

	public boolean checkWrite(LogRecord currentRecord) {
		boolean add = true;
		if (partitionSt > currentRecord.getEndTime()) {
			add = false;
		}
		return add;

	}

	public boolean checkWriteSt(LogRecord currentRecord) {
		boolean add = true;
		if (partitionSt > currentRecord.getStartTime()) {
			add = false;
		}
		return add;

	}

	public void checkPartitionStartEnd(LogRecord earlyRecord, int vid, Validator validator) {
		if (earlyRecord.getStartTime() < partitionSt) {
			// get previous partition
			double prevPartitionSt = st0 + (vid * partitionDur
					+ ValidatorData.numThreadsPerPartition * partitionDur * (partitionCounter - 2));
			double prevPartitionEt = prevPartitionSt + partitionDur - 1;
			if (earlyRecord.getStartTime() <= prevPartitionEt) {
				System.out.println(
						"Must return to previous partition because of revalidaion exception. This case is not handeled yet. Exiting...");
				System.exit(0);

			}

		}

	}

	public void initReadAll(Validator validator) {
		// if (validator.validatorID!=0)
		// try {
		// Thread.sleep(1000);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		synchronized (ValidatorData.class) {
			reads = new LinkedList<>();
			writes = new LinkedList<>();
			if (threadsStart == null) {
				int count = numThreadsPerPartition;
				if (Validator.tempId != -1) {
					count = 1;
				}
				threadsStart = new CountDownLatch(count);
			}
			long st = System.currentTimeMillis();
			System.out.println(validator.validatorID + ":Start Reading read files...");
			readAll(reads, bReaders, records, validator);
			System.out.println(validator.validatorID + ":Start Reading write files...");
			readAll(writes, updateBReaders, writeRecords, validator);
			System.out.println(validator.validatorID + ":Filtering...");
			filter(validator);
			long et = System.currentTimeMillis();
			System.out.println(
					validator.validatorID + ":Reading all files took:" + ValidationMain.getDurationStr(st, et));

		}
	}

	private void filter(Validator validator) {
		HashMap<Integer, LogRecord> maxReadEnds = filterReads(validator);
		filterWrites(validator, maxReadEnds);

	}

	private void filterWrites(Validator validator, HashMap<Integer, LogRecord> maxReadEnds) {
		int pCounter = 0;
		double pSt = st0 + (validator.validatorID * partitionDur
				+ ValidatorData.numThreadsPerPartition * partitionDur * pCounter);
		double pEt = pSt + partitionDur - 1;
		pCounter++;
		long maxReadEnd = 0;
		Iterator<String> it = writes.iterator();
		while (it.hasNext()) {
			String s = it.next();
			LogRecord log = LogRecord.createLogRecord(s, true, true);
			if (log.getStartTime() > pEt) {
				LogRecord plog = maxReadEnds.get(pCounter - 1);
				if (plog.getEndTime() < maxReadEnd) {
					plog.setEndTime(maxReadEnd);
				}
				pSt = st0 + (validator.validatorID * partitionDur
						+ ValidatorData.numThreadsPerPartition * partitionDur * pCounter);
				pEt = pSt + partitionDur - 1;
				maxReadEnd = 0;
				pCounter++;

			}
			boolean removed = false;
			if (pSt > log.getEndTime()) {
				if (pCounter == 1) {
					removed = true;
					it.remove();
				} else {
					LogRecord log2 = maxReadEnds.get(pCounter - 2);
					if (log2 == null) {
						System.out.println("At " + (pCounter - 2));
						System.exit(0);
					}
					if (!log.overlap(log2)) {
						it.remove();
						removed = true;
					}

				}
			}
			if (!removed && log.getType() == ValidationParams.READ_WRITE_RECORD && log.getEndTime() > maxReadEnd) {
				maxReadEnd = log.getEndTime();

			}

		}

	}

	private HashMap<Integer, LogRecord> filterReads(Validator validator) {
		int pCounter = 0;
		double pSt = st0 + (validator.validatorID * partitionDur
				+ ValidatorData.numThreadsPerPartition * partitionDur * pCounter);
		double pEt = pSt + partitionDur - 1;
		pCounter++;
		HashMap<Integer, LogRecord> maxReadEnds = new HashMap<>();
		Iterator<String> it = reads.iterator();
		long maxReadEnd = 0;
		while (it.hasNext()) {
			String s = it.next();
			LogRecord log = LogRecord.createLogRecord(s, true, true);
			if (log.getStartTime() > pEt) {
				LogRecord r = new LogRecord("0", null, (long) pSt, maxReadEnd, 'X', null);
				maxReadEnds.put(pCounter - 1, r);
				pSt = st0 + (validator.validatorID * partitionDur
						+ ValidatorData.numThreadsPerPartition * partitionDur * pCounter);
				pEt = pSt + partitionDur - 1;
				maxReadEnd = 0;
				pCounter++;
			}
			boolean removed = false;
			if (log.getStartTime() < pSt) {
				it.remove();
				removed = true;

			}
			if (!removed && log.getEndTime() > maxReadEnd) {
				maxReadEnd = log.getEndTime();
			}

		}
		return maxReadEnds;

	}

}