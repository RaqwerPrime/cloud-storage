package araslanov.ruslan.cloudserver.dto;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FileRenameRequest {

    @NotBlank(message = "New filename is required")
    @JsonAlias({"name", "filename", "newName"})
    private String newFilename;

    public FileRenameRequest() {}

    public FileRenameRequest(String newFilename) {
        this.newFilename = newFilename;
    }

    @JsonProperty("name")
    public String getName() {
        return newFilename;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.newFilename = name;
    }

    @JsonProperty("filename")
    public String getFilename() {
        return newFilename;
    }

    @JsonProperty("filename")
    public void setFilename(String filename) {
        this.newFilename = filename;
    }

    public String getNewFilename() {
        return newFilename;
    }

    public void setNewFilename(String newFilename) {
        this.newFilename = newFilename;
    }
}