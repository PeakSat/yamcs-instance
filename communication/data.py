from enum import Enum

EXCLAMATION_MARK = 0x021
HASH_TAG = 0x023
QUESTION_MARK = 0X03F
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
    COMMS = 3

connection_state = ConnectionState.NOT_CONNECTED