import binascii
import io
import socket
import random
import struct
import sys
from struct import unpack_from
from threading import Thread
from time import sleep

PACKET_HEADER_LENGTH = 11

STRING_DELIMITER = 0x000


def processTC(data: bytearray) -> None:
    serviceType = data[7]
    messageType = data[8]
    print("Message type is {} and service type is {}".format(messageType, serviceType))
    if serviceType == 24 and messageType == 1:
        processFileSegment(data[PACKET_HEADER_LENGTH - 1 :])
    pass


def processFileSegment(data: bytearray) -> None:
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
    # src/main/resources/source/files smallFile.txt

    print(
        "Path is {} , name is {} and offset is {}".format(
            targetFilePath, targetFileName, offset
        )
    )

    currentChunk = data[offset] * 256 + data[offset + 1]
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
    fileData = data[offset:]

    file = open(targetFileName, "ab")
    file.write(fileData)
