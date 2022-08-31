# Yamcs backup scripts

This folder contains a bash script `backup.sh` and a Google Drive CLI called [gdrive](https://github.com/prasmussen/gdrive).

### Yamcs Setup

Yamcs has implemented some helper programms, one of which is `yamcsadmin`. In order to download the required files, we should first run:

    mvn clean package

The yamcsadmin script is in the directory `target/bundle-tmp/bin/yamcsadmin`. Hot backups require JMX to be enabled, so **make sure you start yamcs using the command:**

    mvn yamcs:run -Dyamcs.jvmArgs="-Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

### `Backup.sh` Setup

This script takes care of automatic hot backups (meaning while yamcs is running). We can change some script parameters such as the backup time interval in seconds (`time`) and the backups `path` (if `path` is altered, be sure to update the local directory gdrive argument also).

### Gdrive Setup

`gdrive` manages the files using **our** account, so some setup is required. The easiest way to do it is by navigating to backup-scripts folder and running `./gdrive about`. You may be concerned giving access to a weirdly-named project, but that's how the developer named it (see [this](https://github.com/prasmussen/gdrive#news)). Currently the files are uploaded to an account dedicated for backups (yamcs.backup.acubesat@gmail.com). The account details are stored at [Nextcloud](https://cloud.spacedot.gr/index.php/apps/files/?dir=/AcubeSAT/Subsystems/OPS%20-%20Spacecraft%20Operations/YAMCS&fileid=391449).
The first time gdrive is launched , you will be prompted for a verification code. The code is obtained by following the printed url and authenticating with the google account for the drive you want access to (if you are not signed in with the afforementioned account, the script will fail). This will create a token file inside the .gdrive folder in your home directory. Note that anyone with access to this file will also have access to your google drive.

### Instructions:

If the gdrive setup is done correctly, each local backup will be mirrored to [this](https://drive.google.com/drive/u/1/folders/1MQDNgPfBtXBGCVxOcK3GrmajePFCZewy) Google Drive folder.

#### `backup.sh`

Simply run `sh backup.sh` in the directory yamcs-instance/backup-scripts .

#### yamcsadmin

`yamcsadmin` is a helper programm and can be used to create,restore, purge, delete and list backups. In order to manually manage the backups, you can:

Create a new **hot** backup at the backups directory :

    ./../target/bundle-tmp/bin/yamcsadmin backup create --backup-dir {BACKUP-PATH} --host localhost:9999 myproject` (works but does not display a confirmation message).

Create a new **cold** backup (instance not running) at the backups directory:

    ./../target/bundle-tmp/bin/yamcsadmin backup create --backup-dir {BACKUP-PATH} --data-dir {YAMCS_FOLDER}/target/yamcs/yamcs-data myproject

**List** all backups:

    ./../target/bundle-tmp/bin/yamcsadmin backup list --backup-dir {BACKUP-PATH}

**Restore** a backup (where ID is the id of the backup you want to restore) :

    ./../target/bundle-tmp/bin/yamcsadmin backup restore --backup-dir {BACKUP-PATH} --restore-dir ../../target/yamcs/yamcs-data/myproject.rdb {ID}

### Notes

- The `{BACKUP_PATH}` is the relative path from the root folder yamcs-instance/target/yamcs, so if the backups folder is in `yamcs-instance/backups`, the `{BACKUP_PATH}` becomes ` ../../backups`

- If you want to download the backup folder from Drive, it's best to use `./gdrive download {folder_id} --recursive` (you can find the folder_id in the `backup.sh` script). This is because when manually downloading the folder from the browser, some file extensions break and the backups become unusable.
