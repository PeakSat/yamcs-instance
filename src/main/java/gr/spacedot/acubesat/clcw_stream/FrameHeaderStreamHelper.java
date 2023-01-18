package gr.spacedot.acubesat.clcw_stream;

import java.util.Arrays;

import org.yamcs.ConfigurationException;
import org.yamcs.utils.TimeEncoding;
import org.yamcs.yarch.ColumnDefinition;
import org.yamcs.yarch.DataType;
import org.yamcs.yarch.Stream;
import org.yamcs.yarch.Tuple;
import org.yamcs.yarch.TupleDefinition;
import org.yamcs.yarch.YarchDatabase;
import org.yamcs.yarch.YarchDatabaseInstance;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * 
 * Saves frame headers into the "frame_header" stream.
 *  
 * 
 * 
 * @author nm
 *
 */
public class FrameHeaderStreamHelper {

    Stream stream;
    static TupleDefinition fhtdef;
    public static final String GENTIME_COLUMN = "gentime";
    public static final String SEQNUM_COLUMN = "seqNum";
    public static final String TM_RECTIME_COLUMN = "rectime";
    public static final String TM_STATUS_COLUMN = "status";
    public static final String TM_PACKET_COLUMN = "packet";
    public static final String TM_ERTIME_COLUMN = "ertime";
    public static final String SCID_CNAME = "scid";
    public static final String VCID_CNAME = "vcid";

    static {
        fhtdef = new TupleDefinition();
        fhtdef.addColumn(new ColumnDefinition(GENTIME_COLUMN, DataType.TIMESTAMP));
        fhtdef.addColumn(new ColumnDefinition(SEQNUM_COLUMN, DataType.INT));
        fhtdef.addColumn(new ColumnDefinition(TM_RECTIME_COLUMN, DataType.TIMESTAMP));
        fhtdef.addColumn(new ColumnDefinition(TM_STATUS_COLUMN, DataType.INT));
        fhtdef.addColumn(new ColumnDefinition(TM_PACKET_COLUMN, DataType.BINARY));
        fhtdef.addColumn(new ColumnDefinition(TM_ERTIME_COLUMN, DataType.HRES_TIMESTAMP));
        fhtdef.addColumn(new ColumnDefinition(SCID_CNAME, DataType.INT));
        fhtdef.addColumn(new ColumnDefinition(VCID_CNAME, DataType.INT));

    }

    public FrameHeaderStreamHelper(String yamcsInstance, String FrameHeaderStreamName) {
        YarchDatabaseInstance ydb = YarchDatabase.getInstance(yamcsInstance);
        stream = ydb.getStream(FrameHeaderStreamName);
        if (stream == null) {
            try {
                ydb.execute("create stream " + FrameHeaderStreamName + fhtdef.getStringDefinition());
            } catch (Exception e) {
                throw new ConfigurationException(e);
            }
            stream = ydb.getStream(FrameHeaderStreamName);
        }
    }

    public Stream getStream(YarchDatabaseInstance ydb, String streamName) {
        stream = ydb.getStream(streamName);
        if (stream == null) {
            try {
                ydb.execute("create stream " + streamName + fhtdef.getStringDefinition());
            } catch (Exception e) {
                throw new ConfigurationException(e);
            }
            stream = ydb.getStream(streamName);
        }
        return stream;
    }

    public void sendFrameHeaderStream(int seq, DownlinkTransferFrame frame, byte[] data, int offset, int length) {
        long rectime = TimeEncoding.getWallclockTime();
        // added 2 hours to the desired date (01-01-2022 00:00:00) in order to be compatible with the GMT+0000 time. 
        // and the 37 leap seconds 
        String fullDate = "01-01-2022 02:00:37"; 
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        long gentime = 0L;
        try {
            gentime = sdf.parse(fullDate).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        int status = 0; 
        stream.emitTuple(new Tuple(fhtdef, Arrays.asList(gentime, seq, rectime, status, getData(data, 0, 6), frame.getEarthRceptionTime(), frame.getSpacecraftId(), frame.getVirtualChannelId())));
    }

    private byte[] getData(byte[]data, int offset, int length) {
        if(offset==0 && length == data.length) {
            return data;
        } else {
            return Arrays.copyOfRange(data, offset, offset+length);
        }
    }
}
