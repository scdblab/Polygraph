<%-- 
    Document   : index
    Created on : Aug 22, 2017, 2:06:23 PM
    Author     : yaz
--%>

<%@page import="edu.usc.polygraph.PolygraphUISettings"%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.HashMap"%>

<%
if ((session.getAttribute("appName") == null) || (session.getAttribute("appName") == "") || (session.getAttribute("er") == null) || (session.getAttribute("er") == "")) {
	response.sendRedirect("app.jsp");
} else {
HashMap<String,String> trans=(HashMap<String,String>) session.getAttribute("trans");

		
			
			
			int tid = (request.getParameter("transID") == null
					? -1
					: Integer.parseInt(request.getParameter("transID")));
			String transName = (request.getParameter("transName") != null
					? request.getParameter("transName")
					: "New Transaction");
			String title = (tid == -1
					? "Adding a new Transaction"
					: "Editing transaction \"" + transName + "\"");
			if (tid == -1) {
				//tid = trans.size();
				//trans.put(tid+":"+transName, "{\"Name\":\""+transName+"\",\"Elements\":[]}");
				
				//System.out.println("i.msg = " + i.msg);
				
				//System.out.println("tid = " + tid);
			}
%>

<!DOCTYPE html>

<html>
    <head>

<script src="js/common.js" type="text/javascript"></script>
<script src="js/trans.js" type="text/javascript"></script>
<script src="js/erCommon.js" type="text/javascript"></script>
<script>
window.onload = function () {
	init("click");
	loadER(false);
	setTimeout(loadT, 10);
	
};
function loadT(){
	loadTrans(<%=tid%>,"<%=transName%>", false);
}
</script>
           <link rel="stylesheet" href="css/style.css" type="text/css">
                         <link rel="stylesheet" href="css/style2.css" type="text/css">
       
       <script type="text/javascript" src="javascript.js"></script>
        
  		<script src="moment.js"></script>
		<script src="Chart.js"></script>
		<script src="utils.js"></script>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        
         
        <title>Authoring/Transactions</title>
		
    <style type="text/css">
    
    </style>
  
 
    </head>
   <body id="mybody" class="myBody" >
   
        <table class="menut" style="width:100%"><tr style="background-color:#134F5C; height: 50px;"><td style="width: 90px" ><img style="width:150px; height:50px" src="img/dblab.png" /></td>
                                    <td style="width: 200px;vertical-align: bottom;"><a class="menu" style="" href="app.jsp"  >Authoring Main</a></td>
                
                <td style="align-content: center; color: white; font-size: x-large" align="center"> <strong>POLYGRAPH</strong><br/> <label style="font-size: large">(Authoring)</label></td>
            </tr>
            <tr ><td style="background-color: #134F5C; vertical-align: top;height: 720px;">
                    <table class="menut" >
                        <tr>
                            <td style="width: 190px; height: 103px">  <ul class="menu" >
                                
                                         </ul>
                                    
                                    </td> </tr>
                        <tr>
                            <td style="width: 190px; height: 145px">  <ul class="menu" >
                                        
                                        
                                         </ul>
                                    
                                    </td> </tr>
                        
                        <tr>
                            <td style="width: 190px; height: 61px">  <ul class="menu" >
                                            
                                          
                                         </ul>
                                    
                                    </td> </tr>
                        
                         <tr>
                            <td style="width: 190px; height: 61px">  <ul class="menu" >
                                          
                                         </ul>
                                    
                                    </td> </tr>
                         
                        
                        
                    </table>
                    
                 <!-- > Body of Page<-->
                <td  style="vertical-align:top; padding-top: 20px; padding-left:50px " align="left" colspan="2">
             	
	
	<table  style="width: <%=PolygraphUISettings.width %>px">
		<tr>
			<td style="text-align: center">Transaction Name <input id="transName" type="text" value="<%=transName%>" /> <!-- onblur="updateTransName(this)" --> <br /></td>
		<td align="center"><button onclick="saveTrans(<%=tid%>,'<%=transName%>')">Save</button></td>
			<td align="center" style="width: 250px"><div align="left" id="buttonResult"></div></td>
		
		
		</tr>
	</table>
	
	<table class="shadow" style="width: <%=PolygraphUISettings.width %>px">
		<tr>
			<td colspan="2" style="text-align: center;">Select participating entity and relationship sets from the ER diagram.</td>
		</tr>
		<tr>
			<td style="vertical-align: top; background-color: #caccd0; width: 394px;">
				<div style="width: 394px; height: 500px; overflow: scroll;">
					<table style="width: 100%; margin: auto;">
						<tr>
							<th style="background-color: white; box-shadow: 5px 5px 10px gray;">E/R Sets</th>
						</tr>
						<tr>
							<td><br />
								<div id="er_sets"></div></td>
						</tr>
					</table>
				</div>
			</td>
			<td>
				<div style="width: 500px; height: 500px; overflow: scroll;">
					<canvas id="myCanvas" width="1000" height="1000" style="border:1px solid #d3d3d3;"></canvas>
				</div>
			</td>
		</tr>
	</table>             
               </td>
            
            </tr>
            
                
        </table>
        
          


    </body>
</html>
<%
	}
	
%>