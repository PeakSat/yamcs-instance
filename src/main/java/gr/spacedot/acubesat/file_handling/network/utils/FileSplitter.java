package gr.spacedot.acubesat.file_handling.network.utils;

import gr.spacedot.acubesat.file_handling.entities.ChunkedFileEntity;
import gr.spacedot.acubesat.file_handling.entities.FileEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;


public class FileSplitter {
    private static final int CHUNK_SIZE = 60_000;

    public Optional<ChunkedFileEntity> splitFileInChunks(FileEntity fileEntity) {
        ChunkedFileEntity chunkedFileEntity = new ChunkedFileEntity();
        chunkedFileEntity.setName(fileEntity.getName());
        chunkedFileEntity.setPath(fileEntity.getPath());

        ArrayList<byte[]> chunks = new ArrayList<>();

        try {
            File file = new File(fileEntity.getPath(), fileEntity.getName());
            System.out.println("Path is " + file.getPath());
            if(file.isDirectory())
                return Optional.empty();
            byte[] contents = Files.readAllBytes(file.toPath());
            int numberOfChunks = contents.length / CHUNK_SIZE + 1;

            for (int chunk = 0; chunk < numberOfChunks; chunk++)
                chunks.add(Arrays.copyOfRange(contents, chunk * CHUNK_SIZE, (chunk + 1) * CHUNK_SIZE));

        } catch (IOException e) {
            System.err.println("Can't read file with exception " + e);
        }
        chunkedFileEntity.setChunkSize(CHUNK_SIZE);
        chunkedFileEntity.setChunks(chunks);
        return Optional.of(chunkedFileEntity);

    }
}
