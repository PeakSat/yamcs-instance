package gr.spacedot.acubesat.file_handling;

public class FileEntity {
    private String path;

    private String name;

    public FileEntity(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public FileEntity(){}

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
}
