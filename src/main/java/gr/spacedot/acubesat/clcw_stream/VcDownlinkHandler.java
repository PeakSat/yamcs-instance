package gr.spacedot.acubesat.clcw_stream;
import gr.spacedot.acubesat.clcw_stream.DownlinkTransferFrame;

/**
 * Called from the {@link MasterChannelFrameHandler} to handle TM frames for a specific virtual channel.
 * 
 * @author nm
 *
 */
public interface VcDownlinkHandler {

    void handle(DownlinkTransferFrame frame);

}
