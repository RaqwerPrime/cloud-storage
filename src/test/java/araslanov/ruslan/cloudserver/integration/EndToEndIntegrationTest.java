package araslanov.ruslan.cloudserver.integration;

import araslanov.ruslan.cloudserver.dto.LoginRequest;
import araslanov.ruslan.cloudserver.dto.LoginResponse;
import araslanov.ruslan.cloudserver.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
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
import java.nio.file.Files;
import java.nio.file.Path;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EndToEndIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private static String tempStoragePath;

    @BeforeAll
    static void setupTempStorage() throws Exception {
        Path tempDir = Files.createTempDirectory("cloud-storage-test");
        tempStoragePath = tempDir.toAbsolutePath().toString();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("cloud.storage.path", () -> tempStoragePath);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void completeUserFileManagementFlow() throws Exception {
        // 1. Регистрация
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setLogin("e2euser");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // 2. Логин
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLogin("e2euser");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String authToken = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                LoginResponse.class
        ).getAuthToken();

        // 3. Загрузка нескольких файлов
        for (int i = 1; i <= 3; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "file" + i + ".txt",
                    MediaType.TEXT_PLAIN_VALUE,
                    ("Content of file " + i).getBytes()
            );

            mockMvc.perform(multipart("/file")
                            .file(file)
                            .param("filename", "file" + i + ".txt")
                            .header("auth-token", "Bearer " + authToken))
                    .andExpect(status().isOk());
        }

        // 4. Получение списка файлов с лимитом 2
        mockMvc.perform(get("/list")
                        .param("limit", "2")
                        .header("auth-token", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // 5. Получение полного списка файлов
        mockMvc.perform(get("/list")
                        .header("auth-token", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}