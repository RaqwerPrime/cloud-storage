package araslanov.ruslan.cloudserver.controller;

import araslanov.ruslan.cloudserver.dto.FileInfoResponse;
import araslanov.ruslan.cloudserver.dto.FileRenameRequest;
import araslanov.ruslan.cloudserver.entity.User;
import araslanov.ruslan.cloudserver.service.FileStorageService;
import araslanov.ruslan.cloudserver.service.UserService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class FileController {

    private final FileStorageService fileStorageService;
    private final UserService userService;

    public FileController(FileStorageService fileStorageService,
                          UserService userService) {
        this.fileStorageService = fileStorageService;
        this.userService = userService;
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userService.findByLogin(userDetails.getUsername());
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String filename,
            @RequestPart("file") MultipartFile file) throws IOException {

        User user = getCurrentUser(userDetails);
        fileStorageService.uploadFile(user, filename, file);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String filename) throws IOException {

        User user = getCurrentUser(userDetails);
        Path filePath = fileStorageService.downloadFile(user, filename);

        Resource resource = new UrlResource(filePath.toUri());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(filePath))
                .body(resource);
    }

    @DeleteMapping("/file")
    public ResponseEntity<Void> deleteFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String filename) throws IOException {

        User user = getCurrentUser(userDetails);
        fileStorageService.deleteFile(user, filename);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/file")
    public ResponseEntity<Void> renameFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String filename,
            @Valid @RequestBody FileRenameRequest request) {

        User user = getCurrentUser(userDetails);
        fileStorageService.renameFile(user, filename, request.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileInfoResponse>> getFileList(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer limit) {

        User user = getCurrentUser(userDetails);
        List<FileInfoResponse> files = fileStorageService.getUserFiles(user, limit)
                .stream()
                .map(file -> new FileInfoResponse(file.getFilename(), file.getSize()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(files);
    }
}