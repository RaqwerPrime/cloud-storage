package araslanov.ruslan.cloudserver.service;

import araslanov.ruslan.cloudserver.entity.User;
import araslanov.ruslan.cloudserver.entity.UserFile;
import araslanov.ruslan.cloudserver.repository.UserFileRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FileStorageService {

    private final String baseStoragePath;
    private final UserFileRepository fileRepository;

    public FileStorageService(
            @Value("${cloud.storage.path}") String baseStoragePath,
            UserFileRepository fileRepository) {
        this.baseStoragePath = baseStoragePath;
        this.fileRepository = fileRepository;
        createStorageDirectory();
    }

    private void createStorageDirectory() {
        try {
            Path path = Paths.get(baseStoragePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Base storage directory created: " + baseStoragePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    private Path getUserStoragePath(User user) {
        // Используем userId для создания уникальной папки
        String userFolderName = "user_" + user.getId();
        return Paths.get(baseStoragePath).resolve(userFolderName);
    }

    private void createUserDirectoryIfNotExists(User user) throws IOException {
        Path userPath = getUserStoragePath(user);
        if (!Files.exists(userPath)) {
            Files.createDirectories(userPath);
        }
    }

    public void uploadFile(User user, String filename, MultipartFile file) throws IOException {
        validateFilename(filename);

        if (fileRepository.existsByUserAndFilename(user, filename)) {
            throw new IllegalArgumentException("File already exists: " + filename);
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        createUserDirectoryIfNotExists(user);

        String uniqueFilename = UUID.randomUUID().toString();
        Path userStoragePath = getUserStoragePath(user);
        Path targetLocation = userStoragePath.resolve(uniqueFilename);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        UserFile userFile = new UserFile();
        userFile.setUser(user);
        userFile.setFilename(filename); // Оригинальное имя
        userFile.setFilePath(targetLocation.toString()); // Полный путь на диске
        userFile.setSize(file.getSize());

        fileRepository.save(userFile);
    }

    public Path downloadFile(User user, String filename) throws IOException {
        UserFile userFile = fileRepository.findByUserAndFilename(user, filename)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + filename));

        Path filePath = Paths.get(userFile.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IOException("File not found on disk: " + filename);
        }

        return filePath;
    }

    public void deleteFile(User user, String filename) throws IOException {
        UserFile userFile = fileRepository.findByUserAndFilename(user, filename)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + filename));

        Path filePath = Paths.get(userFile.getFilePath());
        Files.deleteIfExists(filePath);

        fileRepository.delete(userFile);

        cleanupUserDirectory(user);
    }

    public void renameFile(User user, String oldFilename, String newFilename) {
        validateFilename(newFilename);

        UserFile userFile = fileRepository.findByUserAndFilename(user, oldFilename)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + oldFilename));

        if (!oldFilename.equals(newFilename) &&
                fileRepository.existsByUserAndFilename(user, newFilename)) {
            throw new IllegalArgumentException("File with new name already exists: " + newFilename);
        }

        userFile.setFilename(newFilename);
        fileRepository.save(userFile);
    }

    public List<UserFile> getUserFiles(User user, Integer limit) {
        List<UserFile> files = fileRepository.findByUserOrderByUploadDateDesc(user);

        if (limit != null && limit > 0 && limit < files.size()) {
            return files.subList(0, limit);
        }

        return files;
    }

    private void validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename: " + filename);
        }

        if (filename.length() > 255) {
            throw new IllegalArgumentException("Filename too long");
        }
    }

    private void cleanupUserDirectory(User user) {
        try {
            Path userPath = getUserStoragePath(user);
            if (Files.exists(userPath)) {
                try (var files = Files.list(userPath)) {
                    if (files.findAny().isEmpty()) {
                        Files.deleteIfExists(userPath);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not cleanup user directory: " + e.getMessage());
        }
    }

}