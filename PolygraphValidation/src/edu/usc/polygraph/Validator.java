package edu.usc.polygraph;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.usc.dblab.cm.sizers.ProfileSizer;
import edu.usc.dblab.cm.sizers.Sizer;
import edu.usc.polygraph.cdse.CDSE;
import edu.usc.polygraph.initial_data.YCSBLoader;
import edu.usc.polygraph.snapshot.Snapshot;
import edu.usc.polygraph.snapshot.SnapshotInfo;
import edu.usc.polygraph.snapshot.SnapshotResult;
import edu.usc.polygraph.treemap.IntegerTreeType;
import edu.usc.polygraph.treemap.TreeType;

public class Validator implements Callable<Integer> {
	// public HashMap<HashMap<String, Boolean> ,
	// ArrayList<String>>updateNotAllowed=null;
	public HashMap<String, ArrayList<String>> updateNotAllowed = null;

	public static final Logger logger = LoggerFactory.getLogger(Validator.class);
	public static AtomicInteger threadCount = new AtomicInteger(0);
	public ValidatorData validatorData[];
	public static CountDownLatch threadsStart;
	public int[] scheduleCounter = { 0 };
	public boolean cdseUsed = false;
	public boolean factUsed = false;
	public long cdseCount = 0;
	public long seq = 0;
	public long readOverLappingCount = 0;
	public long factorialCount = 0;
	public HashMap<String, Boolean> overlapMap = null;
	public HashMap<String, HashSet<String>> commonDI = null;
	public static String[] deletedArr = { ValidationParams.NULL_STRING };
	public ArrayList<EntitySpec> generatedEntities = new ArrayList<EntitySpec>();
	public HashSet<String> participatingEntities = new HashSet<String>();
	public ArrayList<CandidateValue> newDBStateListInit;
	public ArrayList<CandidateValue> newDBStateListUpdated;
	public static long discardCount;
	public ArrayList<String> discardedSchIdList;
	public static Database database;
	public long totalReadLogsCount = 0;
	public int readRoundRobin = 0;
	private static volatile boolean initFiles = false;
	public int totalStaleCount = 0;

	public static int numToDivideBy = 10000;

	long previousSnapshot;// NewSnapshot
	int snapshotCounter = 0;// NewSnapshot

	enum TransactionOverlap {
		Inc, NVU, None, Both;
	}

	public double dbStateSize = 0;
	public double serialSchedSize = 0;
	Sizer sizer;
	public long measureCount = 0;

	public KafkaProducer<String, String> statsProducer = null;
	public static int numValidators = 1;
	public static int numPartitions = 1;
	public static long skew = 0;
	public int validatorID = 0;
	public int partitionTimeIndex;
	public static int clientId = 0;
	public static int numClients = 1;

	public long memoryUsage = 0;
	public long memoryUsageMax = 0;
	public long avgSchedules = 0;
	public long maxSchedules = 0;
	public long avgRecords = 0;
	public long maxRecords = 0;
	// public HashMap<String, Integer> unorderdLogs = new HashMap<String,
	// Integer>();
	public static int myNumThreads;

	public static String logDir = "";

	static String application;
	private static String statsTopic;
	private ArrayList<String> staleRecords = new ArrayList<String>();
	public static String kafkaLogDir;

	public static int tempId = -1;

	public long pullTime = 0;

	public long totalScheds = 0;
	public long uniqueSS = 0;
	public long finishTime = -1;
	public long csdseLogCounter = 0;

	public static long validatorStartTime;

	////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// Constructor
	//////////////////////////////////////////////////////////////////////////////////////////////////////// ///////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Validator(int id, ArrayList<Integer> partitions, int pid) {
		partitionTimeIndex = pid;
		if (ValidationParams.YAZ_FIX) {
			// updateNotAllowed= new HashMap<HashMap<String, Boolean> ,ArrayList<String>>();
			updateNotAllowed = new HashMap<String, ArrayList<String>>();

		}
		if (ValidationParams.CDSE) {
			overlapMap = new HashMap<String, Boolean>();
			commonDI = new HashMap<String, HashSet<String>>();
		}
		newDBStateListUpdated = new ArrayList<CandidateValue>();
		newDBStateListInit = new ArrayList<CandidateValue>();
		discardedSchIdList = new ArrayList<String>();
		if (ValidationParams.MEASURE_MEMORY)
			sizer = new ProfileSizer();
		validatorID = id;
		validatorData = new ValidatorData[partitions.size()];
		for (int i = 0; i < validatorData.length; i++) {
			validatorData[i] = ValidatorData.partitions.get(partitions.get(i) + "-" + partitionTimeIndex);
		}

