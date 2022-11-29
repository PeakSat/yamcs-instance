package gr.spacedot.acubesat.file_handling;

import gr.spacedot.acubesat.file_handling.entities.ChunkedFileEntity;
import gr.spacedot.acubesat.file_handling.entities.FileEntity;
import gr.spacedot.acubesat.file_handling.network.utils.Butcher;
import gr.spacedot.acubesat.file_handling.network.utils.FileReconstructor;

import java.io.File;


class MainApplication {

    static Butcher butcher = new Butcher();

    static FileReconstructor fileReconstructor = new FileReconstructor();

    public static void main(String[] args) {

        System.out.println("Searching files in resources directory..");

        //In java, folders are File objects.
        File folder = new File("src/main/resources/images");
        String[] files = folder.list();

        for (String file : files) {

            System.out.println("File " + file + " found");
            FileEntity fileEntity = new FileEntity(folder.getPath(),file);
            ChunkedFileEntity chunked = butcher.splitFileInChunks(fileEntity).orElseThrow(()->new RuntimeException("File is directory"));
            System.out.println("Chunks size is " + chunked.getChunks().size());

            fileReconstructor.reconstruct(chunked);
        }

    }
}