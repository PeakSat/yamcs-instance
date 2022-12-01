package gr.spacedot.acubesat.file_handling.network.utils;

import com.google.common.primitives.Bytes;
import gr.spacedot.acubesat.file_handling.entities.ChunkedFileEntity;
import gr.spacedot.acubesat.file_handling.entities.FileEntity;
import gr.spacedot.acubesat.file_handling.enums.LocalPaths;

import java.io.File;

public class FileReconstructor {

    public void createIfNotExists(String directory){
        File folder = new File(directory);
        if(!folder.exists()){
            folder.mkdir();
        }
    }

    public FileEntity reconstruct(ChunkedFileEntity chunkedFileEntity) {

        createIfNotExists(LocalPaths.RECEIVED_PATH.toString());
        File folder = new File(LocalPaths.RECEIVED_PATH.toString(), chunkedFileEntity.getName());

        FileEntity reconstructed = new FileEntity();
        reconstructed.setName(chunkedFileEntity.getName());
        reconstructed.setPath(folder.getPath());

        byte[] contents = new byte[]{};
        for (byte[] chunk : chunkedFileEntity.getChunks()) {
            contents = Bytes.concat(contents, chunk);
        }
        reconstructed.setContents(contents);
        reconstructed.save(folder.getPath());

        return reconstructed;
    }


}
