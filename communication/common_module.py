from threading import Thread
from dataclasses import dataclass
from logging import Logger
from enum import Enum
import logging
from logs import getFileLogger, change_log_file
from data import *
import socket
from time import sleep
import serial
from cobs import cobs
from cobs.cobs import DecodeError

class EmptyLogsNames(Exception):
    """Raised when changes all logs name to empty string."""

    def __init__(
        self,
        message="All logs name are empty strings.",
    ):
        self.message = message
        super().__init__(self.message)

class YAMCSClosedPortException(Exception):
    """Raised when YAMCS refuses connection."""

    def __init__(
        self,
        message="Probably packet header fields are initialized improperly or packet is corrupt.",
    ):
        self.message = message
        super().__init__(self.message)


@dataclass
class Settings:
    """
    Attributes:
        yamcs_port_out: The port for sending TCs to MCU.
        obc_port: The port for sending TMs to OBC.
        adcs_port: The port for sending TMs to ADCS.
        can_bus_port: The port for sending TMs to CAN.
        usb_serial_0: The default port if the devboard is connected via USB cable directly.
        uart_serial_0: The default port if the devboard is connected via a UART-to-USB adapter.
        baud_rate: The usart rate for sending TCs the MCU.
        IP: The application IP for establishing connection.
        timeout: Read data from the serial port untl a \n is received or N=timeout seconds have passed.
        max_size: Max bytes to compose a TC.
        enabled/disabled: Used to enable/disable socket options.
        reconnection_timeout: Seconds to wait if device is not connected.
    """
    yamcs_port_out: int
    comms_port_in: int
    comms_port_out: int
    obc_port_in: int
    adcs_port_in: int
    adcs_port_out: int
    uart_serial_0: str
    usb_serial_0: str
    uart_serial_1: str
    uart_serial_2: str
    usb_serial_1: str
    usb_serial_2: str
    baud_rate: int
    IPv4: str
    serial_timeout: int
    max_tc_size: int
    socket_backlog_level: int
    socket_options_enabled: int
    socket_options_disabled: int
    reconnection_timeout: int


def clamp(n, smallest, largest):
    """
    Clips the input given a lower bound and an upper bound.
    It is necessary because if a byte larger than 256 is appended,
    an exception will occur.
    This means that some data might not be accurate, but later evaluation
    by the operator will make this clear.
    """
    return max(smallest, min(n, largest))


def connect_to_port(settings: Settings, port: int) -> socket:
    """
    This function is used to connect to a TCP socket.
    If the processes initialized by this script are not terminated properly (this can happen
    if an exception occurs),the TCP connections might not close. By enabling the REUSEADDR
    (reuse address) option, the script will try to reconnect to the already opened port
    that is in a TIME_WAIT state.
    """
    logging.debug("Awaiting socket connection with YAMCS port " + str(port) + "...")
    yamcs_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    yamcs_socket.setsockopt(
        socket.SOL_SOCKET, socket.SO_REUSEADDR, settings.socket_options_enabled
    )
    try:
        yamcs_socket.bind((settings.IPv4, port))
    except OSError:
        logging.exception(
            "Port " + str(port) + " not available (check if YAMCS is running)."
        )
        exit(1)

    yamcs_socket.listen(settings.socket_backlog_level)
    yamcs_client, _ = yamcs_socket.accept()
    logging.debug("Connected to " + str(port))
    return yamcs_client


