package gr.spacedot.acubesat.HttpSender;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


public class TC_sender {

    private static final Logger LOGGER = Logger.getLogger(TC_sender.class.getName());

    public void sendTC(){

        HttpClient client = HttpClient.newHttpClient();
        JsonObject data1 = new JsonObject();
        JsonObject data2 = new JsonObject();
        JsonArray array = new JsonArray();
        data1.addProperty("bool_parameter", "MagnetometerSignX");
        data1.addProperty("new_value", true);
        data2.addProperty("bool_parameter", "MagnetometerSignY");
        data2.addProperty("new_value", false);
        array.add(data1);
        array.add(data2);
        JsonObject args = new JsonObject();
        args.addProperty("number_of_parameters", 2);
        args.add("parameter_ids_and_new_values", array);
        JsonObject args2 = new JsonObject();
        args2.add("args", args);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(
                "http://localhost:8090/api/processors/AcubeSAT/realtime/commands/set-values/TC(20,3)_set_bool_parameter_values"))
                .POST(HttpRequest.BodyPublishers.ofString(args2.toString()))
                .build();
        
        try{
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.info(response.body());
            LOGGER.info(data1.toString());
            LOGGER.info(data2.toString());
            LOGGER.info(array.toString());
            LOGGER.info(args.toString());
        } catch(Exception e){
            LOGGER.info("Error sending request" + e);
        }
    }

    public static void main(String[] args) {
        TC_sender sender = new TC_sender();
        sender.sendTC();
    }
    
}
