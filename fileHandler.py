import base64
from threading import Thread
import binascii
import socket
import sys
from time import sleep

PACKET_HEADER_LENGTH = 11

STRING_DELIMITER = 0x000

previousChunk = -1


def processTC(packet: bytearray) -> None:
    """
    Checks if the telecommand packet is of type [24,1] and
    forwards the packet minus the headers to further processing.
    """
    serviceType = packet[7]
    messageType = packet[8]

    if serviceType == 24 and messageType == 1:
        processFileSegment(packet[PACKET_HEADER_LENGTH - 1 :])
    pass


def processFileSegment(data: bytearray) -> None:
    """
    Parses all necessary information regarding the target
    file and writes the data to it.
    """
    global previousChunk
    targetFilePath: str = ""
    targetFileName: str = ""
    currentChunk: int
    totalChunks: int
    chunkSize: int
    stringsFound = 0
    offset = 0
    for index in range(len(data)):
        character = data[index]
        if character != STRING_DELIMITER:
            if stringsFound == 0:
                targetFilePath += chr(character)
            elif stringsFound == 1:
                targetFileName += chr(character)
        elif character == STRING_DELIMITER:
            stringsFound += 1
        if stringsFound == 2:
            offset = index + 1
            break

    global previousChunk
    if previousChunk == -1:
        print(
            "Saving file from Path {} and name {}".format(
                targetFilePath, targetFileName
            )
        )

    currentChunk = data[offset] * 256 + data[offset + 1]
    if currentChunk - previousChunk != 1:
        exit("Packet loss: Missed packet {}".format(previousChunk + 1))
    previousChunk = currentChunk

    offset += 2
    totalChunks = data[offset] * 256 + data[offset + 1]
    offset += 2
    chunkSize = data[offset] * 256 + data[offset + 1]
    offset += 2

    print(
        "currentChunk is {} , totalChunks is {} and chunkSize is {} ".format(
            currentChunk, totalChunks, chunkSize
        )
    )

    fileBase64 = ""
    for character in data[offset:]:
        fileBase64 += chr(character)

    file = open(targetFileName, "ab")
    file.write(base64.urlsafe_b64decode(fileBase64))


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
        data, _ = clientconnTC.recvfrom(4096)
        simulator.last_tc = data
        processTC(data)
        if data != b"":
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
        cmdhex: str = ""
        if self.last_tc:
            cmdhex = binascii.hexlify(self.last_tc).decode("ascii")
        return "Received: {} commands. Last command size {}".format(self.tc_counter, len(cmdhex))


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
                sys.stdout.write("\n")
                sys.stdout.flush()
                prev_status = status
            sleep(0.5)
    except KeyboardInterrupt:
        sys.stdout.write("\n")
        sys.stdout.flush()
