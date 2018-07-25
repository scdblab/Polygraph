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
<script>
	window.onload = function() {
		var tableToShow = "<%=request.getAttribute("tableToShow")%>";
		switch (tableToShow) {
		case "login":
			loginUI();
			break;
		case "reg":
			registerUI();
			break;
		default:
			loginUI();
			break;
		}
	};
	function loginUI() {
		var div = document.getElementById("table");
		while (div.firstChild) {
			div.removeChild(div.firstChild);
		}

		var form = document.createElement("form");
		form.setAttribute("method", "post");
		form.setAttribute("action", "1login.jsp");

		var trUser = document.createElement("tr");
		var tdUserLabel = document.createElement("td");
		var tdUserInput = document.createElement("td");
		tdUserLabel.appendChild(document.createTextNode("Username"));
		tdUserInput.appendChild(createInput("username", "username", "text", null, null, [], [], null));
		trUser.appendChild(tdUserLabel);
		trUser.appendChild(tdUserInput);

		var trPass = document.createElement("tr");
		var tdPassLabel = document.createElement("td");
		var tdPassInput = document.createElement("td");
		tdPassLabel.appendChild(document.createTextNode("Password"));
		tdPassInput.appendChild(createInput("password", "password", "password", null, null, [], [], null));
		trPass.appendChild(tdPassLabel);
		trPass.appendChild(tdPassInput);

		var trSubmit = document.createElement("tr");
		var tdSubmit = document.createElement("td");
		tdSubmit.setAttribute("colspan", "2");
		tdSubmit.appendChild(createInput(null, null, "submit", null, null, [], [], "Login"));
		trSubmit.appendChild(tdSubmit);

		var table = createTable(null, null, "width: 260px; background-color: #80a9e6;", [ trUser, trPass, trSubmit ]);

		form.appendChild(table);
		div.appendChild(form);
	}
	function registerUI() {
		var div = document.getElementById("table");
		while (div.firstChild) {
			div.removeChild(div.firstChild);
		}
		var form = document.createElement("form");
		form.setAttribute("method", "post");
		form.setAttribute("action", "1reg.jsp");

		var trUser = document.createElement("tr");
		var tdUserLabel = document.createElement("td");
		var tdUserInput = document.createElement("td");
		tdUserLabel.appendChild(document.createTextNode("Username"));
		tdUserInput.appendChild(createInput("username", "username", "text", null, null, [], [], null));
		trUser.appendChild(tdUserLabel);
		trUser.appendChild(tdUserInput);

		var trPass = document.createElement("tr");
		var tdPassLabel = document.createElement("td");
		var tdPassInput = document.createElement("td");
		tdPassLabel.appendChild(document.createTextNode("Password"));
		tdPassInput.appendChild(createInput("password", "password", "password", null, null, [], [], null));
		trPass.appendChild(tdPassLabel);
		trPass.appendChild(tdPassInput);

		var trSubmit = document.createElement("tr");
		var tdSubmit = document.createElement("td");
		tdSubmit.setAttribute("colspan", "2");
		tdSubmit.appendChild(createInput(null, null, "submit", null, null, [], [], "Register"));
		trSubmit.appendChild(tdSubmit);

		var table = createTable(null, null, "width: 260px; background-color: #ade86b;", [ trUser, trPass, trSubmit ]);

		form.appendChild(table);
		div.appendChild(form);
	}
</script>
</head>
<body>
	<img alt="Polygraph" src="img/polygraph.png"><br/><br/>
	<%
		if (request.getAttribute("info") != null)
			if (request.getAttribute("info").equals("error")) {
	%>
	<div style="text-align: center; margin: auto; font-family: sans-serif; font-weight: bold; color: red;"><%=request.getAttribute("errorMSG")%></div>
	<%
		}
	%>
	<table class="Shadow" style="margin-left: auto; margin-right: auto; border: solid 1px black;border-collapse: collapse; width: 260px;">
		<tr>
			<td id="loginTD" class="clickable" style="background-color: #80a9e6; text-align: center; width: 50%;"><a onclick="loginUI()">Login</a></td>
			<td id="registerTD" class="clickable" style="background-color: #ade86b; text-align: center; width: 50%;"><a onclick="registerUI()">Register</a></td>
		</tr>
		<tr>
			<td colspan="2" style="padding: 0px;"><div id="table"></div></td>
		</tr>
	</table>
</body>
</html>