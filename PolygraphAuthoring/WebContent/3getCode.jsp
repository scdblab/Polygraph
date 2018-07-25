<%@page import="edu.usc.polygraph.Common"%>
<%@page import="edu.usc.polygraph.codegenerator.CodeGeneratorFunctions"%>
<%@page import="java.util.ArrayList"%>

<%@ page import="java.sql.*"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@page import="java.util.HashMap"%>

<%

		HashMap<String,String> trans=(HashMap<String,String>) session.getAttribute("trans");

		
		String appName = request.getParameter("appName");
		
		
		ArrayList<String> trans1 = new ArrayList<String>();
		for (String key:trans.keySet()) {
			
			trans1.add(trans.get(key));
		}
		String er = (String)session.getAttribute("er");
		ArrayList<String> result = CodeGeneratorFunctions.getCodeSnippet(er, trans1, appName);		
		Common.export(response, Common.getZipFile(result, 1, 1));
	
%>