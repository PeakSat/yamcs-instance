# AcubeSAT YAMCS Instance - COMMS Interface

This repository holds the source code of the YAMCS application used for the communication with COMMS. To find more information regarding the specific features of this communication as well as the changes made in Yamcs visit the [COMMS - YAMCS interface](https://gitlab.com/acubesat/ops/yamcs-instance/-/wikis/COMMS-YAMCS-Interface-info) wiki page.

The primary **Mission's Database** is stored at YAMCS server, in which **Telemetry** data can be archived and then processed using the YAMCS web interface. The operator can also send **Telecommands** that are stored at the YAMCS database.

YAMCS includes a web interface which provides quick access and control over many of its features. The web interface runs on port 8090 and integrates with the security system of YAMCS.

The application's structure is mainly following the XTCE encoding schema, with the exception of the constraints imposed by the YAMCS mission control software.


## Running YAMCS

[Here](https://yamcs.org/getting-started) you can find prerequisites, basic commands and information to get things started with YAMCS.

To simply start the main YAMCS instance, run:

    mvn yamcs:run

View the YAMCS web interface by visiting http://localhost:8090

In order to start YAMCS with JMX enabled (required for hot backups) the commmand is:

    mvn yamcs:run -Dyamcs.jvmArgs="-Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

## Telemetry

To start pushing CCSDS packets into YAMCS, run the included Python script:

    python simulator.py

This script opens the packets.raw file and sends packets at 1 Hz over TCP and frames over UDP to YAMCS. To see information regarding the incoming packets and updates of the values of the parameters go to the "Packets" and "Parameters" pages, in the Telemetry section, on YAMCS web interface. 

The structure of the TM packets complies with the [CCSDS Space Packet Protocol](https://public.ccsds.org/Pubs/132x0b3.pdf#page=60) , consisting of a 6-byte tranfer frame primary header and optional 6-byte trailers.
If there is a need to send specific packets, then [CCSDS Space Data Link Protocols](https://gitlab.com/acubesat/comms/software/ccsds-telemetry-packets) is required. To install, run:

    git clone https://gitlab.com/acubesat/comms/software/ccsds-telemetry-packets.git

Currently, this functionality is not yet implemented in TCP. When implemented the generated packets will be sent to port 10013 that YAMCS listens to.

## Telecommanding

This project defines a few example CCSDS telecommands. Yamcs won't be sending Frames to COMMS. It will continue to send the TC packets, the structure of which complies with the [CCSDS Space Packet Protocol](https://public.ccsds.org/Pubs/133x0b2e1.pdf#page=32) and the [ECSS-E-ST-70-41C](https://ecss.nl/standard/ecss-e-st-70-41c-space-engineering-telemetry-and-telecommand-packet-utilization-15-april-2016/) standard, consisting of a 6-byte primary header, a 5-byte secondary header and the data field. COMMS will, then, distribute these TCs into Frames.

Telecommands can be sent through the "Commanding" section on YAMCS web interface.

## Instances 

In the **yamcs.yaml** file (located in yamcs-instance/src/main/yamcs/etc) three instances are currently being loaded when yamcs starts, the OBC, the ADCS and the COMMS instance. Each one of them is configured appropriately for the respective subsystem and the operator can choose which instance they want to work with from yamcs' web interface. The file for each instance's configuration is the **yamcs.subsystemName.yaml**. 

## Data-Links

In each **yamcs.subsystemName.yaml** file (located in yamcs-instance/src/main/yamcs/etc), five Data-Links are implemented sending and receiving data in different ports. Every time a packet gets sent or received, the count of the respective data-link is increased.

* Telemetry Data-Links 
    * "COMMS-UART", receiving data through port 10013.
    * "COMMS", receiving data and more specifically frames through port 10014.
    * "OBC-UART", receiving data through port 10015.
    * "ADCS-UART", receiving data through port 10016.

* Telecommanding Data-Links
    * "tcp-out", which is common for all instances and is sending data at port 10025.

For now, simulator.py sends randomly packets to all three TCP TM Data-Links and custom frames to the COMMS link.

## Mission Database

The Mission Database describes the telemetry and commands that are processed by YAMCS. It tells YAMCS how to decode packets or how to encode telecommands. 

The .xml files (located in yamcs-instance/src/main/yamcs/mdb) contain all the information regarding the parameters, the containers and the commands used in AcubeSAT YAMCS Instance.

The mdb is split across multiple folders to ensure readability and maintainability. The folders, as well as their contents are the following: 

- The **subystemName folders** which are split between two xmls: 
    - `subsystemName-dt.xml` --> Contains complex datatypes used by the subsystem.
    - `subsystemName-xtce.xml` --> Contains all of the subsystem's parameters.
- The **common folder** which contains two subfolders: 
    - The **dt folder** which contains **ParameterTypes** and more specifically: 
        - `base-dt.xml` --> Contains primitive datatypes.
        - `dt.xml` --> Contains complex datatypes used by the OBC and ADCS subsystems.
        - `file-handling-dt.xml` --> Contains the datatypes required for the file handling and the image transmission.
        - `ST[01]-dt.xml` --> Contains the enumeration datatypes used in ST[01].
        - `time-based-dt.xml` --> Contains the complex datatypes used in ST[11].
        - `writeable-dt.xml` --> Contains writeable argument datatypes used in ST[20].
    - The **pus folder** which contains: 
        - `pus-not-used.xml` --> Its elements are used for monitoring parameters (ST[12] on-board-monitoring service).
        - `pus.xml` --> Contains the headers used to make up the telemetry and the telecommand packets, as well as the necessary parameters that make up the headers. 
- The **frames folder**: 
    - `frames-dt.xml` --> Contains the necessary complex datatypes used in frames.
    - `frames.xml` --> Contains the frame packets, as well as the required parameters to make them up.
- The **services folder**: 
    - `ST[x].xml` --> Contains the parameters and the packets for service ST[x].
    - `Logger.xml` --> Contains a custom telemetry packet used for transporting message logs.

## Backup

Backup scripts, which automate the uploading of the artifacts of the archive to a cloud service as backup, have been implemented.

There are some helper programms integrated in YAMCS, such as `yamcsadmin`. In order to install them, run:

    mvn clean package

This step is **required** for the backup scripts to work. After execution, these programms will be installed in the directory `yamcs-instance/target/bundle-tmp/bin`.

The backup scripts are in the `yamcs-instance/backup-scripts` directory. After navigating to that folder, simply run `sh backup.sh` to initiate the script. In order for the script to work, JMX **must** be enabled (see Running YAMCS section).

The backups are instance-wide, meaning *everything* is saved; parameters, commands, alerts, logs, etc. These files are saved both locally, at a specified directory (in the backup.sh script) and online at the Google Drive folder of the account `yamcs.backup.acubesat@gmail.com`.

## Communication with the development board

The development board sends all telemetry over USB and also expects all telecommands via the same route. Although Yamcs should be able to handle communication over usb ports, the lack of documentation on this part has created the need for a different approach. The MessageHandler scripts (inside the `communication` path). 

There are several files there, each of which serves a different purpose:
- `logging.conf` is the configuration file for all the logging (where it's stored/displayed, the format of each message etc)
- `requirements.txt` contains all dependencies of the main script
- `settings.yaml` stores all static data the script uses
- `messageHandler.py` is the main script for communication with OBC
- `COMMSmessageHandler.py` is the main script for communication with COMMS


#### Configuration

It's required to install all dependencies for the main script, using:
```bash
pip3 install -r requirements.txt
```

After successfully installing everything, the main script can be run simply by navigating to the `communication/` folder and executing:

```bash
python3 messageHandler.py -adcs "name_for_adcs_log_file" -obc "name_for_obc_log_file"
```
or
```bash
python3 COMMSmessageHandler.py -comms "name_for_comms_file"
```

depending on the subsystem you want to communicate with (The name of the log file is optional, if not specified, the default name will be used "obc.log" for OBC, "adcs.log" for ADCS and "comms.log" for COMMS).

After that, and if Yamcs is running and the board is connected, all TMs and TCs should appear on the console the moment they are sent.

#### Script Logic

Although the purpose of the script seems simple, the way it's achieved is a bit complicated. Some basic info needed to understand the process is:
1. The devboard is connected either using a USB-to-USB cable (one end on the PC and the other on the board directly), in which case it shows up as a `usbACM` device, or using a breakout board. The second connection is as follows: Board `<-jumper cables->` breakout board `<-usb cable-> pc`. In this case, the device shows up as a `usbtty` device. In order to find out on which port the devboard is assigned to, the following command can be executed:

    ```bash
    ls /dev/tty*
    ```

    If wether /dev/ttyACM<N> or /dev/ttyUSB<N> shows up (where N is a number, for example /dev/ttyACM0), this means the devboard is connected to the computer. Make sure this serial port matches to the thread configuration, or else no messages will be sent/received.

2. Yamcs sends all TCs and expects all TMs through TCP ports. A TCP port is basically a network service, allowing applications to communicate with each other.

So what the script basically does is that it forwards the messages from the usb port, the device is connected to, over to the tcp port, that Yamcs listens for, and vice versa.

In order to accommodate for multiple devboards connected at once, this process is implemented using different threads. There are two main thread categories, a "Yamcs listener" and an "mcu listener";
- "Yamcs listener" threads, forwrard the messages (TCs) **from Yamcs** to the devboard.



- "mcu listener" threads, forward the messages (TMs) **from the devboard** (mcu) to Yamcs. To distinguish the subsystems the messages are sent from, a special-unique symbol (#, ! or ?) is sent along with each message.
This is required in order to log the messages into separate files, one for each subsystem. After some simple pre-processing (removing redundant spaces and decoding) the final message is sent to Yamcs.