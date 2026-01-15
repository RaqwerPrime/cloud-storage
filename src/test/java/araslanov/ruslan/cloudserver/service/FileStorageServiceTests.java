package araslanov.ruslan.cloudserver.service;

import araslanov.ruslan.cloudserver.entity.User;
import araslanov.ruslan.cloudserver.entity.UserFile;
import araslanov.ruslan.cloudserver.repository.UserFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTests {

    @Mock
    private UserFileRepository fileRepository;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService("./test-storage", fileRepository);
    }

    @Test
    void getUserFilesReturnsLimitedResultsWhenLimitSpecified() {
        User user = createTestUser();

        List<UserFile> allFiles = Arrays.asList(
                createUserFile(user, "file1.txt", 100L),
                createUserFile(user, "file2.txt", 200L),
                createUserFile(user, "file3.txt", 300L)
        );

        when(fileRepository.findByUserOrderByUploadDateDesc(user)).thenReturn(allFiles);

        List<UserFile> result = fileStorageService.getUserFiles(user, 2);

        assertEquals(2, result.size());
        assertEquals("file1.txt", result.get(0).getFilename());
        assertEquals("file2.txt", result.get(1).getFilename());

        verify(fileRepository, times(1)).findByUserOrderByUploadDateDesc(user);
    }

    @Test
    void getUserFilesReturnsAllResultsWhenLimitIsNull() {
        User user = createTestUser();

        List<UserFile> expectedFiles = Arrays.asList(
                createUserFile(user, "file1.txt", 100L),
                createUserFile(user, "file2.txt", 200L)
        );

        when(fileRepository.findByUserOrderByUploadDateDesc(user)).thenReturn(expectedFiles);

        List<UserFile> result = fileStorageService.getUserFiles(user, null);

        assertEquals(2, result.size());
        assertIterableEquals(expectedFiles, result);
    }

    @Test
    void getUserFilesReturnsEmptyListWhenNoFilesExist() {
        User user = createTestUser();

        when(fileRepository.findByUserOrderByUploadDateDesc(user)).thenReturn(List.of());

        List<UserFile> result = fileStorageService.getUserFiles(user, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUserFilesReturnsAllFilesWhenLimitGreaterThanFileCount() {
        User user = createTestUser();

        List<UserFile> expectedFiles = Arrays.asList(
                createUserFile(user, "file1.txt", 100L),
                createUserFile(user, "file2.txt", 200L)
        );

        when(fileRepository.findByUserOrderByUploadDateDesc(user)).thenReturn(expectedFiles);

        List<UserFile> result = fileStorageService.getUserFiles(user, 5);

        assertEquals(2, result.size());
        assertIterableEquals(expectedFiles, result);
    }

    @Test
    void getUserFilesReturnsAllFilesWhenLimitIsZero() {
        User user = createTestUser();

        List<UserFile> expectedFiles = Arrays.asList(
                createUserFile(user, "file1.txt", 100L),
                createUserFile(user, "file2.txt", 200L)
        );

        when(fileRepository.findByUserOrderByUploadDateDesc(user)).thenReturn(expectedFiles);

        List<UserFile> result = fileStorageService.getUserFiles(user, 0);

        assertEquals(2, result.size());
        assertIterableEquals(expectedFiles, result);
    }

    private User createTestUser() {
        User user = new User("testuser", "password");
        user.setId(1L);
        return user;
    }

    private UserFile createUserFile(User user, String filename, Long size) {
        UserFile file = new UserFile(user, filename, "/path/" + filename, size);
        file.setId(1L);
        return file;
    }
}