package gr.spacedot.acubesat.file_handling;

import java.io.File;
import java.util.List;


class MainApplication {

    static Butcher butcher = new Butcher();

    public static void main(String[] args) {

        System.out.println("Searching files in resources directory");

        //In java, folders are File objects.
        File folder = new File("src/main/resources/images");
        String[] files = folder.list();

        for (String file : files) {

            System.out.println("File " + file + " found");
            FileEntity fileEntity = new FileEntity(file, folder.getPath());
            List<byte[]> chunks = butcher.splitFileInChunks(fileEntity);
            System.out.println("Current chunks size is " + chunks.size());
        }

    }
}