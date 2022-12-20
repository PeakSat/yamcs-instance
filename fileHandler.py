from threading import Thread
import socket
import sys
from time import sleep

PACKET_HEADER_LENGTH = 11

STRING_DELIMITER = 0x000


def processTC(packet: bytearray) -> None:
    """
    Checks if the telecommand packet is of type [24,1] and
    forwards the packet minus the headers to further processing.
    """
    serviceType = packet[7]
    messageType = packet[8]
    if serviceType == 6 and messageType == 1:
        processFileSegment(packet[PACKET_HEADER_LENGTH:])


def processFileSegment(data: bytearray) -> None:
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
    for currentObject in range(numberOfObjects):

        offset = int.from_bytes(data[packetIndex:packetIndex+4], byteorder='big')
        packetIndex += 4
        dataLength = int.from_bytes(data[packetIndex:packetIndex+2], byteorder='big')
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
            + " offset is "
            +str(offset)
        )
        fileBinary = data[packetIndex : packetIndex + dataLength]

        # for character in fileBinary:
        #     print("byte is "+str(int(character)))

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
    """

    host = "localhost"
    portTC = 10025
    tc_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    tc_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    tc_socket.bind((host, portTC))
    tc_socket.listen(1)
    print("\nServer  10025 listening")
    clientconnTC, _ = tc_socket.accept()
    while True:
        data, _ = clientconnTC.recvfrom(65536)
        simulator.last_tc = data
        if data != b"":
            processTC(data)
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
