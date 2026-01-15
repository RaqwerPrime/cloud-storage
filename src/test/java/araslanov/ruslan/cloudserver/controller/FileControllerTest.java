package araslanov.ruslan.cloudserver.controller;

import araslanov.ruslan.cloudserver.dto.FileInfoResponse;
import araslanov.ruslan.cloudserver.dto.FileRenameRequest;
import araslanov.ruslan.cloudserver.entity.User;
import araslanov.ruslan.cloudserver.entity.UserFile;
import araslanov.ruslan.cloudserver.service.FileStorageService;
import araslanov.ruslan.cloudserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private UserService userService;

    @Mock
    private UserDetails userDetails;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileController fileController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password");
        testUser.setId(1L);
    }

    @Test
    void uploadFileReturnsOkWhenSuccessful() throws IOException {
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userService.findByLogin("testuser")).thenReturn(testUser);

        doNothing().when(fileStorageService).uploadFile(any(User.class), anyString(), any(MultipartFile.class));

        ResponseEntity<Void> response = fileController.uploadFile(userDetails, "test.txt", multipartFile);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(fileStorageService).uploadFile(testUser, "test.txt", multipartFile);
    }

    @Test
    void deleteFileReturnsOkWhenSuccessful() throws IOException {
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userService.findByLogin("testuser")).thenReturn(testUser);

        doNothing().when(fileStorageService).deleteFile(any(User.class), anyString());

        ResponseEntity<Void> response = fileController.deleteFile(userDetails, "test.txt");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(fileStorageService).deleteFile(testUser, "test.txt");
    }

    @Test
    void renameFileReturnsOkWhenSuccessful() {
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userService.findByLogin("testuser")).thenReturn(testUser);

        FileRenameRequest renameRequest = new FileRenameRequest();
        renameRequest.setName("newfile.txt");

        doNothing().when(fileStorageService).renameFile(any(User.class), anyString(), anyString());

        ResponseEntity<Void> response = fileController.renameFile(userDetails, "oldfile.txt", renameRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(fileStorageService).renameFile(testUser, "oldfile.txt", "newfile.txt");
    }

    @Test
    void getFileListReturnsFilesWithLimit() {
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userService.findByLogin("testuser")).thenReturn(testUser);

        List<UserFile> userFiles = Arrays.asList(
                new UserFile(testUser, "file1.txt", "/path/file1.txt", 100L),
                new UserFile(testUser, "file2.txt", "/path/file2.txt", 200L)
        );

        when(fileStorageService.getUserFiles(testUser, 10)).thenReturn(userFiles);

        ResponseEntity<List<FileInfoResponse>> response = fileController.getFileList(userDetails, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("file1.txt", response.getBody().get(0).getFilename());
        assertEquals(100L, response.getBody().get(0).getSize());

        verify(fileStorageService).getUserFiles(testUser, 10);
    }

    @Test
    void getFileListReturnsAllFilesWhenLimitNotSpecified() {
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userService.findByLogin("testuser")).thenReturn(testUser);

        List<UserFile> userFiles = Arrays.asList(
                new UserFile(testUser, "file1.txt", "/path/file1.txt", 100L)
        );

        when(fileStorageService.getUserFiles(testUser, null)).thenReturn(userFiles);

        ResponseEntity<List<FileInfoResponse>> response = fileController.getFileList(userDetails, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }
}