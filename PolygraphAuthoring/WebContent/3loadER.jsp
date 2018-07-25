
<%@ page import="java.sql.*"%>
<%
	
		if ((session.getAttribute("appName") == null) || (session.getAttribute("er") == null)) {
			response.sendRedirect("app.jsp");
		} else {
			
			//MySQLResponse i = MySQL.loadER(userid, appid);
			out.print(session.getAttribute("er"));
		}
	
%>