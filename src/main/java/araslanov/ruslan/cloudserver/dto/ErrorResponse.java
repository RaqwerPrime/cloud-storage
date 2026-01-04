package araslanov.ruslan.cloudserver.dto;

public class ErrorResponse {
    private String message;
    private Integer id;

    // Конструкторы
    public ErrorResponse() {}

    public ErrorResponse(String message, Integer id) {
        this.message = message;
        this.id = id;
    }

    // Геттеры и сеттеры
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}