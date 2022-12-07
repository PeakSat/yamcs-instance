package gr.spacedot.acubesat.file_handling.network.out;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

import com.google.common.primitives.Bytes;
import com.google.gson.JsonObject;
import gr.spacedot.acubesat.file_handling.entities.ChunkedFileEntity;
import io.netty.handler.codec.spdy.SpdyHttpResponseStreamIdHandler;


public class PacketSender {
    private static final Logger LOGGER = Logger.getLogger(PacketSender.class.getName());

    public static int THRESHOLD_BYTES = 64000;

    /**
     * It sends each file chunk in a separate TC, including all important
     * file metadata and information.
     * <p>
     * TODO: Log response only if it is not of status 200: OK
     *
     * @param chunkedFileEntity : the already split file to be sent.
     */
    public void sentPacketSegments(ChunkedFileEntity chunkedFileEntity) {

        HttpClient client = HttpClient.newHttpClient();

        List<byte[]> chunks = chunkedFileEntity.getChunks();
        int totalChunks = chunks.size();


        JsonObject mainBody = new JsonObject();

        byte[] currentChunk = chunks.get(chunk);
        String data = Base64.getEncoder().encodeToString(currentChunk);

        mainBody.addProperty("base", new File(chunkedFileEntity.getPath(), chunkedFileEntity.getName()).toString());
        mainBody.addProperty("number_of_objects", totalChunks);

        mainBody.addProperty("binary_data", dataToLoad);

        JsonObject args = new JsonObject();
        args.add("args", mainBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8090/api/processors/AcubeSAT/realtime/commands/file-handling/TC(6,1)_load_object_memory_data"))
                .POST(HttpRequest.BodyPublishers.ofString(args.toString()))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//                LOGGER.info(response.body());
        } catch (Exception e) {
            LOGGER.info("Error sending request " + e);
        }


    }

    private byte[] getDataSegmentForPacket(List<byte[]> chunks, int initialOffset) {
        int messageSize = 0;
        int offset = initialOffset;
        int currentChunk;

        while (messageSize < THRESHOLD_BYTES) {
            byte[] information = {(byte) offset, (byte) currentChunk.length};
            byte[] dataToLoad = Bytes.concat(information);

        }

    }

}
