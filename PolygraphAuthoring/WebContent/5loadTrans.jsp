
<%@ page import="java.sql.*"%>
<%@page import="java.util.HashMap"%>

<%

		HashMap<String,String> trans=(HashMap<String,String>) session.getAttribute("trans");


		if ((session.getAttribute("trans") == null)) {
			response.sendRedirect("app.jsp");
		} else {
			
			//System.out.println("request.getParameter(\"tid\") = " + request.getParameter("tid"));
			int tid = Integer.parseInt(request.getParameter("tid"));
			String tname=request.getParameter("tname");
			if (tid != -1) {
				
				out.print(trans.get(tid+":"+tname));
			}
		}
	
%>