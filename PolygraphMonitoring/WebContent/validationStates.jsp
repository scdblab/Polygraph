<%
	if ((session.getAttribute("userid") == null) || (session.getAttribute("userid") == "")) {
		out.print("You are not logged in<br/><a href=\"index.jsp\">Please Login</a>");
	} else {
		int userid = (int) session.getAttribute("userid");
		String appName = request.getParameter("appName");
		String topic = "STATS_" + appName;
		session.setAttribute("appName", appName);
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
<script src="js/stats.js" type="text/javascript"></script>
<link rel="stylesheet" href="css/style.css" type="text/css">
<link rel="stylesheet" href="css/menubar.css" type="text/css">
<style>
table.evenOdd tr:nth-child(even) {
	background: #FFF
}

table.evenOdd tr:nth-child(odd) {
	background: #CCC
}
</style>
</head>
<body style="text-align: center;">
	<ul>
		<li><a href="mainPage.jsp">Applications</a></li>
		<li><a id="barAppName" class="active">Visualize <%=appName%></a></li>
		<li style="float: right"><a href="logout.jsp">Logout</a></li>
	</ul>
	<br />
	<table style="margin-left: auto; margin-right: auto;">
		<tr>
			<td colspan="3">Running Application: <%=appName%><br>
			<br>
				<table style="margin-left: auto; margin-right: auto;">
					<tr>
						<td width="200px"><button onclick="getData()">test</button>
						</td>
						<td width="200px"><a href="mainPage.jsp">Main Page</a></td>
					</tr>
				</table>
			</td>
		</tr>
		<tr>
			<td colspan="3"><table id="Stats"
					style="margin-left: auto; margin-right: auto;">
					<tr>
						<td style="text-align: left;">Read count: 0<br> Write
							count: 0<br> stale count: 0<br> schedule avg: 0<br>
							schedule max: 0<br> mem avg: 0<br> mem max: 0<br>
							partially discarded: 0<br> fully discarded: 0<br> Run
							duration: 00:00.000
						</td>
					</tr>
				</table></td>
		</tr>
		<tr>
			<td valign="top">
				<table style="border: solid 1px black;">
					<tr>
						<th width="120px">E/R Name</th>
						<th width="100px">Stale count</th>
					</tr>
				</table>
				<div style="overflow: auto; width: 250px; height: 500px">
					<table id="ERTable" class="evenOdd"
						style="border: solid 1px black; border-collapse: collapse;">
					</table>
				</div>
			</td>
			<td width="100px"></td>
			<td valign="top">
				<table style="border: solid 1px black;">
					<tr>
						<th width="120px">T Name</th>
						<th width="100px">Stale count</th>
					</tr>
				</table>
				<div style="overflow: auto; width: 250px; height: 500px">
					<table id="TTable" class="evenOdd"
						style="border: solid 1px black; border-collapse: collapse;">
					</table>
				</div>
			</td>
		</tr>
	</table>
</body>
</html>
<%
	}
%>