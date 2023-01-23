# Versatune Receiver #

This README would normally document whatever steps are necessary to get your application up and running.

### What is this repository for? ###

* Quick summary
* Version 1.1.0.8
* 

### How do I get set up? ###

* Summary of set up
* Configuration
* Dependencies
* Database configuration
* How to run tests
* Deployment instructions


## Summary of set up
 - Use Raspberry Pi Imager to create a new SD with 64-bit OS (Lite Version)
 - Install the SD into your PI
 - Install Java 17
 - Install VLC
 - Install git
 - Install Maven
 - Set JAVA_HOME and MAVEN_HOME 
 - Get PIJ4 files
 - Get Versatune files
 - Install Versatune

## create new SD

## install the SD card

## install Java 17
I followed the instructions [here](https://linuxhint.com/install-java-17-raspberry-pi/)

The main steps are:
```
   sudo apt update
   sudo apt upgrade -y
   sudo apt install openjdk-17-jdk -y
   java --version
   ```
   
## Install VLC
 ```
   sudo apt update
   sudo apt upgrade -y
   sudo apt install -y vlc
   ```

## Install git
 ```
   sudo apt update
   sudo apt install git
   ```
## Install Maven - (updated because the wget command did not work - the wget command fails)

 Download Apache Maven. Use the following command:
   ```
   wget https://dlcdn.apache.org/maven/maven-3/3.8.7/binaries/apache-maven-3.8.7-bin.tar.gz -P /tmp
   ```
If that command fails (it did for me) then go to [here](https://maven.apache.org/download.cgi) and download the apache-maven-3.8.7-bin.tar.gz file to /tmp.
   
Extract Apache Maven tar.gz file. Use the following command:
 ```sudo tar xf /tmp/apache-maven-3.8.7-bin.tar.gz -C /opt
 ```
## Set up ENVIRONEMENT variables for JAVA and MAVEN 
Set up environment variables for Maven. Create a file named /etc/profile.d/maven.sh. I used vi but, you can use nano or any editor of your choosing.
```
   sudo vi /etc/profile.d/maven.sh
```
After the file is created, insert the following. 
```
   export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-armhf
   export M2_HOME=/opt/apache-maven-3.8.6
   export MAVEN_HOME=/opt/apache-maven-3.8.6
   export PATH=${M2_HOME}/bin:${PATH}
```
Make the maven.sh file executable
 ```
   sudo chmod  +x /etc/profile.d/maven.sh
 ```

Load environment variables:
```
source /etc/profile.d/maven.sh
```


Check to see that it is installed correctly. Do the following command:
```
$ mvn --version
```

## Structure
Create a structure like this on the PI with the following folders

/usr/local/apps/versatune

/usr/local/apps/versatune/conf

/usr/local/apps/versatune/data

/usr/local/apps/versatune/scripts

/usr/local/apps/versatune/logs



## Dependencies
The application depends on GPIO from PI4J. Download these from teh PifJ Download repository which can be found [here](https://pi4j.com/download/)
I just downloaded the zip file from [here](https://github.com/Pi4J/download/raw/main/pi4j-2.2.1.zip)
After unzipping move the files to /usr/local/apps/versatune 
