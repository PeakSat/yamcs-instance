package gr.spacedot.acubesat.clcw_stream;

import org.yamcs.yarch.Tuple;

@FunctionalInterface
public interface StreamSubscriber {

    void onTuple(Stream stream, Tuple tuple);

    default void streamClosed(Stream stream) {
    }
}