def mcu_client(
    settings: Settings,
    serial_port: str = None,
    obc_log_name: str = "obc.log",
    adcs_log_name: str = "adcs.log",
    comms_log_name: str = "comms.log"
):
    """

    Opens a new TCP stream socket.
    It listens to the specified serial port for TM messages.
    They usually come in the following form:
    '1801 [debug    ]OBC New TM[3,25] message! 8 1 192 2 0 15 32 3 25 0 2 0 1 37 165 53 0 0 0 0 0 \n'
    This string is split after the "!", "#" or "?" depending on the subsystem, returning the actual packet.
    All the bytes after that are sent to YAMCS to the corresponding subsystem port.
    If we try to convert the characters one by one from ASCII to integer, we will get something like:
    8 1 1 9 2 0 0 2 0 ... -> garbage
    So we need to parse a sequence of numbers as a single decimal.
    In order to do this, we must first convert all ascii numbers to decimal form.
    Also we need to keep track of the space characters. If we receive numbers one after the other
    (whithout space characters in between), we need to multiply the previous entry by 10, in order
    to parse the whole decimal.
    Note:
        If the debugging messages are altered, this script will have undetermined behavior, since
        it relies on the existence of the exclamation mark "!" or the hastag "#" or the question mark "?" to detect actual TMs being sent.
    """

    if serial_port is None:
        serial_port = settings.usb_serial_0

    #stdout_handler = logging.StreamHandler(sys.stdout)
    #stdout_handler.setLevel(logging.DEBUG)
    #stdout_handler.setFormatter(formatter)
    
    obcFileLogger: Logger = None
    adcsFileLogger: Logger = None
    commsFileLogger: Logger = None

    if obc_log_name != "" and adcs_log_name != "" and comms_log_name != "":
        raise EmptyLogsNames()

    if obc_log_name != "":
        obcFileLogger = getFileLogger(ThreadType.OBC)
        obcFileLogger = change_log_file(obcFileLogger, obc_log_name)

    if adcs_log_name != "":
        adcsFileLogger = getFileLogger(ThreadType.ADCS)
        adcsFileLogger = change_log_file(adcsFileLogger, adcs_log_name)


    if comms_log_name != "":
        commsFileLogger = getFileLogger(ThreadType.COMMS)
        commsFileLogger = change_log_file(commsFileLogger, comms_log_name)


    while True:

        try:
            ser = serial.Serial(
                serial_port,
                baudrate=settings.baud_rate,
                timeout=settings.serial_timeout,
            )

            # Read any messages already stored to the buffer.
            # If YAMCS receives this, it's gonna fail and the script will produce a TCP exception.
            # ser.readline()

            while True:
                line = ser.readline()
                try:
                    # logging.info(f"{ser.name} ENCODED: {line}")
                    # message = cobs.decode(line)
                    message = line
                except DecodeError:
                    print("Cobs decode error!")
                    continue
                finally:
                    pass
                # not using decode("utf-8") since it will break printing (all new line characters will result in an new line)
                logging.info(f"{ser.name}: {message}")

                # if b"message!" in message and b"END" in message:
                if b"message!" in message:
                    idx = message.find(b"message!") + len(b"message!")
                    logging.info(f"IDX: {idx}")
                    yamcs_port_in = settings.obc_port_in

                    # id_end = message.find(b"END", idx + 1)
                    # logging.info(f"ID_END: {id_end}")
                    # raw_packet = message[idx + 1:id_end]  # Changed idx + 2 to idx + 1
                    raw_packet = message[idx + 1:]
                    packet = bytearray()
                    packet_byte_decimal = 0
                    for packet_byte in raw_packet:
                        if packet_byte == SPACE:
                            packet_byte_decimal = clamp(packet_byte_decimal, 0, 255)
                            packet.append(packet_byte_decimal)
                            packet_byte_decimal = 0
                        else:
                            packet_byte_int = packet_byte - 48
                            packet_byte_decimal = packet_byte_decimal * 10 + packet_byte_int

                    decimal_string = ' '.join(str(byte) for byte in packet)
                    logging.info(f"Packet in decimal: {decimal_string}")

                    # packet = packet + bytearray([0, 0])
                    # Verify packet integrity before sending
                    if len(packet) > 6:
                        sendIfConnected(packet, settings, yamcs_port_in)
                    else:
                        logging.error("Empty packet, not sending.")
                
                # decoded_packet = cobs.decode(packet)
                # if yamcs_global_socket is not None:
                # socket.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 1)
                # sendIfConnected(packet, settings, yamcs_port_in)
                # Thread(
                #     target=sendIfConnected,
                #     args=(
                #         packet,
                #         settings,
                #         yamcs_port_in,
                #     ),
                # ).start()
        except serial.SerialException:
            logging.warning(
                "No device is connected at port "
                + serial_port
                + ". Please connect a device."
            )
            sleep(settings.reconnection_timeout)
        except socket.error as error:
            raise YAMCSClosedPortException(error)


