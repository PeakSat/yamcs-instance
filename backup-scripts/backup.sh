#! /bin/bash

# Time interval in seconds
time=30s

# Backup path (the root directory is /target/yamcs). Change also the ./gdrive argument if this is altered. 
path=../../backups

# The Google Drive folder 
folder_id=1MQDNgPfBtXBGCVxOcK3GrmajePFCZewy

# run the about command in order to ensure setup has been completed 
./gdrive about

while : ; do
    # this is a hot backup, meaning yamcs-instance is still running
    ./../target/bundle-tmp/bin/yamcsadmin backup create --backup-dir $path --host localhost:9999 myproject
    echo "Backup complete. List backups with the command ./../target/bundle-tmp/bin/yamcsadmin backup list --backup-dir $path"
    # this is the gdrive upload commmand that mirrors the backups folder to a specific Google Drive folder (currently this is a folder in an account dedicated for backups: yamcs.backup.acubesat@gmail.com).
    # if you want to try it yourself , create a new folder on your personal drive and copy the id 
     ./gdrive sync upload ../backups $folder_id
    sleep $time
done

