<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
<script src="js/common.js" type="text/javascript"></script>
<style>
td.clickable a {
	display: block;
	width: 100%;
}

table {
	margin: auto;
}

td {
	text-align: center;
}
</style>
<link rel="stylesheet" href="css/style.css" type="text/css">
<link rel="stylesheet" href="css/menubar.css" type="text/css">
</head>
<body>
	<ul>
		<li><a class="active" href="visual.jsp">Main Page</a></li>
	</ul><br/>
<%
		if (request.getAttribute("info") != null)
			if (request.getAttribute("info").equals("error")) {
	%>
	<div style="text-align: center; margin: auto; font-family: sans-serif; font-weight: bold; color: red;"><%=request.getAttribute("errorMSG")%></div>
	<%
		}
	%>
	<form method="post" action="runVisualizer2.jsp" style="margin-bottom: 0px;">
	<table class="shadow2" style="width: 330px;">
		<tr><td style="text-align: left;">Topic Name</td><td><input type="text" id="appName" name="appName" /></td></tr>
		<tr><td></td><td style="text-align: center; font-family: monospace; font-size: small; font-style: italic;">IP:Port[[,IP2:Port2] ...]</td></tr>
		<tr><td style="text-align: left;">Kafka Host(s)</td><td><input type="text" id="kafkaIP" name="kafkaIP" /></td></tr>
		<tr><td style="text-align: left;">Zookeeper Host(s)</td><td><input type="text" id="zookeeperIP" name="zookeeperIP" /></td></tr>
		<tr><td colspan="2"><input type="submit" value="Visualize" /></td></tr>
	</table>
	</form>
</body>
</html>