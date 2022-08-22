#! /bin/bash

# This script takes care of automatic hot backups (meaning while yamcs is running). The time interval and backup location are variables and can be altered.
# Yamcs has implemented some helper programms, one of which is yamcsadmin. 
# In order to install the required files, we should first run mvn clean package, which will download them in the target folder. 
# The yamcsadmin script is in the directory target/bundle-tmp/bin/yamcsadmin. 

# In order to:
# - Create a new hot backup at the backups directory* : ./target/bundle-tmp/bin/yamcsadmin backup create --backup-dir ../../backups --host localhost:9999 myproject (works but does not display a confirmation message)
# - Create a new cold backup (instance not running) at the backups directory: ./target/bundle-tmp/bin/yamcsadmin backup create --backup-dir ../../backups --data-dir {YAMCS_FOLDER}/target/yamcs/yamcs-data myproject
# - List all backups at a directory: ./target/bundle-tmp/bin/yamcsadmin backup list --backup-dir ../../backups

# Make sure you start yamcs using the command: 
# mvn yamcs:run -Dyamcs.jvmArgs="-Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
 
# Time in seconds
time=5s

# Backup path (the root directory is  /target/bundle-tmp)
path=../../backups

while [ : ] 
do
    ./target/bundle-tmp/bin/yamcsadmin backup create --backup-dir $path --host localhost:9999 myproject
    echo "Backup complete. List backups with the command ./target/bundle-tmp/bin/yamcsadmin backup list --backup-dir $path"
    sleep $time
done
