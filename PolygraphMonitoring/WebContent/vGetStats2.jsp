<%@page import="edu.usc.polygraph.website.PolygraphUISettings"%>
<%@ page import="java.sql.*"%>
<%@ page import="edu.usc.polygraph.*"%>
<%
	if ((session.getAttribute("appName") == null) || ((String)session.getAttribute("appName") == "")) {
		out.println("ERROR:Name missing.");
		out.flush();
	} else {
		String appName = (String) session.getAttribute("appName");
		String topic = "STATS_" + appName;

		StatsConsumer consumerStale = (StatsConsumer) session.getAttribute(topic + "_consumerStale");
		StatsConsumer consumerStats = (StatsConsumer) session.getAttribute(topic + "_consumerStats");
		if ((consumerStale == null) || (consumerStats == null)) {
			out.println("ERROR:Consumers missing.");
			out.flush();
		} else {
			String result = KafkaScripts.getStatsJSON(consumerStale, consumerStats);
			if (result==null)
			{
				out.println("ERROR:Please check topic:"+topic+" exists!");
				out.flush();
			}
			else{
			out.println(result);
			out.flush();
			}
		}
	}
%>