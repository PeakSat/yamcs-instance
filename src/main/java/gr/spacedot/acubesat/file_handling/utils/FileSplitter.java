package gr.spacedot.acubesat.file_handling.utils;

import gr.spacedot.acubesat.file_handling.entities.ChunkedFileEntity;
import gr.spacedot.acubesat.file_handling.entities.FileEntity;

import java.util.ArrayList;
import java.util.Arrays;

public class FileSplitter {
    private static final int CHUNK_SIZE_BYTES = 4000; //max value is 4032

    public ChunkedFileEntity splitFileInChunks(FileEntity fileEntity) {
        ChunkedFileEntity chunkedFileEntity = new ChunkedFileEntity();
        chunkedFileEntity.setName(fileEntity.getName());
        chunkedFileEntity.setPath(fileEntity.getPath());

        ArrayList<byte[]> chunks = new ArrayList<>();
        byte[] contents = fileEntity.getContents();
        int numberOfChunks = contents.length / CHUNK_SIZE_BYTES + 1;

        int chunkSize = Math.min(contents.length, CHUNK_SIZE_BYTES);

        for (int chunk = 0; chunk < numberOfChunks - 1; chunk++)
            chunks.add(Arrays.copyOfRange(contents, chunk * chunkSize, (chunk + 1) * chunkSize));
        // add last chunk manually to prevent zero padding
        chunks.add(Arrays.copyOfRange(contents, (numberOfChunks - 1) * chunkSize, contents.length));

        chunkedFileEntity.setChunkSize(chunkSize);
        chunkedFileEntity.setChunks(chunks);
        return chunkedFileEntity;

    }
}
