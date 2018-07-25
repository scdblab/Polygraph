<%@page import="edu.usc.polygraph.StatsConsumer"%>
<%@page import="java.util.ArrayList"%>
<%@page import="edu.usc.polygraph.KafkaScripts"%>
<%@page import="edu.usc.polygraph.website.AppElement"%>
<%@ page import="java.sql.*"%>
<%
	if (       (request.getParameter("appName") == null)
			|| (request.getParameter("appName") == "")
			|| (request.getParameter("kafkaIP") == null)
			|| (request.getParameter("kafkaIP") == "")
			|| (request.getParameter("zookeeperIP") == null)
			|| (request.getParameter("zookeeperIP") == "")) {
		request.setAttribute("info", "error");
		request.setAttribute("errorMSG", "Missing inputs.");
		request.getRequestDispatcher("visual.jsp").forward(request, response);
	} else {
		String appName = request.getParameter("appName");
		String kafka = request.getParameter("kafkaIP");
		String zookeeper = request.getParameter("zookeeperIP");

		session.setAttribute("appName", appName);
		session.setAttribute("kafkaIP", kafka);
		session.setAttribute("zookeeperIP", zookeeper);
		
		String topic = "STATS_" + appName;
	//	KafkaScripts.isTopicExist(topic, zookeeper)
		if(true){
			StatsConsumer consumerStale = new StatsConsumer(topic, topic, 1, kafka);
			session.setAttribute(topic+"_consumerStale", consumerStale);
			
			StatsConsumer consumerStats = new StatsConsumer(topic, topic, 0, kafka);
			session.setAttribute(topic+"_consumerStats", consumerStats);			
			
			response.sendRedirect("validationStates2.jsp");
		} else {
			request.setAttribute("info", "error");
			request.setAttribute("errorMSG", "Topic:" + topic + " doesn't exist in Kafka.");
			request.getRequestDispatcher("visual.jsp").forward(request, response);				
		}
	}
%>