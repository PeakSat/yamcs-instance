"""
This file takes care of the communication between ATSAM development boards 
running COMMS software and a running YAMCS instance.
"""
from threading import Thread
from logging import config
import logging
import yaml
from common_module import ThreadType, ConnectionState, Settings, mcu_client, mcu_client_logger, sendIfConnected, yamcs_client, getFileLogger


if __name__ == "__main__":
    # setup logging
    config.fileConfig("logging.conf")
    with open("settings.yaml", "r") as stream:
        try:
            data = yaml.safe_load(stream)
            settings = Settings(**data)
            logging.debug("Successfully read settings file.")
        except yaml.YAMLError:
            logging.exception("File reading error.")

    serial_port = settings.usb_serial_0
    yamcs_listener_thread = Thread(target=yamcs_client, args=(settings,serial_port, 'COMMS'))
    yamcs_listener_thread.start()

    comms_serial_port = settings.usb_serial_0
    # comms_logs_serial_port = settings.usb_serial_0

    comms_listener_thread = Thread(
        target=mcu_client,
        args=(
            settings,
            comms_serial_port,
        ),
    ).start()

    # comms_logger_thread = Thread(
    #     target=mcu_client_logger,
    #     args=(
    #         settings,
    #         ThreadType.COMMS,
    #         comms_logs_serial_port,
    #     ),
    # ).start()

