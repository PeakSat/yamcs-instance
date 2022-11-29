package gr.spacedot.acubesat.FileHandling;

import java.io.File;
import java.net.URISyntaxException;

import static com.google.common.io.Resources.getResource;

class MainApplication {
    public static void main(String[] args) {

        System.out.println("Searching files in resources directory");
        String[] files;
        try {
            File folder = new File(getResource("images").toURI());
            files = folder.list();
            for(String file : files){
                System.out.println("File "+file+" found");
            }
        }
        catch (URISyntaxException e){
            System.out.println("Error reading files: "+e);
        }

    }
}