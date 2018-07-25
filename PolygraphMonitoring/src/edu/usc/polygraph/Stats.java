package edu.usc.polygraph;

public class Stats {
	public int clientID;
	public long reads;
	public long write;
	public long stales;
	public long avgSS;
	public long maxSS;
	public long avgMem;
	public long maxMem;
	public long partialDiscard;
	public long fullDiscard;
	public String bucket;
	public String duration;

	public Stats() {
	}

	public Stats(String line) {
		String[] tokens = line.split(",");
		clientID = Integer.parseInt(tokens[1]);
		reads = Long.parseLong(tokens[2]);
		write = Long.parseLong(tokens[3]);
		stales = Long.parseLong(tokens[4]);
		avgSS = Long.parseLong(tokens[5]);
		maxSS = Long.parseLong(tokens[6]);
		avgMem = Long.parseLong(tokens[7]);
		maxMem = Long.parseLong(tokens[8]);
		partialDiscard = Long.parseLong(tokens[9]);
		fullDiscard = Long.parseLong(tokens[10]);
		duration = tokens[11].trim();
		if (tokens.length>12)
		bucket= tokens[12].trim();
	}
}
