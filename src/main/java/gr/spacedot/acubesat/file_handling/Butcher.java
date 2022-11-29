package gr.spacedot.acubesat.file_handling;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Butcher {
    private static final int CHUNK_SIZE = 60_000;

    public List<byte[]> splitFileInChunks(FileEntity fileEntity) {
        File file;
        ArrayList<byte[]> chunks = new ArrayList<>();
        try {

            file = new File(fileEntity.getPath() , fileEntity.getName());
            System.out.println("Path is "+file.getPath());
            byte[] contents = Files.readAllBytes(file.toPath());
            int numberOfChunks = contents.length / CHUNK_SIZE + 1;

            for(int chunk = 0; chunk < numberOfChunks; chunk++ )
                chunks.add(Arrays.copyOfRange(contents,chunk*CHUNK_SIZE, (chunk+1)*CHUNK_SIZE));
        } catch (IOException e){
            System.err.println("Can't read file with exception "+e);
        }
        return chunks;
    }
}
