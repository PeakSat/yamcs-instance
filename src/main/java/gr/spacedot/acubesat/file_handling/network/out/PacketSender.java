package gr.spacedot.acubesat.file_handling.network.out;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.JsonObject;
import gr.spacedot.acubesat.file_handling.entities.ChunkedFileEntity;

public class PacketSender {
    public void sentPacketSegments(ChunkedFileEntity chunkedFileEntity) {

        HttpClient client = HttpClient.newHttpClient();

        List<byte[]> chunks = chunkedFileEntity.getChunks();
        int totalChunks = chunks.size();

        for (int chunk = 0; chunk < totalChunks; chunk++) {

            JsonObject mainBody = new JsonObject();

            byte[] currentChunk = chunks.get(chunk);
            String data = new String(currentChunk, StandardCharsets.UTF_8);

            mainBody.addProperty("target_file_path", chunkedFileEntity.getPath());
            mainBody.addProperty("target_file_name", chunkedFileEntity.getName());
            mainBody.addProperty("total_chunks", totalChunks);
            mainBody.addProperty("current_chunk", chunk);
            mainBody.addProperty("chunk_size", currentChunk.length * 8); //get bits
            System.out.println(currentChunk.length);
            mainBody.addProperty("data", data);

            JsonObject args = new JsonObject();
            args.add("args", mainBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://localhost:8090/api/processors/AcubeSAT/realtime/commands/file-handling/TC(24,1):send_file_segment"))
                    .POST(HttpRequest.BodyPublishers.ofString(args.toString()))
                    .build();
//            System.out.println("REQUEST SENT "+args);

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//                System.out.println(response.body());
            } catch (Exception e) {
                System.out.println("Error sending request " + e);
            }
        }
    }

}
