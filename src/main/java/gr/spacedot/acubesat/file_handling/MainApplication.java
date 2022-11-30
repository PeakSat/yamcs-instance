package gr.spacedot.acubesat.file_handling;

import gr.spacedot.acubesat.file_handling.entities.ChunkedFileEntity;
import gr.spacedot.acubesat.file_handling.entities.FileEntity;
import gr.spacedot.acubesat.file_handling.network.utils.FileSplitter;
import gr.spacedot.acubesat.file_handling.network.utils.FileReconstructor;

import java.io.File;
import java.util.logging.Logger;


class MainApplication {

    static FileSplitter fileSplitter = new FileSplitter();

    static FileReconstructor fileReconstructor = new FileReconstructor();

    private static final Logger LOGGER = Logger.getLogger(MainApplication.class.getName());

    public static void main(String[] args) {

        LOGGER.info("Searching files in resources directory..");

        //In java, folders are File objects.
        File folder = new File("src/main/resources/images");
        String[] files = folder.list();

        for (String file : files) {

            LOGGER.info("File " + file + " found");
            FileEntity fileEntity = new FileEntity(folder.getPath(),file);
            ChunkedFileEntity chunked = fileSplitter.splitFileInChunks(fileEntity).orElseThrow(()->new RuntimeException("File is directory"));
            LOGGER.info("Chunks size is " + chunked.getChunks().size());

            fileReconstructor.reconstruct(chunked);
        }

    }
}