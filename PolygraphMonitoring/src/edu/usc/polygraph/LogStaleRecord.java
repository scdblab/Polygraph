package edu.usc.polygraph;

import edu.usc.polygraph.website.PolygraphUISettings;

public class LogStaleRecord {
	private LogRecord log;
	private long readOffset;
	private long writeOffset;

	public LogRecord getLog() {
		return log;
	}

	public void setLog(LogRecord log) {
		this.log = log;
	}

	public long getReadOffset() {
		return readOffset;
	}

	public void setReadOffset(long readOffset) {
		this.readOffset = readOffset;
	}

	public long getWriteOffset() {
		return writeOffset;
	}

	public void setWriteOffset(long writeOffset) {
		this.writeOffset = writeOffset;
	}

	LogStaleRecord(String line) {
		String LogLine = getInfo(line);
		log = LogRecord.createLogRecord(LogLine);
	}

	public String getId() {
		return log.getId();
	}

	private String getInfo(String line) {
		String tokens[] = line.split("[" + PolygraphUISettings.RECORD_ATTRIBUTE_SEPERATOR + "]");
		String result = "";
		for (int i = 0; i < tokens.length; i++) {
			if (i == 5) {
				readOffset = Long.parseLong(tokens[i]);
			} else if (i == 6) {
				writeOffset = Long.parseLong(tokens[i]);
			} else if (i == 2) {
				String ids[] = tokens[i].split("[" + PolygraphUISettings.KEY_SEPERATOR + "]");
				result += PolygraphUISettings.RECORD_ATTRIBUTE_SEPERATOR;
				result += ids[0];
				result += PolygraphUISettings.RECORD_ATTRIBUTE_SEPERATOR;
				result += ids[1];
			} else {
				if (i != 0)
					result += PolygraphUISettings.RECORD_ATTRIBUTE_SEPERATOR;
				result += tokens[i];
			}
		}
		return result;
	}

}
