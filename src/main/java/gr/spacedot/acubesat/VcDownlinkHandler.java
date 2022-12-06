package gr.spacedot.acubesat;
import gr.spacedot.acubesat.DownlinkTransferFrame;

/**
 * Called from the {@link MasterChannelFrameHandler} to handle TM frames for a specific virtual channel.
 * 
 * @author nm
 *
 */
public interface VcDownlinkHandler {

    void handle(DownlinkTransferFrame frame);

}
