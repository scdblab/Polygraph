
<%@ page import="java.sql.*"%>
<%
	
		
		
			String data = request.getParameter("data");
			//MySQLResponse i = MySQL.updateER(userid, appid, data);
			session.setAttribute("er", data);
			out.print( "Saved");
		

%>