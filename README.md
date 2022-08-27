# AcudeSAT Yamcs Instance

This repository holds the source code of the Yamcs application used for the AcubeSAT mission.

The primary Mission's Database is stored at Yamcs server, in which delivered data can be archived and then processed using the Yamcs web interface. The operator can also send TCs that are stored at the Yamcs database.

Its structure is mainly following the XTCE encoding schema, with the exception of the constraints imposed by the Yamcs mission control software.


## Running Yamcs

[Here](https://yamcs.org/getting-started) you can find prerequisites, basic commands and information to get things started with Yamcs.
To simply start the main yamcs instance, run:

    mvn yamcs:run

In order to start yamcs with JMX enabled (required for hot backups) the commmand is:

    mvn yamcs:run -Dyamcs.jvmArgs="-Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

## Telemetry

To start pushing CCSDS packets into Yamcs, run the included Python script:

    python simulator.py


This script will send packets at 1 Hz over UDP to Yamcs. There is enough test data to run for a full calendar day. The packets are a bit artificial and include a mixture of HK and accessory data.

If there is a need to send specific packets, then [ecss-services](https://gitlab.com/acubesat/obc/ecss-services) is required. To install, run:

    git clone https://gitlab.com/acubesat/obc/ecss-services.git --recurse-submodules

Currently, this functionality is implemented in the branches `OPS-Testing` and `ops-ecss`. After modifying `main.cpp`, the generated packets will be sent to port 10025 that Yamcs listens to.

## Telecommanding

This project defines a few example CCSDS telecommands. They are sent to UDP port 10025. The simulator.py script listens to this port. Commands have no side effects. The script will only count them.

## Backup

There are some helper programms integrated in Yamcs, such as `yamcsadmin`. In order to install them, run:

    mvn clean package

This step is **required** for the backup scripts to work. After execution, these programms will be installed in the directory `yamcs-instance/target/bundle-tmp/bin`.

The backup scripts are in the `yamcs-instance/backup-scripts` directory. After navigatint to that folder, simply run `sh backup.sh` to initiate the script. In order for the script to work, JMX **must** be enabled (see Running Yamcs section).

The backups are instance-wide, meaning *everything* is saved; parammeters, commands, alerts, logs, etc. These files are saved both locally, at a specified directory (in the backup.sh script) and online at the Google Drive folder of the account `yamcs.backup.acubesat@gmail.com`.

## Bundling

Running through Maven is useful during development, but it is not recommended for production environments. Instead bundle up your Yamcs application in a tar.gz file:

    mvn package
