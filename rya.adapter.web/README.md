<!-- Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License. -->

# Rya Adapter Web

___

This package provides a web REST service end point for interacting with a Rya repository either with a Jena reasoner or Pellet.

## Running with Eclipse and MAC Example

### Steps to configure Apache Tomcat

1. First set up the Tomcat Runtime from the Eclipse top-menu *"Window->Preferences->Server->Runtime Environment"*.
2. Select *"Add"*
3. Select *"Apache Tomcat v8.0"* and press *"Next"*.
4. Choose *"Download and Install..."* or "Browse..." to select your Tomcat 8 install location.
5. Select your Java 8 JDK to go with Tomcat under *"JRE"*.
6. Press *"Finish"*
7. Now from the Eclipse top-menu select *"Window->Show View->Servers"*
8. Right-click on the *"Tomcat v8.0 Server at localhost"* and choose *"Start"*

### Starting the Web Service

1. Find the *"rya.adapter.web/src/test/java/org/apache/rya/adapter/rest/reasoner/helper/TestMACDriver.java"* file in the test directory and run it as a Java application in order to start a MiniAccumuloCluster (MAC).
2. Look at the console to wait for the MAC to finish starting up.  When it's ready it will say *"MiniAccumuloCluster running with zookeeper at localhost:<port_number>"*.  Copy the *"port_number"* and paste its value into the *"instance.zk=localhost:<port_number>"* property found in the *"rya.adapter.web/src/main/resources/environment.properties"* file.
3. Right-click on the "rya.adapter.web" project and select *"Run As->Run on Server"*.  Choose the Apache Tomcat 8 Server and hit *"Finish"*
4. In a web browser goto: "http://localhost:8080/rya.adapter.web/ReasonerFileUpload.html"


### Running the Example
1. From the web page select *"Browse..."*
2. Select the file *"rya.adapter.web/src/test/resources/rdf_format_files/notation3_files/rule_files/n3_rules.txt"*
3. Use *"Jena"* as the reasoner.
4. Hit *"Upload File"*
5. This should bring back a results page that produces 1 Rya Statement.


### Building from Source

Using Git, pull down the latest code from the url above.

Run the command to build the code `mvn clean install`

If all goes well, the build should be successful and a war should be produced in *"rya.adapter.web/target/rya.adapter.web.war"*

Note: The following profiles are available to tailor the build:


| Profile ID | Purpose |
| ---------- | ------- |
| nodbspecified | build with the default Accumulo configuration |
| mongodb | build with MongoDB configuration |

To run the build with the profile 'mongodb' `mvn clean install -P mongodb`.

Note: If you are building on windows, you will need hadoop-common 2.6.0's `winutils.exe` and `hadoop.dll`.  You can download it from [here](https://github.com/amihalik/hadoop-common-2.6.0-bin/archive/master.zip).  This build requires the [Visual C++ Redistributable for Visual Studio 2015 (x64)](https://www.microsoft.com/en-us/download/details.aspx?id=48145).   Also you will need to set your path and Hadoop home using the commands below:

```
set HADOOP_HOME=c:\hadoop-common-2.6.0-bin
set PATH=%PATH%;c:\hadoop-common-2.6.0-bin\bin
```

### Deployment Using Tomcat

Unwar the above war into the webapps directory.

To point the rya.adapter.web war to the appropriate database instance, make a properties file `environment.properties` and put it in the classpath.

In order to view the statements produced from running the reasoner, set `rya.adapter.web.print.statements` to *"true"*.  If set to *"false"* it will only display the number of statements produced.

Here is an example for Accumulo:

```
# Accumulo instance name
instance.name=accumulo
# Accumulo Zookeepers
instance.zk=localhost:2181
# Accumulo username
instance.username=root
# Accumulo password
instance.password=secret

# Rya Table Prefix
rya.tableprefix=triplestore_
# To display the query plan
rya.displayqueryplan=true
# To print the statements produced by the reasoner's rules
rya.adapter.web.print.statements=true
```

Please consult the [Accumulo], [ZooKeeper], and [Hadoop] documentation for help with setting up these prerequisites.

Here is an example for MongoDB (populate user/userpassword if authentication to mongoDB required):
```
rya.displayqueryplan=true
rya.adapter.web.print.statements=true
sc.useMongo=true
sc.use_freetext=true
sc.geo.predicates=http://www.opengis.net/ont/geosparql#asWKT
sc.freetext.predicates=http://www.w3.org/2000/01/rdf-schema#label
mongo.db.instance=localhost
mongo.db.port=27017
mongo.db.name=rya
mongo.db.collectionprefix=rya_
mongo.db.user=
mongo.db.userpassword=
mongo.geo.maxdist=1e-10
```
Start the Tomcat server. `./bin/startup.sh`

## Usage

### Load Data

If you need data to load, use the web.rya REST endpoint `http://server/web.rya/loadrdf` to load in triples.  More information on how to do this can be found in the Rya README file.

#### Web REST endpoint

The War sets up a Web REST endpoint at `http://localhost:8080/rya.adapter.web/rest/ReasonerService/uploadFile` that allows a reasoner rule file to be loaded and executed on a Rya repository.  A web page is located at `http://localhost:8080/rya.adapter.web/ReasonerFileUpload.html` to handle interactions with the service.


[RYA]: http://rya.incubator.apache.org/
[Accumulo]: https://accumulo.apache.org/
[ZooKeeper]: https://zookeeper.apache.org/
[Hadoop]: http://hadoop.apache.org/
