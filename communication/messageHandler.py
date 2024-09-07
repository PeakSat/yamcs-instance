"""
This file takes care of the communication between ATSAM development boards 
running OBC/ADCS software and a running YAMCS instance.
"""
from threading import Thread
from logging import config
import logging
import yaml
from common_module import Settings, mcu_client, yamcs_client
import argparse
from os import path


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

    args_parser = argparse.ArgumentParser(
            prog='messageHandler',
            description='A simple message handler',
            epilog='Made by SpaceDot')


    args_parser.add_argument('--obclog', default="obc.log", help="(--obclog obc.log) Change the log file for the OBC system")
    args_parser.add_argument('--adcslog', default="adcs.log", help="(--adcslog obc.log) Change the log file for the ADCS system")

    args = args_parser.parse_args()

    # setup logging
    config.fileConfig('logging.conf')
    with open("settings.yaml", "r") as stream:
        try:
            data = yaml.safe_load(stream)
            settings = Settings(**data)
            logging.debug("Successfully read settings file.")
        except yaml.YAMLError:
            logging.exception("File reading error.")

    serial_port = settings.uart_serial_0
    yamcs_listener_thread = Thread(target=yamcs_client, args=(settings, serial_port, 'OBC'))
    yamcs_listener_thread = Thread(target=yamcs_client, args=(settings, serial_port, 'ADCS'))
    yamcs_listener_thread.start()

    obc_adcs_serial_port = settings.uart_serial_0
    # adcs_logs_serial_port = settings.uart_serial_0
    # obc_logs_serial_port = settings.uart_serial_1

    obc_adcs_listener_thread = Thread(
        target=mcu_client,
        args=(
            settings,
            obc_adcs_serial_port,
            str(args.obclog), # default = "abc.log",
            str(args.adcslog), # default = "adcs.log",
            "" # default = "comms.log"
        ),
    ).start()

    # adcs_logger_thread = Thread(
    #     target=mcu_client_logger,
    #     args=(
    #         settings,
    #         ThreadType.ADCS,
    #         adcs_logs_serial_port,
    #     ),
    # ).start()

    # obc_logger_thread = Thread(
    #     target=mcu_client_logger,
    #     args=(
    #         settings,
    #         ThreadType.OBC,
    #         obc_logs_serial_port,
    #     ),
    # ).start()

