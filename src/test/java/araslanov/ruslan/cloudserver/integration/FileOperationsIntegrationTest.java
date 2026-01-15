// src/test/java/araslanov/ruslan/cloudserver/integration/FileOperationsIntegrationTest.java
package araslanov.ruslan.cloudserver.integration;

import araslanov.ruslan.cloudserver.dto.FileRenameRequest;
import araslanov.ruslan.cloudserver.dto.LoginRequest;
import araslanov.ruslan.cloudserver.dto.LoginResponse;
import araslanov.ruslan.cloudserver.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FileOperationsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        try {
            File tempDir = File.createTempFile("file-ops-test", "");
            tempDir.delete();
            tempDir.mkdir();
            registry.add("cloud.storage.path", tempDir::getAbsolutePath);
        } catch (IOException e) {
            registry.add("cloud.storage.path", () -> "./test-storage");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;
    private String uniqueUsername;

    @BeforeEach
    void setUp() throws Exception {
        // Генерируем уникальное имя пользователя для каждого теста
        uniqueUsername = "fileuser_" + UUID.randomUUID().toString().substring(0, 8);

        // Регистрация пользователя
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setLogin(uniqueUsername);
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Логин для получения токена
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLogin(uniqueUsername);
        loginRequest.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(responseContent, LoginResponse.class);
        authToken = loginResponse.getAuthToken();
    }

    @Test
    void uploadAndListFilesWorksCorrectly() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "testfile.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );

        // Загрузка файла
        mockMvc.perform(multipart("/file")
                        .file(file)
                        .param("filename", "testfile.txt")
                        .header("auth-token", "Bearer " + authToken))
                .andExpect(status().isOk());

        // Получение списка файлов
        mockMvc.perform(get("/list")
                        .param("limit", "10")
                        .header("auth-token", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].filename").value("testfile.txt"))
                .andExpect(jsonPath("$[0].size").value(13L));
    }

    @Test
    void uploadRenameAndDeleteFileFlow() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "original.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Test content".getBytes()
        );

        // 1. Загрузка файла
        mockMvc.perform(multipart("/file")
                        .file(file)
                        .param("filename", "original.txt")
                        .header("auth-token", "Bearer " + authToken))
                .andExpect(status().isOk());

        // 2. Переименование файла
        FileRenameRequest renameRequest = new FileRenameRequest();
        renameRequest.setName("renamed.txt");

        mockMvc.perform(put("/file")
                        .param("filename", "original.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renameRequest))
                        .header("auth-token", "Bearer " + authToken))
                .andExpect(status().isOk());

        // 3. Проверка, что файл переименован в списке
        mockMvc.perform(get("/list")
                        .header("auth-token", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].filename").value("renamed.txt"));

        // 4. Удаление файла
        mockMvc.perform(delete("/file")
                        .param("filename", "renamed.txt")
                        .header("auth-token", "Bearer " + authToken))
                .andExpect(status().isOk());

        // 5. Проверка, что файла больше нет
        mockMvc.perform(get("/list")
                        .header("auth-token", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}