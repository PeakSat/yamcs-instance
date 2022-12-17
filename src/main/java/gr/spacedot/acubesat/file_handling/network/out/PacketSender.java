package gr.spacedot.acubesat.file_handling.network.out;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import javax.xml.bind.DatatypeConverter;
import java.util.List;
import java.util.logging.Logger;

import com.google.common.primitives.Bytes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gr.spacedot.acubesat.file_handling.entities.ChunkedFileEntity;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.HttpResponseStatus;

public class PacketSender {
    private static final Logger LOGGER = Logger.getLogger(PacketSender.class.getName());

    public static final int THRESHOLD_BYTES = 64000;
    
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * It sends a file split in chunks using one or more packets
     *
     * @param chunkedFileEntity : the already split file to be sent.
     */
    public void sentPacketSegments(ChunkedFileEntity chunkedFileEntity) {

        HttpClient client = HttpClient.newHttpClient();

        List<byte[]> chunks = chunkedFileEntity.getChunks();

        int offset = 0;

        do {
            JsonObject mainBody = new JsonObject();
            mainBody.addProperty("base", new File(chunkedFileEntity.getPath(), chunkedFileEntity.getName()).toString());
            offset = putChunksIntoPacket(chunks, mainBody, offset);

            JsonObject args = new JsonObject();
            args.add("args", mainBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://localhost:8090/api/processors/AcubeSAT/realtime/commands/file-handling/TC(6,1)_load_object_memory_data"))
                    .POST(HttpRequest.BodyPublishers.ofString(args.toString()))
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != HttpResponseStatus.OK.code())
                    LOGGER.info(response.body());
            } catch (Exception e) {
                LOGGER.info("Error sending request " + e);
            }
        }while (offset < chunks.size() - 1);

    }

    /**
     * Places the chunks into groups smaller than the maximum CCSDS packet limit.
     *
     * @param chunks: The chunks of the file to be transmitted
     * @return the starting index for the next packet.
     */
    private int putChunksIntoPacket(List<byte[]> data, JsonObject mainbody, int startChunkIndex) {

        int messageSize = 0;
        int lastChunkIndex = 0;
        int byteOffset = 0;

        JsonArray memoryData = new JsonArray();

        for (int chunk = startChunkIndex; chunk < data.size(); chunk++) {
            byte[] currentChunk = data.get(chunk);
            int chunkLength = currentChunk.length;
            byteOffset = chunkLength * chunk;

            byte[] information = { (byte) byteOffset, (byte) chunkLength };
            byteOffset += chunkLength;
            messageSize += chunkLength + information.length;

            lastChunkIndex = chunk;

            if (messageSize <= THRESHOLD_BYTES) {
                System.out.println("Added chunk " + chunk);
                String byteData = bytesToHex(Bytes.concat(information, currentChunk));
                memoryData.add(byteData);
            } else
                break;
        }
        int numberOfObjects = lastChunkIndex + 1 - startChunkIndex;

        System.out.println("number_of_objects is " + numberOfObjects);

        mainbody.addProperty("number_of_objects", numberOfObjects);
        mainbody.add("binary_data", memoryData);

        return lastChunkIndex;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

}
