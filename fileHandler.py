from threading import Thread
import socket
import sys
import time
import os
from time import sleep

PACKET_HEADER_LENGTH = 11

STRING_DELIMITER = 0x000


def processTC(packet: bytearray, outScocket: socket) -> None:
    """
    Checks if the telecommand packet is of type [24,1] and
    forwards the packet minus the headers to further processing.
    """
    serviceType = packet[7]
    messageType = packet[8]
    if serviceType == 6 and messageType == 1:
        saveFileSegmentTM(packet[PACKET_HEADER_LENGTH:])
    elif serviceType == 6 and messageType == 3:
        processFileSegmentTC(packet[PACKET_HEADER_LENGTH:], outScocket)


def bytesToByteArray(packet: bytes) -> bytearray:
    return bytearray(packet)


def getByteFromNumber(target: int, byte_index: int) -> int:
    return (target >> (8 * byte_index)) & 0xFF


def processFileSegmentTC(data: bytearray, outSocket: socket) -> None:
    """
    Sends the file segments defined in the packet
    """

    base: str = ""
    packetIndex = 0
    for stringIndex in range(len(data)):
        character = data[stringIndex]
        if character != STRING_DELIMITER:
            base += chr(character)
        else:
            packetIndex = stringIndex + 1
            break

    numberOfObjects = data[packetIndex]
    print("numberOfObjects is "+str(numberOfObjects))
    packetIndex += 1
    now = (int(time.time()) - 1640988000) * 10  # CUC time

    packet: bytearray = [
        8,
        1,
        192,
        0,
        0,
        0,
    ]  # populate primary header with all variables except size ( last 2 bytes )
    packet.extend(
        [
            20,
            6,
            4,
            0,
            0,
            0,
            1,
            getByteFromNumber(now, 3),
            getByteFromNumber(now, 2),
            getByteFromNumber(now, 1),
            getByteFromNumber(now, 0),
        ]
    )  # add secondary header with counter = 1
    allSegments = bytearray([])
    for _ in range(numberOfObjects):
        offset = int.from_bytes(data[packetIndex : packetIndex + 4], byteorder="big")
        packetIndex += 4
        dataLength = int.from_bytes(
            data[packetIndex : packetIndex + 2], byteorder="big"
        )
        packetIndex += 2

        print(
            "base is "
            + base
            + " offset is "
            + str(offset)
            + " datalength is "
            + str(dataLength)
            + " packet index is "
            + str(packetIndex)
        )

        file = open(base, "rb")
        file.seek(offset)
        fileData = file.read(dataLength)
        fileSegment = bytearray([])
        fileSegment.extend(offset.to_bytes(4, "big"))
        fileSegment.extend(dataLength.to_bytes(2, "big"))
        fileSegment.extend(fileData)
        allSegments.extend(fileSegment)
        file.close()
    path, filename = os.path.split(base)
    filename += "\0"  # string terminator
    packet.extend(bytes(filename, "utf-8"))
    packet.extend(numberOfObjects.to_bytes(1, "big"))
    print("numberOfObjects is "+str(numberOfObjects))
    packet.extend(allSegments)
    dataLength = len(packet) - 6
    print("Data length is " + str(dataLength))
    packet[4] = getByteFromNumber(dataLength, 1)
    packet[5] = getByteFromNumber(dataLength, 0)

    outSocket.send(bytes(packet))


def saveFileSegmentTM(data: bytearray) -> None:
    """
    Parses all necessary information regarding the target
    file and writes the data to it.
    """
    base: str = ""
    packetIndex = 0
    for stringIndex in range(len(data)):
        character = data[stringIndex]
        if character != STRING_DELIMITER:
            base += chr(character)
        else:
            packetIndex = stringIndex + 1
            break

    numberOfObjects = data[packetIndex]
    packetIndex += 1
    for _ in range(numberOfObjects):

        offset = int.from_bytes(data[packetIndex : packetIndex + 4], byteorder="big")
        packetIndex += 4
        dataLength = int.from_bytes(
            data[packetIndex : packetIndex + 2], byteorder="big"
        )
        packetIndex += 2

        print(
            "base is "
            + base
            + " offset is "
            + str(offset)
            + " datalength is "
            + str(dataLength)
            + " packet index is "
            + str(packetIndex)
        )
        fileBinary = data[packetIndex : packetIndex + dataLength]
        packetIndex += dataLength

        if offset == 0:
            file = open(base, "wb")
            file.seek(offset)
            file.write(fileBinary)
            file.close()
        else:
            file = open(base, "ab")
            file.seek(offset)
            file.write(fileBinary)
            file.close()


def receive_tc(simulator):
    """
    Listens to YAMCS TCP port and prints in the terminal the received command.
    Also sends packets to OBC port.
    """

    host = "localhost"
    portTC = 10025
    portOBC = 10015
    tc_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    tc_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    tc_socket.bind((host, portTC))
    tc_socket.listen(1)
    print("\Socket  10025 listening")
    clientconnTC, _ = tc_socket.accept()

    tm_socket_OBC = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    tm_socket_OBC.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    tm_socket_OBC.bind((host, portOBC))
    tm_socket_OBC.listen(1)
    print("\nSocket  10015 connected")
    clientconnOBC, _ = tm_socket_OBC.accept()
    while True:
        data, _ = clientconnTC.recvfrom(65536)
        simulator.last_tc = data
        if data != b"":
            processTC(data, clientconnOBC)
            simulator.tc_counter += 1


class Simulator:
    def __init__(self):
        self.tc_counter = 0
        self.tc_thread = None
        self.last_tc = None

    def start(self):
        self.tc_thread = Thread(target=receive_tc, args=(self,))
        self.tc_thread.daemon = True
        self.tc_thread.start()

    def print_status(self):
        data: bytearray = []
        if self.last_tc:
            data = self.last_tc
        return "Received: {} commands. Last command size {}".format(
            self.tc_counter, len(data)
        )


if __name__ == "__main__":
    simulator = Simulator()
    simulator.start()

    try:
        prev_status = None
        while True:
            status = simulator.print_status()
            if status != prev_status:
                sys.stdout.write("\r")
                sys.stdout.write(status)
                sys.stdout.flush()
                prev_status = status
            sleep(0.5)
    except KeyboardInterrupt:
        sys.stdout.write("\n")
        sys.stdout.flush()
