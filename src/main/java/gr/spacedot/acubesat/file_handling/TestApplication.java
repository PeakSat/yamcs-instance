package gr.spacedot.acubesat.file_handling;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Logger;

import com.google.gson.JsonObject;

class TestApplication {

    private static final Logger LOGGER = Logger.getLogger(TestApplication.class.getName());

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static void main(String[] args) {

        HttpClient client = HttpClient.newHttpClient();

        JsonObject mainBody = new JsonObject();
        mainBody.addProperty("base", "test base string");
        byte[] data = { 0x32, 0x33 };
        String dataString = "";
        for (byte word : data)
            dataString += word;
        LOGGER.info("data base 64 is: " + Base64.getEncoder().encodeToString(data));
        LOGGER.info("data string is: " + dataString);
        LOGGER.info("data utf8 is: |" + (new String(data, StandardCharsets.UTF_8)) + "|");
        LOGGER.info("data to hex is: |" + bytesToHex(data) + "|");
        mainBody.addProperty("binary_data", data.toString());

        JsonObject arguments = new JsonObject();
        arguments.add("args", mainBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8090/api/processors/AcubeSAT/realtime/commands/file-handling/TC(6,1)_load_object_memory_data"))
                .POST(HttpRequest.BodyPublishers.ofString(arguments.toString()))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HttpResponseStatus.OK.code())
                LOGGER.info(response.body());
        } catch (Exception e) {
            LOGGER.info("Error sending request " + e);
        }

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