# AcudeSAT Yamcs Instance

This repository holds the source code of the Yamcs application used for the AcubeSAT mission.

The primary **Mission's Database** is stored at Yamcs server, in which **Telemetry** data can be archived and then processed using the Yamcs web interface. The operator can also send **Telecommands** that are stored at the Yamcs database.

Yamcs includes a web interface which provides quick access and control over many of its features. The web interface runs on port 8090 and integrates with the security system of Yamcs.

The application's structure is mainly following the XTCE encoding schema, with the exception of the constraints imposed by the Yamcs mission control software.


## Running Yamcs

[Here](https://yamcs.org/getting-started) you can find prerequisites, basic commands and information to get things started with Yamcs.

To simply start the main yamcs instance, run:

    mvn yamcs:run

View the Yamcs web interface by visiting http://localhost:8090

In order to start yamcs with JMX enabled (required for hot backups) the commmand is:

    mvn yamcs:run -Dyamcs.jvmArgs="-Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

## Telemetry

To start pushing CCSDS packets into Yamcs, run the included Python script:

    python simulator.py

This script opens the packets.raw file and sends packets at 1 Hz over UDP to Yamcs. To see information regarding the incoming packets and updates of the values of the parameters go to the "Packets" and "Parameters" pages, in the Telemetry section, on Yamcs web interface. 

The structure of the TM packets complies with the CCSDS Space Packet Protocol, consisting of a 6-byte primary header, a 10-byte secondary header and the data field.

* Primary Header
    * bits 0-2 contain the **Packet Version Number** (set to '000')
    * bits 3-15 contain the **Packet Identification Field**
        * bit 3 contains the **Packet Type** ('0' for TMs and '1' for TCs)
        * bit 4 contains the **Secondary Header Flag** which indicates the presence or absence of the packet secondary header (set to '1' as all (with the exception of CPDU command packets) packets have a secondary header) 
        * bits 5-15 contain the **Application Process ID** which, uniquely, identifies the on-board application process that is source of the TM packet/ destination of the TC packet (OBC, COMMS, ADCS, SU, GS)
    * bits 16-31 contain the **Packet Sequence Control Field**
        * bits 16-17 contain the **Sequence Flags** (set to'11' to indicate a stand-alone packet. All telemetry packets and telecommand packets defined within this Standard are stand alone packets)
        * bits 18-31 contain the **Packet Sequence Count or Packet Name** (For TM packets it is incremented by 1 whenever the sourrce application process releases a packet.The telecommand packets carry either a packet sequence count or a packet name to identify them within the same communication session. For the purpose of this Standard, the telecommand packet sequence count or packet name field carries an identifier that, if used in combination with the source identifier, can uniquely identify the telecommand packet.)
    * bits 32-47 contain the **Packet Data Length** which is equal to the number of octets contained within the packet data field. The length of the entire packet, including the packet primary header, is 6 octets more than the length of the packet data field.

* Secondary Header
    * bits 0-3 contain the **TM Packet PUS Version Number** which reflects the different versions of this Standard
    * bits 4-7 contain the **Spacecraft Time Reference Status** which is the status of the on-board time reference used when time tagging that telemetry packet.
    * bits 8-23 contain the **Message Type** identifier of a specific report
        * bits 8-15 contain the **Service Type ID**
        * bits 16-23 contain the **Message SubType ID**
    * bits 24-31 contain the **Type Counter** each application process sets the message type counter of the related telemetry packet to the value of the related counter.
    * bits 32-47 contain the **Destination ID**, meaning the application process user identifier of the application process addressed by the related report.
    * bits 48-79 contain the **Absolute Time** which represents the time passed since the 2020-01-01 epoch on 100ms intervals.

If there is a need to send specific packets, then [ecss-services](https://gitlab.com/acubesat/obc/ecss-services) is required. To install, run:

    git clone https://gitlab.com/acubesat/obc/ecss-services.git --recurse-submodules

Currently, this functionality is implemented in the branches `OPS-Testing` and `ops-ecss`. After modifying `main.cpp`, the generated packets will be sent to port 10025 that Yamcs listens to.

## Telecommanding

This project defines a few example CCSDS telecommands. The structure of the TC packets complies with the CCSDS Space Packet Protocol, consisting of a 6-byte primary header, a 5-byte secondary header and the data field.

* Primary Header (same as Telemetry)

* Secondary Header
    * bits 0-3 contain the **TC Packet PUS Version Number** which reflects the different versions of this Standard
    * bits 4-7 contain the **Acknowledgement Flags** each of which is set by the operator for each TC and determines the type of acknowledgement flag to be sent
        * bit 4: acknowledges completion whether the execution of the TC was successful or not
        * bit 5: acknowledges the progress of execution
        * bit 6: acknowledges that execution has started 
        * bit 7: acknowledges that the corresponding packet has been accepted by the application process
    * bits 8-23 contain the **Message Type** identifier of a specific report
        * bits 8-15 contain the **Service Type ID**
        * bits 16-23 contain the **Message SubType ID**
    * bits 24-39 contain the **Source ID** which corresponds to the application process user identifier of the application process that hosts the subservice user that generates that request

Telecommands can be sent through the "Commanding" section on Yamcs web interface.

## Data-Links

In yamcs.myproject.yaml file (located in yamcs-instance/src/main/yamcs/etc), four Data-Links are implemented sending and receiving data in different ports. Every time a packet gets sent or received, the count of the respective data-link is increased.

* Telemetry Data-Links 
    * "CAN-bus", receiving data through port 10017
    * "ADCS-UART", receiving data through port 10016
    * "OBC-UART", receiving data through port 10015

* Telecommanding Data-Link
    * "udp-out", sending data at port 10025

For now, simulator.py sends randomly packets to all three TM Data-Links, but they will, later, be used for the differentiation of the incoming packets based on their origin, as reflected by their names.

## Mission Database

The Mission Database describes the telemetry and commands that are processed by Yamcs. It tells Yamcs how to decode packets or how to encode telecommands. 

The .xml files (located in yamcs-instance/src/main/yamcs/mdb) contain all the information regarding the parameters, the containers and the commands used in AcubeSAT Yamcs Instance.

* The dt.xml file contains all **ParameterTypes** for Telemetry and **ArgumentTypes** for Telecommanding.

* The rest of the .xml files are used to define parameters, containers and commands for the mission. The .xml file in which a paremeter or container or command is defined, reflects its use. More specifically:
    * pus.xml: contains parameters, containers and commands, which implement the principal services that offer great control over reading/writing parameters from the Ground Station. This control refers to accessing and modifying parameter values (ST[20] parameter management service), generating periodic reports containing parameter values (ST[03] housekeeping service) and receiving statistics for a large number of parameters (ST[04] parameter statistics reporting service).
    * pus-not-used.xml: its elements are used for monitoring parameters (ST[12] on-board-monitoring service) 
    * pus-verification.xml: contains parameters and containers used to transmit to the Ground Station information about the status of a request's acceptance verification (ST[01] request verification service)
    * time-based-scheduling.xml: contains commands that will be scheduled to be executed later in the mission timeline (ST[11] time-based scheduling service)
    * xtce.xml: contains the ADCS and OBC parameters that will be used during the Environmental Campaign. 

## Backup

Backup scripts, which automate the uploading of the artifacts of the archive to a cloud service as backup, have been implemented.

There are some helper programms integrated in Yamcs, such as `yamcsadmin`. In order to install them, run:

    mvn clean package

This step is **required** for the backup scripts to work. After execution, these programms will be installed in the directory `yamcs-instance/target/bundle-tmp/bin`.

The backup scripts are in the `yamcs-instance/backup-scripts` directory. After navigating to that folder, simply run `sh backup.sh` to initiate the script. In order for the script to work, JMX **must** be enabled (see Running Yamcs section).

The backups are instance-wide, meaning *everything* is saved; parammeters, commands, alerts, logs, etc. These files are saved both locally, at a specified directory (in the backup.sh script) and online at the Google Drive folder of the account `yamcs.backup.acubesat@gmail.com`.

