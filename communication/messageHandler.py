"""
This file takes care of the communication between ATSAM development boards 
running OBC/ADCS software and a running YAMCS instance.
"""
from threading import Thread
from dataclasses import dataclass
from logging import Logger, config
from enum import Enum
import logging
import socket
from time import sleep
import yaml
import serial
from cobs import cobs

EXCLAMATION_MARK = 0x021
SPACE = 0x020
DELIMITER = b"\x00"
TC_HEADER = 11
yamcs_global_socket = None


class ConnectionState(Enum):
    NOT_CONNECTED = 0
    CONNECTING = 1
    CONNECTED = 2


class ThreadType(Enum):
    TELEMETRY = 0
    OBC = 1
    ADCS = 2


connection_state = ConnectionState.NOT_CONNECTED


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
    obc_port_in: int
    adcs_port_in: int
    canBus_port_in: int
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
    type: ThreadType,
    serial_port: str = None,
    yamcs_port_in: int = None,
):
    """

    Opens a new TCP stream socket.
    It listens to the specified serial port for TM messages.
    They usually come in the following form:
    '1801 [debug    ]OBC New TM[3,25] message! 8 1 192 2 0 15 32 3 25 0 2 0 1 37 165 53 0 0 0 0 0 \n'
    This string is split after the "!", returning the actual packet.
    All the bytes after that are sent to YAMCS.
    If we try to convert the characters one by one from ASCII to integer, we will get something like:
    8 1 1 9 2 0 0 2 0 ... -> garbage
    So we need to parse a sequence of numbers as a single decimal.
    In order to do this, we must first convert all ascii numbers to decimal form.
    Also we need to keep track of the space characters. If we receive numbers one after the other
    (whithout space characters in between), we need to multiply the previous entry by 10, in order
    to parse the whole decimal.
    Note:
        If the debugging messages are altered, this script will have undetermined behavior, since
        it relies on the existence of the exclamation mark "!" to detect actual TMs being sent.
    """
    if yamcs_port_in is None:
        yamcs_port_in = settings.obc_port_in
    if serial_port is None:
        serial_port = settings.usb_serial_0

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
                message = cobs.decode(line)
                # not using decode("utf-8") since it will break printing (all new line characters will result in an new line)
                logging.info(f"{ser.name}: {message}")
                fileLogger.info(message)

                idx = message.find(EXCLAMATION_MARK)
                if idx == -1:
                    continue

                raw_packet = message[idx + 2 :]
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

                Thread(
                    target=sendIfConnected,
                    args=(
                        packet,
                        settings,
                        yamcs_port_in,
                    ),
                ).start()
        except serial.SerialException:
            logging.warning(
                "No device is connected at port "
                + serial_port
                + ". Please connect a device."
            )
            sleep(settings.reconnection_timeout)
        except socket.error as error:
            raise YAMCSClosedPortException(error)


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
        yamcs_global_socket.send(bytes(packet))

    elif connection_state == ConnectionState.CONNECTED:
        try:
            yamcs_global_socket.send(bytes(packet))
        except BrokenPipeError:
            yamcs_global_socket = None
            connection_state = ConnectionState.NOT_CONNECTED


def yamcs_client(settings: Settings, serial_port: str = None):
    """
    Opens a new TCP stream socket to listen for any TC messages from YAMCS.
    When they arrive, they are encoded using COBS and sent to the serial port.
    The devboard processes the TC when it receives the "\00" delimiter.
    They are also logged in the telemetry.log file.
    If YAMCS crashes the socket will read 5 empty messages every millisecond,
    cluttering completely the logfile and the terminal, so we only print large
    messages.
    """

    if serial_port is None:
        serial_port = settings.usb_serial_0

    tcp_client = connect_to_port(settings, settings.yamcs_port_out)

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


def getFileLogger(type: ThreadType) -> Logger:
    if type == ThreadType.TELEMETRY:
        return logging.getLogger("telemetry")
    elif type == ThreadType.OBC:
        return logging.getLogger("obc")
    elif type == ThreadType.ADCS:
        return logging.getLogger("adcs")


if __name__ == "__main__":
    """
    Example setup of a thread listening to the ADCS serial port and sending messages to the corresponding YAMCS socket.
    When connecting more than one devices using the same protocol (UART \ USB),
    be careful of their corresponding serial port. Each new device is assigned the next available ID.
    For example, if a devboard is connected with a USB cable, the serial port will be /dev/ttyACM0.
    The next devboard connected via usb will be assigned to /dev/ttyACM1.
    If a third board is connected using a uart-to-usb adapter, the serial port will be /dev/ttyUSB0.

    Example:
        adcs_listener_thread = Thread(
            target=mcu_client,
            args=(
                settings,
                adcs_serial_port,
                settings.adcs_port_in,
            ),
        ).start()
    """

    # setup logging
    config.fileConfig("logging.conf")
    with open("settings.yaml", "r") as stream:
        try:
            data = yaml.safe_load(stream)
            settings = Settings(**data)
            logging.debug("Successfully read settings file.")
        except yaml.YAMLError:
            logging.exception("File reading error.")

    yamcs_listener_thread = Thread(target=yamcs_client, args=(settings,))
    yamcs_listener_thread.start()

    obc_serial_port = settings.usb_serial_0
    adcs_serial_port = settings.uart_serial_0
    can_serial_port = settings.uart_serial_1

    obc_listener_thread = Thread(
        target=mcu_client,
        args=(
            settings,
            ThreadType.OBC,
            obc_serial_port,
            settings.obc_port_in,
        ),
    )
    obc_listener_thread.start()

    adcs_listener_thread = Thread(
        target=mcu_client,
        args=(
            settings,
            ThreadType.ADCS,
            adcs_serial_port,
            settings.adcs_port_in,
        ),
    ).start()

    can_listener_thread = Thread(
        target=mcu_client,
        args=(
            settings,
            ThreadType.TELEMETRY,
            can_serial_port,
            settings.canBus_port_in,
        ),
    ).start()
