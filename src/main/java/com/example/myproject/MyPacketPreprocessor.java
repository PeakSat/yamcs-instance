package com.example.myproject;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;
import org.yamcs.TmPacket;
import org.yamcs.YConfiguration;
import org.yamcs.tctm.AbstractPacketPreprocessor;

public class MyPacketPreprocessor extends AbstractPacketPreprocessor {
    private static final Logger LOGGER = Logger.getLogger(MyPacketPreprocessor.class.getName());
    ConsoleHandler fh;

    private Map<Integer, AtomicInteger> seqCounts = new HashMap<>();

    // Constructor used when this preprocessor is used without YAML configuration
    public MyPacketPreprocessor(String yamcsInstance) {
        this(yamcsInstance, YConfiguration.emptyConfig());
    }

    // Constructor used when this preprocessor is used with YAML configuration
    // (packetPreprocessorClassArgs)
    public MyPacketPreprocessor(String yamcsInstance, YConfiguration config) {
        super(yamcsInstance, config);
    }

    @Override
    public TmPacket process(TmPacket packet) {
        fh = new ConsoleHandler();

        byte[] bytes = packet.getPacket();

        // Expect at least the length of CCSDS primary and secondary header
        if (bytes.length < 17) {
            eventProducer.sendWarning("SHORT_PACKET",
                    "Short packet received, length: " + bytes.length + "; minimum required length is 17 bytes.");
            // If we return null, the packet is dropped.
            return null;
        }

        // Verify continuity for a given APID based on the CCSDS sequence counter
        int apidseqcount = ByteBuffer.wrap(bytes).getInt(0); // first 4 bytes (0-3)
        short packetlength = ByteBuffer.wrap(bytes).getShort(4); // get 2 bytes (5-6)
        packetlength++;
        int apid = (apidseqcount >> 16) & 0x07FF; // 11 bits ()
        int seqcount = (apidseqcount) & 0x3FFF; // 14 bits
        int packversion = (apidseqcount >> 29) & 0x7; // 3 bits
        int secheader = (apidseqcount >> 27) & 0x1; // 1 bit
        int pusversion = ByteBuffer.wrap(bytes).get(6) & 0xF;// 4 bits
        int serviceType = ByteBuffer.wrap(bytes).get(7) & 0xFF;// 8 bits
        int messageType = ByteBuffer.wrap(bytes).get(8) & 0xFF;// 8 bits
        long time = ByteBuffer.wrap(bytes).getInt(11) & 0xFFFFFFFF; // 32 bits. Long to prevent overflow

        AtomicInteger ai = seqCounts.computeIfAbsent(apid, k -> new AtomicInteger());
        int oldseq = ai.getAndSet(seqcount);

        if (((seqcount - oldseq) & 0x3FFF) != 2) {
            LOGGER.info("SEQ");
            eventProducer.sendWarning("SEQ_COUNT_JUMP",
                    "Sequence count jump for APID: " + apid + " old seq: " + oldseq + " newseq: " + seqcount);
        }
        
        if (packversion != 0) {
            LOGGER.info("PACKET_VERSION: " + String.valueOf(packversion));
            eventProducer.sendWarning("PACKET_VERSION_ERROR",
                    "Wrong version number. Expected 0 and got " + String.valueOf(packversion));

        }

        if (secheader != 1) {
            LOGGER.info("SEC_HEAD: " + String.valueOf(secheader));
            eventProducer.sendWarning("SEC_HEADER_FLAG_ERROR",
                    "Wrong secondary flag. Expected 1 and got " + String.valueOf(secheader));
        }

        if (packetlength != (bytes.length - 6)) {
            LOGGER.info("LENGTH");
            eventProducer.sendWarning("PACKET_LENGTH_ERROR",
                    "Wrong packet data length. Expected " + String.valueOf((bytes.length - 6)) + " and got "
                            + String.valueOf(packetlength));
        }
        LOGGER.info("Sequence_count:" + String.valueOf(seqcount));
        LOGGER.info("APID:" + String.valueOf(apid));
        LOGGER.info("PUS:" + String.valueOf(pusversion));
        LOGGER.info("Secondary_header:" + String.valueOf(secheader));
        LOGGER.info("Buffer:" + String.valueOf(apidseqcount));
        LOGGER.info("Time:" + String.valueOf(time));
        LOGGER.info("ServiceType:" + String.valueOf(serviceType));
        LOGGER.info("MessageType:" + String.valueOf(messageType));
        LOGGER.info("Packet data length:" + String.valueOf(packetlength));
        // // Our custom packets don't include a secundary header with time information.
        // Use Yamcs-local time instead.
        packet.setGenerationTime(CUCtoUnix(time));

        // Use the full 32-bits, so that both APID and the count are included.
        // Yamcs uses this attribute to uniquely identify the packet (together with the
        // gentime)
        // packet.setSequenceCount(apidseqcount);

        return packet;

    }

    // Returns the unix timestamp in milliseconds
    long CUCtoUnix(long time) {
        long start = 1577836800; // 1/1/2020
        return start * 1000 + time * 100 + 7237000; // adjust from UTC to GMT +0200
    }
}
