import binascii
import io
import socket
import random
import sys
from struct import unpack_from
from threading import Thread
from time import sleep


def send_tm(simulator):
    host10015='localhost'
    port10015=10015
    tm_socket_10015 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    host10016='localhost'
    port10016=10016
    tm_socket_10016 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    host10017='localhost'
    port10017=10017
    tm_socket_10017 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    tm_socket_10015.bind((host10015,port10015))
    tm_socket_10015.listen(1)
    print ('\n','server  10015 1istening')

    tm_socket_10016.bind((host10016,port10016))
    tm_socket_10016.listen(1)
    print ('\n','server  10016 1istening')

    tm_socket_10017.bind((host10017,port10017))
    tm_socket_10017.listen(1)
    print ('\n','server  10017 1istening')

    clientconn10015, clientaddr10015=tm_socket_10015.accept()
    print ('\n','connection 10015 established with',clientaddr10015)

    clientconn10016, clientaddr10016=tm_socket_10016.accept()
    print ('\n','connection 10016 established with',clientaddr10016)

    clientconn10017, clientaddr10017=tm_socket_10017.accept()
    print ('\n','connection 10017 established with',clientaddr10017)

    n=0

    while n<900:
        with io.open('packets-1.raw', 'rb') as f:
            simulator.tm_counter = 0
            header = bytearray(6)

            while f.readinto(header) == 6:
                (len,) = unpack_from('>H', header, 4)
                packet = bytearray(len+7)

                f.seek(-6, io.SEEK_CUR)
                f.readinto(packet)
    
                clientconn10015.send(packet)
                print('\n', 'packet sent 10015')

                clientconn10016.send(packet)
                print('\n', 'packet sent 10016')

                clientconn10017.send(packet)
                print('\n', 'packet sent 10017')
                simulator.tm_counter+=1


                sleep(0.01)
        n+=1
            

    clientconn10015.close()
    clientconn10016.close()
    clientconn10017.close()
    print("communication ended")


def receive_tc(simulator):
    tc_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    tc_socket.bind(('127.0.0.1', 10025))
    while True:
        data, _ = tc_socket.recvfrom(4096)
        simulator.last_tc = data
        simulator.tc_counter += 1


class Simulator():

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
        cmdhex = None
        if self.last_tc:
            cmdhex = binascii.hexlify(self.last_tc).decode('ascii')
        return 'Sent: {} packets. Received: {} commands. Last command: {}'.format(
                         self.tm_counter, self.tc_counter, cmdhex)


if __name__ == '__main__':
    simulator = Simulator()
    simulator.start()

    try:
        prev_status = None
        while True:
            status = simulator.print_status()
            if status != prev_status:
                sys.stdout.write('\r')
                sys.stdout.write(status)
                sys.stdout.flush()
                prev_status = status
            sleep(0.5)
    except KeyboardInterrupt:
        sys.stdout.write('\n')
        sys.stdout.flush()
