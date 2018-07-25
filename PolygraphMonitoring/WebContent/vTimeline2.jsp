<%@page import="edu.usc.polygraph.*"%>
<%@page import="edu.usc.polygraph.Entity"%>
<%@page import="edu.usc.polygraph.Property"%>
<%@page import="edu.usc.polygraph.LogRecord"%>
<%@page import="edu.usc.polygraph.KafkaScripts"%>
<%@page import="java.util.ArrayList"%>
<%
	if ((session.getAttribute("appName") == null) || (session.getAttribute("appName") == "")) {
		request.setAttribute("info", "error");
		request.setAttribute("errorMSG", "Missing inputs.");
		request.getRequestDispatcher("visual.jsp").forward(request, response);
	} else {
		String appName = (String) session.getAttribute("appName");
		String topic = "STATS_" + appName;

		StatsConsumer consumerStale = (StatsConsumer) session.getAttribute(topic + "_consumerStale");
		if (consumerStale == null) {
			request.setAttribute("info", "error");
			request.setAttribute("errorMSG", "Session timed out.");
			request.getRequestDispatcher("visual.jsp").forward(request, response);
		} else {
			String key = request.getParameter("key");
			String type = request.getParameter("type");
			if ((key == null) || (key == "") || (type == null) || (type == "")) {
				request.setAttribute("info", "error");
				request.setAttribute("errorMSG", "Missing key or type.");
				request.getRequestDispatcher("validationStates2.jsp").forward(request, response);
			} else {
				ArrayList<StaleLog> list = null;
				if (type.equals("e")) {
					list = consumerStale.eStaleLog.get(key);
				} else if (type.equals("t")) {
					list = consumerStale.tStaleLog.get(key);
				}
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
<script src="js/common.js" type="text/javascript"></script>
<script src="js/visualization.js" type="text/javascript"></script>
<link rel="stylesheet" href="css/style.css" type="text/css">
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
<style>
.selected {
	background-color: #97a6ff;
	color: #ffffff;
}

table.evenOdd tr:nth-child(even) {
	background: #FFF
}

table.evenOdd tr:nth-child(odd) {
	background: #CCC
}

.Mitem {
	height: 20px;
}

.vis-item.Read {
	background-color: #a5ffa9;
	border-color: #4caf50;
}

.vis-item.vis-selected.Read {
	/* custom colors for selected orange items */
	background-color: #31e638;
	border-color: #37793a;
}

.vis-item.Write {
	background-color: #c9d0f9;
	border-color: #3F51B5;
}

.vis-item.vis-selected.Write {
	/* custom colors for selected orange items */
	background-color: #97a6ff;
	border-color: #28378a;
}

.vis-item.Stale {
	background-color: #ffc0c0;
	border-color: #ff0000;
}

.vis-item.vis-selected.Stale {
	/* custom colors for selected orange items */
	background-color: #f56262;
	border-color: #8a0000;
}

.Read {
	background-color: #a5ffa9;
	border-color: #4caf50;
}

.Write {
	background-color: #c9d0f9;
	border-color: #3F51B5;
}

.Stale {
	background-color: #ffc0c0;
	border-color: #ff0000;
}
</style>
<script src="js/vis.js"></script>
<link href="css/vis.css" rel="stylesheet" type="text/css" />
<link rel="stylesheet" href="css/style.css" type="text/css">
<link rel="stylesheet" href="css/menubar.css" type="text/css">
<link rel="stylesheet" href="css/logInfo.css" type="text/css">
</head>
<body id="body" style="text-align: center;">
	<ul>
		<li><a href="visual.jsp">Main Page</a></li>
		<li><a href="validationStates2.jsp">Visualize <%=appName%></a></li>
		<li><a class="active"><%=key%></a></li>
	</ul>
	<br />
	<table style="margin: auto;">
		<tr>
			<td valign="top">
				<div class="shadow" style="padding: 5px;">
					<table style="border-collapse: separate; border-collapse: initial; margin-bottom: 2px; margin-left: 2px;">
						<tr>
							<th width="125px" style="border-radius: 10px; text-align: center;">Transaction ID</th>
						</tr>
					</table>
					<div style="overflow: auto; width: 150px; height: 500px;">
						<table id="Table" class="evenOdd" style="border: solid 1px black; border-collapse: collapse;">
							<%
								for (int i = 0; i < list.size(); i++) {
							%>
							<tr>
								<td id="td_<%=i%>" width="126px"><a onclick="selectedNewIndex(<%=list.get(i).lastReadOffset%>,<%=list.get(i).lastWriteOffset%>,'<%=appName%>', true, true, '<%=key%>',<%=i%>, '<%=type%>')"><%=list.get(i).tid%></a></td>
							</tr>
							<%
								}
							%>
						</table>
					</div>
				</div>
			</td>
			<td style="width: 10px"></td>
			<td valign="top" style="width: 700px">
				<div class="menu shadow">
					<button id="getMore" onclick="getMore(true, false)"><span class="glyphicon glyphicon-align-justify"></span> Get More Logs</button> 
					<button id="zoomIn"><span class="glyphicon glyphicon-zoom-in"></span> Zoom in</button> 
					<button id="zoomOut"><span class="glyphicon glyphicon-zoom-out"></span> Zoom out</button>
					<button id="moveLeft"><span class="glyphicon glyphicon-step-backward"></span> Move left</button> 
					<button id="moveRight"><span class="glyphicon glyphicon-step-forward"></span> Move right</button>
				</div> <br />
				<div id="visualization" class="shadow" style="height: 424px;">
				<table style="margin: auto;"><tr>
				<td><div class="Stale" style="border: 1px solid; border-radius: 8px; padding-left: 5px; padding-right: 5px;"><span class="glyphicon glyphicon-alert"></span> Anomalous Read</div></td>
				<td style="width: 10px;"></td>
				<td><div class="Read" style="border: 1px solid; border-radius: 8px; padding-left: 5px; padding-right: 5px;"><span class="glyphicon glyphicon-check"></span> Read Transaction</div></td>
				<td style="width: 10px;"></td>
				<td><div class="Write" style="border: 1px solid; border-radius: 8px; padding-left: 5px; padding-right: 5px;"><span class="glyphicon glyphicon-edit"></span> Write Transaction</div></td>
				</tr></table>
				</div>
				<script type="text/javascript">
	window.onload = function () {
		// attach events to the navigation buttons
		document.getElementById('zoomIn').onclick = function() {zoom(-0.2);};
		document.getElementById('zoomOut').onclick = function() {zoom(0.2);};
		document.getElementById('moveLeft').onclick = function() {move(0.2);};
		document.getElementById('moveRight').onclick = function() {move(-0.2);};
	}
	</script>
				<div id="logsInfoDiv" style="width: 700px; height: 200px; overflow: auto;">
				<table id="logsInfoTable"><tr id="logsInfoTableTr"></tr></table>
				</div>
			</td>
		</tr>
	</table>
</body>
</html>
<%
	}
		}
	}
%>