		switch (Validator.application) {
		// DEMO-D44561F405597AD0622B0BB178899AD2
		case "DEMO-EA34E97083131F5B5291B98AA8BD7278":
			if (ValidationParams.hasInitState) {
				Map<String, List<DBState>> b = new ConcurrentHashMap<String, List<DBState>>();
				List<DBState> ll = new LinkedList<DBState>();
				ll.add(new DBState(0, "100000000"));
				b.put("Account-" + "DEMO-EA34E97083131F5B5291B98AA8BD7278", ll);
				this.validatorData[0].dbState = b;
			}
			break;
		case "BG":
		case "BGS100T1P":
		case "BG10":
		case "BG100":
		case "BG1000":

		case "MR1":
		case "MR2":

		case "CD":
		case "DD":
			int BG_numMembers = 10000;
			int BG_FriendCount = 0;
			int BG_PendingCount = 0;
			if (ValidationParams.hasInitState)
				this.validatorData[0].dbState = Utilities.generateBGInitialState(BG_numMembers, BG_FriendCount,
						BG_PendingCount);
			break;

		case "YCSB":
			if (ValidationParams.hasInitState) {
				int scaleFactor = 1;
				ValidationParams.ENTITY_PROPERTIES[0] = Arrays.copyOf(ValidationParams.ENTITY_PROPERTIES[0], 10);
				this.validatorData[0].dbState = YCSBLoader.generateInitialState(scaleFactor);
				if (ValidationParams.USE_DATABASE) {
					database.initializeState(this.validatorData[0].dbState);
				}
			}
			break;

		case "TPCC":
			// TPCC_numWarehouses = 1;
			// TPCC_numDistrictsPerW = 10;
			// TPCC_numCustomersPerD = 3000;
			// numToDivideBy = 1;
			// if (ValidationConstants.hasInitState)
			// dbState =
			// TPCCLoader.generateInitialState_Entites(TPCC_numWarehouses,
			// TPCC_numDistrictsPerW, TPCC_numCustomersPerD);
			break;
		default:
			// System.out.println("ERROR: Unknown Application");
			// System.exit(0);
		}
		if (ValidationParams.USE_KAFKA) {
			if (ValidationParams.PRODUCE_STATS)
				statsProducer = initStatsProducer(application);
		}
		if (ValidationParams.CREATE_SNAPSHOT) {
			try {
				Snapshot.semaphore.acquire();
			} catch (InterruptedException e1) {

				e1.printStackTrace(System.out);
			}
			previousSnapshot = System.currentTimeMillis();// NewSnapshot
			for (int i = 0; i < validatorData.length; i++) {
				String path = Snapshot.SANPSHOT_DIR + validatorID + "/" + i + "/";
				String pathss = path + snapshotCounter + "/";
				Snapshot.initSnapshotsInfo(path);
				try {
					Snapshot.createSnapshot(snapshotCounter, path, pathss, validatorData[i],
							validatorData[i].readStartTime, validatorData[i].readEndTime);
				} catch (IOException e) {
					System.out.println(e.getMessage());
					e.printStackTrace(System.out);
					System.exit(0);
				}
			}
			Snapshot.semaphore.release();

			snapshotCounter++;
		}
	}

	public static void init(String args[]) {
		Validator.readProps(args);
		if (!ValidationParams.timePartition) {
			tempId = -1;
		}
		if (!ValidationParams.USE_KAFKA) {
			numValidators = 1;
			numPartitions = 1;
		}

		if (ValidationParams.erFile != null)
			Validator.parseERFile(ValidationParams.erFile);
		else
			System.out.println("Entity/Relationsip file is not provided");

		// ValidationConstants.topic=ValidationMain.application;
		if (!ValidationParams.USE_KAFKA) {

			Validator.logDir = Validator.fixDir(Validator.logDir);
			if (!Validator.isDirectory(Validator.logDir)) {
				System.out.println("\"" + Validator.logDir + "\" is not a directory.");
				System.exit(0);
			}
		}
		ValidationParams.countDiscardedWrites = false && !ValidationParams.hasInitState;
		Validator.application = Validator.application.toUpperCase();
		System.out.println("Running " + Validator.application + " Application");

		System.out.println("Validator is running with HasInitState set to:" + ValidationParams.hasInitState
				+ " , Validation set to:" + ValidationParams.DO_VALIDATION + " and Database set to:"
				+ ValidationParams.USE_DATABASE);
		// stale file
		String fileName = "id-";
		String templog = Validator.logDir;
		int current = 0;
		String pattern = "init";
		if (!ValidationParams.USE_KAFKA) {

			fileName = getFileName(Validator.logDir);

			if (!ValidationParams.hasInitState)
				pattern = "noinit";
		}

		switch (Validator.application) {
		case "BG10":
		case "BG100":
		case "BG1000":
		case "BGU32":
		case "BGGU32":
		case "BG211":
		case "BGU100T1P":
		case "BGS100T1P":
		case "BGSTALE":
		case "BG":
		case "BG1":
		case "BG32":
		case "BGG32":
		case "MR1":
		case "MR2":
		case "CD":
		case "DD":
			ValidationParams.ENTITY_NAMES = new String[1];
			ValidationParams.ENTITY_NAMES[0] = ValidationParams.MEMBER_ENTITY;
			ValidationParams.ENTITY_PROPERTIES = new String[1][];
			ValidationParams.ENTITY_PROPERTIES[0] = ValidationParams.MEMBER_PROPERIES;
			ValidationParams.ENTITIES_INSERT_ACTIONS = new String[1][];
			ValidationParams.ENTITIES_INSERT_ACTIONS[0] = null;
			break;
		case "YCSB":
			ValidationParams.ENTITY_NAMES = new String[1];
			ValidationParams.ENTITY_NAMES[0] = ValidationParams.USER_ENTITY;
			ValidationParams.ENTITY_PROPERTIES = new String[1][];
			ValidationParams.ENTITY_PROPERTIES[0] = ValidationParams.USER_PROPERIES;
			ValidationParams.ENTITIES_INSERT_ACTIONS = new String[1][];
			ValidationParams.ENTITIES_INSERT_ACTIONS[0] = null;
			break;
		case "TPCC_MR1":
		case "TPCC":
		case "TPCCSTALE":
		case "TPCC48":
		case "TPCC1P":
		case "TPCC211":
		case "TPCCTEST":
		case "TPCC1":
		case "TPCC32":
			ValidationParams.ENTITY_NAMES = new String[4];
			ValidationParams.ENTITY_NAMES[0] = ValidationParams.CUSTOMER_ENTITY;
			ValidationParams.ENTITY_NAMES[1] = ValidationParams.ORDER_ENTITY;
			ValidationParams.ENTITY_NAMES[2] = ValidationParams.CUST_ORDER_REL;
			ValidationParams.ENTITY_NAMES[3] = "Dist";
			ValidationParams.ENTITY_PROPERTIES = new String[4][];
			ValidationParams.ENTITY_PROPERTIES[0] = ValidationParams.CUSTOMER_PROPERIES;
			ValidationParams.ENTITY_PROPERTIES[1] = ValidationParams.ORDER_PROPERIES;
			ValidationParams.ENTITY_PROPERTIES[2] = ValidationParams.CUST_ORDER_REL_PROPERIES;
			String[] arr = { "d_next_o_id" };
			ValidationParams.ENTITY_PROPERTIES[3] = arr;

			ValidationParams.ENTITIES_INSERT_ACTIONS = new String[4][];
			ValidationParams.ENTITIES_INSERT_ACTIONS[0] = null;
			String[] o = { ValidationParams.NEWORDER_ACTION };
			ValidationParams.ENTITIES_INSERT_ACTIONS[1] = o;
			ValidationParams.ENTITIES_INSERT_ACTIONS[2] = null;
			ValidationParams.ENTITIES_INSERT_ACTIONS[3] = null;

			break;
		case "SEATS":
			ValidationParams.ENTITY_NAMES = new String[4];
			ValidationParams.ENTITY_NAMES[0] = "FLIT";
			ValidationParams.ENTITY_NAMES[1] = "CUST";
			ValidationParams.ENTITY_NAMES[2] = "FF";
			ValidationParams.ENTITY_NAMES[3] = "RES";

			ValidationParams.ENTITY_PROPERTIES = new String[4][];
			String flitProps[] = { "S_LEFT" };
			String resProps[] = { "rid" };

			String custProps[] = { "C00", "C10", "BAL", "C12" };
			String ffProps[] = { "F00", "F10", "F11" };
			ValidationParams.ENTITY_PROPERTIES[0] = flitProps;
			ValidationParams.ENTITY_PROPERTIES[1] = custProps;
			ValidationParams.ENTITY_PROPERTIES[2] = ffProps;
			ValidationParams.ENTITY_PROPERTIES[3] = resProps;

			ValidationParams.ENTITIES_INSERT_ACTIONS = new String[4][];
			// ValidationConstants.ENTITIES_INSERT_ACTIONS[0] = null;
			// String[] o = { ValidationConstants.NEWORDER_ACTION };
			ValidationParams.ENTITIES_INSERT_ACTIONS[0] = null;
			ValidationParams.ENTITIES_INSERT_ACTIONS[1] = null;
			ValidationParams.ENTITIES_INSERT_ACTIONS[2] = null;
			ValidationParams.ENTITIES_INSERT_ACTIONS[3] = null;

			break;
		case "TPCC_1":
			ValidationParams.ENTITY_NAMES = new String[5];
			ValidationParams.ENTITY_NAMES[0] = "Customer";
			ValidationParams.ENTITY_NAMES[1] = "Order";
			ValidationParams.ENTITY_NAMES[2] = "Last_Order";
			ValidationParams.ENTITY_NAMES[3] = "District";
			ValidationParams.ENTITY_NAMES[4] = "Warehouse";

			ValidationParams.ENTITY_PROPERTIES = new String[5][];
			String[] custArr = { "balance", "payment_cnt", "ytd_payment" };
			String[] lastOrderArr = { "o_id" };
			String[] orderArr = { "carrier_id", "ol_cnt", "delivery_date" };
			ValidationParams.ENTITY_PROPERTIES[0] = custArr;
			ValidationParams.ENTITY_PROPERTIES[1] = orderArr;
			ValidationParams.ENTITY_PROPERTIES[2] = lastOrderArr;
			String[] arr2 = { "d_next_o_id" };
			ValidationParams.ENTITY_PROPERTIES[3] = arr2;

			ValidationParams.ENTITIES_INSERT_ACTIONS = new String[5][];
			ValidationParams.ENTITIES_INSERT_ACTIONS[0] = null;
			String[] o2 = { "New-Order" };
			ValidationParams.ENTITIES_INSERT_ACTIONS[1] = o2;
			ValidationParams.ENTITIES_INSERT_ACTIONS[2] = null;
			ValidationParams.ENTITIES_INSERT_ACTIONS[3] = null;
			ValidationParams.ENTITIES_INSERT_ACTIONS[4] = null;

			break;
		default:
			// System.out.println("ERROR: Unknown Application");
			// System.exit(0);
		}

		// retrieveUpdatesThread = new RetrieveUpdatesThread(logDir);
		if (!ValidationParams.USE_KAFKA) {
			ValidationParams.threadCount = Utilities.getNumOfFiles(Validator.logDir) / 2;
		}
		// readUpdateFiles2(logDir);
		// retrieveUpdatesThread.start();

		if (ValidationParams.USE_DATABASE || ValidationParams.useTreeMap) {
			Validator.database = new Database();
			if (ValidationParams.USE_DATABASE)
				Validator.database.init();
		}

	}

	public static void main(String[] args) {
	}

	public static String readFile(String filename) {
		String result = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			while (line != null) {
				sb.append(line);
				line = br.readLine();
			}
			result = sb.toString();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	static void parseERFile(String erFile) {

		// obj= new JsonP
		// pr.p
		JSONObject obj = new JSONObject(readFile(erFile));
		JSONObject app = obj.getJSONObject("application");
		if (!application.equalsIgnoreCase(app.getString("name"))) {
			System.out.println(
					"Warning:Application provided in ER file is diffferent than the application in the arguments. Exiting!");
			// System.exit(0);
		}
		JSONArray entities = app.getJSONArray("entities");

		ValidationParams.ENTITY_NAMES = new String[entities.length()];
		ValidationParams.ENTITY_PROPERTIES = new String[entities.length()][];
		ValidationParams.ENTITIES_INSERT_ACTIONS = new String[entities.length()][];

		for (int i = 0; i < entities.length(); i++) {
			JSONObject entity = (JSONObject) entities.get(i);
			ValidationParams.ENTITY_NAMES[i] = entity.getString("name");

			JSONArray props = entity.getJSONArray("properties");
			String[] propertyArr = new String[props.length()];

			for (int j = 0; j < props.length(); j++) {
				propertyArr[j] = props.getString(j);
			}
			if (!entity.isNull("inserttrans")) {
				JSONArray insertT = entity.getJSONArray("inserttrans");

				String[] insertTArry = new String[insertT.length()];
				for (int j = 0; j < insertT.length(); j++) {
					insertTArry[j] = insertT.getString(j);
				}
				ValidationParams.ENTITIES_INSERT_ACTIONS[i] = insertTArry;
			}
			ValidationParams.ENTITY_PROPERTIES[i] = propertyArr;

		}
		// ValidationConstants.ENTITY_NAMES[0] =
		// ValidationConstants.MEMBER_ENTITY;
		// ValidationConstants.ENTITY_PROPERTIES = new String[1][];
		// ValidationConstants.ENTITY_PROPERTIES[0] =
		// ValidationConstants.MEMBER_PROPERIES;
		// ValidationConstants.ENTITIES_INSERT_ACTIONS = new String[1][];
		// ValidationConstants.ENTITIES_INSERT_ACTIONS[0] = null;

	}

	static void readProps(String[] args) {
		// -app bg -init false -kafka true -globalschedule false -er=
		// -filelogdir -kafkalogdir
		for (int i = 0; i < args.length; i = i + 2) {
			args[i] = args[i].substring(1);
			if (args[i].equals(ValidationParams.APPLICATION_PROP))
				Validator.application = args[i + 1];

			else if (args[i].equals(ValidationParams.INIT_PROP))
				ValidationParams.hasInitState = Boolean.parseBoolean(args[i + 1]);

			else if (args[i].equals(ValidationParams.USE_KFKA_PROP))
				ValidationParams.USE_KAFKA = Boolean.parseBoolean(args[i + 1]);

			else if (args[i].equals(ValidationParams.USE_BUFFER_PROP))
				ValidationParams.useBuffer = Boolean.parseBoolean(args[i + 1]);
			else if (args[i].equals(ValidationParams.VALIDATION_PROP))
				ValidationParams.DO_VALIDATION = Boolean.parseBoolean(args[i + 1]);

			else if (args[i].equals(ValidationParams.GLOBAL_SCHEDULE_PROP))
				ValidationParams.GLOBAL_SCHEDULE = Boolean.parseBoolean(args[i + 1]);

			else if (args[i].equals(ValidationParams.ONLINE_RUNNING_PROP)) {
				ValidationParams.onlineRunning = Boolean.parseBoolean(args[i + 1]);

			}
			
			else if (args[i].equals(ValidationParams.FRESHNESS_PROP)) {
				ValidationParams.COMPUTE_FRESHNESS = Boolean.parseBoolean(args[i + 1]);

			}

			else if (args[i].equals(ValidationParams.USE_DATABASE_PROP)) {
				ValidationParams.USE_DATABASE = Boolean.parseBoolean(args[i + 1]);

			} else if (args[i].equals(ValidationParams.USE_INTERVAL_TREE_PROP)) {
				ValidationParams.useTreeMap = Boolean.parseBoolean(args[i + 1]);

			}

			else if (args[i].equals(ValidationParams.ER_PROP))
				ValidationParams.erFile = args[i + 1];

			else if (args[i].equals(ValidationParams.FILE_LOG_PROP))
				Validator.logDir = args[i + 1];

			else if (args[i].equals(ValidationParams.KAFKA_HOST_PROP)) {
				ValidationParams.KAFKA_HOST = args[i + 1];

			}

			else if (args[i].equals(ValidationParams.ZOOK_HOST_PROP))
				ValidationParams.ZOOKEEPER_HOST = args[i + 1];

			else if (args[i].equals(ValidationParams.PRINT_FREQ_PROP))
				Validator.numToDivideBy = Integer.parseInt(args[i + 1]);

			else if (args[i].equals(ValidationParams.CLIENT_ID_PROP))
				Validator.clientId = Integer.parseInt(args[i + 1]);

			else if (args[i].equals(ValidationParams.NUM_CLIENTS_PROP))
				Validator.numClients = Integer.parseInt(args[i + 1]);

			else if (args[i].equals(ValidationParams.NUM_VALIDATORS_PROP))
				Validator.numValidators = Integer.parseInt(args[i + 1]);

			else if (args[i].equals("tempid"))
				Validator.tempId = Integer.parseInt(args[i + 1]);

			else if (args[i].equals(ValidationParams.THREADS_PER_PARTITION_PROP))
				ValidatorData.numThreadsPerPartition = Integer.parseInt(args[i + 1]);

			else if (args[i].equals(ValidationParams.PARTITION_DURATION_PROP))
				ValidatorData.partitionDur = Double.parseDouble(args[i + 1]) * ValidatorData.SEC_TO_NANO;

			else if (args[i].equals(ValidationParams.NUM_PARTITIONS_PROP))
				Validator.numPartitions = Integer.parseInt(args[i + 1]);

			else if (args[i].equals(ValidationParams.MAX_BUFFER_SIZE_PROP))
				ValidationParams.maxBufferSize = Long.parseLong(args[i + 1]);

			else if (args[i].equals(ValidationParams.BUFFER_THRESHOLD_PROP))
				ValidationParams.bufferThreshold = Float.parseFloat(args[i + 1]);

			else if (args[i].equals(ValidationParams.SKEW_PROP))
				skew = Long.parseLong(args[i + 1]);

			else if (args[i].equals(ValidationParams.SNAPSHOT_DELTA))
				Snapshot.SnapshotDeltaMillis = Integer.parseInt(args[i + 1]);

		}
	}

	public static KafkaProducer<String, String> initStatsProducer(String application2) {
		Properties props = new Properties();
		props.put("bootstrap.servers", ValidationParams.KAFKA_HOST);
		props.put("acks", "all");
		props.put("retries", 0);
		props.put("batch.size", 16384);
		props.put("linger.ms", 1);
		props.put("buffer.memory", 33554432);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		KafkaProducer<String, String> statsProducer = new KafkaProducer<String, String>(props);
		statsTopic = "STATS_" + application2;
		KafkaScripts.createTopic(statsTopic, 2);
		return statsProducer;

	}

	static String getFileName(String logDir) {

		// stale file
		String templog = logDir;
		int current = 0;
		int prev = -1;
		while (templog.indexOf(ValidationParams.DIR_SEPERATOR) >= 0) {
			prev = current;
			current += templog.indexOf(ValidationParams.DIR_SEPERATOR) + 1;
			templog = templog.substring(templog.indexOf(ValidationParams.DIR_SEPERATOR) + 1);
		}
		String fileName = logDir.substring(prev, logDir.length() - 1);
		return fileName;
	}

	static boolean isDirectory(String logDir) {
		File dir = new File(logDir);
		return dir.isDirectory();
	}

	private void startValidation(String logDir) {
		// if (!ValidationConstants.USE_KAFKA) {
		// fileValidation(logDir);
		// } else {
		if (ValidationParams.USE_KAFKA) {
			ValidationParams.USE_SEQ = false;
		} else { // file
			ValidationParams.PRODUCE_STATS = false;
		}
		resourceValidation();
		if (ValidationParams.USE_KAFKA) {

			for (int i = 0; i < validatorData.length; i++) {
				synchronized (validatorData[i]) {
					if (validatorData[i].consumerRead != null)
						validatorData[i].consumerRead.close();
					if (validatorData[i].consumerUpdate != null)
						validatorData[i].consumerUpdate.close();

					validatorData[i].consumerRead = null;
					validatorData[i].consumerUpdate = null;
				}

			}

			if (ValidationParams.PRODUCE_STATS) {
				statsProducer.flush();
				statsProducer.close();

			}
		}

		// }
	}

	private LogRecord getRead(ValidatorData vd, int readIndex, LogRecord[] records, BufferedReader[] bReaders,
			int[] index) throws UnorderedWritesException {
		LogRecord currentRecordResource = null;

		if (vd.currentRead == null) {
			if (ValidationParams.USE_KAFKA) {
				if (vd.consumerRead != null) {
					long mStart;
					if (ValidationParams.KAFKA_TIME)
						mStart = System.currentTimeMillis();

					currentRecordResource = getReadFromKafkaBuffred(vd, readIndex);
					if (currentRecordResource == null /* && !ValidationParams.onlineRunning */) {
						vd.consumerRead.close();
						vd.consumerRead = null;
					}
					if (ValidationParams.KAFKA_TIME)
						pullTime += (System.currentTimeMillis() - mStart);
				}
			} else { // no kafka
				LogRecord log = null;
				if (ValidationParams.readAllFiles) {
					if (vd.reads == null) {
						long st = System.currentTimeMillis();
						vd.initReadAll(this);
						ValidatorData.threadsStart.countDown();
						try {
							ValidatorData.threadsStart.await();
							long et = System.currentTimeMillis();
							System.out.println(validatorID + ":waiting took:" + ValidationMain.getDurationStr(st, et));

						} catch (Exception e) {
							e.printStackTrace();
						}

					}
					if (!vd.reads.isEmpty()) {
						String logStr = vd.reads.remove(0);
						log = LogRecord.createLogRecord(logStr, true, false);
					}
				} else {
					boolean dummy = false;
					if (ValidationParams.timePartition)
						dummy = true;
					log = getReadFromFiles(records, bReaders, dummy);
				}
				currentRecordResource = log;

			}
		} else
			currentRecordResource = vd.currentRead;

		vd.currentRead = null;

		////// RW/////////////////////////////
		LogRecord currentRecordRW = null;
		if (vd.readWrite.size() != 0) {
			currentRecordRW = vd.readWrite.get(0);
			vd.readWrite.remove(0);
		}
		LogRecord currentRead = null;
		if (currentRecordRW != null && currentRecordResource != null) {
			if (currentRecordResource.getStartTime() < currentRecordRW.getStartTime()) {
				currentRead = currentRecordResource;
				vd.readWrite.add(currentRecordRW);
				currentRecordRW = null;
				currentRecordResource = null;

			} else {
				vd.currentRead = currentRecordResource;
				currentRecordResource = null;
				currentRead = currentRecordRW;
				currentRecordRW = null;

			}
		} else { // one or both is null
			if (currentRecordRW != null) {
				currentRead = currentRecordRW;
				currentRecordRW = null;

			} else if (currentRecordResource != null) {
				currentRead = currentRecordResource;
				currentRecordResource = null;

			} else { // both null
				IntervalsList ll = vd.updateLogs.intervalList;
				while (true) {

					int size = ll.size();
					for (int i = 0; i < vd.updateLogs.size(); i++) {
						if (ll.intervals.get(i).getType() == ValidationParams.READ_WRITE_RECORD) {
							currentRead = ll.intervals.remove(i);
							return currentRead;
						}
					}
					if (ValidationParams.USE_KAFKA)
						retriveUpdatesFromKafkaBuffered(vd, null, readIndex);
					if (ll.size() == size)
						break;

				}
			}

		}

		return currentRead;
	}

	private void resourceValidation() {

		long lastStats = 0;
		int doneCount = 0;
		threadCount.incrementAndGet();
		LogRecord currentRead = null;
		// LogRecord fakeReadRecord = null;

		int readIndex = 0;
		ValidatorData vd = validatorData[readIndex];
		HashSet<Integer> skipIndexes = new HashSet<Integer>();
		while (true) {

			do {

				if (readRoundRobin < 0)
					readRoundRobin = 0;
				readRoundRobin++;
				readIndex = readRoundRobin % validatorData.length;
			} while (skipIndexes.contains(readIndex));
			vd = validatorData[readIndex];
			vd.lock();
			try {

				if (ValidationParams.timePartition) {

					boolean skip = true;
					do {
						currentRead = getRead(vd, readIndex, vd.records, vd.bReaders, vd.index);
						if (currentRead == null)
							break;

						skip = evaluate(vd, currentRead, this);

					} while (skip);
					if (currentRead != null && currentRead.getEntities() == null)
						currentRead = LogRecord.createLogRecord(currentRead.getActionName(), true, false);

				} else
					currentRead = getRead(vd, readIndex, vd.records, vd.bReaders, vd.index);

				if (currentRead == null/* && !ValidationParams.onlineRunning */) {
					skipIndexes.add(readIndex);
					doneCount++;
					if (doneCount == validatorData.length) {
						vd.unlock();
						break;
					}

				}

				if (currentRead == null) {
					vd.unlock();
					continue;
				}

				if (currentRead.getStartTime() < vd.readStartTime) {

					System.out.printf("%d: Found earlier read in partition %d. Current Read is:%s %n", validatorID,
							readIndex, currentRead.getId());

					restoreSnapshot(readIndex, currentRead, 'R');

					vd.unlock();
					continue;
				}
				long str = 0;
				if (logger.isDebugEnabled()) {
					str = System.currentTimeMillis();
				}

				validateRead(currentRead);

				logger.debug("Time to validate(sec):{}", (System.currentTimeMillis() - str) / 1000);
				// if (currentRead.getId().equals("7-25965"))//||
				// record.getId().equals("4-1176"))
				// System.exit(0);
				if (currentRead.getStartTime() > vd.readStartTime) {
					vd.readStartTime = currentRead.getStartTime();
					vd.readEndTime = currentRead.getEndTime();
				}

				vd.readLogsCount.incrementAndGet();
				totalReadLogsCount++;
				if (ValidationParams.timePartition) {
					int lastIndex = vd.numReadLogsForPartitions.size() - 1;
					long v = vd.numReadLogsForPartitions.get(lastIndex);
					vd.numReadLogsForPartitions.set(lastIndex, ++v);
				}

				if (totalReadLogsCount % numToDivideBy == 0) {
					System.out.println(validatorID + ":Read log records= " + totalReadLogsCount + ". Anomalies = "
							+ totalStaleCount);

				}
				if (currentRead.getType() == ValidationParams.READ_WRITE_RECORD) {
					convertToUpdate(currentRead);

					if (vd.updateLogs == null) {
						vd.updateLogs = new resourceUpdateStat();
					}
					vd.updateLogs.addIntervalSorted(currentRead);
				}
				measureCount++;

				if (createSnapshot()) {
					Date start = new Date();

					try {

						System.out.println(validatorID + ": Start creating snapshot at:"
								+ ValidationParams.DATE_FORMAT.format(start));
						for (int i = 0; i < validatorData.length; i++) {
							String path = Snapshot.SANPSHOT_DIR + validatorID + "/" + i + "/";
							String pathss = path + snapshotCounter + "/";

							Snapshot.createSnapshot(snapshotCounter, path, pathss, validatorData[i],
									validatorData[i].readStartTime, validatorData[i].readEndTime);
							// System.out.println("Processed:" +
							// validatorData[i].bufferedReads.processedOffsets);
							// System.out.println("skip:" +
							// validatorData[i].bufferedReads.skipList);

						}
						snapshotCounter++;
						// printInfo(vd, mr1counter+"-after");
						previousSnapshot = System.currentTimeMillis();
						// Snapshot.currentThread = (Snapshot.currentThread + 1)
						// % myNumThreads;
						Snapshot.semaphore.release();
					} catch (IOException e) {
						System.out.println(e.getMessage());
						e.printStackTrace(System.out);
						System.exit(0);
					}

					long secs = (System.currentTimeMillis() - start.getTime()) / 1000;
					String duration = String.format("%02d:%02d:%02d", secs / 3600, (secs % 3600) / 60, (secs % 60));
					System.out.println(validatorID + ": Done creating snapshot. Duration:" + duration);

				} // end if

				if (ValidationParams.MEASURE_MEMORY && totalReadLogsCount % 10000 == 0) {

					serialSchedSize += sizer.GetSize(vd.currentSS);
					dbStateSize += sizer.GetSize(vd.dbState);
					totalScheds += vd.currentSS.size();
					HashSet<ScheduleList> ss = new HashSet<ScheduleList>();
					for (String key : vd.currentSS.keySet()) {
						if (!ss.contains(vd.currentSS.get(key))) {
							ss.add(vd.currentSS.get(key));
							uniqueSS++;

						}
					}

				}
				String key = currentRead.getEntities()[0].getEntityKey();
				if (ValidationParams.GLOBAL_SCHEDULE)
					key = "1";

				updateStats(vd.currentSS.get(key));
				long currenTime = System.currentTimeMillis();
				if (currenTime > lastStats + ValidationParams.STATS_INTERVAL_SECONDS * 1000) {
					String staleValue;
					String staleKey = null;
					lastStats = currenTime;

					for (int i = 0; i < staleRecords.size(); i++) {
						staleValue = staleRecords.get(i);
						staleKey = staleValue.split(ValidationParams.RECORD_ATTRIBUTE_SEPERATOR + "")[1];
						if (ValidationParams.USE_KAFKA && ValidationParams.PRODUCE_STATS)
							statsProducer.send(new ProducerRecord<String, String>(statsTopic, 1, staleKey, staleValue));
					}
					staleRecords.clear();

				}
				currentRead = null;

			} catch (ReValidateException ex) {
				// System.out.println(this.validatorID + "Revalidate
				// exception");
				if (currentRead.getType() == ValidationParams.READ_WRITE_RECORD) {
					vd.readWrite.add(currentRead);
					Collections.sort(vd.readWrite);
				} else {

					vd.currentRead = currentRead;

				}
				if (ValidationParams.timePartition) {
					vd.checkPartitionStartEnd(vd.readWrite.get(0), partitionTimeIndex, this);
				}
				vd.unlock();

			} catch (UnorderedWritesException e1) {
				restoreSnapshot(readIndex, e1.record, 'U');
				vd.unlock();
			}

		} // end while true
		this.finishTime = System.currentTimeMillis();
		if (!ValidationParams.USE_KAFKA) {
			synchronized (ValidationParams.class) {
				if (threadCount.decrementAndGet() == 0)
					vd.closeFiles();

			}
		} else {
			// make sure all stale stats are sent
			String staleValue;
			String staleKey = null;

			for (int i = 0; i < staleRecords.size(); i++) {
				staleValue = staleRecords.get(i);
				staleKey = staleValue.split(ValidationParams.RECORD_ATTRIBUTE_SEPERATOR + "")[2];
				if (ValidationParams.USE_KAFKA && ValidationParams.PRODUCE_STATS)
					statsProducer.send(new ProducerRecord<String, String>(statsTopic, 1, staleKey, staleValue));
			}
			staleRecords.clear();

		}

		for (int p = 0; p < validatorData.length; p++) {
			if (validatorData[p].readWrite.size() > 0) {
				System.out.println(validatorID + ":still have logs. Partition=" + p);
				System.exit(0);
			}
		}

	}

	private boolean debug_check(LinkedList<LogRecord> intervals, String string) {
		for (LogRecord log : intervals) {
			if (log.getId().equals(string))
				return true;
		}
		return false;
	}

	private boolean evaluate(ValidatorData vd, LogRecord currentRead, Validator validator) {
		boolean skip = true;
		if (currentRead.getStartTime() > vd.partitionEt) {
			vd.computeNewPartition(partitionTimeIndex, validator, currentRead);
		}

		if (currentRead.getStartTime() < vd.partitionSt) {
			skip = true;

		}

		else if (currentRead.getStartTime() <= vd.partitionEt) {
			skip = false;

		} else {
			System.out.println("Unexpected case with time partitioninging exiting..");
			System.out.printf("Read start:%d partition start:%d partition end:%d %n", currentRead.getStartTime(),
					vd.partitionSt, vd.partitionEt);
			System.exit(0);
		}
		if (skip) {
			if (currentRead.getType() == ValidationParams.READ_WRITE_RECORD) {
				boolean add = vd.checkWrite(currentRead);
				if (add) {
					currentRead = LogRecord.createLogRecord(currentRead.getActionName(), true, false);
					convertToUpdate(currentRead);

					if (vd.updateLogs == null) {
						vd.updateLogs = new resourceUpdateStat();
					}

					vd.updateLogs.addIntervalSorted(currentRead);
				}
			}
		}
		return skip;
	}

	private void restoreSnapshot(int readIndex, LogRecord currentRead, char type) {
		if (!ValidationParams.CREATE_SNAPSHOT) {
			System.out.println("Found unordered log and snapshot is diabled. Exiting ... ");
			System.exit(0);
		}
		String path = Snapshot.SANPSHOT_DIR + validatorID + "/" + readIndex + "/";
		try {
			ArrayList<SnapshotInfo> al = Snapshot.getSnapshotsInfo(path);
			boolean b = false;
			for (int i = al.size() - 1; i >= 0; i--) {
				if (type == 'R') {
					validatorData[readIndex].unorderedReadsCount++;
					System.out.println(this.validatorID + ": Unordered Read found! ID: " + currentRead.getId());
					System.out.printf("%d: Snapshot(%d).startTime [%d] < [%d] currentRead.getStartTime() %n",
							this.validatorID, al.get(i).id, al.get(i).startTime, currentRead.getStartTime());
					if (al.get(i).startTime < currentRead.getStartTime()) {

						System.out.printf("%d: Restoring partition %d with snapshot no. %d. %n", validatorID, readIndex,
								al.get(i).id);
						String pathss = al.get(i).path;
						Snapshot.updateUnordered(pathss, Snapshot.unorderedReadsFile, currentRead.getOffset());
						if (ValidationParams.BATCH_UNORDERED) {
							batchUnordered(validatorData[readIndex], currentRead, pathss);
						}
						SnapshotResult ssr = Snapshot.restoreSnapshot(path, al.get(i).id, pathss,
								validatorData[readIndex], validatorID, readIndex, this, b);
						validatorData[readIndex] = ssr.data;
						break;
					}
				} else if (type == 'U') {
					validatorData[readIndex].unorderedWritesCount++;
					System.out.println(this.validatorID + ": Unordered Update found! ID: " + currentRead.getId());
					System.out.printf("%d: Snapshot(%d).endTime [%d] < [%d] unorderedWrite.getStartTime() %n",
							this.validatorID, al.get(i).id, al.get(i).endTime, currentRead.getStartTime());
					if (al.get(i).endTime < currentRead.getStartTime()) {

						System.out.printf("%d: Restoring partition %d with snapshot no. %d. %n", validatorID, readIndex,
								al.get(i).id);
						String pathss = al.get(i).path;
						Snapshot.updateUnordered(pathss, Snapshot.unorderedWritesFile, currentRead.getOffset());
						if (ValidationParams.BATCH_UNORDERED) {
							batchUnordered(validatorData[readIndex], currentRead, pathss);
						}
						SnapshotResult ssr = Snapshot.restoreSnapshot(path, al.get(i).id, pathss,
								validatorData[readIndex], validatorID, readIndex, this, b);
						validatorData[readIndex] = ssr.data;
						break;
					}
				} else {
					System.out.println(this.validatorID + ": Incorrect type");
					System.exit(0);
				}
				b = true;
			}
		} catch (IOException e) {
			System.out.println(this.validatorID + ": " + e.getMessage());
			e.printStackTrace(System.out);
			System.exit(0);
		}
	}

	private void batchUnordered(ValidatorData vd, LogRecord currentRead, String pathss) {
		Buffer br = vd.bufferedReads;
		br.sort();
		ArrayList<Long> unorderedOffsets = new ArrayList<Long>();
		while (true) {
			LogRecord log = br.viewLog(0);
			if (log != null && log.getStartTime() < vd.readStartTime) {
				LogRecord record = br.getFirst();
				assert record == log : "Getting different log!";
				unorderedOffsets.add(record.getOffset());

			} else {
				break;
			}
			if (!unorderedOffsets.isEmpty()) {
				try {
					vd.unorderedReadsCount = +unorderedOffsets.size();
					Snapshot.updateUnordered(pathss, Snapshot.unorderedReadsFile, unorderedOffsets);
				} catch (IOException e) {

					e.printStackTrace(System.out);
				}
			}
		}

		// Writes

		Buffer bw = vd.bufferedWrites;
		bw.sort();
		unorderedOffsets.clear();

		while (true) {
			LogRecord log = bw.viewLog(0);

			if (log != null && log.getStartTime() < vd.readEndTime) {
				LogRecord record = bw.getFirst();
				assert record == log : "Getting different log!";
				unorderedOffsets.add(record.getOffset());

			} else {
				break;
			}
			if (!unorderedOffsets.isEmpty()) {
				try {
					vd.unorderedWritesCount = +unorderedOffsets.size();

					Snapshot.updateUnordered(pathss, Snapshot.unorderedWritesFile, unorderedOffsets);
				} catch (IOException e) {

					e.printStackTrace(System.out);
				}
			}
		}

		// Writes

	}

	// System.out.println("Read " + readLogsCount + " read log.");

	public static LogRecord getReadFromFiles(LogRecord[] records, BufferedReader[] bReaders, boolean dummy) {

		String line = null;
		// for (int i = 0; i < ValidationParams.threadCount; i++) {
		// if (records[i] == null) {
		// try {
		// if ((line = bReaders[i].readLine()) != null) {
		//
		// records[i] = LogRecord.createLogRecord(line, true);
		//
		// }
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		// }
		int i = Utilities.getEarilestRecordIndex(records);
		LogRecord log = records[i];
		records[i] = null;
		try {
			if ((line = bReaders[i].readLine()) != null) {

				records[i] = LogRecord.createLogRecord(line, true, dummy);

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return log;
	}

	public static int getReadFromFilesIndex(LogRecord[] records, BufferedReader[] bReaders, boolean dummy) {
		String line = null;
		for (int i = 0; i < ValidationParams.threadCount; i++) {
			if (records[i] == null) {
				try {
					if ((line = bReaders[i].readLine()) != null) {

						records[i] = LogRecord.createLogRecord(line, true, dummy);

					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return Utilities.getEarilestRecordIndex(records);
	}

	private LogRecord getReadFromKafkaBuffred(ValidatorData vd, int readIndex) {

		boolean maxTries = false;

		Buffer br = vd.bufferedReads;
		long lastReadOffset = br.getOffset();
		KafkaConsumer<String, String> consomerR = vd.consumerRead;
		while (br.canInsertBuffer() && (!br.noLogs) && !maxTries) {

			maxTries = fillBuffer(validatorID, br, readIndex, consomerR, 'R', null, vd);

		} // while

		if (lastReadOffset != br.getOffset())
			consomerR.seek(vd.readTopicPartitions, br.getOffset() + 1);

		LogRecord log = br.getLogToProcess();
		if (log != null)
			log.setPartitionID(readIndex);
		return log;
	}

	@SuppressWarnings("unused")
	private boolean debugCheckDuplicate(Buffer br) {
		HashSet<String> X = new HashSet<String>();
		for (LogRecord r : br.logs) {
			if (X.contains(r.getId()))
				return true;
			X.add(r.getId());
		}
		return false;
	}

	private boolean createSnapshot() {// NewSnapshot
		if (!ValidationParams.CREATE_SNAPSHOT)
			return false;
		long now = System.currentTimeMillis();
		if (now - previousSnapshot > Snapshot.SnapshotDeltaMillis) {

			// if (validatorID == Snapshot.currentThread) {
			if (Snapshot.semaphore.tryAcquire()) {
				return true;
			}

			return false;
		} else {
			return false;
		}

	}

	public static boolean fillBuffer(int validatorID, Buffer buffer, int readIndex,
			KafkaConsumer<String, String> consumer, char type, LogRecord read, ValidatorData vd) {
		boolean bufferFull = false;
		boolean maxTries = false;

		ConsumerRecords<String, String> records = null;
		while (!bufferFull && !maxTries) {

			for (int i = 1; i <= ValidationParams.POLL_MAX_TRIES; i++) {

				records = consumer.poll(ValidationParams.KAFKA_POLL_WAIT_MILLIS * (i));

				if (records.isEmpty() && i >= ValidationParams.POLL_MAX_TRIES) {

					SimpleDateFormat ft = new SimpleDateFormat("'at' hh:mm:ss a");
					String startedString = ft.format(new Date());
					String op = "Read";
					if (type == 'U') {
						op = "Update";
					}
					System.out.println(validatorID + ": " + "Trying: " + i + " " + op + " . Index:" + readIndex
							+ ". Time " + startedString);

					// if (!ValidationParams.onlineRunning){
					maxTries = true;
					buffer.noLogs = true;
					// }

				}
				if (!records.isEmpty())
					break;
			}

			if (records.isEmpty())
				break;
			for (ConsumerRecord<String, String> rec : records) {
				// if (checkProcessed) {
				// if (buffer.processedOffsets.contains(rec.offset()))
				// continue;
				// }
				if (buffer.getOffset() == -1) {

					buffer.setOffset(rec.offset() - 1);
				}
				LogRecord r = LogRecord.createLogRecord(rec.value(), true, false);
				adjustSkew(r);
				r.setOffset(rec.offset());
				r.setPartitionID(readIndex);

				if (!buffer.add(r, rec.value().length())) {
					bufferFull = true;
					break;

				}
				buffer.setOffset(rec.offset());
				if (!ValidationParams.useBuffer && type == 'R') {
					bufferFull = true;
					maxTries = true;
					// consumer.seek(vd.readTopicPartitions, rec.offset()+1);

					break;
				}
				if (!ValidationParams.useBuffer && type == 'U') {
					if (read == null || r.getStartTime() > read.getEndTime()) {

						bufferFull = true;
						maxTries = true;
						// consumer.seek(vd.updateTopicPartitions,
						// rec.offset()+1);

						break;
					}
				}
			} // for

			if (bufferFull)
				break;
		}
		buffer.setEarliestLatest();
		return maxTries;

	}

	public static void adjustSkew(LogRecord r) {
		r.setStartTime(r.getStartTime() - skew);
		r.setEndTime(r.getEndTime() + skew);

	}

	private void updateStats(ScheduleList scheduleList) {

		int numS = 0;
		// for (ScheduleList scheduleList : currentSS.values()) {
		if (scheduleList != null) {
			numS += scheduleList.schedules.size();
			if (maxSchedules < scheduleList.schedules.size())
				maxSchedules = scheduleList.schedules.size();
		}
		// }
		// if (currentSS.size() > 0)
		// numS = numS / currentSS.size();

		avgSchedules += numS;

		// if (numS > 0) {
		// int numR = currentSS.schedules.get(0).size();
		// avgRecords += numR;
		// if (maxRecords < numR)
		// maxRecords = numR;
		// }
	}

	// static boolean stopInit = true;

	// static int maxSizeRecords = 0;

	// private static boolean debug_hasDup(ScheduleList s) {
	//// for (Schedule schedule : s.schedules) {
	//// HashSet<String> records = new HashSet<String>();
	//// for (LogRecord record : schedule.getRecords()) {
	//// if (records.contains(record.getId()))
	//// return true;
	//// else
	//// records.add(record.getId());
	//// }
	//// }
	// HashSet<String> elements = new HashSet<String>();
	// for (LogRecord record : s.overlapList) {
	// if (elements.contains(record.getId()))
	// return true;
	// else
	// elements.add(record.getId());
	// }
	// elements.clear();
	//
	// for (LogRecord record : s.newLogs) {
	// if (elements.contains(record.getId()))
	// return true;
	// else
	// elements.add(record.getId());
	// }
	// elements.clear();
	// for (String record : s.notShrink) {
	// if (elements.contains(record))
	// return true;
	// else
	// elements.add(record);
	// }
	// elements.clear();
	// for (LogRecord record : s.ReadWrite) {
	// if (elements.contains(record.getId()))
	// return true;
	// else
	// elements.add(record.getId());
	// }
	// elements.clear();
	// return false;
	// }

	// private int debug_SS(String id1, String id2) {
	// int count = 0;
	// for (ScheduleList current : currentSS.values()) {
	// for (int i = 0; i < current.schedules.size(); i++) {
	// Schedule s = current.get(i);
	// boolean found1 = false;
	// boolean found2 = false;
	//
	// for (LogRecord r : s.getRecords()) {
	// if (r.getId().equals(id1))
	// found1 = true;
	// if (id2 != null) {
	// if (found1 && r.getId().equals(id2)) {
	// found2 = true;
	// break;
	// }
	// }
	// }
	// if (found1 && found2 || found1 && id2 == null) {
	// count++;
	//
	// }
	// }
	// }
	// return count;
	// }

	// private void debug_impacted(ScheduleList s) {
	// int sum = 0;

	// for (Schedule s1:s.schedules){
	//
	//
	// for (String key: s1.getImpactedStates().keySet()){
	// if (s1.getImpactedStates().get(key).refrenceCount<=0 ||
	// (s1.getImpactedStates().get(key).refrenceCount!= s.schedules.size()
	// && s1.getImpactedStates().get(key).refrenceCount!=
	// s.schedules.size()/2) ){
	// System.out.println("Refcount is not
	// valid:"+s1.getImpactedStates().get(key).refrenceCount +" for "+ key +
	// "Reads:"+readLogsCount);
	// System.out.println();
	// // System.exit(0);
	// }
	// }
	//
	// }
	// }

	// private void debug_isDBStateValid() {
	// for (String key : dbState.keySet()) {
	// LinkedList<DBState> a = dbState.get(key);
	// for (DBState st : a) {
	//
	// for (String v : st.value) {
	// if (Utilities.isNumeric(v)) {
	// double d = Double.parseDouble(v);
	// if (d < 0) {
	// System.out.println("Value is invalid for " + key + " Read:" +
	// readLogsCount);
	// System.exit(0);
	// }
	// }
	// }
	//
	// }
	// }
	// }

	// static int stopAt = 828;// 955

	private void validateRead(LogRecord record) throws ReValidateException, UnorderedWritesException {
		ValidatorData vd = validatorData[record.getPartitionID()];
		// if (vd.dbState.get("FLIT-3442720508872491702")!=null) {
		// String str=vd.dbState.get("FLIT-3442720508872491702").get(0).value[0];
		//
		// System.out.println(str);
		// }

		// if (record.getId().equals("1-2492") || record.getId().equals("1-4897"))
		// System.out.println("PASS");
		scheduleCounter[0] = 0;
		logger.debug("Read:{}:{}", this.validatorID, record.getId());
		participatingEntities.clear();

		getParticipatingEntities(null, participatingEntities, record);
		if ((ValidationParams.USE_DATABASE || ValidationParams.useTreeMap)
				&& record.getActionName().equalsIgnoreCase("scan")) {
			if (ValidationParams.OPTIMIZE_SCAN) {
				sortScanEntities(record);
			}
			getScanEntities(vd, participatingEntities, record);
		}

		// for (ScheduleList sv : currentSS.values())
		// if (debug_hasDup(sv)) {
		// System.out.println("Has Dupl530");
		// System.exit(0);
		// }

		// if (intervalTrees.get(record.getPartitionID()) == null||
		// record.getEndTime() >=
		// intervalTrees.get(record.getPartitionID()).maxStartTime) {
		if (!ValidationParams.USE_KAFKA) {
			readMoreUpdates(vd, record.getEndTime());
		} else {
			// if (intervalTrees.get(record.getPartitionID()) == null||
			// intervalTrees.get(record.getPartitionID()).nextStartTime <=
			// record.getEndTime()) {

			long mStart;
			if (ValidationParams.KAFKA_TIME)
				mStart = System.currentTimeMillis();
			retriveUpdatesFromKafkaBuffered(vd, record, record.getPartitionID());
			if (ValidationParams.KAFKA_TIME)
				pullTime += System.currentTimeMillis() - mStart;

			// }
		}
		// }

		if (!ValidationParams.DO_VALIDATION) {

			vd.updateLogs.intervalList.intervals.clear();

			return;

		}

		HashSet<ScheduleList> schedsTobeMerged = new HashSet<>();
		ScheduleList finalSS = null;
		if (participatingEntities.size() > 1) {
			finalSS = acquireLocksForScheduleLists(record.getId(), vd, null, participatingEntities, schedsTobeMerged);

		} else {
			String key = record.getEntities()[0].getEntityKey();
			if (ValidationParams.GLOBAL_SCHEDULE)
				key = "1";
			finalSS = vd.currentSS.get(key);
			if (finalSS == null) {
				finalSS = new ScheduleList();
				finalSS.initCurrentSS();
				// currentSS.put(record.getEntities()[0].getEntityKey(),
				// finalSS);
				finalSS.participatingKeys.add(key);
				vd.currentSS.put(key, finalSS);

			}

		}

		finalSS.lock();

		HashSet<LogRecord> willBeShrinked = new HashSet<LogRecord>();

		for (int i = 0; i < finalSS.overlapList.size(); i++) {
			if (finalSS.overlapList.get(i).getEndTime() <= record.getStartTime()) {// TODO
				// =
				// or
				// no
				// =
				willBeShrinked.add(finalSS.overlapList.get(i));
			}
		}
		HashSet<LogRecord> willBeShrinked2 = new HashSet<LogRecord>();
		ArrayList<LogRecord> intervals = new ArrayList<>();
		getOverlapingIntervales(finalSS, vd, record, finalSS.endTime, record.getEndTime(), willBeShrinked, intervals);
		int start = 0;
		if (ValidationParams.timePartition) {
			checkFirstRead(vd, record, finalSS);
		}
		do {

			if (!willBeShrinked2.isEmpty()) {
				getOverlapingIntervales(finalSS, vd, record, finalSS.endTime, record.getEndTime(), willBeShrinked2,
						intervals);

				willBeShrinked.addAll(willBeShrinked2);
				willBeShrinked2.clear();

			}
			int preSize = participatingEntities.size();
			for (int i = start; i < intervals.size(); i++) {
				getParticipatingEntities(finalSS, participatingEntities, intervals.get(i));
			}
			if (participatingEntities.size() > preSize) {

				// assert record.getStartTime() == finalSS.endTime : "";
				finalSS = acquireLocksForScheduleLists(record.getId(), vd, finalSS, participatingEntities,
						schedsTobeMerged);

				// assert record.getStartTime() == finalSS.endTime : "";

			}

			if (participatingEntities.size() > preSize) {
				for (int i = 0; i < finalSS.overlapList.size(); i++) {
					if (finalSS.overlapList.get(i).getEndTime() <= record.getStartTime()) {// TODO
						if (!willBeShrinked.contains(finalSS.overlapList.get(i)))
							willBeShrinked2.add(finalSS.overlapList.get(i));
					}
				}
			}

			start = intervals.size();
		} while (!willBeShrinked2.isEmpty());
		finalSS.endTime = record.getStartTime();

		for (LogRecord r : finalSS.overlapList) {
			boolean found = false;
			for (LogRecord i : intervals) {

				// }
				if (i.equals(r)) {
					found = true;
				}
			}
			if (!found) {
				removeFromBucket(vd, r);
				intervals.add(r);
			}
		}

		if (!ValidationParams.hasInitState) {
			for (LogRecord r : finalSS.newLogs) {
				intervals.add(r);
			}
			finalSS.newLogs.clear();
		}

		Collections.sort(intervals);
		ArrayList<LogRecord> overlaping = vd.updateLogs.query(record.getStartTime(), record.getEndTime());

		ArrayList<LogRecord> myRWs = new ArrayList<LogRecord>();

		for (LogRecord r : vd.readWrite) {
			if (record.intersect(r)) {
				myRWs.add(r);
			}
		}
		factUsed = false;

		// System.out.println(this.validatorID+": released the lock");
		vd.unlock();
		combineCurrentSS(record.getId(), vd, finalSS, schedsTobeMerged);
		logger.debug("SSE={}", intervals.size());
		// boolean r= debugCheckInterval(intervals, "0-253");
		// if (r)
		// System.out.println();
		ScheduleList SSe = computeSerialSchedule6(this, scheduleCounter, intervals, finalSS.endTime, record, null,
				vd.notAllowedList, false);
		logger.debug("SSE Schedules={}", SSe.schedules.size());
		if (this.cdseUsed) {
			this.cdseCount++;
		}

		combineSerialSchedules(record.getId(), vd, finalSS, SSe, false);
		shrinkSerialSchedules(vd, finalSS);
		updateCollapsed(vd, finalSS);// Yazxx
		shrinkNotAllowedList(vd, finalSS);
		if (record.getType() == ValidationParams.READ_RECORD) {
			vd.collapsedIntervals.put(record, "");
		}

		boolean generatelogs = true;
		// if (ValidationMain.readLogsCount >= 700)
		// System.out.println();
		if (ValidationParams.hasInitState) {
			generatelogs = false;
		}
		assert record.getStartTime() == finalSS.endTime : "";
		validate(vd, record, generatelogs, finalSS, overlaping, myRWs);

	}

	private boolean debugCheckOverlap(String string, ArrayList<LogRecord> overlapList) {
		for (LogRecord log : overlapList) {
			if (log.getId().equals(string))
				return true;
		}
		return false;
	}

	private void checkFirstRead(ValidatorData vd, LogRecord record, ScheduleList finalSS) {
		LogRecord newlog = null;
		// if (vd.partitionCounter != 0 || partitionTimeIndex != 0)
		newlog = vd.checkFirstRead(record, this);
		if (newlog != null) {
			finalSS.newLogs.add(newlog);
		}

	}

	private void sortScanEntities(LogRecord record) {
		boolean sorted = true;
		TreeType prevobj = null;
		ArrayList<TreeType> treeElments = new ArrayList<>();
		HashMap<String, Entity> entities = new HashMap<>();
		for (Entity e : record.getEntities()) {
			try {
				entities.put(e.getKey(), e);
				TreeType myobj = ValidationParams.treeMapObj.getClass().newInstance().convert(e.getKey());
				treeElments.add(myobj);
				if (prevobj != null && prevobj.compareTo(myobj) > 0) {
					sorted = false;
				}
				prevobj = myobj;
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		if (sorted)
			return;
		if (!sorted) {
			System.out.println("This test assumes elemnts are sorted");
			System.exit(0);
		}
		Collections.sort(treeElments);
		for (int i = 0; i < treeElments.size(); i++) {
			TreeType e = treeElments.get(i);
			String key = e.getStringValue();
			Entity entity = entities.get(key);
			record.getEntities()[i] = entity;
		}

	}

	private void getScanEntities(ValidatorData vd, HashSet<String> participatingEntities2, LogRecord record) {
		String tokens[] = record.getQueryParams().split(":");

		if (ValidationParams.OLTP_SCAN) {
			String upper = tokens[0];
			String lower = tokens[1];
			if (ValidationParams.integerScan && !ValidationParams.scanHoles) {
				int lowerBound = Integer.parseInt(lower);
				int upperBound = Integer.parseInt(upper);

				for (int i = lowerBound + 1; i < upperBound; i++) {
					participatingEntities.add(ValidationParams.ENTITY_NAMES[0] + ValidationParams.KEY_SEPERATOR + i);
				}
			} else {
				// check common
				NavigableMap<TreeType, DBState> res = vd.commonTree.searchAll(lower, false, upper, false,
						ValidationParams.treeMapObj);
				for (TreeType k : res.keySet()) {
					participatingEntities.add(
							ValidationParams.ENTITY_NAMES[0] + ValidationParams.KEY_SEPERATOR + k.getStringValue());

				}
			}

		} else {
			String lowert = tokens[0];
			int limit = Integer.parseInt(tokens[1]);
			if (ValidationParams.integerScan && !ValidationParams.scanHoles) {
				int lowerBound = Integer.parseInt(lowert);
				for (int i = lowerBound; i < lowerBound + limit; i++) {
					participatingEntities.add(ValidationParams.ENTITY_NAMES[0] + ValidationParams.KEY_SEPERATOR + i);
				}

			} else {

				// check common
				NavigableMap<TreeType, DBState> res = vd.commonTree.searchAll(lowert, true, null, false,
						ValidationParams.treeMapObj);
				int count = 0;
				for (Entry<TreeType, DBState> e : res.entrySet()) {
					boolean insert = isScanRelated(record, e.getKey(), limit);
					count++;
					if (e.getValue().getValue() == Validator.deletedArr) {
						count--;
					}
					if (count > limit)
						break;
					if (insert)
						participatingEntities.add(ValidationParams.ENTITY_NAMES[0] + ValidationParams.KEY_SEPERATOR
								+ e.getKey().getStringValue());

				}
			}
		}

	}

	public static boolean isScanRelated(LogRecord record, TreeType key, int limit) {
		if (!ValidationParams.OPTIMIZE_SCAN)
			return true;
		if (record.getEntities().length == limit) {
			TreeType lastEntity = null;
			try {
				lastEntity = ValidationParams.treeMapObj.getClass().newInstance();
				lastEntity.convert(record.getEntities()[record.getEntities().length - 1].key);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (key.compareTo(lastEntity) > 0)
				return false;
		}
		return true;
	}

	@SuppressWarnings("unused")
	private boolean debugCheckInterval(ArrayList<LogRecord> intervals, String string) {
		for (LogRecord i : intervals) {
			if (i.getId().equals(string)) {
				return true;
			}

		}
		return false;
	}

	private void updateCollapsed(ValidatorData vd, ScheduleList finalSS) {
		for (Schedule s : finalSS.schedules) {
			for (LogRecord r : s.getRecords()) {
				vd.collapsedIntervals.remove(r);

			}

		}

	}

	@SuppressWarnings("unused")
	private static void debugCheckImpacted(ScheduleList finalSS) {
		for (Schedule s1 : finalSS.schedules) {
			for (String key : s1.getImpactedStates().keySet()) {
				for (Schedule s2 : finalSS.schedules) {
					if (s1 == s2)
						continue;
					if (!s2.getImpactedStates().containsKey(key)) {
						System.out.println("found");
						System.exit(0);
					}
				}
			}
		}

	}

	@SuppressWarnings("unused")
	private void fixWillBeShrinked(ScheduleList finalSS, ScheduleList finalSSNew, HashSet<LogRecord> willBeShrinked) {

		for (LogRecord r : finalSS.overlapList) {
			if (!finalSSNew.overlapList.contains(r))
				willBeShrinked.remove(r);
		}
	}

	@SuppressWarnings("unused")
	private boolean debug_Search(ArrayList<LogRecord> intervals, String string) {
		for (LogRecord r : intervals) {
			if (r.getId().equals(string))
				return true;
		}
		return false;
	}

	private void retriveUpdatesFromKafkaBuffered(ValidatorData vd, LogRecord read, int readIndex)
			throws UnorderedWritesException {

		boolean maxTries = false;

		Buffer bw = vd.bufferedWrites;
		long lastUpdateOffset = bw.getOffset();
		KafkaConsumer<String, String> consumerU = vd.consumerUpdate;
		getUpdatesFromBuffer(vd, read, lastUpdateOffset);
		boolean done = false;
		if (ValidationParams.useBuffer == false) {
			LogRecord r = bw.viewLog(0);

			if (r != null && read != null && r.getStartTime() > read.getEndTime())
				done = true;
		}
		while (!done && bw.canInsertBuffer() && (!bw.noLogs) && !maxTries) {

			maxTries = fillBuffer(validatorID, bw, readIndex, consumerU, 'U', read, vd);

			getUpdatesFromBuffer(vd, read, lastUpdateOffset);

		} // while

		if (lastUpdateOffset != bw.getOffset())
			consumerU.seek(vd.updateTopicPartitions, bw.getOffset() + 1);

	}

	private void getUpdatesFromBuffer(ValidatorData vd, LogRecord read, long lastUpdateOffsetEnd)
			throws UnorderedWritesException {
		long endTime = -1;
		if (read != null)
			endTime = read.getEndTime();
		Buffer bw = vd.bufferedWrites;
		bw.sort();
		while (!bw.isEmpty()) {
			LogRecord r = bw.viewLog(0);

			if (read != null && r.getStartTime() > endTime)
				break;

			r = bw.getFirst();

			long offset = lastUpdateOffsetEnd;
			if (offset == -1) {
				offset = vd.consumerUpdate.position(vd.updateTopicPartitions);
			}
			vd.updateLogs.addIntervalSortedFromLast(r, offset);
			Long readet = vd.readEndTime;

			if (r.getStartTime() < readet) {
				// System.out.println(validatorID + ": Found earlier Update.
				// Previous read endtime=" + readet
				// + ", currentUpdateTime=" + r.getStartTime());
				throw new UnorderedWritesException(r);
			}

			vd.writeLogsCount++;
			if (read == null && r.getType() == ValidationParams.READ_WRITE_RECORD) {
				break;
			}

		}

	}

	private void combineCurrentSS(String readId, ValidatorData vd, ScheduleList finalSS,
			HashSet<ScheduleList> scheToBeMerges) {
		for (ScheduleList s : scheToBeMerges) {

			mergeSS(readId, vd, finalSS, s);
			// s.unlock();
		}

	}

	private ScheduleList acquireLocksForScheduleLists(String readId, ValidatorData vd, ScheduleList oldFinalSS,
			HashSet<String> participatingEntities, HashSet<ScheduleList> scheToBeMerged) {

		ScheduleList finalSS = null;
		if (oldFinalSS == null) {
			finalSS = new ScheduleList();
			finalSS.initCurrentSS();
		} else {
			finalSS = oldFinalSS;
		}

		Iterator<String> it = participatingEntities.iterator();
		while (it.hasNext()) {
			String key = it.next();
			ScheduleList sl = vd.currentSS.get(key);
			if (sl == null)
				continue;
			sl.lock();
			for (String key2 : sl.participatingKeys) {
				if (!key2.equals(key) && participatingEntities.contains(key2)) {
					it.remove();
					break;
				}
			}
			sl.unlock();

		}

		if (participatingEntities.size() == 0) {
			return finalSS;
		}
		if (participatingEntities.size() == 1 && oldFinalSS == null) {
			return vd.currentSS.get(participatingEntities.iterator().next());
		}
		for (String key : participatingEntities) {
			ScheduleList s = vd.currentSS.get(key);
			if (finalSS == s)
				continue;
			if (s == null) {
				finalSS.participatingKeys.add(key);
				vd.currentSS.put(key, finalSS);
				continue;
			}

			s.lock();
			scheToBeMerged.add(s);
			finalSS.newLogs.addAll(s.newLogs);
			finalSS.overlapList.addAll(s.overlapList);
			finalSS.endTime = Math.max(finalSS.endTime, s.endTime);
			finalSS.participatingKeys.addAll(s.participatingKeys);
			for (String keyS : s.participatingKeys) {
				vd.currentSS.put(keyS, finalSS);
			}
			s.unlock();

		}
		combineCurrentSS(readId, vd, finalSS, scheToBeMerged);
		scheToBeMerged.clear();

		return finalSS;
	}
	// private static boolean debug_checkRefCount(ScheduleList s) {
	// for (Schedule schedule : s.schedules) {
	// for (Entry<String, DBState> e : schedule.getImpactedStates().entrySet())
	// {
	// LinkedList<DBState> ll = dbState.get(e.getKey());
	// int sum = 0;
	//
	// for (DBState st : ll) {
	// sum = sum + st.refrenceCount;
	// }
	// if (sum != s.schedules.size())
	// return false;
	// }
	// }
	// return true;
	// }

	private void mergeSS(String readId, ValidatorData vd, ScheduleList finalSS, ScheduleList s) {

		ArrayList<Schedule> toBeAdded = new ArrayList<Schedule>();

		if (finalSS.schedules.size() == 0) {
			// if (s.schedules.size() != 0) {
			finalSS.schedules.addAll(s.schedules);
			// finalSS.ReadWrite.addAll(s.ReadWrite);
			finalSS.notShrink.addAll(s.notShrink);

			// finalSS.participatingKeys.addAll(s.participatingKeys);
			// finalSS = s;
			// }
		} else {
			for (int i = 0; i < finalSS.schedules.size(); i++) {
				Schedule f1 = finalSS.schedules.get(i);
				for (int j = 0; j < s.schedules.size(); j++) {
					Schedule f2 = s.schedules.get(j);
					// 1- records
					if (j + 1 == s.schedules.size()) {
						f1.getRecords().addAll(f2.getRecords());
						for (Entry<String, DBState> e : f2.getImpactedStates().entrySet()) {
							f1.getImpactedStates().put(e.getKey(), e.getValue());
							if (ValidationParams.useTreeMap)
								f1.tree.insert(Entity.getEntityPK(e.getKey()), e.getValue(),
										ValidationParams.treeMapObj);

						}
						if (i + 1 != finalSS.schedules.size()) {
							for (DBState st : f2.getImpactedStates().values()) {
								st.increment();
							}
						}

					} else if (i + 1 == finalSS.schedules.size()) {
						Schedule temp = new Schedule(readId, this.scheduleCounter);
						temp.getRecords().addAll(f1.getRecords());
						temp.getRecords().addAll(f2.getRecords());
						toBeAdded.add(temp);
						for (Entry<String, DBState> e : f1.getImpactedStates().entrySet()) {
							temp.getImpactedStates().put(e.getKey(), e.getValue());
							if (ValidationParams.useTreeMap)
								temp.tree.insert(Entity.getEntityPK(e.getKey()), e.getValue(),
										ValidationParams.treeMapObj);

						}
						for (Entry<String, DBState> e : f2.getImpactedStates().entrySet()) {
							temp.getImpactedStates().put(e.getKey(), e.getValue());
							if (ValidationParams.useTreeMap)
								temp.tree.insert(Entity.getEntityPK(e.getKey()), e.getValue(),
										ValidationParams.treeMapObj);

						}

						for (DBState st : f1.getImpactedStates().values()) {

							st.increment();
						}
					} else {
						Schedule temp = new Schedule(readId, this.scheduleCounter);
						temp.getRecords().addAll(f1.getRecords());
						temp.getRecords().addAll(f2.getRecords());
						toBeAdded.add(temp);
						for (Entry<String, DBState> e : f1.getImpactedStates().entrySet()) {
							temp.getImpactedStates().put(e.getKey(), e.getValue());
							if (ValidationParams.useTreeMap)
								temp.tree.insert(Entity.getEntityPK(e.getKey()), e.getValue(),
										ValidationParams.treeMapObj);

						}
						for (Entry<String, DBState> e : f2.getImpactedStates().entrySet()) {
							temp.getImpactedStates().put(e.getKey(), e.getValue());
							if (ValidationParams.useTreeMap)
								temp.tree.insert(Entity.getEntityPK(e.getKey()), e.getValue(),
										ValidationParams.treeMapObj);

						}
						for (DBState st : f2.getImpactedStates().values()) {
							st.increment();
						}
						for (DBState st : f1.getImpactedStates().values()) {

							st.increment();
						}
					}

				}

			}

			// finalSS.ReadWrite.addAll(s.ReadWrite);
			finalSS.notShrink.addAll(s.notShrink);
			finalSS.schedules.addAll(toBeAdded);
			// if (debug_hasDup(finalSS)) {
			// debug_hasDup(finalSS);
			// System.out.println("Has Dupl818");
			// System.exit(0);
			// }
			// if (!debugcheckRefCount(finalSS)) {
			// System.out.println(" final RefCount not valid:"+readLogsCount);
			// System.exit(0);
			// }
		}

	}

	private void getParticipatingEntities(ScheduleList finalSS, HashSet<String> participatingEntities,
			LogRecord record) {
		if (ValidationParams.GLOBAL_SCHEDULE) {
			participatingEntities.add("1");
			return;
		}
		// if ((ValidationParams.USE_DATABASE || ValidationParams.useTreeMap)&&
		// record.getActionName().equalsIgnoreCase("scan")) {
		// String[] tokens = record.getQueryParams().split(":");
		// int upperBound = Integer.parseInt(tokens[0]);
		// int lowerBound = Integer.parseInt(tokens[1]);
		// for (int i = lowerBound + 1; i < upperBound; i++) {
		// participatingEntities.add(ValidationParams.ENTITY_NAMES[0] +
		// ValidationParams.KEY_SEPERATOR + i);
		// }
		//
		// } else {
		for (Entity e : record.getEntities()) {
			if (finalSS == null || !finalSS.participatingKeys.contains(e.getEntityKey()))
				participatingEntities.add(e.getEntityKey());
		}
		// }
	}

	private void validate(ValidatorData vd, LogRecord record, boolean generateRecords, ScheduleList finalSS,
			ArrayList<LogRecord> overlaping, ArrayList<LogRecord> myRWs) {

		// ===================================
		Iterator<LogRecord> it = overlaping.iterator();
		while (it.hasNext()) {
			LogRecord i = it.next();
			if (!i.intersect(record)) {
				if (!intersectWith(myRWs, i)) {
					it.remove();
				}
			}
		}
		for (LogRecord r : myRWs) {
			getFromBucketWithoutDelete(vd, r, overlaping);
			overlaping.add(r);
		}
		// ===================================
		for (LogRecord r : finalSS.overlapList) {
			boolean found = false;
			for (LogRecord i : overlaping) {
				if (i.equals(r)) {
					found = true;
				}
			}
			if (!found) {
				overlaping.add(r);
			}
		}
		// ==================================

		if (!ValidationParams.hasInitState) {
			for (LogRecord r : finalSS.newLogs) {
				overlaping.add(r);
			}
		}
		Collections.sort(overlaping);// TODO: remove since we don't care about
		// order.
		// remove duplicates
		for (int i = overlaping.size() - 1; i > 0; i--) {
			if (overlaping.get(i).getId().equals(overlaping.get(i - 1).getId())) {
				overlaping.remove(i);
			}
		}

		boolean readHasOverlapping[] = { false };
		logger.debug("ssi={}", overlaping.size());

		ScheduleList SSi = computeSerialSchedule6(this, scheduleCounter, overlaping, record.getEndTime(), record,
				readHasOverlapping, vd.notAllowedList, true);

		logger.debug("SSI Sched={}", SSi.schedules.size());
		if (generateRecords && factUsed)
			factorialCount++;
		if (readHasOverlapping[0] && generateRecords)
			this.readOverLappingCount++;

		boolean validateOnce = false;
		boolean valid = false;
		long[] updateTime = { 0L };
		if (ValidationParams.COMBINE_AND_VALIDATE) {

			// --------------Validate DTs-----------------------------------
			HashSet<ArrayList<Schedule>> newSS = new HashSet<ArrayList<Schedule>>();
			ArrayList<LogRecord> beforeReadLogs = new ArrayList<LogRecord>();
			HashMap<Schedule, HashMap<String, String>> expectedValues = new HashMap<Schedule, HashMap<String, String>>();

			boolean scan = false;
			boolean hasGeneratedState[] = { false };
			if ((ValidationParams.USE_DATABASE || ValidationParams.useTreeMap)
					&& record.getActionName().equalsIgnoreCase("scan")) {
				scan = true;
				// System.out.println(record.getId());
				// System.out.println("Query params:"+record.getQueryParams());
				// System.out.println("num Schedules:"+validationSS.schedules.size());
			}

			// -----------------------------------------------------

			valid = combineAndValidate(record, vd, finalSS, SSi, this, generateRecords, readHasOverlapping[0], scan,
					newSS, expectedValues, beforeReadLogs, valid, hasGeneratedState, updateTime);

			validateOnce = afterValidation(valid, generateRecords, vd, record, expectedValues, readHasOverlapping[0],
					beforeReadLogs, finalSS, this, newSS);

		} else {
			ScheduleList ValidationSS = combineSerialSchedules(record.getId(), vd, finalSS, SSi, true);
			boolean result[] = validateReadWithSerialSchedule(this, vd, ValidationSS, record, finalSS, generateRecords,
					readHasOverlapping[0], finalSS.schedules.size(), updateTime);
			validateOnce = result[0];
			valid = result[1];
		}

		if (ValidationParams.YAZ_FIX) {
			for (Entry<String, ArrayList<String>> ent : updateNotAllowed.entrySet()) {
				// ArrayList<String> arr = updateNotAllowed.get(list);
				String key = ent.getKey();
				Map<String, Boolean> listB = vd.notAllowedList.get(key + "Before");
				Map<String, Boolean> listA = vd.notAllowedList.get(key + "After");

				for (String s : ent.getValue()) {
					listA.remove(s);
					listB.remove(s);

				}

			}
			updateNotAllowed.clear();
		}

		if (!validateOnce) {
			validate(vd, record, false, finalSS, overlaping, myRWs);
			return;
		}
		// assert !readHasOverlapping[0];
		if (ValidationParams.COMPUTE_FRESHNESS) {
			if (readHasOverlapping[0]) {
				// discardCount++;
			}
			if (!readHasOverlapping[0]) {

				if (updateTime[0] > record.getStartTime()) {
					updateTime[0] = record.getStartTime();
					if (!valid)
						discardCount++;
				}

				if (updateTime[0] != 0 /* && record.getStartTime() >= updateTime[0] */) {
					double endTime;
					long timeMillis = (record.getStartTime() - updateTime[0]) / Bucket.NANO_TO_MILLIS;
					assert timeMillis >= 0 : "Read:" + record.getId() + ", read start:" + record.getStartTime()
							+ " update=" + updateTime[0];
					long key = timeMillis / Bucket.bucketDuration;
					endTime = (key + 1) * Bucket.bucketDuration;
					if (key > Bucket.maxBuckets - 1) {
						key = Bucket.maxBuckets - 1;
						endTime = Double.POSITIVE_INFINITY;
					}
					assert key >= 0;
					Bucket bucket = vd.freshnessBuckets.get(key);
					if (bucket == null) {
						bucket = new Bucket(key, key * Bucket.bucketDuration, endTime);
						vd.freshnessBuckets.put(key, bucket);
					}

					if (valid) {
						bucket.incValidReads();
					} else {
						bucket.incStaleReads();

					}
				}
			}
		}
		finalSS.shrinkImpacted(vd);

		// moveWritesToBucket(vd, finalSS);

		// for (String key : finalSS.participatingKeys) {
		//
		// vd.currentSS.put(key, finalSS);
		// }
		split(finalSS, vd);
		finalSS.unlock();

	}

	private void split(ScheduleList finalSS, ValidatorData vd) {
		if (finalSS.participatingKeys.size() == 1)
			return;

		Iterator<String> it = finalSS.participatingKeys.iterator();
		while (it.hasNext()) {
			if (finalSS.participatingKeys.size() == 1)
				return;
			String key = it.next();
			if (vd.dbState.get(key) != null && vd.dbState.get(key).size() > 1) {
				continue;
			}
			boolean existImpacted = finalSS.existInImpacted(key);
			if (existImpacted) {
				continue;
			}
			ScheduleList newSS = new ScheduleList();
			newSS.initCurrentSS();
			newSS.participatingKeys.add(key);
			newSS.endTime = finalSS.endTime;
			boolean split = ScheduleList.checkList(key, finalSS.overlapList, newSS.overlapList);
			if (!split) {
				finalSS.overlapList.addAll(newSS.overlapList);
				continue;
			}
			split = ScheduleList.checkList(key, finalSS.newLogs, newSS.newLogs);
			if (!split) {
				finalSS.overlapList.addAll(newSS.overlapList);
				finalSS.newLogs.addAll(newSS.newLogs);
				continue;
			}
			ArrayList<LogRecord> listtemp = null;
			if (finalSS.schedules.size() > 1) { // Added to fix a bug
				split = false;

			} else {
				for (Schedule s : finalSS.schedules) {

					split = ScheduleList.checkList(key, s.getRecords(), listtemp);
					if (!split)
						break;
					// if (!listtemp.isEmpty()) {
					// Schedule s2 = new Schedule();
					// s2.getRecords().addAll(listtemp);
					// newSS.schedules.add(s2);
					// listtemp.clear();
					// }

				}
			}
			if (!split) {
				finalSS.overlapList.addAll(newSS.overlapList);
				finalSS.newLogs.addAll(newSS.newLogs);
				continue;
			}
			it.remove();
			vd.currentSS.put(key, newSS);

		}

	}

	private boolean combineAndValidate(LogRecord record, ValidatorData vd, ScheduleList s1, ScheduleList s2,
			Validator validator, boolean generateRecords, boolean readHasOver, boolean scan,
			HashSet<ArrayList<Schedule>> newSS, HashMap<Schedule, HashMap<String, String>> expectedValues,
			ArrayList<LogRecord> beforeReadLogs, boolean valid, boolean[] hasGeneratedState, long[] updateTime) {

		logger.debug("Start combine and validate s1 org size={}", s1.schedules.size());

		ScheduleList s1temp = new ScheduleList();
		// ScheduleList s3 = new ScheduleList();

		ArrayList<boolean[]> isOverlapShrinked = duplicateWithoutOverlap(record.getId(), vd, s1, s1temp, true);

		int parentsSize = s1temp.schedules.size();

		if (s2.isEmpty()) {
			logger.debug("ssi combine={}*{}", s1temp.schedules.size(), 1);

			for (Schedule x1 : s1temp.schedules) {
				boolean readMatch = validateSchedule(x1, record, scan, hasGeneratedState, expectedValues, vd, valid,
						generateRecords, newSS, beforeReadLogs, parentsSize, updateTime, readHasOver);
				if (readMatch)
					valid = true;
			}
		} else if (s1temp.isEmpty()) {
			logger.debug("ssi combine={}*{}", s2.schedules.size(), 1);

			for (Schedule x2 : s2.schedules) {

				boolean readMatch = validateSchedule(x2, record, scan, hasGeneratedState, expectedValues, vd, valid,
						generateRecords, newSS, beforeReadLogs, parentsSize, updateTime, readHasOver);

				if (readMatch)
					valid = true;

			}
		} else {
			int s1Size = s1temp.schedules.size();
			int s2Size = s2.schedules.size();
			logger.debug("ssi combine={}*{}", s1Size, s2Size);

			for (int i = 0; i < s1Size; i++) {
				Schedule x1 = s1temp.schedules.get(i);
				boolean lastOne = false;
				for (int j = 0; j < s2Size; j++) {
					Schedule x2 = s2.schedules.get(j);
					if (j + 1 == s2Size)
						lastOne = true; // YAZXX
					Schedule y = new Schedule(record.getId(), scheduleCounter, vd.dbState, x1, x2, s1temp.overlapList,
							isOverlapShrinked.get(i), s1Size, true, lastOne, s1.endTime);
					y.setParent(x1.getParents());

					boolean readMatch = validateSchedule(y, record, scan, hasGeneratedState, expectedValues, vd, valid,
							generateRecords, newSS, beforeReadLogs, parentsSize, updateTime, readHasOver);

					if (readMatch)
						valid = true;
				}
			}
		}
		logger.debug("Done SSI combine");
		isOverlapShrinked.clear();
		isOverlapShrinked = null;
		return valid;

	}

	private boolean afterValidation(boolean valid, boolean generateRecords, ValidatorData vd, LogRecord record,
			HashMap<Schedule, HashMap<String, String>> expectedValues, boolean readHasOverlapping,
			ArrayList<LogRecord> beforeReadLogs, ScheduleList finalSS, Validator v,
			HashSet<ArrayList<Schedule>> newSS) {
		if (!ValidationParams.hasInitState) {
			if (!generateRecords || ValidationParams.timePartition) // YAZXX
				generatedEntities.clear();

			if (valid && !generatedEntities.isEmpty()) {
				Entity[] entities = new Entity[generatedEntities.size()];
				entities = generatedEntities.toArray(entities);
				for (int i = 0; i < entities.length; i++) {
					Property[] props = entities[i].getProperties();
					EntitySpec entitySp = (EntitySpec) entities[i];
					props = new Property[entitySp.getPropertiesArrayLis().size()];
					props = entitySp.getPropertiesArrayLis().toArray(props);
					entities[i].setProperties(props);
					entitySp.getPropertiesArrayLis().clear();
					entitySp.setPropertiesArrayLis(null);

				}

				if (readHasOverlapping) {
					createLogRecordForRead(finalSS, record, entities, v);
					generatedEntities.clear();
					return false;
				} else {
					createDBStateEntities(vd, entities);
					generatedEntities.clear();

					// return true;
				}

			}
		}
		if (!valid) {
			if (record.getActionName().equalsIgnoreCase("scan")) {
				System.out.println("Anomalous Scan:" + record.getId());
				// System.exit(0);
			}
			totalStaleCount++;
			vd.staleCount.incrementAndGet();
			int rPartition = 0;
			int uPartition = 0;
			if (ValidationParams.USE_KAFKA) {
				rPartition = vd.readTopicPartitions.partition();
				uPartition = vd.updateTopicPartitions.partition();
			}
			String staleRecord = Utilities.getStaleLogString(record.getType(), record.getId(), record.getOffset(),
					rPartition, vd.bufferedReads.getOffset(), uPartition, vd.bufferedWrites.getOffset(),
					record.getActionName(), expectedValues);

			staleRecords.add(staleRecord);

			System.out.println(validatorID + ":" + totalReadLogsCount + "- Anomalous Read (no. " + totalStaleCount
					+ ") - ID: " + record.getId());

			for (Schedule s : expectedValues.keySet()) {
				HashMap<String, String> values = expectedValues.get(s);
				if (ValidationParams.PRINT_EXPECTED) {
					for (Entity e : record.getEntities()) {
						for (Property p : e.getProperties()) {
							// for (String key : values.keySet()) {
							String key = p.getProprtyKey(e);
							if (values.get(key) != null)
								System.out.println(key + ": Log Value (" + p.getValue() + ") "
										+ (p.getValue().equals(values.get(key)) ? "==" : "!=") + " Expected ("
										+ values.get(key) + ")");
						}
					}
				}
			}

			String log = logDir;
			if (ValidationParams.USE_KAFKA)
				log = kafkaLogDir;
			if (ValidationParams.ANOMALY_EXIT)
				System.exit(0); // TODO Debug point
		} else { // read is valid

			for (LogRecord r : beforeReadLogs) {
				if (record.getEndTime() < r.getEndTime())
					r.setEndTime(record.getEndTime());
			}

			// if (readLogsCount % numToDivideBy == 0)
			// System.out.println(readLogsCount + "- Read is successful:" +
			// record.getId());
			finalSS.validSchedules(vd, newSS, record.getStartTime(), this.discardedSchIdList);
		}

		updateDatabase(finalSS, vd);
		return true;

	}

	private boolean validateSchedule(Schedule s, LogRecord record, boolean scan, boolean[] hasGeneratedState,
			HashMap<Schedule, HashMap<String, String>> expectedValues, ValidatorData vd, boolean valid,
			boolean generateRecords, HashSet<ArrayList<Schedule>> newSS, ArrayList<LogRecord> beforeReadLogs,
			int parentsSize, long[] updateTime, boolean readHasOver) {
		// if (!readHasOver)
		// assert s.getRecords().size()==1;
		// System.out.println(s.getRecords());
		HashMap<String, String> hm = new HashMap<String, String>();
		expectedValues.put(s, hm);
		boolean readMatch = false;
		long readMatchUpdateTime[] = { 0L };
		if (scan) {
			readMatch = database.processScan(vd, record, generateRecords, hm, generatedEntities, s, hasGeneratedState,
					readMatchUpdateTime);

		} else {
			readMatch = s.isReadMatch(vd.dbState, record, generateRecords, hm, generatedEntities, hasGeneratedState,
					readMatchUpdateTime);

		}
		assert readMatchUpdateTime[0] >= 0;
		if (readMatch) {
			if (!valid) {
				updateTime[0] = readMatchUpdateTime[0];
			} else {
				updateTime[0] = Math.max(updateTime[0], readMatchUpdateTime[0]);
			}
			processValidSchedule(vd, s, !valid, beforeReadLogs, record, newSS, parentsSize);
			valid = true;
		} else { // read not match
			if (!valid)
				updateTime[0] = Math.max(updateTime[0], readMatchUpdateTime[0]);

		}

		return readMatch;

	}

	@SuppressWarnings("unused")
	private void filterSchedulesForRead(ScheduleList ssi) {
		HashSet<String> existingSched = new HashSet<String>();
		for (int i = ssi.schedules.size() - 1; i >= 0; i--) {
			Schedule s = ssi.schedules.get(i);
			String logIds = "";
			long previousStart = 0;
			for (LogRecord record : s.getRecords()) {
				assert previousStart <= record.getEndTime();
				if (record.getStartTime() > previousStart)
					previousStart = record.getStartTime();
				logIds += "-" + record.getId();
				if (record.getType() == ValidationParams.READ_RECORD) {
					if (existingSched.contains(logIds)) {
						ssi.schedules.remove(i);
					} else {
						existingSched.add(logIds);
					}
					break;
				}
			}
		}

	}

	private void getFromBucketWithoutDelete(ValidatorData vd, LogRecord record, ArrayList<LogRecord> intervals) {
		for (Entity e : record.getEntities()) {
			for (Property p : e.getProperties()) {
				if (p == null)
					continue;
				String pKey = Property.getProprtyKey(e, p);
				if (!vd.bucket.get(e.name).containsKey(pKey)) {
					continue;
				} else {
					ArrayList<LogRecord> temp = vd.bucket.get(e.name).get(pKey);
					for (int i = temp.size() - 1; i >= 0; i--) {
						if (temp.get(i).getStartTime() <= record.getEndTime()) {
							intervals.add(temp.get(i));
						}
					}
				}
			}
		}
	}

	private static boolean intersectWith(ArrayList<LogRecord> myRWs, LogRecord i) {
		for (LogRecord r : myRWs) {
			if (i.getStartTime() <= r.getEndTime() && r.intersect(i)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unused")
	private void moveWritesToBucket(ValidatorData vd, ScheduleList current) {

		if (current.schedules.size() <= 1) {
			return;

		}

		HashSet<String> entityKeys = new HashSet<String>();
		for (int x = current.notShrink.size() - 1; x >= 0; x--) {
			String key = current.notShrink.get(x);

			String ename = key.substring(0, key.indexOf(ValidationParams.KEY_SEPERATOR));
			String ekey = key.substring(key.indexOf(ValidationParams.KEY_SEPERATOR) + 1,
					key.lastIndexOf(ValidationParams.KEY_SEPERATOR));
			String pname = key.substring(key.lastIndexOf(ValidationParams.KEY_SEPERATOR) + 1);
			String dbStateKey = key.substring(0, key.lastIndexOf(ValidationParams.KEY_SEPERATOR));
			if (entityKeys.contains(dbStateKey))
				continue;
			entityKeys.add(dbStateKey);
			boolean stop = false;
			for (int iR = 0; iR < current.overlapList.size(); iR++) {
				for (LogRecord r : current.overlapList) {
					for (Entity e : r.getEntities()) {
						if (dbStateKey.equals(e.getEntityKey())) {
							stop = true;
							break;
						}
					}
					if (stop)
						break;
				}
			}
			if (stop)
				continue;

			// check diff count
			List<DBState> ll = vd.dbState.get(dbStateKey);
			DBState first = ll.get(0);
			boolean[] isDifferent = new boolean[first.getValue().length];
			int diffTrueCount = 0;
			for (int v = 0; v < first.value.length; v++) {
				for (int i = 0; i < ll.size() - 1; i++) {
					for (int j = i + 1; j < ll.size(); j++) {
						if (ll.get(i).getValue().length - 1 < v || ll.get(j).getValue().length - 1 < v) {
							// YAZXX
							isDifferent[v] = true;
							diffTrueCount++;
							break;

						}
						if (!ValidationParams.hasInitState) {
							if (ll.get(i).getValue()[v] == null && ll.get(j).getValue()[v] == null)
								continue;
						}
						if (!ll.get(i).getValue()[v].equals(ll.get(j).getValue()[v])) {
							isDifferent[v] = true;
							diffTrueCount++;
							break;
						}
					}
					if (isDifferent[v]) {
						break;
					}

				}
			}
			if (diffTrueCount == 0) {
				current.notShrink.remove(x);
				continue;
			}

			// HashSet<String> firstCreatedProps= new HashSet<String>();
			assert diffTrueCount >= 1 && diffTrueCount <= first.getValue().length : "Diff count " + diffTrueCount;
			if (diffTrueCount != 1) {
				continue;
			}
			current.notShrink.remove(x);
			for (Schedule s : current.schedules) {
				if (s.getImpactedStates().get(dbStateKey) == null)
					continue;
				first = vd.dbState.get(dbStateKey).get(0);
				DBState oldState = s.getImpactedStates().get(dbStateKey);
				oldState.decrement();
				// first.increment();
				s.getImpactedStates().remove(dbStateKey);
				if (ValidationParams.useTreeMap)
					s.tree.remove(Entity.getEntityPK(dbStateKey), ValidationParams.treeMapObj);

			}
			addLogRecordsToBucket(vd, dbStateKey, ename, ekey, isDifferent, diffTrueCount);
		} // end key for

	}

	private void addLogRecordsToBucket(ValidatorData vd, String dbStateKey, String ename, String ekey,
			boolean[] isDifferent, int diffTrueCount) {
		int entityIndex = -1;
		for (int i = 0; i < ValidationParams.ENTITY_NAMES.length; i++) {
			if (ename.equals(ValidationParams.ENTITY_NAMES[i])) {
				entityIndex = i;
				break;
			}
		}
		List<DBState> ll = vd.dbState.get(dbStateKey);

		for (int i = ll.size() - 1; i >= 0; i--) {
			DBState state = ll.get(i);

			createBucketLog(vd, ename, ekey, state, entityIndex, isDifferent, diffTrueCount);

			assert state.getRefrenceCount() == 0 : "Ref count not Zero:" + state.getRefrenceCount();
			if (i != 0)
				vd.dbState.get(dbStateKey).remove(state);

		} // end linkedlist loop

		assert vd.dbState.get(dbStateKey).size() == 1 : "MORE THAN ONE STATE (" + vd.dbState.get(dbStateKey).size()
				+ ")";

	}

	private void createBucketLog(ValidatorData vd, String ename, String ekey, DBState state, int entityIndex,
			boolean[] isDifferent, int diffTrueCount) {
		Property[] properties = new Property[diffTrueCount];
		int index = 0;
		for (int i = 0; i < ValidationParams.ENTITY_PROPERTIES[entityIndex].length; i++) {
			if (isDifferent[i])
				properties[index++] = new Property(ValidationParams.ENTITY_PROPERTIES[entityIndex][i],
						state.getValue()[i], ValidationParams.NEW_VALUE_UPDATE);
		}
		Entity e = new Entity(ekey, ename, properties);
		Entity[] entities = { e };
		String id = "B" + ValidationParams.KEY_SEPERATOR + seq;
		seq++;
		LogRecord r = new LogRecord(id, ValidationParams.BUCKET_ACTION, 1, 2, ValidationParams.UPDATE_RECORD, entities);

		addToBucket(vd, r, new HashMap<String, TransactionOverlap>(), false);
	}

	// private static void debug_bucket() {
	// sizes.clear();
	// for (String key : bucket.keySet()) {
	// int i = bucket.get(key).size();
	// if (sizes.get(i) == null)
	// sizes.put(i, 1);
	// else
	// sizes.put(i, sizes.get(i) + 1);
	// }
	// for (int key : sizes.keySet()) {
	// System.out.print(sizes.get(key) + " has " + key + " element, ");
	// }
	// System.out.println();
	// }

	private void readMoreUpdates(ValidatorData vd, long endTime) {
		String line = null;
		boolean allDone = false;
		while (!allDone) {
			allDone = false;
			LogRecord currentRecord = null;
			if (ValidationParams.readAllFiles) {
				if (!vd.writes.isEmpty()) {
					String logStr = vd.writes.remove(0);
					currentRecord = LogRecord.createLogRecord(logStr, true, false);

				}
			} else {
				boolean dummy = false;
				if (ValidationParams.timePartition)
					dummy = true;
				int i = Utilities.getEarilestRecordIndex(vd.writeRecords);
				currentRecord = vd.writeRecords[i];
				vd.writeRecords[i] = null;
				try {
					if ((line = vd.updateBReaders[i].readLine()) != null) {
						vd.writeRecords[i] = LogRecord.createLogRecord(line, true, dummy);

					}
				} catch (IOException e) {
					e.printStackTrace(System.out);
				}
			}

			if (currentRecord == null)
				allDone = true;
			if (!allDone) {

				boolean add = true;
				if (ValidationParams.timePartition) {

					add = vd.checkWrite(currentRecord);
					if (add) {
						if (currentRecord.getEntities() == null)
							currentRecord = LogRecord.createLogRecord(currentRecord.getActionName(), true, false);
						int lastIndex = vd.numWriteLogsForPartitions.size() - 1;
						long v = vd.numWriteLogsForPartitions.get(lastIndex);
						vd.numWriteLogsForPartitions.set(lastIndex, ++v);
					}

				}
				if (add) {
					vd.updateLogs.addInterval(currentRecord);
					vd.writeLogsCount++;
				}

				if (currentRecord.getStartTime() > endTime) {
					allDone = true;
				}
			}
		}
	}

	// RetrieveUpdatesThread.maxTime = endTime;
	// RetrieveUpdatesThread.syncSemaphore.release();
	// try {
	// RetrieveUpdatesThread.syncSemaphore.acquire();
	// } catch (InterruptedException e) {
	// e.printStackTrace(System.out);
	// }

	private void removeFromBucket(ValidatorData vd, LogRecord r) {
		for (Entity e : r.getEntities()) {
			for (Property p : e.getProperties()) {
				if (p == null)
					continue;
				String pKey = Property.getProprtyKey(e, p);
				ArrayList<LogRecord> arr = vd.bucket.get(e.name).get(pKey);
				if (arr != null)
					arr.remove(r);
			}
		}
	}

	private void addTo_new_pKeys(HashMap<String, TransactionOverlap> new_pKeys, LogRecord i) {

		for (Entity e : i.getEntities()) {
			for (Property p : e.getProperties()) {
				String pKey = Property.getProprtyKey(e, p);
				if (new_pKeys.containsKey(pKey)) {
					TransactionOverlap oldValue = new_pKeys.get(pKey);
					if (oldValue == TransactionOverlap.Both)
						continue;
				}
				TransactionOverlap state;
				state = new_pKeys.get(pKey);
				if (state == null) {
					state = TransactionOverlap.None;
				}
				if (p.getType() == ValidationParams.INCREMENT_UPDATE) {
					if (state == TransactionOverlap.NVU) {
						new_pKeys.put(pKey, TransactionOverlap.Both);

					} else {

						new_pKeys.put(pKey, TransactionOverlap.Inc);
					}
				}
				// NVU
				else if (p.getType() == ValidationParams.NEW_VALUE_UPDATE) {
					if (state == TransactionOverlap.Inc) {
						new_pKeys.put(pKey, TransactionOverlap.Both);

					} else {

						new_pKeys.put(pKey, TransactionOverlap.NVU);
					}
				}
			}
		}
	}

	private static void convertToUpdate(LogRecord log) {
		assert log
				.getType() == ValidationParams.READ_WRITE_RECORD : "Tryint to convert a record that is not a read and write log";
		for (Entity e : log.getEntities()) {
			int size = 0;
			for (int i = 0; i < e.getProperties().length; i++) {
				if (e.getProperties()[i].getType() != ValidationParams.VALUE_READ
						&& e.getProperties()[i].getType() != ValidationParams.VALUE_NA) {
					size++;
				}
			}
			Property[] properties = new Property[size];
			int j = 0;
			for (int i = 0; i < e.getProperties().length; i++) {
				if (e.getProperties()[i].getType() != ValidationParams.VALUE_READ
						&& e.getProperties()[i].getType() != ValidationParams.VALUE_NA) {
					properties[j++] = e.getProperties()[i];
				}
			}
			e.setProperties(properties);
		}
		log.setType(ValidationParams.UPDATE_RECORD);
	}

	private void getOverlapingIntervales(ScheduleList finalSS, ValidatorData vd, LogRecord readLog, long st, long et,
			HashSet<LogRecord> willBeShrinked, ArrayList<LogRecord> intervals) throws ReValidateException {

		ArrayList<LogRecord> treeIntervals = vd.updateLogs.query(st, et);
		HashMap<String, TransactionOverlap> new_pKeys = new HashMap<String, TransactionOverlap>();
		if (intervals.isEmpty()) {
			for (int i = 0; i < treeIntervals.size(); i++) {
				LogRecord log = treeIntervals.get(i);
				if (log.getType() == ValidationParams.READ_WRITE_RECORD) {

					boolean add = true;
					if (ValidationParams.timePartition) {
						if (log.getStartTime() > vd.partitionEt || log.getStartTime() < vd.partitionSt) {
							add = false;
						}

					}
					if (add) {
						vd.updateLogs.removeInterval(log);
						vd.readWrite.add(treeIntervals.get(i));
						treeIntervals.remove(i);
						i--;
					}

				}
			}

			Collections.sort(vd.readWrite);

			// for (int i = 0; i < ReadWrite.size(); i++) {
			// LogRecord log = ;
			if (vd.readWrite.size() > 0 && vd.readWrite.get(0).getStartTime() < readLog.getStartTime()) {
				// System.out.println(validatorID + ":exp");
				finalSS.unlock();
				throw new ReValidateException();
			}
			// }

		}
		Iterator<LogRecord> itIntervals = treeIntervals.iterator();
		while (itIntervals.hasNext()) {
			LogRecord i = itIntervals.next();

			// if (debug_isEqual(i.getId()))
			// System.out.println(readLogsCount + "- ID: " + i.getId() + ",
			// found it");

			if (!i.intersect(readLog)) {
				if (i.getEndTime() <= readLog.getStartTime()) {
					// if (debug_isEqual(i.getId()))
					// System.out.println(readLogsCount + "- ID: " + i.getId() +
					// ", is put into the bucket.");
					itIntervals.remove();
					addToBucket(vd, i, new_pKeys, true);
					vd.updateLogs.removeInterval(i);
				} else if (overlapWith(willBeShrinked, i)) {
					// if (debug_isEqual(i.getId()))
					// System.out.println(readLogsCount + "- ID: " + i.getId()
					// + ", is added to currentSS because of willBeShrinked.");

					vd.updateLogs.removeInterval(i);
					itIntervals.remove();
					intervals.add(i);
					addTo_new_pKeys(new_pKeys, i);
				} else {
					// if (debug_isEqual(i.getId()))
					// System.out.println(readLogsCount + "- ID: " + i.getId() +
					// ", left in LL.");
					itIntervals.remove();
				}
			} else {
				// if (debug_isEqual(i.getId()))
				// System.out.println(readLogsCount + "- ID: " + i.getId() + ",
				// is added to currentSS because of intersect(readLog).");
				vd.updateLogs.removeInterval(i);
				itIntervals.remove();
				intervals.add(i);
				addTo_new_pKeys(new_pKeys, i);
			}
		}

		// if (debug_Search(intervals, "148-24506"))
		// System.out.println(readLogsCount + " before getFromBucket");
		getFromBucket(vd, readLog, intervals);
		// if (debug_Search(intervals, "148-24506"))
		// System.out.println(readLogsCount + " after getFromBucket");
		for (LogRecord log : willBeShrinked) {
			getFromBucket(vd, log, intervals);
		}
		// if (debug_Search(intervals, "148-24506"))
		// System.out.println(readLogsCount + " after getFromBucket 2");
		for (int i = 0; i < intervals.size(); i++) {
			getFromBucket(vd, intervals.get(i), intervals);
		}
		// if (debug_Search(intervals, "148-24506"))
		// System.out.println(readLogsCount + " after getFromBucket 3");

		Collections.sort(intervals);

		// remove duplicates
		for (int i = intervals.size() - 1; i > 0; i--) {
			if (intervals.get(i).getStartTime() == intervals.get(i - 1).getStartTime()) {
				if (intervals.get(i).getId().equals(intervals.get(i - 1).getId())) {
					intervals.remove(i);
				}
			}
		}

		treeIntervals = vd.updateLogs.query(st, et);
		HashSet<LogRecord> newWillbeShriinked = new HashSet<>();
		while (true) {
			int size = willBeShrinked.size();
			newWillbeShriinked.clear();
			for (int i = 0; i < intervals.size(); i++) {
				LogRecord r = intervals.get(i);
				if (r.getEndTime() <= readLog.getStartTime()) {// TODO
					// =
					// or
					// no
					// =
					if (!willBeShrinked.contains(r)) {
						newWillbeShriinked.add(r);
					}
					willBeShrinked.add(r);
				}
			}
			if (size == willBeShrinked.size())
				break;

			int start = intervals.size();
			itIntervals = treeIntervals.iterator();
			while (itIntervals.hasNext()) {
				LogRecord i = itIntervals.next();

				if (overlapWith(newWillbeShriinked, i)) {
					vd.updateLogs.removeInterval(i);
					itIntervals.remove();
					intervals.add(i);
					addTo_new_pKeys(new_pKeys, i);
				}
			}

			for (LogRecord log : newWillbeShriinked) {
				getFromBucket(vd, log, intervals);
			}

			for (int i = start; i < intervals.size(); i++) {
				getFromBucket(vd, intervals.get(i), intervals);
			}
		}

		// intervals.addAll(getWritesReduceSS(vd, finalSS));

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

	private void getOverlapingIntervales_For_WillBeShrinked(ScheduleList finalSS, ValidatorData vd, LogRecord readLog,
			long st, long et, HashSet<LogRecord> willBeShrinked, ArrayList<LogRecord> intervals) {

		ArrayList<LogRecord> treeIntervals = vd.updateLogs.query(st, et);

		HashMap<String, TransactionOverlap> new_pKeys = new HashMap<String, TransactionOverlap>();

		Iterator<LogRecord> itIntervals = treeIntervals.iterator();
		while (itIntervals.hasNext()) {
			LogRecord i = itIntervals.next();

			if (!i.intersect(readLog)) {
				if (i.getEndTime() <= readLog.getStartTime()) {

					itIntervals.remove();
					addToBucket(vd, i, new_pKeys, true);
					vd.updateLogs.removeInterval(i);
				} else if (overlapWith(willBeShrinked, i)) {

					vd.updateLogs.removeInterval(i);
					itIntervals.remove();
					intervals.add(i);
					addTo_new_pKeys(new_pKeys, i);
				} else {
				}
			} else {
				vd.updateLogs.removeInterval(i);
				itIntervals.remove();
				intervals.add(i);
				addTo_new_pKeys(new_pKeys, i);
			}
		}

		getFromBucket(vd, readLog, intervals);
		for (LogRecord log : willBeShrinked) {
			getFromBucket(vd, log, intervals);
		}
		for (int i = 0; i < intervals.size(); i++) {
			getFromBucket(vd, intervals.get(i), intervals);
		}

		Collections.sort(intervals);

		// remove duplicates
		for (int i = intervals.size() - 1; i > 0; i--) {
			if (intervals.get(i).getStartTime() == intervals.get(i - 1).getStartTime()) {
				if (intervals.get(i).getId().equals(intervals.get(i - 1).getId())) {
					intervals.remove(i);
				}
			}
		}

		while (!ValidationParams.GLOBAL_SCHEDULE) {
			int size = willBeShrinked.size();
			for (int i = 0; i < intervals.size(); i++) {
				if (intervals.get(i).getEndTime() <= readLog.getStartTime()) {// TODO
					// =
					// or
					// no
					// =
					willBeShrinked.add(intervals.get(i));
				}
			}
			if (size == willBeShrinked.size())
				break;
			itIntervals = treeIntervals.iterator();
			while (itIntervals.hasNext()) {
				LogRecord i = itIntervals.next();

				if (overlapWith(willBeShrinked, i)) {
					vd.updateLogs.removeInterval(i);
					itIntervals.remove();
					intervals.add(i);
					addTo_new_pKeys(new_pKeys, i);
				}
			}
			for (LogRecord log : willBeShrinked) {
				getFromBucket(vd, log, intervals);
			}

			for (int i = 0; i < intervals.size(); i++) {
				getFromBucket(vd, intervals.get(i), intervals);
			}
		}

		// intervals.addAll(getWritesReduceSS(vd, finalSS));

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

	@SuppressWarnings("unused")
	private static boolean doINeedThisLog(Set<String> participatingKeys, LogRecord log) {
		for (Entity e : log.getEntities()) {
			if (participatingKeys.contains(e.getEntityKey()))
				return true;
		}
		return false;
	}

	@SuppressWarnings("unused")
	private static boolean debug_isEqual(String id) {
		String[] arr = { "148-24506" };
		for (String logID : arr) {
			if (id.equals(logID)) {
				return true;
			}
		}
		return false;
	}

	private static boolean overlapWith(HashSet<LogRecord> willBeShrinked, LogRecord log) {
		for (LogRecord willBeShrinkedLog : willBeShrinked) {
			if (willBeShrinkedLog.intersect(log))
				return true;
		}
		return false;
	}

	private ArrayList<LogRecord> getWritesReduceSS(ValidatorData vd, ScheduleList current) {
		// it only reduce if it is NVU
		// TODO: this function will get writes only if it is going to retrieve
		// one log record to reduce SS.
		// It needs to be improved to check for cases where multiple writes may
		// reduce SS without producing additional SS.
		ArrayList<LogRecord> records = new ArrayList<LogRecord>();
		boolean print = false;
		if (current.schedules.size() > 1) {

			for (int k = current.notShrink.size() - 1; k >= 0; k--) {
				String key = current.notShrink.get(k);
				String ename = key.substring(0, key.indexOf(ValidationParams.KEY_SEPERATOR));
				if (!vd.bucket.get(ename).containsKey(key) || vd.bucket.get(ename).get(key).size() > 1
						|| vd.bucket.get(ename).get(key).isEmpty()) {
					continue;
				}

				LogRecord r = vd.bucket.get(ename).get(key).get(0);

				String ekey = key.substring(key.indexOf(ValidationParams.KEY_SEPERATOR) + 1,
						key.lastIndexOf(ValidationParams.KEY_SEPERATOR));
				String pname = key.substring(key.lastIndexOf(ValidationParams.KEY_SEPERATOR) + 1);
				// check NVU
				for (Entity e : r.getEntities()) {
					if (e.getName().equals(ename) && e.getKey().equals(ekey)) {
						// if(haveAllProperties(e, logE))
						for (int i = 0; i < e.getProperties().length; i++) {
							if (e.getProperties()[i] == null)
								continue;
							if (e.getProperties()[i].getName().equals(pname)
									&& e.getProperties()[i].getType() == ValidationParams.NEW_VALUE_UPDATE) {

								if (checkBucket(vd, r)) {
									records.add(r);
									current.notShrink.remove(k);
								}

								break;
							} // end prop if
						} // end property for
						break;
					} // end e if
				} // end entity for
			} // end key for
		} // end if
		return records;
	}

	private void getFromBucket(ValidatorData vd, LogRecord record, ArrayList<LogRecord> intervals) {
		for (Entity e : record.getEntities()) {
			if ((ValidationParams.USE_DATABASE || ValidationParams.useTreeMap)
					&& record.getActionName().equalsIgnoreCase("scan")) {
				getFromBucketForScan(record, e, vd, intervals);
				return;
			}
			for (Property p : e.getProperties()) {
				if (p == null)
					continue;
				String pKey = null;
				if (e.key != null) {
					pKey = Property.getProprtyKey(e, p);
				}

				// if (pKey.contains("ORD-1-1-200-3003"))
				// System.out.println();
				// System.out.println(e.name);
				// System.out.println(vd.bucket.size());

				if (!vd.bucket.get(e.name).containsKey(pKey)) {
					continue;
				} else {
					ArrayList<LogRecord> temp = vd.bucket.get(e.name).get(pKey);
					for (int i = temp.size() - 1; i >= 0; i--) {
						if (temp.get(i).getStartTime() <= record.getEndTime()) {
							intervals.add(temp.get(i));

							// if (debug_isEqual(temp.get(i).getId()))
							// System.out.println(readLogsCount + "- ID: " +
							// intervals.get(i).getId() + ", left bucket.");
							temp.remove(temp.get(i));

						}
					}
					// bucket.remove(pKey);
					// for (LogRecord r : temp) {
					// intervals.add(r);
					// }
					if (temp.isEmpty())
						vd.bucket.get(e.name).remove(pKey);
				}
			}

		}
	}

	private void getFromBucketForScan(LogRecord read, Entity e, ValidatorData vd, ArrayList<LogRecord> intervals) {
		String tokens[] = read.getQueryParams().split(":");
		TreeType upper = null;
		TreeType lower = null;
		int l = -1, limit = -1;
		if (ValidationParams.OLTP_SCAN) {
			upper = Validator.createObject(tokens[0]);
			lower = Validator.createObject(tokens[1]);
		} else {
			lower = Validator.createObject(tokens[0]);
			limit = Integer.parseInt(tokens[1]);
			if (ValidationParams.integerScan)
				l = Integer.parseInt(tokens[0]);
		}
		HashMap<String, ArrayList<LogRecord>> myBucket = vd.bucket.get(e.name);
		HashSet<String> myProps = new HashSet<String>();
		for (Property p : e.getProperties()) {
			myProps.add(p.getName());
		}
		for (String bucketKey : myBucket.keySet()) {

			String properyName = bucketKey.substring(bucketKey.lastIndexOf(ValidationParams.KEY_SEPERATOR) + 1);
			String primaryKey = bucketKey.substring(bucketKey.indexOf(ValidationParams.KEY_SEPERATOR) + 1,
					bucketKey.lastIndexOf(ValidationParams.KEY_SEPERATOR));
			boolean scancheck = false;
			if (ValidationParams.OLTP_SCAN) {
				TreeType myobj = Validator.createObject(primaryKey);
				scancheck = (myobj.compareTo(lower) > 0 && myobj.compareTo(upper) < 0);
			} else {
				TreeType myobj = Validator.createObject(primaryKey);
				if (myobj.compareTo(lower) < 0)
					scancheck = false;
				else {
					if (ValidationParams.scanHoles || !ValidationParams.integerScan) {
						scancheck = Validator.isScanRelated(read, myobj, limit);

					} else {
						int primaryKeyint = Integer.parseInt(primaryKey);
						scancheck = primaryKeyint >= l && primaryKeyint < l + limit;

					}

				}
			}
			if (myProps.contains(properyName) && scancheck) {
				ArrayList<LogRecord> temp = myBucket.get(bucketKey);
				for (int i = temp.size() - 1; i >= 0; i--) {
					if (temp.get(i).getStartTime() <= read.getEndTime()) {
						intervals.add(temp.get(i));

						temp.remove(temp.get(i));

					}
				}

				// if (temp.isEmpty())
				// myBucket.remove(bucketKey);

			}
		}

	}

	private boolean checkBucket(ValidatorData vd, LogRecord record) {
		HashMap<String, ArrayList<LogRecord>> removed = new HashMap<String, ArrayList<LogRecord>>();
		for (Entity e : record.getEntities()) {
			for (Property p : e.getProperties()) {
				if (p == null)
					continue;
				String pKey = Property.getProprtyKey(e, p);
				// if (pKey.contains("ORD-1-1-200-3003"))
				// System.out.println();
				// if (!bucket.containsKey(pKey)) {
				// continue;
				// } else {
				ArrayList<LogRecord> temp = vd.bucket.get(e.name).get(pKey);
				for (int i = temp.size() - 1; i >= 0; i--) {
					if (temp.get(i).getStartTime() <= record.getEndTime()) {
						if (!temp.get(i).equals(record)) {
							// return taken
							for (String k : removed.keySet()) {
								for (LogRecord rr : removed.get(k)) {
									vd.bucket.get(e.name).get(k).add(rr);
								}
							}
							return false;
						} else { // equal so add it

							ArrayList<LogRecord> array = removed.get(pKey);
							if (array == null) {
								array = new ArrayList<LogRecord>();
							}
							array.add(temp.get(i));
							temp.remove(temp.get(i));
						}
					}
				}

				// }
			}
		}
		return true;
	}

	private void addToBucket(ValidatorData vd, LogRecord i, HashMap<String, TransactionOverlap> new_pKeys,
			boolean withShrink) {

		for (Entity e : i.getEntities()) {
			for (Property p : e.getProperties()) {
				String pKey = Property.getProprtyKey(e, p);
				if (!vd.bucket.get(e.name).containsKey(pKey)) {
					ArrayList<LogRecord> temp = new ArrayList<LogRecord>();
					temp.add(i);
					vd.bucket.get(e.name).put(pKey, temp);
				} else {
					ArrayList<LogRecord> b = vd.bucket.get(e.name).get(pKey);
					boolean found = false;
					for (LogRecord r : b) {
						if (i.getId().equals(r.getId())) {
							found = true;
							break;
						}
					}
					if (!found) {
						if (withShrink && ValidationParams.SHRINK_BUCKET)
							bucketShrink(vd.bucket.get(e.name).get(pKey), i, e, p, new_pKeys);
						vd.bucket.get(e.name).get(pKey).add(i);
					}
				}
			}
		}
	}

	private static void bucketShrink(ArrayList<LogRecord> arrayList, LogRecord log, Entity logE, Property logP,
			HashMap<String, TransactionOverlap> new_pKeys) {
		boolean hasInsert = false;
		int index = 0;
		for (index = 0; index < ValidationParams.ENTITY_NAMES.length; index++) {
			if (logE.getName().equals(ValidationParams.ENTITY_NAMES[index])) {
				break;
			}
		}
		String[] insertActions = ValidationParams.ENTITIES_INSERT_ACTIONS[index];
		Iterator<LogRecord> it = arrayList.iterator();
		String pKey = Property.getProprtyKey(logE, logP);

		ArrayList<Entity> entities = new ArrayList<Entity>();
		double sum = 0;
		boolean notAllInc = false;
		while (it.hasNext()) {
			hasInsert = false;
			LogRecord r = it.next();
			if (insertActions != null) {
				for (String actionName : insertActions) {
					if (r.getActionName().equals(actionName)) {
						hasInsert = true;
						break;
					}
				}
			}
			if (logP.getType() == ValidationParams.NEW_VALUE_UPDATE && !hasInsert) {
				// must check if new update not has all values
				// if(!
				// new_pKeys.containsKey(pKey)||new_pKeys.get(pKey)==TransactionOverlap.NVU
				// ||new_pKeys.get(pKey)==TransactionOverlap.Inc ) {
				for (Entity e : r.getEntities()) {
					if (e.getName().equals(logE.getName()) && e.getKey().equals(logE.getKey())) {
						// if(haveAllProperties(e, logE))
						for (int i = 0; i < e.getProperties().length; i++) {
							if (e.getProperties()[i] == null)
								continue;
							if (e.getProperties()[i].getName().equals(logP.getName())) {
								if (r.getEndTime() < log.getStartTime()) {
									e.getProperties()[i] = null;

									it.remove();
								}
								break;
							}
						}
						break;
					}
				}
				// }

			} else if (logP.getType() == ValidationParams.INCREMENT_UPDATE) {
				if (!new_pKeys.containsKey(pKey) || new_pKeys.get(pKey) == TransactionOverlap.Inc) {

					for (Entity e : r.getEntities()) {
						if (e.getName().equals(logE.getName()) && e.getKey().equals(logE.getKey())) {
							for (Property p : e.getProperties()) {
								if (p == null)
									continue;
								if (p.getName().equals(logP.getName())) {
									if (p.getType() == ValidationParams.INCREMENT_UPDATE) {
										double result = Double.parseDouble(p.getValue());
										sum += result;
										entities.add(e);
									} else {
										notAllInc = true;
										break;
									}
									break;
								}
							}
							break;
						}
					}
				}
				if (notAllInc)
					break;
			}
		}
		if (!notAllInc && logP.getType() == ValidationParams.INCREMENT_UPDATE)

		{
			for (Entity e : entities) {
				for (int i = 0; i < e.getProperties().length; i++) {
					if (e.getProperties()[i] == null)
						continue;
					if (e.getProperties()[i].getName().equals(logP.getName())) {
						e.getProperties()[i] = null;
						// TODO:should we also remove the record from the bucket
						// entry
					}

				}

			}
			arrayList.clear();
			String str = null;
			if (sum == 0)
				str = "0.00";
			else
				str = ValidationParams.DECIMAL_FORMAT.format(sum);
			logP.setValue(Utilities.applyIncrements(logP.getValue(), str));
		}
	}

	private boolean[] validateReadWithSerialSchedule(Validator v, ValidatorData vd, ScheduleList validationSS,
			LogRecord record, ScheduleList finalSS, boolean generateRecords, boolean readHasOverlapping,
			int parentsSize, long[] updateTime) {
		HashSet<ArrayList<Schedule>> newSS = new HashSet<ArrayList<Schedule>>();
		ArrayList<Schedule> schedulesGeneratingState = new ArrayList<Schedule>();
		// ArrayList<EntitySpec> generatedEntities= new ArrayList<EntitySpec>();
		// if (record.getId().equals("0-749") )
		// System.out.println("found it");

		ArrayList<LogRecord> beforeReadLogs = new ArrayList<LogRecord>();
		HashMap<Schedule, HashMap<String, String>> expectedValues = new HashMap<Schedule, HashMap<String, String>>();
		boolean valid = false, firstValidSS = true;
		boolean scan = false;
		boolean hasGeneratedState[] = { false };
		if ((ValidationParams.USE_DATABASE || ValidationParams.useTreeMap)
				&& record.getActionName().equalsIgnoreCase("scan")) {
			scan = true;
			// System.out.println(record.getId());
			// System.out.println("Query params:"+record.getQueryParams());
			// System.out.println("num Schedules:"+validationSS.schedules.size());
		}
		// assert validationSS.schedules.size()==1;
		for (Schedule s : validationSS.schedules) {
			boolean readMatch = validateSchedule(s, record, scan, hasGeneratedState, expectedValues, vd, valid,
					generateRecords, newSS, beforeReadLogs, parentsSize, updateTime, readHasOverlapping);

			if (readMatch)
				valid = true;
		}
		if (!ValidationParams.hasInitState) {
			if (!generateRecords || ValidationParams.timePartition) // YAZXX
				generatedEntities.clear();

			if (valid && !generatedEntities.isEmpty()) {
				Entity[] entities = new Entity[generatedEntities.size()];
				entities = generatedEntities.toArray(entities);
				for (int i = 0; i < entities.length; i++) {
					Property[] props = entities[i].getProperties();
					EntitySpec entitySp = (EntitySpec) entities[i];
					props = new Property[entitySp.getPropertiesArrayLis().size()];
					props = entitySp.getPropertiesArrayLis().toArray(props);
					entities[i].setProperties(props);
					entitySp.getPropertiesArrayLis().clear();
					entitySp.setPropertiesArrayLis(null);

				}

				if (readHasOverlapping) {
					createLogRecordForRead(finalSS, record, entities, v);
					generatedEntities.clear();
					boolean[] result = { false, false };
					return result;
				} else {
					createDBStateEntities(vd, entities);
					generatedEntities.clear();

					// return true;
				}

			}
		}
		if (!valid) {
			if (record.getActionName().equalsIgnoreCase("scan")) {
				System.out.println("Anomalous Scan:" + record.getId());
				// System.exit(0);
			}
			totalStaleCount++;
			vd.staleCount.incrementAndGet();
			int rPartition = 0;
			int uPartition = 0;
			if (ValidationParams.USE_KAFKA) {
				rPartition = vd.readTopicPartitions.partition();
				uPartition = vd.updateTopicPartitions.partition();
			}
			String staleRecord = Utilities.getStaleLogString(record.getType(), record.getId(), record.getOffset(),
					rPartition, vd.bufferedReads.getOffset(), uPartition, vd.bufferedWrites.getOffset(),
					record.getActionName(), expectedValues);

			staleRecords.add(staleRecord);

			/*
			 * System.out.println(totalReadLogsCount + "- Stale Data (no. " +
			 * totalStaleCount + ") - ID: " + record.getId());
			 * System.out.println(this.validatorID + ":Read:" + staleRecord); for (Schedule
			 * s : expectedValues.keySet()) { HashMap<String, String> values =
			 * expectedValues.get(s); for (String key : values.keySet()) {
			 * System.out.println(key + ":" + values.get(key)); }
			 * 
			 * }
			 */

			System.out.println(validatorID + ":" + totalReadLogsCount + "- Anomalous Read (no. " + totalStaleCount
					+ ") - ID: " + record.getId());
			// System.out.println(this.validatorID + ":Read:" + staleRecord);
			// System.out.println("Expected:-");
			if (ValidationParams.PRINT_EXPECTED) {
				for (Schedule s : expectedValues.keySet()) {
					HashMap<String, String> values = expectedValues.get(s);
					for (Entity e : record.getEntities()) {
						for (Property p : e.getProperties()) {
							for (String key : values.keySet()) {
								// String key = p.getProprtyKey(e);
								if (values.get(key) != null)
									System.out.println(key + ": Log Value (" + p.getValue() + ") "
											+ (p.getValue().equals(values.get(key)) ? "==" : "!=") + " Expected ("
											+ values.get(key) + ")");
							}
						}
					}

				}
			}

			if (ValidationParams.ANOMALY_EXIT)
				System.exit(0);
		} else { // read is valid

			for (LogRecord r : beforeReadLogs) {
				if (record.getEndTime() < r.getEndTime())
					r.setEndTime(record.getEndTime());
			}

			// if (readLogsCount % numToDivideBy == 0)
			// System.out.println(readLogsCount + "- Read is successful:" +
			// record.getId());
			finalSS.validSchedules(vd, newSS, record.getStartTime(), this.discardedSchIdList);
		}

		updateDatabase(finalSS, vd);
		boolean[] result = { true, valid };
		return result;

	}

	private void processValidSchedule(ValidatorData vd, Schedule s, boolean firstValidSS,
			ArrayList<LogRecord> beforeReadLogs, LogRecord record, HashSet<ArrayList<Schedule>> newSS,
			int parentsSize) {

		if (firstValidSS) {
			firstValidSS = false;
			for (int i = 0; i < s.getRecords().size(); i++) {
				if (s.getRecords().get(i).equals(record))
					break;
				beforeReadLogs.add(s.getRecords().get(i));
			}
		} else {
			for (int i = 0; i < beforeReadLogs.size(); i++) {
				boolean found = false;
				for (int j = 0; j < s.getRecords().size(); j++) {
					if (s.getRecords().get(j).equals(record))
						break;
					if (s.getRecords().get(j).equals(beforeReadLogs.get(i))) {
						found = true;
						break;
					}
				}
				if (!found) {
					beforeReadLogs.remove(i);
					i--;
				}
			}
		}

		getNotAllowedList2(vd, s, record);
		// newSS.addAll(s.getParents(),false);YAZXX
		if (newSS.size() != parentsSize)

			newSS.add(s.getParents());

	}

	private void updateDatabase(ScheduleList finalSS, ValidatorData vd) {
		// 1- Discard Schedules in discarded list
		// 2- Insert documents in the doc list: some doc has deleted state, some has
		// update flag, some insert
		if (ValidationParams.USE_DATABASE) {
			if (ValidationParams.hasInitState) {
				assert newDBStateListInit.isEmpty() : "Establishing values while Polygraph has initial state ";
			}
			if (true) {
				boolean hasNewSchedule = checkNewSchedule(finalSS);
				if (/* updateDB|| */ hasNewSchedule) {
					database.updateDatabase(finalSS);

				} else {
					database.processDBStatesCheckRefCount(newDBStateListUpdated, vd);
					database.deleteSchedules(discardedSchIdList);

				}

				database.processDBStates(newDBStateListInit);
			} else {
				database.updateDatabase(finalSS);
				database.processDBStates(newDBStateListInit);
			}
		}
		discardedSchIdList.clear();
		newDBStateListInit.clear();
		newDBStateListUpdated.clear();

	}

	private boolean checkNewSchedule(ScheduleList finalSS) {

		for (Schedule schedule : finalSS.schedules) {
			if (schedule.newSchedule) {
				return true;
			}
		}
		return false;
	}

	private void createDBStateEntities(ValidatorData vd, Entity[] entities) {
		for (Entity e : entities) {
			int entityindex = -1;
			for (int i = 0; i < ValidationParams.ENTITY_NAMES.length; i++) {
				if (e.name.equals(ValidationParams.ENTITY_NAMES[i])) {
					entityindex = i;
					break;
				}
			}
			String props[] = new String[ValidationParams.ENTITY_PROPERTIES[entityindex].length];

			for (int i = 0; i < e.getProperties().length; i++) {
				int pIndex = -1;
				for (int p = 0; p < props.length; p++) {
					if (e.getProperties()[i].getName().equals(ValidationParams.ENTITY_PROPERTIES[entityindex][p])) {
						pIndex = p;
						break;
					}
				}
				props[pIndex] = e.getProperties()[i].getValue();

			}

			DBState newSt = new DBState(0, props);
			List<DBState> ll = null;
			String entityKey = e.getEntityKey();
			if (vd.dbState.containsKey(entityKey)) {

				// merge the two states
				ll = vd.dbState.get(entityKey);
				if (!ll.isEmpty()) {
					newDBStateListInit.add(new CandidateValue(e.name, e.key, null, newSt, true));
				}
				for (DBState oldDBState : ll) {
					for (int i = 0; i < newSt.value.length; i++) {

						if (oldDBState.getValue()[i] == null) {
							assert newSt.value[i] != null : "Both DBStates can't be null";
							oldDBState.getValue()[i] = newSt.value[i];
						}
					}
				}

			} else {
				ll = new LinkedList<DBState>();

				ll.add(newSt);
				newDBStateListInit.add(new CandidateValue(e.name, e.key, ValidationParams.SID_INIT, newSt));
				vd.dbState.put(entityKey, ll);
				if (ValidationParams.useTreeMap)
					vd.commonTree.insert(Entity.getEntityPK(entityKey), newSt, ValidationParams.treeMapObj);
			}
		} // end of entity

	}

	public static TreeType createObject(String key) {
		TreeType t = null;
		try {
			t = ValidationParams.treeMapObj.getClass().newInstance();
			t = t.convert(key);

		} catch (Exception e) {

			e.printStackTrace();
		}
		return t;
	}

	private static void createLogRecordForRead(ScheduleList finalSS, LogRecord record, Entity[] entities, Validator v) {
		String id = "R" + ValidationParams.KEY_SEPERATOR + v.seq;

		v.seq++;
		LogRecord r = new LogRecord(id, record.getActionName() + "Init", record.getStartTime(), record.getEndTime(),
				ValidationParams.UPDATE_RECORD, entities);
		r.setCreatedFromRead(true);
		r.setPartitionID(record.getPartitionID());
		finalSS.newLogs.add(r);
	}

	static String fixDir(String dir) {
		String separator = System.getProperty("file.separator");
		if (!dir.endsWith(separator))
			dir = dir + separator;
		dir = dir.replaceAll(separator + "{2,}", separator);
		return dir;
	}

	public void shrinkSerialSchedules(ValidatorData vd, ScheduleList SS) {
		if (ValidationParams.countDiscardedWrites)
			countDiscardedWritesForAll(vd, SS, SS.endTime);
		for (Schedule s : SS.schedules) {
			s.shrink(SS.endTime, vd, this.newDBStateListUpdated);
		}
	}

	private void countDiscardedWritesForAll(ValidatorData vd, ScheduleList ss, long et) {
		ArrayList<HashMap<String, Character>> discardedWrites = new ArrayList<HashMap<String, Character>>();
		for (Schedule s : ss.schedules) {
			discardedWrites.add(s.countDiscardedWrites(vd.dbState, et));
		}
		if (discardedWrites.isEmpty())
			return;
		HashMap<String, Character> discarded1 = discardedWrites.get(0);
		Iterator<String> it = discarded1.keySet().iterator();
		while (it.hasNext() && discardedWrites.size() > 1) {
			String key = it.next();
			for (int i = 1; i < discardedWrites.size(); i++) {
				if (!discardedWrites.get(i).containsKey(key)) {
					it.remove();
					break;
				}
			}
		}
		for (String key : discarded1.keySet()) {
			if (discarded1.get(key) == 'P')
				vd.partiallyDiscardedWritesCount++;
			else
				vd.fullyDiscardedWritesCount++;
		}

	}

	public ScheduleList combineSerialSchedules(String readId, ValidatorData vd, ScheduleList s1, ScheduleList s2,
			boolean isValidation) {

		ScheduleList s1temp = new ScheduleList();
		ScheduleList s3 = new ScheduleList();

		ArrayList<boolean[]> isOverlapShrinked = duplicateWithoutOverlap(readId, vd, s1, s1temp, isValidation);
		int s1Size = s1temp.schedules.size();
		int s2Size = s2.schedules.size();
		if (s2.isEmpty()) {
			for (Schedule x1 : s1temp.schedules) {
				s3.add(x1);
			}
		} else if (s1temp.isEmpty()) {
			for (Schedule x2 : s2.schedules) {
				if (isValidation)
					s3.add(x2);
				else
					s3.addAndUpdateImpcated(vd.dbState, x2);
			}
		} else {

			logger.debug("sse combine={}*{}", s1Size, s2Size);
			for (int i = 0; i < s1Size; i++) {
				Schedule x1 = s1temp.schedules.get(i);
				boolean lastOne = false;
				for (int j = 0; j < s2Size; j++) {
					Schedule x2 = s2.schedules.get(j);
					if (j + 1 == s2Size)
						lastOne = true; // YAZXX
					Schedule y = new Schedule(readId, scheduleCounter, vd.dbState, x1, x2, s1temp.overlapList,
							isOverlapShrinked.get(i), s1Size, isValidation, lastOne, s1.endTime);
					// firstOne = false;
					if (isValidation) {// YAZXX
						// y.addAllParents(x1.getParents());
						y.setParent(x1.getParents());
					}
					s3.add(y);
				}
			}
		}
		// try {
		// System.out.println("isOverlapShrinked,size = " +
		// isOverlapShrinked.size() + ".... isOverlapShrinked[0].size = " +
		// isOverlapShrinked.get(0).length);
		// } catch (Exception e) {
		// }
		isOverlapShrinked.clear();
		isOverlapShrinked = null;
		if (!isValidation) { // YAZXX

			s3.removeDuplicates(vd, isValidation, this.discardedSchIdList, s1Size, s2Size);

			s1.schedules = s3.schedules;

			s1.overlapList.clear();
			s1.overlapList.addAll(s2.overlapList);
			s1.endTime = s2.endTime;
			// if (s1.ReadWrite == null)
			// System.out.println();
			// s3.ReadWrite = s1.ReadWrite;
			// s3.notShrink = s1.notShrink;
			// s3.newLogs = s1.newLogs;
			// s3.participatingKeys = s1.participatingKeys;
			return null;
		} else {
			s3.overlapList.clear();
			s3.overlapList.addAll(s2.overlapList);
			s3.endTime = s2.endTime;
			// if (s1.ReadWrite == null)
			// System.out.println();
			// s3.ReadWrite = s1.ReadWrite;
			s3.notShrink = s1.notShrink;
			s3.newLogs = s1.newLogs;
			s3.participatingKeys = s1.participatingKeys;
			return s3;
		}
	}

	private ArrayList<boolean[]> duplicateWithoutOverlap(String readId, ValidatorData vd, ScheduleList scheduleList,
			ScheduleList newList, boolean isValidation) {
		ArrayList<boolean[]> isOverlapShrinked = new ArrayList<boolean[]>();

		for (Schedule s : scheduleList.schedules) {
			Schedule temp = new Schedule(readId, scheduleCounter);
			boolean[] currentBArray = new boolean[scheduleList.overlapList.size()];
			Arrays.fill(currentBArray, true);
			for (int i = 0; i < s.size(); i++) {
				for (int j = 0; j < scheduleList.overlapList.size(); j++) {
					if (s.getLogRecord(i).equals(scheduleList.overlapList.get(j))) {
						currentBArray[j] = false;
						break;
					}
				}
			}
			temp.duplicateNoRecords(s);
			if (isValidation) // YAZXX
				temp.addParent(s);
			boolean notFound = true;
			// for (Schedule ss : newList.schedules) {
			for (int i = 0; i < newList.schedules.size(); i++) {
				Schedule ss = newList.schedules.get(i);
				if (ss.same(temp)) {
					if (equalBArray(currentBArray, isOverlapShrinked.get(i))) {
						notFound = false;
						if (!isValidation) {
							s.destructor(vd);
							discardedSchIdList.add(s.sid);
						} else
							ss.addAllParents(temp.getParents());
					}
				}
			}
			if (notFound) {
				newList.add(temp);
				isOverlapShrinked.add(currentBArray);
			}
		}
		newList.overlapList = scheduleList.overlapList;
		return isOverlapShrinked;
	}

	private static boolean equalBArray(boolean[] bA1, boolean[] bA2) {
		for (int i = 0; i < bA1.length; i++) {
			if (bA1[i] != bA2[i])
				return false;
		}
		return true;
	}

	// ===================================================================================
	// ===================================================================================
	// ===================================================================================
	// ===================================================================================
	// ===================================================================================
	// ===================================================================================
	// ===================================================================================
	// ===================================================================================
	// ===================================================================================

	public static ScheduleList computeSerialSchedule6(Validator validator, int scheduleCounter[],
			List<LogRecord> intervals, long ET, LogRecord read, boolean[] readHasOverlapping,
			Map<String, Map<String, Boolean>> NotAllowedList, boolean isValidation) {
		ScheduleList SS = new ScheduleList();
		validator.cdseUsed = false;
		ArrayList<LogRecord> currentIntervals = new ArrayList<LogRecord>();
		short groupNum = 0;
		while (!intervals.isEmpty() || isValidation/* read != null */) {
			currentIntervals.clear();
			groupNum++;
			LogRecord currentLog = null;
			if (isValidation/* read != null */) {
				currentLog = read;
			} else {
				currentLog = intervals.get(0);
				intervals.remove(0);

			}
			if (!isValidation)
				currentLog.setGroupNum(groupNum);
			currentIntervals.add(currentLog);
			for (int j = 0; j < currentIntervals.size(); j++) {
				for (int i = 0; i < intervals.size(); i++) {
					currentLog = intervals.get(i);
					if (currentIntervals.get(j).intersect(currentLog)) // &&
					// currentIntervals.get(j).overlap(currentLog))
					{
						if (!isValidation/* read == null */)
							currentLog.setGroupNum(groupNum);
						currentIntervals.add(currentLog);
						intervals.remove(i);
						i--;
					}
				}
			}
			if (currentIntervals.size() > 1) {
				if (readHasOverlapping != null) {
					// readHasOverlapping[0] = true;
					for (LogRecord r : currentIntervals) {
						if (r != read) {
							if (r.overlap(read) && r.intersect(read)) {
								readHasOverlapping[0] = true;
								break;
							}

						}
					}

				}

				Collections.sort(currentIntervals, LogRecord.Comparators.START);
				ScheduleList currentGroupSS = computeGroupSerialSchedule2(validator, currentIntervals, NotAllowedList,
						read, isValidation);
				SS.append(currentGroupSS);
			} else {
				// validator.cdseUsed=true;
				SS.append(currentIntervals);
			}
			if (isValidation/* read != null */)
				break;
		}
		SS.updateOverlapList(ET);
		if (!isValidation) {
			for (int i = 0; i < SS.schedules.size(); i++) {
				Schedule schedule = SS.schedules.get(i);
				scheduleCounter[0]++;
				if (read != null)
					schedule.sid = read.getId() + "-" + scheduleCounter[0];
				else
					schedule.sid = "NULLREAD-" + System.currentTimeMillis() + "-" + scheduleCounter[0];

			}
		}
		return SS;
	}

	private static ScheduleList computeGroupSerialSchedule2(Validator validator, ArrayList<LogRecord> intervals,
			Map<String, Map<String, Boolean>> notAllowedList, LogRecord read, boolean isValidation) {
		ArrayList<Schedule> NSS = new ArrayList<Schedule>();
		ArrayList<LogRecord> cdseLogs = null;

		if (ValidationParams.CDSE && !isValidation) {
			cdseLogs = CDSE.runAlg(validator, read, notAllowedList, intervals);
		} else {

			validator.factUsed = true;

		}
		ScheduleList SS = new ScheduleList();
		for (LogRecord i : intervals) {
			NSS.clear();
			if (SS.isEmpty()) {
				Schedule temp = new Schedule();
				temp.add(i);
				SS.add(temp);
			} else {
				for (int ssindex = SS.schedules.size() - 1; ssindex >= 0; ssindex--) {
					Schedule S = SS.schedules.remove(ssindex);
					List<Integer> overL = new ArrayList<Integer>();
					int k = 0;
					int thisIndexAndUp = -1;
					int thisIndexAndBelow = Integer.MAX_VALUE;
					Map<String, Boolean> list;
					for (k = S.size() - 1; k >= 0; k--) {
						if (notAllowedList != null) {
							if (thisIndexAndUp == -1) {
								list = notAllowedList.get(i.getId() + "Before");
								if (list != null) {
									Boolean b = list.get(S.getLogRecord(k).getId());
									if (b != null && b == true)
										thisIndexAndUp = k + 1;
								}
							}
							list = notAllowedList.get(i.getId() + "After");
							if (list != null) {
								Boolean b = list.get(S.getLogRecord(k).getId());
								if (b != null && b == true)
									thisIndexAndBelow = k;
							}
						}
						if (S.getLogRecord(k).overlap(i)/* && S.getLogRecord(k).intersect(i) */) {
							overL.add(k);
							overL.add(k + 1);
						} else
							break;
					}

					assert thisIndexAndUp <= thisIndexAndBelow : "thisIndexAndUp(" + thisIndexAndUp
							+ ") is not less than thisIndexAndBelow (" + thisIndexAndBelow + ").";
					if (overL.isEmpty()) {
						S.add(i);
						NSS.add(S);
						// System.out.println("NSS="+NSS.size());
						// System.out.println("SS="+SS.schedules.size());

					} else {
						Collections.sort(overL);
						for (int m = overL.size() - 1; m > 0; m--) {
							if (overL.get(m) == overL.get(m - 1))
								overL.remove(m);
						}
						for (int m = overL.size() - 1; m >= 0; m--) {
							if (overL.get(m) > thisIndexAndBelow) {
								overL.remove(m);
								continue;
							}
							if (overL.get(m) < thisIndexAndUp) {
								overL.remove(m);
							}
						}
						for (int m = 0; m < overL.size(); m++) {
							Schedule newS = new Schedule();
							if (overL.get(m) == 0) {
								newS.add(i);
							}
							for (int p = 0; p < S.size(); p++) {
								newS.add(S.getLogRecord(p));
								if (p == (overL.get(m) - 1))
									newS.add(i);
							}
							NSS.add(newS);
							// System.out.println("NSS="+NSS.size());
							// System.out.println("SS="+SS.schedules.size());
							// System.out.println("OVERL="+overL.size());
						}
					}
				}
				SS.clearSchedules();
				SS.clearOverlaps();
				SS.addAll(NSS, true);

			}
		}

		if (cdseLogs != null) {
			CDSE.combineSchedule(cdseLogs, SS);
		}

		return SS;
	}

	private void shrinkNotAllowedList(ValidatorData vd, ScheduleList current) {
		if (!vd.collapsedIntervals.isEmpty()) {
			Iterator<LogRecord> it = vd.collapsedIntervals.keySet().iterator();
			while (it.hasNext()) {
				LogRecord r = it.next();
				if (r.getEndTime() < current.endTime) {
					vd.notAllowedList.remove(r.getId() + "Before");
					vd.notAllowedList.remove(r.getId() + "After");
					it.remove();
				}
			}
		}
		// collapsedIntervals.clear();
	}

	private void getNotAllowedList2(ValidatorData vd, Schedule s, LogRecord record) {
		// if (debugNotAllowed(s))
		// System.out.println(s);
		// The commented code caused a bug that a schedule that has the read only
		// included some records from its
		// parent producing not allowed incorrectly.
		//
		// Schedule parent = null;
		// if (!s.getParents().isEmpty())
		// parent=s.getParents().get(0);
		// if (parent!=null){
		// if (parent.shrinked!=null){
		// for (LogRecord r:parent.shrinked){
		// s.getRecords().add(0,r);
		//
		// }
		// parent.shrinked=null;
		// }
		// }
		for (int i = 0; i < s.getRecords().size(); i++) {
			// short g1=s.getRecords().get(i).getGroupNum(); //YAZXXX
			// if (g1==-1)
			// continue;
			Map<String, Boolean> listB = vd.notAllowedList.get(s.getRecords().get(i).getId() + "Before");
			if (listB == null) {
				listB = new HashMap<String, Boolean>();
				vd.notAllowedList.put(s.getRecords().get(i).getId() + "Before", listB);
			}
			Map<String, Boolean> listA = vd.notAllowedList.get(s.getRecords().get(i).getId() + "After");
			if (listA == null) {
				listA = new HashMap<String, Boolean>();
				vd.notAllowedList.put(s.getRecords().get(i).getId() + "After", listA);
			}
			for (int j = 0; j < i; j++) {
				// short g2=s.getRecords().get(j).getGroupNum(); //YAZXXX
				// if (g1!=g2)
				// continue;

				Boolean b = listA.get(s.getRecords().get(j).getId());
				if (b == null) {
					listB.put(s.getRecords().get(j).getId(), true);
				} else if (b == true) {
					if (ValidationParams.YAZ_FIX) {
						ArrayList<String> arr = updateNotAllowed.get(s.getRecords().get(i).getId());
						if (arr == null) {
							arr = new ArrayList<String>();
							updateNotAllowed.put(s.getRecords().get(i).getId(), arr);

						}
						arr.add(s.getRecords().get(j).getId());

						// if (!updateNotAllowed.containsKey(listA)){
						// updateNotAllowed.put(listA, new ArrayList<String>());
						// }
						//
						//
						// if (!updateNotAllowed.containsKey(listB)){
						// updateNotAllowed.put(listB, new ArrayList<String>());
						// }
						// updateNotAllowed.get(listA).add(s.getRecords().get(j).getId());
						// updateNotAllowed.get(listB).add(s.getRecords().get(j).getId());

					}

					listA.put(s.getRecords().get(j).getId(), false);
					listB.put(s.getRecords().get(j).getId(), false);

				}
			}
			for (int j = i + 1; j < s.getRecords().size(); j++) {
				// short g2=s.getRecords().get(j).getGroupNum(); //YAZXXX
				// if (g1!=g2)
				// continue;
				Boolean b = listB.get(s.getRecords().get(j).getId());
				if (b == null) {
					listA.put(s.getRecords().get(j).getId(), true);
				} else if (b == true) {
					if (ValidationParams.YAZ_FIX) {
						ArrayList<String> arr = updateNotAllowed.get(s.getRecords().get(i).getId());
						if (arr == null) {
							arr = new ArrayList<String>();
							updateNotAllowed.put(s.getRecords().get(i).getId(), arr);

						}
						arr.add(s.getRecords().get(j).getId());

					}

					listA.put(s.getRecords().get(j).getId(), false);
					listB.put(s.getRecords().get(j).getId(), false);

				}
			}

		}

		// if (vd.notAllowedList.get("9-95Before")
		// !=null&&vd.notAllowedList.get("9-95Before").get("18-94")!=null&&vd.notAllowedList.get("9-95Before").get("18-94")==true)
		// System.out.println();

	}

	@SuppressWarnings("unused")
	private boolean debugNotAllowed(Schedule s) {
		boolean a = false;
		boolean b = false;
		for (LogRecord r : s.getRecords()) {
			if (r.getId().equals("2-13734"))
				a = true;
			if (r.getId().equals("1-13920"))
				b = true;
			if (a && b)
				return true;

		}
		return a && b;
	}

	@Override
	public Integer call() {
		try {
			Validator.threadsStart.countDown();
			Validator.threadsStart.await();
			validatorStartTime = System.currentTimeMillis();
			System.out.println("Validator " + validatorID + " is starting ...");
			startValidation(logDir);
			System.out.println("Validator " + validatorID + " is Done");
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(0);
		}
		return validatorID;

	}

	public static void close() {
		if (ValidationParams.USE_DATABASE) {
			Validator.database.close();
		}

	}
}
