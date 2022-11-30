package gr.spacedot.acubesat.file_handling.entities;

import java.io.FileOutputStream;
import java.io.IOException;

public class FileEntity {
    private String path;

    private String name;

    private byte[] contents;



    public FileEntity(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public FileEntity() {
    }

    public byte[] getContents() {
        return contents;
    }

    public void setContents(byte[] contents) {
        this.contents = contents;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void save(String location){
        try(FileOutputStream fileOutputStream = new FileOutputStream(location)){
            if(this.contents!=null)
                fileOutputStream.write(this.contents);
        }catch (IOException e){
            System.err.println("Error writing to file: "+e);
        }

    }

}
