package gr.spacedot.acubesat.file_handling.network.out;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;

import gr.spacedot.acubesat.file_handling.entities.ChunkedFileEntity;

public class PacketSender {
    public void sentPacketSegments(ChunkedFileEntity chunkedFileEntity) {

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();

        HashMap<String, String> stringArguments = new HashMap<>();
        stringArguments.put("target_file_path", chunkedFileEntity.getPath());
        stringArguments.put("target_file_name", chunkedFileEntity.getName());
        stringArguments.put("data", null);

        List<byte[]> chunks = chunkedFileEntity.getChunks();
        int totalChunks = chunks.size();

        HashMap<String, Integer> integerArguments = new HashMap<>();
        integerArguments.put("current_chunk", null);
        integerArguments.put("total_chunks", totalChunks);
        integerArguments.put("chunk_size", null);


        try {

            for (int chunk = 0; chunk < totalChunks; chunk++) {

                byte[] currentChunk = chunks.get(chunk);
                String data = new String(currentChunk, "UTF-8");

                integerArguments.replace("current_chunk", chunk);
                integerArguments.replace("chunk_size", currentChunk.length);

                // stringArguments.replace("data", data);

                String stringBody = objectMapper.writeValueAsString(stringArguments);
                String integerBody = objectMapper.writeValueAsString(integerArguments);

                String argumentsBody = stringBody + integerBody;
                HashMap<String,String> args = new HashMap<>();
                args.put("args",argumentsBody);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "http://localhost:8090/api/processors/AcubeSAT/realtime/commands/file-handling/TC(24,1):send_file_segment"))
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(args)))
                        .build();
                System.out.println("REQUEST SENT " + args);

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println(response.body());
                } catch (Exception e) {
                    System.out.println("Error sending request " + e);
                }
            }
        } catch (JsonProcessingException e) {
            System.out.println("Error processing json: " + e);
        } catch (UnsupportedEncodingException e1) {
            System.out.println("Byte array to string conversion failed with error : " + e1);
        }
    }

}
