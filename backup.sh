#! /bin/bash

# This script takes care of automatic hot backups (meaning while yamcs is running). The time interval and backup location are variables and can be altered.
# Make sure you start yamcs using the command: 
# mvn yamcs:run -Dyamcs.jvmArgs="-Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
 

# List all backups at a directory: ./target/bundle-tmp/bin/yamcsadmin backup list --backup-dir $path

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
