package gr.spacedot.acubesat.file_handling.utils;

import gr.spacedot.acubesat.file_handling.entities.ChunkedFileEntity;
import gr.spacedot.acubesat.file_handling.entities.FileEntity;

import java.util.ArrayList;
import java.util.Arrays;

public class FileSplitter {
    private static final int CHUNK_SIZE = 60_000;

    public ChunkedFileEntity splitFileInChunks(FileEntity fileEntity) {
        ChunkedFileEntity chunkedFileEntity = new ChunkedFileEntity();
        chunkedFileEntity.setName(fileEntity.getName());
        chunkedFileEntity.setPath(fileEntity.getPath());

        ArrayList<byte[]> chunks = new ArrayList<>();
        byte[] contents = fileEntity.getContents();
        int numberOfChunks = contents.length / CHUNK_SIZE + 1;

        for (int chunk = 0; chunk < numberOfChunks; chunk++)
            chunks.add(Arrays.copyOfRange(contents, chunk * CHUNK_SIZE, (chunk + 1) * CHUNK_SIZE));

        chunkedFileEntity.setChunkSize(CHUNK_SIZE);
        chunkedFileEntity.setChunks(chunks);
        return chunkedFileEntity;

    }
}
