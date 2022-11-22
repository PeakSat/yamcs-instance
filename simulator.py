import binascii
import io
import socket
import random
import sys
from struct import unpack_from
from threading import Thread
from time import sleep


def send_tm(simulator):
    host='localhost'
    portOBC=10015
    tm_socket_OBC = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    portADCS=10016
    tm_socket_ADCS = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    portCAN=10017
    tm_socket_CAN = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    tm_socket_OBC.bind((host,portOBC))
    tm_socket_OBC.listen(1)
    print ('server  10015 1istening')

    tm_socket_ADCS.bind((host,portADCS))
    tm_socket_ADCS.listen(1)
    print ('server  10016 1istening')

    tm_socket_CAN.bind((host,portCAN))
    tm_socket_CAN.listen(1)
    print ('server  10017 1istening')

    clientconnOBC, clientaddrOBC=tm_socket_OBC.accept()

    clientconnADCS, clientaddrADCS=tm_socket_ADCS.accept()

    clientconnCAN, clientaddrCAN=tm_socket_CAN.accept()

    c=0
    simulator.tm_counter = 0
    
#sending 2000 packets
    while c<400:
        with io.open('ecsspackets.raw', 'rb') as f:
            header = bytearray(6)

            while f.readinto(header) == 6:
                (len,) = unpack_from('>H', header, 4)
                packet = bytearray(len+7)

                f.seek(-6, io.SEEK_CUR)
                f.readinto(packet)

        #simultaneous sending
                clientconnOBC.send(packet)
                clientconnADCS.send(packet)
                clientconnCAN.send(packet)

                simulator.tm_counter+=1

        #sequential sending
            #    n = random.randint(0,2)
                #OBC UART
            #    if n == 0: 
            #        clientconnOBC.send(packet)
            #        simulator.tm_counter += 1
                #ADSC UART
            #    elif n == 1:
            #        clientconnADCS.send(packet)
            #        simulator.tm_counter += 1
                #CAN BUS
            #    else :
            #        clientconnCAN.send(packet)
            #        simulator.tm_counter += 1

                sleep(1)
        c+=1

      

    clientconnOBC.close()
    clientconnADCS.close()
    clientconnCAN.close()
    print("communication ended")


def receive_tc(simulator):
    host = 'localhost'
    portTC = 10025
    tc_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    tc_socket.bind((host, portTC))
    tc_socket.listen(1)
    print ('server  10025 1istening')
    clientconnTC, clientaddrTC = tc_socket.accept()
    while True:
        data, _ = clientconnTC.recvfrom(4096)
        simulator.last_tc = data
        if (data!=b''):
            simulator.tc_counter +=1


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
