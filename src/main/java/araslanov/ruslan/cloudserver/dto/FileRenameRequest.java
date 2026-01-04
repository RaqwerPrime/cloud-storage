package araslanov.ruslan.cloudserver.dto;

import jakarta.validation.constraints.NotBlank;

public class FileRenameRequest {
    @NotBlank(message = "New filename is required")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}