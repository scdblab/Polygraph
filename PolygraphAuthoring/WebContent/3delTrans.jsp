<%@page import="java.util.HashMap"%>


<%@ page import="java.sql.*"%>
<%
	
		if ((session.getAttribute("trans") == null) || (session.getAttribute("trans") == "")) {
			response.sendRedirect("app.jsp");
		} else {
	
			String tid = request.getParameter("transID");
			String name=request.getParameter("transName");
			HashMap<String,String> trans=(HashMap<String,String>) session.getAttribute("trans");
			trans.remove(tid+":"+name);
			
			
				response.sendRedirect("app.jsp");
		
		}
	
%>