<%-- 
    Document   : index
    Created on : Aug 22, 2017, 2:06:23 PM
    Author     : yaz
--%>
<%@page import="edu.usc.polygraph.codegenerator.Transaction"%>
<%@page import="java.util.HashMap"%>

<%@page import="edu.usc.polygraph.PolygraphUISettings"%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
	
	
		
			
			if (session.getAttribute("appName")==null ||session.getAttribute("appName")=="")
			session.setAttribute("appName", "AppName");
			if (session.getAttribute("er")==null || session.getAttribute("er")==""){
				String er= PolygraphUISettings.ER;
				session.setAttribute("er", er);
			}
			HashMap<String,String> trans=(HashMap<String,String>) session.getAttribute("trans");
			if (trans==null){
				trans= new HashMap<String,String>();
				trans.put(trans.size()+":"+"Withdraw",PolygraphUISettings.WITHDRAW_STR );
				session.setAttribute("trans", trans);
			}
		
			
%>

<!DOCTYPE html>

<html>
    <head>
    
           <link rel="stylesheet" href="css/style.css" type="text/css">
                         <link rel="stylesheet" href="css/style2.css" type="text/css">
       
       <script type="text/javascript" src="javascript.js"></script>
  		<script src="moment.js"></script>
		<script src="Chart.js"></script>
		<script src="utils.js"></script>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<script src="js/erCommon.js" type="text/javascript"></script>
		<script src="js/app.js" type="text/javascript"></script>
<script>
	window.onload = function() {
		init("view");
		loadER(false);
	};
</script>
        <title>Authoring</title>
		
    <style type="text/css">
    
    </style>
  
 
    </head>
   <body id="mybody" class="myBody" >
   
        <table class="menut" style="width:100%"><tr style="background-color:#134F5C; height: 50px;"><td style="width: 90px" ><img style="width:150px; height:50px" src="img/dblab.png" /></td>
        <td style="width: 200px;vertical-align: bottom;"><a class="menu" style="" href="app.jsp"  >Authoring Main</a></td>
                <td style="align-content: center; color: white; font-size: x-large" align="center"> <strong>POLYGRAPH</strong><br/> <label style="font-size: large">(Authoring)</label></td>
            </tr>
            <tr ><td style="background-color: #134F5C; vertical-align: top;height: 720px;" colspan="1">
                    <table class="menut" >
                           
                    </table>
                    
                 <!-- > Body of Page<-->
                <td  style="vertical-align:top; padding-top: 20px; padding-left:50px " align="left" colspan="2">
       <center>
	<input type="text" id="app" value=<%=session.getAttribute("appName")%> style="width:100px;align:center">
	<button onclick="saveApp()">Save</button> <span id="buttonResult2"></span>
	</center>
	<table class="shadow" style="width: <%=PolygraphUISettings.width%>px;">
		<tr>
			<td style="text-align: center;"><form method="post" action="3getCode.jsp" style="margin-bottom: 0px;">
					<input type="hidden" name="appName" value="<%=session.getAttribute("appName")%>" /><input type="submit" style="width: 200px;" value="Generate Code" />
				</form></td>
				
				
			<td><table style="margin: auto; border: 0px;">
					<tr>
						
						<td>
							<form method="post" action="editER.jsp" style="margin-bottom: 0px;">
								<input type="submit" value="Edit ER" />
							</form>
						</td>
					</tr>
				</table></td>
		</tr>
		<tr>
			<td style="vertical-align: top; width: <%=PolygraphUISettings.transTableWidth%>px; background-color: #caccd0;">
				<table style="width: 100%; height: 65px;">
					<tr>
						<th style="background-color: white;">Transactions</th>
					</tr>
					<tr>
						<td style="text-align: center;">
							<form method="post" action="trans.jsp">
								<input class="add" type="submit" value="Add Transaction" style="width: 250px;" />
							</form>
						</td>
					</tr>
				</table> 
				<div style="overflow: scroll; height: 435px;"><%
 				String res="";
				String seperator = "";

				for(String key:trans.keySet()) {
					String tokens[]= key.split(":");
					res+= seperator + tokens[0] + "," + tokens[1];
					if(seperator.equals(""))
						seperator = ";";
				}
 			String[] lines = res.split("[;]");
 			for (int i = 0; i < lines.length; i++) {
 				String[] tokens = lines[i].split("[,]");
 				if (tokens.length != 2)
 					continue;
 %>
				<table style="width: 95%; margin: auto; margin-bottom: 4px;background-color: #e9e9e9;">
					<tr>
						<td><%=tokens[1]%></td>
						<td style="width: 50px;">
							<form method="get" action="trans.jsp" style="margin-bottom: 0px;">
								<input type="hidden" name="transID" value="<%=tokens[0]%>" /> <input type="hidden" name="transName" value="<%=tokens[1]%>" /> <input type="submit" value="Edit" />
							</form>
						</td>
						<td style="width: 35px;">
							<form method="post" action="3delTrans.jsp" style="margin-bottom: 0px;">
								<input type="hidden" name="transID" value="<%=tokens[0]%>" />
							<input type="hidden" name="transName" value="<%=tokens[1]%>" /> <input class="delete" type="submit" value="&#10006;" />
							
							</form>
						</td>
					</tr>
				</table> <%
 	}
 %></div>
			</td>
			<td>
				<div style="width: <%=PolygraphUISettings.canvasDivWidth%>px; height: <%=PolygraphUISettings.canvasDivHeight%>px; overflow: scroll;">
					<canvas id="myCanvas" width="<%=PolygraphUISettings.canvasWidth%>" height="<%=PolygraphUISettings.canvasHeight%>" style="border:1px solid #d3d3d3;"></canvas>
				</div>
			</td>
		</tr>
	</table>
             
             
             
             
             
             
             
             
             
                          
               </td>
            
            </tr>
            
                
        </table>
        
          


    </body>
</html>