import logging
from data import ThreadType

def change_log_file(logger: logging.Logger, filename: str) -> logging.Logger:
    temp: logging.Logger = logger
    formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')

    file_handler = logging.FileHandler(filename)
    file_handler.setLevel(temp.level)
    file_handler.setFormatter(formatter)

    temp.addHandler(file_handler)
    return temp


def getFileLogger(type: ThreadType) -> logging.Logger:
    if type == ThreadType.TELEMETRY:
        return logging.getLogger("telemetry")
    elif type == ThreadType.OBC:
        return logging.getLogger("obc")
    elif type == ThreadType.ADCS:
        return logging.getLogger("adcs")
    elif type == ThreadType.COMMS:
        return logging.getLogger("comms")
