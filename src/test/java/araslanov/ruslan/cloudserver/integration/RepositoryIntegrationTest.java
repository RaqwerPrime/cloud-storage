package araslanov.ruslan.cloudserver.integration;

import araslanov.ruslan.cloudserver.entity.User;
import araslanov.ruslan.cloudserver.entity.UserFile;
import araslanov.ruslan.cloudserver.repository.UserFileRepository;
import araslanov.ruslan.cloudserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class RepositoryIntegrationTest {

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
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFileRepository userFileRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userFileRepository.deleteAll();

        testUser = new User("repouser", "password");
        testUser = userRepository.save(testUser);
    }

    @Test
    void userRepositorySavesAndFindsUser() {
        Optional<User> foundUser = userRepository.findByLogin("repouser");

        assertTrue(foundUser.isPresent());
        assertEquals("repouser", foundUser.get().getLogin());
        assertEquals(testUser.getId(), foundUser.get().getId());
    }

    @Test
    void userFileRepositorySavesAndFindsFiles() {
        UserFile file1 = new UserFile(testUser, "file1.txt", "/path/file1.txt", 100L);
        UserFile file2 = new UserFile(testUser, "file2.txt", "/path/file2.txt", 200L);

        userFileRepository.save(file1);
        userFileRepository.save(file2);

        List<UserFile> userFiles = userFileRepository.findByUserOrderByUploadDateDesc(testUser);

        assertEquals(2, userFiles.size());
        assertEquals("file1.txt", userFiles.get(0).getFilename());
        assertEquals("file2.txt", userFiles.get(1).getFilename());
    }

    @Test
    void userFileRepositoryFindsFileByUserAndFilename() {
        UserFile file = new UserFile(testUser, "specific.txt", "/path/specific.txt", 300L);
        userFileRepository.save(file);

        Optional<UserFile> foundFile = userFileRepository.findByUserAndFilename(testUser, "specific.txt");

        assertTrue(foundFile.isPresent());
        assertEquals("specific.txt", foundFile.get().getFilename());
        assertEquals(testUser.getId(), foundFile.get().getUser().getId());
    }

    @Test
    void userFileRepositoryChecksExistence() {
        UserFile file = new UserFile(testUser, "existing.txt", "/path/existing.txt", 400L);
        userFileRepository.save(file);

        boolean exists = userFileRepository.existsByUserAndFilename(testUser, "existing.txt");
        boolean notExists = userFileRepository.existsByUserAndFilename(testUser, "nonexistent.txt");

        assertTrue(exists);
        assertFalse(notExists);
    }

}