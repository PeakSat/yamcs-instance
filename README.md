# AcudeSAT Yamcs Instance

This repository holds the source code of the Yamcs application used for the AcubeSAT mission.

The primary Mission's Database is stored at Yamcs server, in which delivered data can be archived and then processed using the Yamcs web interface. The operator can also send TCs that are stored at the Yamcs database.

Its structure is mainly following the XTCE encoding schema, with the exception of the constraints imposed by the Yamcs mission control software. 
 

## Running Yamcs

[Here](https://yamcs.org/getting-started) you can find prerequisites, basic commands and information to get things started with Yamcs.


## Telemetry

To start pushing CCSDS packets into Yamcs, run the included Python script:

    python simulator.py

This script will send packets at 1 Hz over UDP to Yamcs. There is enough test data to run for a full calendar day.

The packets are a bit artificial and include a mixture of HK and accessory data.


## Telecommanding

This project defines a few example CCSDS telecommands. They are sent to UDP port 10025. The simulator.py script listens to this port. Commands  have no side effects. The script will only count them.


## Bundling

Running through Maven is useful during development, but it is not recommended for production environments. Instead bundle up your Yamcs application in a tar.gz file:

    mvn package
