package gr.spacedot.acubesat;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.yamcs.YConfiguration;
import org.yamcs.logging.Log;
import gr.spacedot.acubesat.comms_link.PacketInputStream;
import gr.spacedot.acubesat.comms_link.PacketTooLongException;

/**
 * This input stream reads packets of a configurable fixed packet size.
 *
 * @author st
 *
 */
 
public class FixedPacketInputStream implements PacketInputStream {
    private int frameLength;
    protected DataInputStream dataInputStream;
    static Log log = new Log(FixedPacketInputStream.class);

    public void init(InputStream inputStream, YConfiguration args) {
        this.dataInputStream = new DataInputStream(inputStream);
        this.frameLength = args.getInt("frameLength");
    }      

    public byte[] readPacket() throws IOException, PacketTooLongException {
        log.trace("Reading packet length of fixed size {}", frameLength);
        byte[] data = new byte[frameLength];
        dataInputStream.readFully(data);
        return data;
    }

    public void close() throws IOException {
        dataInputStream.close();
    }
}
