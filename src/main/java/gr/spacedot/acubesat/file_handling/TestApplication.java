package gr.spacedot.acubesat.file_handling;

import gr.spacedot.acubesat.file_handling.entities.ChunkedFileEntity;
import gr.spacedot.acubesat.file_handling.entities.FileEntity;
import gr.spacedot.acubesat.file_handling.enums.LocalPaths;
import gr.spacedot.acubesat.file_handling.network.out.PacketSender;
import gr.spacedot.acubesat.file_handling.utils.FileReconstructor;
import gr.spacedot.acubesat.file_handling.utils.FileSplitter;

import java.io.File;
import java.util.logging.Logger;


class TestApplication {

    static FileSplitter fileSplitter = new FileSplitter();

    static FileReconstructor fileReconstructor = new FileReconstructor();

    private static final Logger LOGGER = Logger.getLogger(TestApplication.class.getName());

    public static void main(String[] args) {

        LOGGER.info("Searching files in resources directory..");

        //In java, folders are File objects.
        File folder = new File(LocalPaths.RESOURCES_PATH +"/images");
        String[] files = folder.list();

        for (String file : files) {

            LOGGER.info("File " + file + " found");
            FileEntity fileEntity = new FileEntity(folder.getPath(), file);
            ChunkedFileEntity chunked = fileSplitter.splitFileInChunks(fileEntity);
            LOGGER.info("Chunks size is " + chunked.getChunks().size());

            fileReconstructor.reconstruct(chunked);
        }

    }
}