# Polygraph
Polygraph quantifies number of anomalies produced by an application.

### Authors  
Yazeed Alabdulkarim, Marwan Almaymoni, and Shahram Ghandeharizadeh

### Features

* Quantifies the number of anomalies produced by an application.

* Operates in either an online or an offline setting.

* Input is log records stored in Kafka or a directory of files.

* Consists of 3 sub-projects:  [Authoring](#polygraph-authoring), [Validation](#polygraph-validation), and [Monitoring](#polygraph-monitoring).   

### Overview

Polygraph is data store agnostic and produces no false positives when quantifying the number of anomalies produced by an application and all its components.  The software and hardware limitations and design choices that produce anomalies are categorized into isolation, linearizability, and atomicity violation of transaction properties.  See [database lab technical report 2017-02](http://dblab.usc.edu/Users/papers/PolygraphMay2017.pdf) for details.

Polygraph consists of three distinct phases:  [Authoring](#polygraph-authoring), [Validation](#polygraph-validation), and [Monitoring](#polygraph-monitoring).  During the authoring phase, the experimentalist employs Polygraph's visualization tool to identify entities and relationship sets of an application, and those actions that constitute each transaction and their referenced entity/relationship sets. An action may insert or delete one or more entities/relationships, and/or read or update one or more attribute values of an entity/relationship. For example, the payment transaction of the TPC-C benchmark reads and updates a customer entity.

Authoring produces the following three outputs.  First, for each transaction, Polygraph generates a transaction specific code snippet to be embedded in each transaction.  These snippets generate a log record for each executed transaction.  They push log records to a distributed framework for processing by a cluster of Polygraph servers. Second, it generates a configuration file for Polygraph servers, customizing it to process the log records.  Third, it identifies how the log records should be partitioned for parallel processing, including both Kafka topics and the partitioning attributes of the log records.

During the deployment phase, the experimentalist deploys (1) the Kafka brokers and Zookeepers and creates the topics provided as output of the Authoring step, (2) Polygraph servers using the configuration files provided as output of the Authoring step, and (3) the application whose transactions are extended with the code snippets as output of the Authoring step.  

During the monitoring phase, the experimentalist uses Polygraph's monitoring tool to view those read transactions that produce anomalies.  For each such transaction, Polygraph shows the transactions that read/ wrote the entities/relationships referenced by the violating transaction.

Below, we provide a description of how to deploy Authoring, Validation, and Monitoring components.

 ### Polygraph Authoring
 
Polygraph Authoring generates code snippets to extend transactions to create and publish log records. It currently supports Java. 

**Getting Started**

1. Download Polygraph Authoring code

2. Import as JavaEE project

3. Run the mainpage "app.jsp". This step may require configuring Tomcat server.

4. Specify the application name

5. Click on "Edit ER" to provide the application ER diagram

6. After completing, click on "Save ER"

7. Click on the Authoring link on the left corner to return to mainpage

8. Click on "Add Transaction" button to add more transactions

9. Specify the transaction name

10. Click on entity/relationship sets (from the ER canvas on the right) referenced by the transaction. 

11. For each referenced entity/relationship set:

    1. Specify the number of entities/relationships referenced

    2. Specify the action type, such as read or update

    3. Specify the variable name holding the primary key value

    4. For each referenced property, specify the variable name holding its value and the action type

12. Click on "Save"

13. Click on the Authoring link on the left corner to return to mainpage

14. Click on "Generate Code" to generate the code snippets

15. Include the source files under "common" folder with the generated code snippets

 ### Polygraph Validation

 Polygraph Validation component processes log records and has the following configuration parameters:

	

<table>
  <tr>
    <td><strong>Parameter</strong></td>
    <td><strong>Description</strong></td>
    <td><strong>Example</strong></td>
  </tr>
  <tr>
    <td>app</td>
    <td>The application name which also represents the generated topic name in Kafka</td>
    <td>-app TPCC</td>
  </tr>
  <tr>
    <td>er</td>
    <td>The entity/relationship sets json file which describes the entities/relationships of the application</td>
    <td>-er /home/yaz/erFile


</td>
  </tr>
  <tr>
    <td>numvalidators</td>
    <td>The total number of validation threads across all Polygraph servers</td>
    <td>-numvalidators 3</td>
  </tr>
  <tr>
    <td>numpartitions</td>
    <td>The number of partitions for the topic</td>
    <td>-numpartitions 10</td>
  </tr>
  <tr>
    <td>numclients</td>
    <td>Number of Polygraph servers</td>
    <td>-numclients 1</td>
  </tr>
  <tr>
    <td>clientid</td>
    <td>The id of this Polygraph server [0 to numclients-1]</td>
    <td>-clientid 0</td>
  </tr>
  <tr>
    <td>kafka</td>
    <td>Boolean flag which configures Polygraph to process log records from Kafka if set to true, or from a directory of files if set to false</td>
    <td>-kafka true</td>
  </tr>
  <tr>
    <td>filelogdir</td>
    <td>The directory of the log records to be processed (kafka parameter must be set to false).</td>
    <td>-filelogdir /home/yaz/dir</td>
  </tr>
  <tr>
    <td>online</td>
    <td>Boolean flag which configures Polygraph servers to online mode if sets to true (kafka parameter must be set to true)or offline if sets to false.</td>
    <td>-online true</td>
  </tr>
  <tr>
    <td>printfreq</td>
    <td>The frequency of printing stats message every this number of reads</td>
    <td>-printfreq 1000</td>
  </tr>
  <tr>
    <td>kafkahosts</td>
    <td>Kafka host string: IP1:port1,IP2:port2,...</td>
    <td>-kafkahost 10.0.0.127:9298</td>
  </tr>
  <tr>
    <td>zookhosts</td>
    <td>Zookeperhost string: IP1:port1,IP2:port2,...</td>
    <td>-zookhost 10.0.0.127:2128</td>
  </tr>
  <tr>
    <td>buffer</td>
    <td>The number of log records to be buffered in memory from each Kafka partition (kafka parameter must be set to true)</td>
    <td>-buffer 100</td>
  </tr>
  <tr>
    <td>freshness</td>
    <td>Boolean flag which configures Polygraph servers to compute freshness confidence</td>
    <td>-freshness true</td>
  </tr>
</table>


   

**Getting Started**

1. Download Polygraph Validation code

2. Import the project as Java project

3. To use Polygraph with Kafka:

    1. Launch Kafka brokers ([see Kafka website](https://kafka.apache.org/))

    2. Create the application topic with the desired number of partitions. The number of partitions must be equivalent to numpartitions (the configuration parameter of Polygraph) * 2

4. Launch your application extended with code snippets to generate log records. 

5. Launch Polygraph "ValidationMain" class configuring the above parameters. For example, you may use the following parameters:

     -numvalidators 1 -app tpcc -printfreq 1000 -buffer 5000 -numpartitions 1 -numclients 1 -clientid 0 -kafkahosts 127.0.0.1:9298 -     zookhosts 127.0.0.1:2128 -er erFilePath -kafka true

 ### Polygraph Monitoring

Polygraph Monitoring visualizes anomalous transactions. 

**Getting Started**

1. Download Polygraph Monitoring code

2. Import as JavaEE project

3. Run the mainpage "visual.jsp". This step may require configuring Tomcat server.

4. Specify the topic name, Kafka and Zookeeper hosts

5. Click on "visualize"

6. It is going to show anomalies based on referenced entities/relationships on the left panel and based on transactions on the right panel. The freshness confidence graph is shown at the bottom if computed by Polygraph servers.

7. Clicking on an entity/relationship or a transaction name show the anomalies referencing it.

8. Click on the transaction id on the left panel to visualize the anomaly. 

