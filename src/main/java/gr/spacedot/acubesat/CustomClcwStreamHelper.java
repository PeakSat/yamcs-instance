package gr.spacedot.acubesat;

import java.util.Arrays;
import java.util.function.IntConsumer;

import org.yamcs.ConfigurationException;
import org.yamcs.yarch.ColumnDefinition;
import org.yamcs.yarch.DataType;
import org.yamcs.yarch.Stream;
import org.yamcs.yarch.StreamSubscriber;
import org.yamcs.yarch.Tuple;
import org.yamcs.yarch.TupleDefinition;
import org.yamcs.yarch.YarchDatabase;
import org.yamcs.yarch.YarchDatabaseInstance;


/**
 * A CLCW stream is a stream used to pass the CLCW (Command Link Control Word - see CCSDS 232.0-B-3) between the
 * receiver and the FOP1 processor.
 * <p>
 * It is just a stream where the tuples have one column, an integer.
 * <p>
 * This class provides some methods to help create, publish and subscribe to such a stream.
 * 
 * @author nm
 *
 */
public class CustomClcwStreamHelper {
    Stream stream;
    final static String CLCW_CNAME = "clcw";
    public static final String GENTIME_COLUMN = "gentime";
    public static final String SEQNUM_COLUMN = "seqNum";
    public static final String TM_RECTIME_COLUMN = "rectime";
    public static final String TM_STATUS_COLUMN = "status";
    public static final String TM_ERTIME_COLUMN = "ertime";
    public static final String TM_OBT_COLUMN = "obt";
    public static final String TM_PACKET_COLUMN = "packet";
    public static final String TM_LINK_COLUMN = "link";
    static TupleDefinition tdef;
    StreamSubscriber subscr;
    static {
        tdef = new TupleDefinition();
        //tdef.addColumn(new ColumnDefinition(CLCW_CNAME, DataType.INT));
        tdef.addColumn(new ColumnDefinition(GENTIME_COLUMN, DataType.TIMESTAMP));
        tdef.addColumn(new ColumnDefinition(SEQNUM_COLUMN, DataType.INT));
        /*tdef.addColumn(new ColumnDefinition(TM_RECTIME_COLUMN, DataType.INT));
        tdef.addColumn(new ColumnDefinition(TM_STATUS_COLUMN, DataType.INT));
        tdef.addColumn(new ColumnDefinition(TM_PACKET_COLUMN, DataType.INT));
        tdef.addColumn(new ColumnDefinition(TM_ERTIME_COLUMN, DataType.HRES_TIMESTAMP));
        tdef.addColumn(new ColumnDefinition(TM_OBT_COLUMN, DataType.LONG));
        tdef.addColumn(new ColumnDefinition(TM_LINK_COLUMN, DataType.ENUM));*/
    }

    /**
     * Creates the stream with the given name in the given yamcs instance, if it does not already exist
     * 
     * @param yamcsInstance
     * @param streamName
     */
    public CustomClcwStreamHelper(String yamcsInstance, String streamName) {
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
     * Sends the CLCW down the stream
     * 
     * @param clcw
     */
    public void sendClcw(int clcw) {
        stream.emitTuple(new Tuple(tdef, Arrays.asList(clcw)));
    }

    /**
     * Register a consumer to be called each time a new CLCW is received
     * 
     * @param c
     */
    public void onClcw(IntConsumer c) {
        if (subscr != null) {
            stream.removeSubscriber(subscr);
        }

        subscr = new StreamSubscriber() {
            @Override
            public void onTuple(Stream stream, Tuple tuple) {
                int clcw = (Integer) tuple.getColumn(0);
                c.accept(clcw);
            }
        };
        stream.addSubscriber(subscr);
    }

    public void quit() {
        if (subscr != null) {
            stream.removeSubscriber(subscr);
        }
    }
}
