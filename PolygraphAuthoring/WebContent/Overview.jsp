<%-- 
    Document   : index
    Created on : Aug 22, 2017, 2:06:23 PM
    Author     : yaz
--%>

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
        
         
        <title>Polygraph Overview</title>
		
    <style type="text/css">
  
    </style>
  
 
    </head>
    <body id="body" >
   
        
   
        <table class="menut" style="width:100%"><tr style="background-color:#134F5C; height: 50px;"><td style="width: 190px" ><img style="width:150px; height:50px" src="img/dblab.png" /></td>
                <td style="align-content: center; color: white; font-size: x-large" align="center"> <strong>POLYGRAPH</strong><br/> <label style="font-size: large">(Overview)</label></td>
            </tr>
            <tr ><td style="background-color: #134F5C; vertical-align: top;height: 1150px;">
                    <table class="menut" >
                        <tr>
                            <td style="width: 190px; height: 103px">  <ul class="menu" >
                                    <li><a class="menu" href="index.jsp" style="margin-left: 0px;padding-left: 0px" >Demo</a></li> 
                                            <li><a class="menu" style="margin-left: 35px;" href="DemoSettings.jsp" >Settings</a></li> 
                                            <li><a class="menu" style="margin-left: 35px;" href="DemoDesc.jsp" >Description</a></li> 

                                            
                                         </ul>
                                    
                                    </td> </tr>
                        <tr>
                            <td style="width: 190px; height: 145px">  <ul class="menu" >
                                    <li><a id="pp" class="menu" href="index.jsp" style=" pointer-events: none;cursor: default;" disabled="disabled" >Polygraph</a></li> 
                                            <li><a class="menu" style="margin-left: 35px;" href="mont.jsp" >Monitoring</a></li> 
                                             <li><a class="menu" style="margin-left: 35px;" href="Overview.jsp" >Overview</a></li> 
                                            <li><a class="menu" style="margin-left: 35px;" href="Papers.jsp" >Papers</a></li> 
                                         </ul>
                                    
                                    </td> </tr>
                        
                        <tr>
                            <td style="width: 190px; height: 61px">  <ul class="menu" >
                                            <li><a class="menu" href="about.jsp" >About us</a></li> 
                                          
                                         </ul>
                                    
                                    </td> </tr>
                        
                         <tr>
                            <td style="width: 190px; height: 61px">  <ul class="menu" >
                                            <li><a class="menu" target="_blank"  href="http://dblab.usc.edu" >http://dblab.usc.edu</a></li> 
                                          
                                         </ul>
                                    
                                    </td> </tr>
                         
                        
                        
                    </table>
                    
                 <!-- > Body of Page<-->
                <td  style="vertical-align:top; padding-top: 20px; padding-left:50px " align="left">
              <div>
             	<p> Polygraph is a tool to quantify system behavior that violates serial execution of transactions.
             	 It is a plug-n-play framework that includes visualization tools to empower an experimentalist to (a) quickly incorporate Polygraph into 
             	 an existing application or benchmark and (b) reason about anomalies and the transactions that produce them. 
  				   
             	</p>
             	
             	<p>
             	An experimentalist uses Polygraph Authoring tool to generate: 1) code snippets for transactions of an application/benchmark, 2) Kafka topics, and 3) Polygraph configuration file. 
             	The code snippets are to embed into transactions to generate and publish log records to Kafka, which is messaging framework (see Kafka <a target="_blank"  href="https://kafka.apache.org/">website</a> ).
             	 Polygraph server(s) pull these log records from Kafka and validates them by computing a serial schedule to quantify the amount of anomalies produced by the application.
             	</p>
             	
             	<p>
             	Polygraph provides a monitoring tool to view those read transactions that produce anomalies. For each
                 such transaction, Polygraph shows the expected value(s) along with the transactions that
                   read/wrote the entities/relationships referenced by the violating transaction.
             	</p>		
					<div style="width=100%; " align="center">
					<img style="width:600px; height:300px" src="img/authdeploy.png" />
					</div>
				
              
              </div>  
                <p> <strong>Configuration Parameters</strong></p>
                <div>
                <table border="1" style="border-collapse: collapse;">
                <tr>
                <th> Parameter</th> <th> Description</th> 
                </tr>
                <tr>
                <td>app </td> <td> The application which represents the generated topic name in Kafka </td> 
                </tr>
                 <tr>
                <td> er</td> <td> The entity/relationship sets json file generated in Authoring step </td> 
                </tr>
                <tr>
                <td> numvalidators</td> <td> The total number of validation threads across all Polygraph servers</td> 
                </tr>
                
                <tr>
                <td> numpartitions</td> <td>The number of partitions for the topic</td> 
                </tr>
                
                <tr>
                <td> numclients</td> <td> Number of Polygraph servers</td> 
                </tr>
                
                <tr>
                <td> clientid</td> <td> The id of this Polygraph server [0 to numclients-1]</td> 
                </tr>
                
                <tr>
                <td> online</td> <td> Boolean flag which configure Polygraph servers to online mode if sets to true or offline if sets to false</td> 
                </tr>
                <tr>
                <td> printfreq</td> <td> The frequency of printing stats message every this number of reads</td> 
                </tr>
                
                <tr>
                <td> buffer</td> <td> The max buffer size for log records. Polygraph sets the size of a log record= number of its characters</td> 
                </tr>
                
                <tr>
                <td> bufferthreshold</td> <td> A number between 0 and 1. If the current size of the buffer is less than (bufferthreshold * buffer), Polygraph starts filling the buffer </td> 
                </tr>
                <tr>
                <td> kafkahost</td> <td> IP1:port1,IP2:port2,...</td> 
                </tr>
                <tr>
                <td> zookhost</td> <td> IP1:port1,IP2:port2,...</td> 
                </tr>
                </table>
                </div>
                
                <p> For example, Polygraph can be ran with the following parameters: <br/>
          java edu.usc.stalemeter.ValidationMain -numvalidators 1 -app topic1 -printfreq 1000 -buffer 10240 -bufferthreshold 0.5 -numpartitions 10 -numclients 1 -clientid 0 -kafkahost 127.0.0.1:9298 -zookhost 127.0.0.1:2128 -online true 
          -er erFilePath </p>
          
          
           <p> <strong>Download:</strong> <a target="_blank"  href="https://github.com/yaz1/Polygraph" >Polygraph code on Github</a></p>
          
          
               </td>
            
            </tr>
            
                
        </table>
        
          


    </body>
</html>