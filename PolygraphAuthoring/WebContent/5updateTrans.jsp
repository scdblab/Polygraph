
<%@ page import="java.sql.*"%>
<%@page import="java.util.HashMap"%>

<%

		HashMap<String,String> trans=(HashMap<String,String>) session.getAttribute("trans");

	
		
			
			String name = request.getParameter("name");
			String tname = request.getParameter("tname");
			//System.out.println(tname+"ggg"+ name);
			String data = request.getParameter("data");
			int tid = Integer.parseInt(request.getParameter("tid"));
			if (tid == -1) {
				if(name == ""){
					name = "New Transactoion";
				}
				int i= trans.size();
				trans.put(i+":"+name, data);
				out.print(i);
			} else {
				trans.remove(tid+":"+tname);
				trans.put(tid+":"+name, data);

				out.print("Saved");
			}
		
	
%>