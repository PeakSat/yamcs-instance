package gr.spacedot.acubesat;

import gr.spacedot.acubesat.file_handling.network.out.PacketParser;
import org.yamcs.YConfiguration;
import org.yamcs.cmdhistory.CommandHistoryPublisher;
import org.yamcs.commanding.PreparedCommand;
import org.yamcs.tctm.CcsdsSeqCountFiller;
import org.yamcs.tctm.CommandPostprocessor;
import org.yamcs.utils.ByteArrayUtils;

import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class CustomCommandPostprocessor implements CommandPostprocessor {

    private final CcsdsSeqCountFiller seqFiller = new CcsdsSeqCountFiller();
    private CommandHistoryPublisher commandHistory;

    private final PacketParser packetParser = new PacketParser();

    private static final Logger LOGGER = Logger.getLogger(CustomCommandPostprocessor.class.getName());

    // Constructor used when this postprocessor is used without YAML configuration
    public CustomCommandPostprocessor(String yamcsInstance) {
        this(yamcsInstance, YConfiguration.emptyConfig());
    }

    // Constructor used when this postprocessor is used with YAML configuration
    // (commandPostprocessorClassArgs)
    public CustomCommandPostprocessor(String yamcsInstance, YConfiguration config) {
    }

    // Called by Yamcs during initialization
    @Override
    public void setCommandHistoryPublisher(CommandHistoryPublisher commandHistory) {
        this.commandHistory = commandHistory;
    }

    // Called by Yamcs *after* a command was submitted, but *before* the link handles it.
    // This method must return the (possibly modified) packet binary.
    @Override
    public byte[] process(PreparedCommand pc) {
        byte[] binary = pc.getBinary();

        try {
            sleep(50);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // Set CCSDS packet length
        ByteArrayUtils.encodeUnsignedShort(binary.length - 6, binary, 4);

        // Set CCSDS sequence count
        int seqCount = seqFiller.fill(binary);

        int serviceType = binary[7];
        int messageType = binary[8];
        LOGGER.info("Message type is " + messageType + " and service type is " + serviceType);
        if (serviceType == 23 && messageType == 14) {
            packetParser.parseFileCopyPacket(binary);
        }


        // Publish the sequence count to Command History. This has no special
        // meaning to Yamcs, but it shows how to store custom information specific
        // to a command.
        commandHistory.publish(pc.getCommandId(), "ccsds-seqcount", seqCount);

        // Since we modified the binary, update the binary in Command History too.
        // commandHistory.publish(pc.getCommandId(), PreparedCommand.CNAME_BINARY, binary);

        return binary;
    }
}
