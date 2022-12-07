package gr.spacedot.acubesat.file_handling.network.out;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import javax.xml.bind.DatatypeConverter;
import java.util.List;
import java.util.logging.Logger;

import com.google.common.primitives.Bytes;
import com.google.gson.JsonObject;
import gr.spacedot.acubesat.file_handling.entities.ChunkedFileEntity;


public class PacketSender {
    private static final Logger LOGGER = Logger.getLogger(PacketSender.class.getName());

    public static final int THRESHOLD_BYTES = 64000;

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

        int offset = 0;

        while (offset != -1) {
            JsonObject mainBody = new JsonObject();
            mainBody.addProperty("base", new File(chunkedFileEntity.getPath(), chunkedFileEntity.getName()).toString());
            offset = putChunksIntoPackets(chunks, mainBody, offset);

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


    }

    /**
     * Places the chunks into groups smaller than the maximum CCSDS packet limit.
     *
     * @param chunks: The chunks of the file to be transmitted
     * @return the starting index for the next packet and -1 If all chunks have been sent.
     */
    private int putChunksIntoPackets(List<byte[]> chunks, JsonObject mainbody, int start) {

        if (start == chunks.size() - 1)
            return -1;
        int messageSize = 0;
        int offset = start;
        int lastChunkIndex = 0;

        byte[] currentPacket = {};
        List<byte[]> packetData = new ArrayList<>();

        for (int chunk = offset; chunk < chunks.size(); chunk++) {
            byte[] currentChunk = chunks.get(chunk);
            int chunkLength = currentChunk.length;

            byte[] information = {(byte) offset, (byte) chunkLength};
            offset += chunkLength;
            messageSize += chunkLength + information.length;

            if (messageSize > THRESHOLD_BYTES) {
                packetData.add(currentPacket);
                lastChunkIndex = chunk;
                break;
            } else {
                System.out.println("Added chunk "+chunk);
                currentPacket = Bytes.concat(currentPacket, information, currentChunk);
            }
        }
        mainbody.addProperty("number_of_objects", lastChunkIndex + 1);
        System.out.println("number_of_objects is "+lastChunkIndex + 1);

        byte[] finalPacket = {};
        for (byte[] chunk : packetData) {
            finalPacket = Bytes.concat(finalPacket, chunk);
        }
        mainbody.addProperty("binary_data", DatatypeConverter.printHexBinary(finalPacket));

        return offset;
    }

}
