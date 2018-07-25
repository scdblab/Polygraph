<%@page import="java.util.Enumeration"%>
<%@page import="java.util.HashMap"%>
<%@page import="edu.usc.polygraph.*"%>
<%@page import="edu.usc.polygraph.website.PolygraphUISettings"%>
<%@page import="java.util.ArrayList"%>
<%
	if ((session.getAttribute("appName") == null) || ((String) session.getAttribute("appName") == "")
			|| (session.getAttribute("kafkaIP") == null) || ((String) session.getAttribute("kafkaIP") == "")
			|| (request.getParameter("rO") == null) || (request.getParameter("rO") == "")
			|| (request.getParameter("wO") == null) || (request.getParameter("wO") == "")
			|| (request.getParameter("count") == null) || (request.getParameter("count") == "")
			|| (request.getParameter("index") == null) || (request.getParameter("index") == "")
			|| (request.getParameter("key") == null) || (request.getParameter("key") == "")
			|| (request.getParameter("type") == null) || (request.getParameter("type") == "")) {
		request.setAttribute("info", "error");
		request.setAttribute("errorMSG", "Missing inputs.");
		request.getRequestDispatcher("visual.jsp").forward(request, response);
	} else {
		long rO = Long.parseLong(request.getParameter("rO"));
		long wO = Long.parseLong(request.getParameter("wO"));
		long count = Integer.parseInt(request.getParameter("count"));
		String appName = (String) session.getAttribute("appName");
		String kafkaIP = (String) session.getAttribute("kafkaIP");
		String key = request.getParameter("key");
		String type = request.getParameter("type");
		int index = Integer.parseInt(request.getParameter("index"));
		String topic = "STATS_" + appName;

		LogRecord staleLogRecord = (LogRecord) session.getAttribute(topic + "_currentStale");
		StatsConsumer consumerStale = (StatsConsumer) session.getAttribute(topic + "_consumerStale");
		if (consumerStale == null) {
			out.println("ERROR:Consumers missing.");
			out.flush();
		} else {
			HashMap<String, ArrayList<StaleLog>> hash = null;
			if (type.equals("e")) {
				hash = consumerStale.eStaleLog;
			} else if (type.equals("t")) {
				hash = consumerStale.tStaleLog;
			}
			StaleLog staleStaleLog = hash.get(key).get(index);
			rO -= count * PolygraphUISettings.numOfLogsBeforeLast;
			wO -= count * PolygraphUISettings.numOfLogsBeforeLast;
			ArrayList<LogRecord> result = null;
			if (count == 0) {
				System.out.println("getForTheFirstTime ... rO = " + rO);
				result = KafkaScripts.getForTheFirstTime(staleStaleLog, appName, kafkaIP);
				for (LogRecord r : result) {
					if (staleStaleLog.tid.equals(r.getId())) {
						staleLogRecord = r;

						break;
					}
				}
				session.setAttribute(topic + "_currentStale", staleLogRecord);
			} //else {
			System.out.println(
					String.format("getMore(%d, %d, %s, %s, %s, %d, %d);", rO, wO, staleLogRecord.getId(),
							appName, kafkaIP, staleStaleLog.readPartition, staleStaleLog.writePartition));
			System.out.println(staleLogRecord.toPrint());
			System.out.println("==============");
			ArrayList<LogRecord> result2 = KafkaScripts.getMore(rO, wO, staleLogRecord, appName, kafkaIP,
					staleStaleLog.readPartition, staleStaleLog.writePartition);
			if (result == null)
				result = result2;
			else
				result.addAll(result2);
			//}

			KafkaScripts.removeUnwanted(result, staleLogRecord);

			for (StaleLog s : hash.get(key)) {

				for (LogRecord l : result) {
					if (s.tid.equals(l.getId())) {
						l.setType('S');

					}
				}
			}
			out.print(KafkaScripts.ArrayListToJSON(result, hash.get(key)));
		}
	}
%>