Jon Pendon
jap229@njit.edu
CS 643 Programming Assignment 1

-- Prerequisites --
This guide assumes you have the following:
Java 1.8 minimum
Maven
WinSCP
PuTTY

-- Compilation --
Open terminal in ./carrecognition folder
In the terminal type: mvn compile assembly:single
This should retrieve a .jar file with dependencies in the ./carrecognition/target folder

Open terminal in ./textrecognition folder
In the terminal type: mvn compile assembly:single
This should retrieve a .jar file with dependencies in the ./textrecognition/target folder

There should be two jar files: carrecognition-1.0-FINAL-jar-with-dependencies.jar and textrecognition-1.0-FINAL-jar-with-dependencies.jar
These jar files will need to go into their respective EC2 Instances.

-- Opening AWS Academy Console --
*AWS Academy specific (Skip section if needed)*
Open AWS Academy vocareum profile.
Click Start Lab.
Once AWS turns green, click on it.
Keep note of your AWS Credentials.


-- Generating EC2 Instances --
Open the AWS Console.
Choose the EC2 Service in the search bar.

Create an instance using Launch Instances.
Name it either CarRecognition or TextRecognition instance.
Keep image and instance type as default and free tier.

** Do only once **
Create a new key pair under key pair (login) section
Use RSA and .ppk. Name it something memorable.
If you know how to use command shell feel free to use that. This guide will not cover SSH.

Under security group, create a new security group.
Check Allow SSH traffic from : My IP (in the drop down)
Leave the other two unchecked

The rest of the instance can be left default.

Create a second instance, but use the same key pair (login) .ppk file.

-- Connecting to Instance using PuTTY --
Open the PuTTY application
In the host name, enter ec2-user@<IP-V4 DNS>
<IP-V4 DNS> corresponds to the Public IPv4 DNS information in the EC2's Details
Note: This IP-V4 will change every time
Go to Category -> Connection -> SSH -> Auth -> Credentials
In the Private key file for authentication: section, browse for the .ppk file created for this instance.

-- AWS Configuration --
For AWS Academy users, this step needs to be completed everytime the console is accessed for each ec2 instance.
In the ec2 instance command window, type aws configure
Insert the access key id, secret access key, and region name. Leave the output format blank.
This information is located in the AWS Details -> AWS CLI of the Vocareum page

aws configure generates a .aws folder and puts this information in the credentials file

Type cd .aws
Type vi credentials
Delete everything in the file and then copy paste the AWS CLI information into the file.
Press : and then enter wq to save.
Enter cd ..

-- Java Installation --
Both EC2 Instances need Java installed to run.
Enter: sudo yum install java-1.8.0-amazon-corretto
Enter y to install java.
When this command is ran, the EC2 Instance should have a java installed. Check with java -version
java -version should return build 1.8 for Correto-8.

-- FTP Jar files into EC2 Instance --
Open WinSCP
Enter Host name as the <IP-V4 DNS> located in the EC2 Instance Details -> Public IPv4 DNS
Enter ec2-user as the user name
Click Advanced -> SSH -> Authentication
Under Private key file, browse for the .ppk file.
This should open a location in the ec2 instance that corresponds to the putty folder.
Drag and drop the .jar file into this folder. For the car ec2 this .jar should be carrecognition-1.0-FINAL-jar-with-dependencies.jar. For the text ec2 this .jar file should be textrecognition-1.0-FINAL-jar-with-dependencies.jar.

Open a new tab for WinSCP and do the same thing for the other EC2 Instance.


-- Running the application --
In the PuTTY window corresponding to the TextRecognitionApplication, enter java -jar textrecognition-1.0-FINAL-jar-with-dependencies.jar
In the PuTTY window corresponding to the CarRecognitionApplicaiton, enter java -jar carrecognition-1.0-FINAL-jar-with-dependencies.jar

The text recognition instance should stall in searching for queue until the car recognition instance creates a queue and sends information.
An output.txt will be generated in the instance with the text recognition application.
Open WinSCP and navigate to the folder with the textrecognition application. Refresh and there should be an output.txt that can be opened.
