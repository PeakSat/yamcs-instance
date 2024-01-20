package gr.spacedot.acubesat.clcw_stream;

import java.util.Arrays;

import org.yamcs.ConfigurationException;
import org.yamcs.utils.TimeEncoding;
import org.yamcs.yarch.ColumnDefinition;
import org.yamcs.yarch.DataType;
import org.yamcs.yarch.Stream;
import org.yamcs.yarch.StreamSubscriber;
import org.yamcs.yarch.Tuple;
import org.yamcs.yarch.TupleDefinition;
import org.yamcs.yarch.YarchDatabase;
import org.yamcs.yarch.YarchDatabaseInstance;
import java.text.ParseException;
import java.text.SimpleDateFormat;


/**
 * A CLCW stream is a stream used to pass the CLCW (Command Link Control Word - see CCSDS 232.0-B-3) between the
 * receiver and the FOP1 processor.
 * <p>
 * The Tuple of this stream has multiple columns and passes the parameters of the clcw field into the defined rootContainer 
 * in order to be visualised in the web-interface
 * <p>
 * This class provides some methods to help create, publish and subscribe to such a stream.
 * 
 * @author nm
 *
 */
public class ClcwParamsStreamHelper {
    Stream stream;
    public static final String GENTIME_COLUMN = "gentime";
    public static final String SEQNUM_COLUMN = "seqNum";
    public static final String TM_RECTIME_COLUMN = "rectime";
    public static final String TM_STATUS_COLUMN = "status";
    public static final String TM_PACKET_COLUMN = "packet";
    public static final String TM_ERTIME_COLUMN = "ertime";
    public static final String SCID_CNAME = "scid";
    public static final String VCID_CNAME = "vcid";
    static TupleDefinition tdef;
    StreamSubscriber subscr;
    static {
        tdef = new TupleDefinition();
        tdef.addColumn(new ColumnDefinition(GENTIME_COLUMN, DataType.TIMESTAMP));
        tdef.addColumn(new ColumnDefinition(SEQNUM_COLUMN, DataType.INT));
        tdef.addColumn(new ColumnDefinition(TM_RECTIME_COLUMN, DataType.TIMESTAMP));
        tdef.addColumn(new ColumnDefinition(TM_STATUS_COLUMN, DataType.INT));
        tdef.addColumn(new ColumnDefinition(TM_PACKET_COLUMN, DataType.BINARY));
        tdef.addColumn(new ColumnDefinition(TM_ERTIME_COLUMN, DataType.HRES_TIMESTAMP));
        tdef.addColumn(new ColumnDefinition(SCID_CNAME, DataType.INT));
        tdef.addColumn(new ColumnDefinition(VCID_CNAME, DataType.INT));
    }

    /**
     * Creates the stream with the given name in the given yamcs instance, if it does not already exist
     * 
     * @param yamcsInstance
     * @param streamName
     */
    public ClcwParamsStreamHelper(String yamcsInstance, String streamName) {
        YarchDatabaseInstance ydb = YarchDatabase.getInstance(yamcsInstance);
        stream = ydb.getStream(streamName);
        if (stream == null) {
            try {
                ydb.execute("create stream " + streamName + tdef.getStringDefinition());
            } catch (Exception e) {
                throw new ConfigurationException(e);
            }
            stream = ydb.getStream(streamName);
        }
    }

    /**
     * Sends the CLCW  fields down the "clcw" stream
     * 
     * @param clcw
     */

    public void sendClcwParams(int seq, DownlinkTransferFrame frame, byte[] data, int offset, int length, boolean errorDetectionEnabled) {
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
        if (errorDetectionEnabled){
            stream.emitTuple(new Tuple(tdef, Arrays.asList(gentime, seq, rectime, status, getData(data, offset+length-6, 4), frame.getEarthRceptionTime(), frame.getSpacecraftId(), frame.getVirtualChannelId())));
        }
        else{
            stream.emitTuple(new Tuple(tdef, Arrays.asList(gentime, seq, rectime, status, getData(data, offset+length-4, 4), frame.getEarthRceptionTime(), frame.getSpacecraftId(), frame.getVirtualChannelId())));
        }
        
    }

    /**
     * Register a consumer to be called each time a new CLCW is received
     * 
     * @param c
     */
    private byte[] getData(byte[]data, int offset, int length) {
        if(offset==0 && length == data.length) {
            return data;
        } else {
            return Arrays.copyOfRange(data, offset, offset+length);
        }
    }
}
