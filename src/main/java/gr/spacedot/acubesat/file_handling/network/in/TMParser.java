package gr.spacedot.acubesat.file_handling.network.in;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.logging.Logger;

import com.google.gson.JsonObject;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import gr.spacedot.acubesat.file_handling.entities.FileEntity;
import gr.spacedot.acubesat.file_handling.enums.LocalPaths;
import gr.spacedot.acubesat.file_handling.utils.PacketParser;
import io.netty.handler.codec.http.HttpResponseStatus;
import gr.spacedot.acubesat.file_handling.enums.PacketType;

import static gr.spacedot.acubesat.file_handling.utils.PacketParser.DELIMITER;

public class TMParser {

    private static final Logger LOGGER = Logger.getLogger(TMParser.class.getName());

    private static final PacketParser packetParser = new PacketParser();

    /**
     * Reads the packet and writes the data to the files.
     *
     * @param packet: the TM[6,4] packet containing file segments
     */
    public void parseFileSegmentPacket(byte[] packet) {

        byte[] data = packetParser.parseData(packet, PacketType.TM);

        StringBuilder builder = new StringBuilder();
        int packetOffset = 0;
        for (byte character : data) {
            if (character != DELIMITER) {
                builder.append((char) character);
                packetOffset++;
            } else {
                packetOffset++;
                break;
            }
        }

        String base = builder.toString();
        FileEntity entity = new FileEntity(LocalPaths.RECEIVED_PATH.toString(), base);
        File path = entity.getTruePath();
        if (!path.exists())
            path.mkdir();
        File file = new File(path, base);
        try {
            LOGGER.info(String.format("Trying to create %s", file.toPath().toAbsolutePath().toString()));
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }


        int numberOfObjects = data[packetOffset];
        packetOffset++;

        LOGGER.info(String.format("Data length is %d base is %s offset is %d objects are %d", data.length, base, packetOffset, numberOfObjects));
        for (int object = 0; object < numberOfObjects; object++) {

            ByteBuffer buffer = ByteBuffer.wrap(data);
            int offset = buffer.getInt(packetOffset);
            packetOffset += 4;
            short length = buffer.getShort(packetOffset);
            packetOffset += 2;

            LOGGER.info(String.format("Saving %s bytes to %s", length, base));
            byte[] fileSegment = Arrays.copyOfRange(data, packetOffset, packetOffset + length);

            try (RandomAccessFile writer = new RandomAccessFile(file, "rw")) {
                writer.seek(offset);
                writer.write(fileSegment);

            } catch (Exception e) {
                e.printStackTrace();
            }


            packetOffset += length;
            String bucketName = "testingBucket";
            if(!bucketExists(bucketName)){
                createBucket(bucketName);   
            }
            uploadObjectToBucket(fileSegment, bucketName);

        }
    }

    public boolean bucketExists(String bucketName){
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest getBucket = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8090/api/buckets/AcubeSAT/" + bucketName))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        try{
            HttpResponse<String> response = client.send(getBucket, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != HttpResponseStatus.OK.code()){
                LOGGER.info(response.body());
                return false;
            }
        }catch(Exception e){
            LOGGER.info("Error sending request" + e);
        }
        return true;
    }

    public void createBucket(String bucketName){
        JsonObject mainBody = new JsonObject();
        HttpClient client = HttpClient.newHttpClient();
        mainBody.addProperty("name", bucketName);

        HttpRequest createBucketRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8090/api/buckets/AcubeSAT"))
            .POST(HttpRequest.BodyPublishers.ofString(mainBody.toString()))
            .build();

        try{
            HttpResponse<String> response = client.send(createBucketRequest, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != HttpResponseStatus.OK.code())
                LOGGER.info(response.body());
        }catch(Exception e){
            LOGGER.info("Error sending request" + e);
        }
    }

    public void uploadObjectToBucket(byte[] object, String bucketName){
        String base64String = Base64.getEncoder().encodeToString(object);
        HttpClient client = HttpClient.newHttpClient();
        JsonObject mainBody = new JsonObject();
        mainBody.addProperty("data", base64String);

        HttpRequest uploadObjectRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8090/api/buckets/AcubeSAT/" + bucketName + "/objects/a3sat.png"))
            .header("Content-Type", "text/html")
            .POST(HttpRequest.BodyPublishers.ofString(mainBody.toString()))
            .build();

        try{
            HttpResponse<String> response = client.send(uploadObjectRequest, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != HttpResponseStatus.OK.code())
                LOGGER.info(response.body());
        }catch(Exception e){
            LOGGER.info("Error sending request" + e);
        }
    }
}
