

<%-- 
    Document   : index
    Created on : Aug 22, 2017, 2:06:23 PM
    Author     : yaz
--%>


<%@page import="edu.usc.polygraph.PolygraphUISettings"%>
<%

		if ((session.getAttribute("appName") == null) || (session.getAttribute("appName") == "") || (session.getAttribute("er") == null) || (session.getAttribute("er") == "")) {
			response.sendRedirect("app.jsp");
		} else {
			
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
        <script src="js/common.js" type="text/javascript"></script>
<script src="js/editER.js" type="text/javascript"></script>
<script src="js/erCommon.js" type="text/javascript"></script>
<script>
window.onload = function () {
	init("edit");
	loadER(false);
};
</script>
<style>
table {
	margin: 0 auto;
}

tr {
	margin: 0 auto;
}

td {
	margin: 0 auto;
}
</style>
<link rel="stylesheet" href="css/style.css" type="text/css">
        
         
        <title>Authoring/ER</title>
		
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
            		 <table  style=" margin: auto;">
		<tr>
			<td align="center"><button onclick="saveER()">Save ER</button></td>
			<td align="center" style="width: 250px"><div align="left" id="buttonResult"></div></td>
			
		</tr>
	</table>
	
	<table class="shadow" style="width: <%=PolygraphUISettings.width%>px; margin: auto;">
		<tr>
		
		<td style="width: 400px; background-color: #caccd0; vertical-align: top; padding: 10px; text-align: center;">
				<div id="details"></div>
			</td>
			<td>
				<table>
					<tr>
						<td><div id="EntityButtonTD">
								<button onClick="newElement('Entity')">
									<img src="img/entity.png" />
								</button>
							</div></td>
						<td><div id="RelationshipButtonTD">
								<button onClick="newElement('Relationship')">
									<img src="img/relationship.png" />
								</button>
							</div></td>
					</tr>
				</table>
				<div style="width: 500px; height: 500px; overflow: scroll;">
					<canvas id="myCanvas" width="1000" height="1000" style="border: 1px solid #d3d3d3;"></canvas>
				</div>
			</td>
			
		</tr>
	</table>             
               
            
                
        </table>
        
          


    </body>
</html>
<%
	}
	
%>