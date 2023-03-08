import binascii
import io
import socket
import random
import sys
from struct import unpack_from
from threading import Thread
from time import sleep


def send_tm(simulator):

    frame_2_packets = [10, 176, 1, 1, 24, 0, 8, 1, 195, 39, 0, 76, 32, 4, 2, 1, 70, 0, 1, 37, 165, 61, 202, 14, 224, 184, 148, 14, 224, 185, 92, 0, 2, 19, 152, 0, 3, 64, 160, 0, 0, 14, 224, 185, 92, 63, 128, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 63, 209, 5, 236, 19, 153, 0, 6, 65, 80, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 14, 224, 185, 92, 65, 0, 0, 0,0,0,0,0, 8, 1, 195, 39, 0, 76, 32, 4, 2, 1, 70, 0, 1, 37, 165, 61, 202, 14, 224, 184, 148, 14, 224, 185, 92, 0, 2, 19, 152, 0, 3, 64, 160, 0, 0, 14, 224, 185, 92, 63, 128, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 63, 209, 5, 236, 19, 153, 0, 6, 65, 80, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 14, 224, 185, 92, 65, 0, 0, 0,0,0,0,0]
    frame_3_packets = [10, 176, 1, 1, 24, 0, 8, 1, 195, 39, 0, 76, 32, 4, 2, 1, 70, 0, 1, 37, 165, 61, 202, 14, 224, 184, 148, 14, 224, 185, 92, 0, 2, 19, 152, 0, 3, 64, 160, 0, 0, 14, 224, 185, 92, 63, 128, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 63, 209, 5, 236, 19, 153, 0, 6, 65, 80, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 14, 224, 185, 92, 65, 0, 0, 0,0,0,0,0, 8, 1, 195, 39, 0, 76, 32, 4, 2, 1, 70, 0, 1, 37, 165, 61, 202, 14, 224, 184, 148, 14, 224, 185, 92, 0, 2, 19, 152, 0, 3, 64, 160, 0, 0, 14, 224, 185, 92, 63, 128, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 63, 209, 5, 236, 19, 153, 0, 6, 65, 80, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 14, 224, 185, 92, 65, 0, 0, 0,0,0,0,0, 8, 1, 195, 39, 0, 10, 32, 17, 2, 1, 70, 0, 1, 37, 165, 61, 202]    
    frame_2_packets_with_clcw = [10, 177, 1, 1, 24, 0, 8, 1, 195, 39, 0, 76, 32, 4, 2, 1, 70, 0, 1, 37, 165, 61, 202, 14, 224, 184, 148, 14, 224, 185, 92, 0, 2, 19, 152, 0, 3, 64, 160, 0, 0, 14, 224, 185, 92, 63, 128, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 63, 209, 5, 236, 19, 153, 0, 6, 65, 80, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 14, 224, 185, 92, 65, 0, 0, 0,0,0,0,0, 8, 1, 195, 39, 0, 76, 32, 4, 2, 1, 70, 0, 1, 37, 165, 61, 202, 14, 224, 184, 148, 14, 224, 185, 92, 0, 2, 19, 152, 0, 3, 64, 160, 0, 0, 14, 224, 185, 92, 63, 128, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 63, 209, 5, 236, 19, 153, 0, 6, 65, 80, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 14, 224, 185, 92, 65, 0, 0, 0,0,0,0,0, 1, 0, 16,1]

    packet42 = [8, 1, 195, 39, 0, 76, 32, 4, 2, 1, 70, 0, 1, 37, 165, 61, 202, 14, 224, 184, 148, 14, 224, 185, 92, 0, 2, 19, 152, 0, 3, 64, 160, 0, 0, 14, 224, 185, 92, 63, 128, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 63, 209, 5, 236, 19, 153, 0, 6, 65, 80, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 14, 224, 185, 92, 65, 0, 0, 0,0,0,0,0]
    packet_17_2 = [8, 1, 195, 39, 0, 10, 32, 17, 2, 1, 70, 0, 1, 37, 165, 61, 202]

    #segmentation and blocking test frames

    frame1 = [10, 176, 0, 1, 24, 0, 8, 1, 195, 39, 0, 76, 32, 4, 2, 1, 70, 0, 1, 37, 165, 61, 202, 14, 224, 184, 148, 14, 224, 185, 92, 0, 2, 19, 152, 0, 3, 64, 160, 0, 0, 14, 224, 185, 92, 63, 128, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 63, 209, 5, 236, 19, 153, 0, 6, 65, 80, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 14, 224, 185, 92, 65, 0, 0, 0,0,0,0,0, 8, 1, 195, 39, 0, 10, 32, 17, 2]
    frame2 = [10, 176, 0, 2, 24, 8, 1, 70, 0, 1, 37, 165, 61, 202, 8, 1, 195, 39, 0, 76, 32, 4, 2, 1, 70, 0, 1, 37, 165, 61, 202, 14, 224, 184, 148, 14, 224, 185, 92, 0, 2, 19, 152, 0, 3, 64, 160, 0, 0, 14, 224, 185, 92, 63, 128, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 63, 209, 5, 236, 19, 153, 0, 6, 65, 80, 0, 0, 14, 224, 185, 92, 64, 64, 0, 0, 14, 224, 185, 92, 65, 0, 0, 0,0,0,0,0, 0]

    # segmentation of one packet into 3 frames
    firstFrame =[10,  176,  0,  1,  8,  0,  8,  1,  195,  39,  0,  10,  32,  17 ]
    secondFrame =[10,  176,  0,  2,  7,  255,  2,  1,  70,  0,  1,  37,  165,  61 ]
    thirdFrame = [10,  176,  0,  3,  16,  1,  202,  1,  70,  0,  1,  37,  165,  61]

    """
    This function reads the packets from the specified .raw
    file and sends them simultaneously to all data links. If
    SEQUENTIAL_SENDING is set to true, then each packet is sent
    to a different Data Link.
    """

    SEQUENTIAL_SENDING = False
    host = "localhost"
    portCOMMS = 10014
    tm_socket_COMMS = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    portOBC = 10015
    tm_socket_OBC = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    tm_socket_OBC.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    portADCS = 10016
    tm_socket_ADCS = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    tm_socket_ADCS.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    portCAN = 10017
    tm_socket_CAN = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    tm_socket_CAN.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    tm_socket_COMMS.bind((host, portCOMMS))
    tm_socket_COMMS.listen(1)
    print("server  10014 listening")

    tm_socket_OBC.bind((host, portOBC))
    tm_socket_OBC.listen(1)
    print("\nserver  10015 listening")

    tm_socket_ADCS.bind((host, portADCS))
    tm_socket_ADCS.listen(1)
    print("server  10016 listening")

    tm_socket_CAN.bind((host, portCAN))
    tm_socket_CAN.listen(1)
    print("server  10017 listening")

    clientconnCOMMS, _ = tm_socket_COMMS.accept()

    clientconnOBC, _ = tm_socket_OBC.accept()

    clientconnADCS, _ = tm_socket_ADCS.accept()

    clientconnCAN, _ = tm_socket_CAN.accept()

    packetCounter = 0
    simulator.tm_counter = 0

    #sending to tcp comms link
    while True:
        clientconnCOMMS.send(bytearray(frame_2_packets_with_clcw))
        simulator.tm_counter += 1
        sleep(10)
        
    # sending 2000 packets
    while packetCounter < 400:
        with io.open("ecsspackets.raw", "rb") as f:
            header = bytearray(6)

            while f.readinto(header) == 6:
                (len,) = unpack_from(">H", header, 4)
                packet = bytearray(len + 7)

                f.seek(-6, io.SEEK_CUR)
                f.readinto(packet)

                if SEQUENTIAL_SENDING:
                    n = random.randint(0, 2)
                    # OBC UART
                    if n == 0:
                        clientconnOBC.send(packet)
                        simulator.tm_counter += 1
                    # ADSC UART
                    elif n == 1:
                        clientconnADCS.send(packet)
                        simulator.tm_counter += 1
                    # CAN BUS
                    else:
                        clientconnCAN.send(packet)
                        simulator.tm_counter += 1
                else:
                    clientconnOBC.send(packet)
                    clientconnADCS.send(packet)
                    clientconnCAN.send(packet)
                    simulator.tm_counter += 1

                sleep(1)
        packetCounter += 1

    clientconnOBC.close()
    clientconnADCS.close()
    clientconnCAN.close()
    print("communication ended")


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
    print("server  10025 1istening")
    clientconnTC, _ = tc_socket.accept()
    while True:
        data, _ = clientconnTC.recvfrom(4096)
        simulator.last_tc = data
        if data != b"":
            simulator.tc_counter += 1


class Simulator:
    def __init__(self):
        self.tm_counter = 0
        self.tc_counter = 0
        self.tm_thread = None
        self.tc_thread = None
        self.last_tc = None

    def start(self):
        self.tm_thread = Thread(target=send_tm, args=(self,))
        self.tm_thread.daemon = True
        self.tm_thread.start()
        self.tc_thread = Thread(target=receive_tc, args=(self,))
        self.tc_thread.daemon = True
        self.tc_thread.start()

    def print_status(self):
        cmdhex: str = ""
        if self.last_tc:
            cmdhex = binascii.hexlify(self.last_tc).decode("ascii")
        return "Sent: {} packets. Received: {} commands. Last command {}".format(
            self.tm_counter, self.tc_counter, cmdhex
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