def mcu_client_logger(
    settings: Settings,
    type: ThreadType,
    serial_port: str = None,
):
    """

    It listens to the specified serial port for TM messages.
    They usually come in the following form:
    '1801 [debug    ]OBC New TM[3,25] message! 8 1 192 2 0 15 32 3 25 0 2 0 1 37 165 53 0 0 0 0 0 \n'
    It simply logs these messages to a logfile.
    """

    fileLogger = getFileLogger(type)
    while True:
        try:
            ser = serial.Serial(
                serial_port,
                baudrate=settings.baud_rate,
                timeout=settings.serial_timeout,
            )

            # Read any messages already stored to the buffer.
            # If YAMCS receives this, it's gonna fail and the script will produce a TCP exception.
            ser.readline()

            while True:
                line = ser.readline()
                # message = cobs.decode(line)
                message = line

                fileLogger.info(message)


        except serial.SerialException:
            logging.warning(
                "No device is connected at port "
                + serial_port
                + ". Please connect a device."
            )
            sleep(settings.reconnection_timeout)

def flush_socket(sock):
    """ Read and discard any leftover data in the socket buffer """
    sock.setblocking(0)  # Set socket to non-blocking mode
    try:
        while sock.recv(4096):  # Read until there's no more data
            pass
    except BlockingIOError:
        pass  # No more data left
    sock.setblocking(1)  # Restore to blocking mode

def sendIfConnected(packet: bytearray, settings: Settings, yamcs_port_in: int):
    """
    This function uses the global variable yamcs_global_socket to connect to yamcs.
    The connect_to_port function is blocking, and more specifically the socket.accept()
    function might take up to 10 seconds to execute, meaning crucial data might be lost,
    since it is not even logged in the file.

    Since this function runs in the background each time a new packet arrives, it does not
    know about previous executions, so yamcs_global_socket remains None for some seconds, producing continuously
    an OSError: Adress already in use, which we want to supress.
    Also, if YAMCS crashes for some reason, a BrokenPipe error will be raised, which we also want
    to supress
    """
    global yamcs_global_socket, connection_state
    if connection_state == ConnectionState.NOT_CONNECTED:
        connection_state = ConnectionState.CONNECTING

        yamcs_global_socket = connect_to_port(settings, yamcs_port_in)

        connection_state = ConnectionState.CONNECTED
        # flush_socket(yamcs_global_socket)
        # try:
        # data = yamcs_global_socket.recv(2)
        # logging.info(f"Received from YAMCS: {data}")
        # except BlockingIOError:
        #     pass
        yamcs_global_socket.sendall(bytes(packet))
        # yamcs_global_socket.send(bytes(packet))

        logging.info(f"Connected. Sent: {bytes(packet)}")

    elif connection_state == ConnectionState.CONNECTED:
        try:
            # try:
            # data = yamcs_global_socket.recv(2)
            # logging.info(f"Received from YAMCS: {data}")
            # except BlockingIOError:
                # pass
            yamcs_global_socket.sendall(bytes(packet))
            logging.info(f"Connected. Sent: {bytes(packet)}")
        except BrokenPipeError:
            yamcs_global_socket = None
            connection_state = ConnectionState.NOT_CONNECTED
            logging.error("YAMCS has crashed. Reconnecting...")


def yamcs_client(settings: Settings, serial_port: str = None, subsystem: str = None):
    """
    Opens a new TCP stream socket to listen for any TC messages from YAMCS.
    When they arrive, they are encoded using COBS and sent to the serial port.
    The devboard processes the TC when it receives the "\00" delimiter.
    They are also logged in the telemetry.log file.
    If YAMCS crashes the socket will read 5 empty messages every millisecond,
    cluttering completely the logfile and the terminal, so we only print large
    messages.
    """

    # if serial_port is None:
    #     serial_port = settings.usb_serial_0

    # if subsystem == 'OBC':
    tcp_client = connect_to_port(settings, settings.yamcs_port_out)
    # elif subsystem == 'COMMS':
    #     tcp_client = connect_to_port(settings, settings.comms_port_out)
    # elif subsystem == 'ADCS':
    #     tcp_client = connect_to_port(settings, settings.adcs_port_out)

    while True:

        try:
            port = serial.Serial(
                port=serial_port,
                baudrate=settings.baud_rate,
                timeout=settings.serial_timeout,
            )
            while True:
                data, _ = tcp_client.recvfrom(settings.max_tc_size)
                if len(data) >= TC_HEADER:
                    logging.info("YAMCS: " + data.hex())
                    encoded_data = cobs.encode(data)
                    port.write(encoded_data)
                    port.write(DELIMITER)

        except serial.SerialException:
            logging.warning(
                "No device is connected at port "
                + serial_port
                + ". Please connect a device."
            )
            sleep(settings.reconnection_timeout)


