package araslanov.ruslan.cloudserver.service;

import araslanov.ruslan.cloudserver.entity.User;
import araslanov.ruslan.cloudserver.entity.UserFile;
import araslanov.ruslan.cloudserver.repository.UserFileRepository;
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
public class FileStorageService {

    private String storagePath;

    private final UserFileRepository fileRepository;

    public FileStorageService(
            @Value("${cloud.storage.path}") String storagePath,
            UserFileRepository fileRepository) {
        this.storagePath = storagePath;
        this.fileRepository = fileRepository;
        createStorageDirectory();
    }

    private void createStorageDirectory() {
        try {
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Storage directory created: " + storagePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    public void uploadFile(User user, String filename, MultipartFile file) throws IOException {
        if (fileRepository.existsByUserAndFilename(user, filename)) {
            throw new IllegalArgumentException("File already exists: " + filename);
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String uniqueFilename = UUID.randomUUID().toString();
        Path targetLocation = Paths.get(storagePath).resolve(uniqueFilename);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        UserFile userFile = new UserFile();
        userFile.setUser(user);
        userFile.setFilename(filename);
        userFile.setFilePath(targetLocation.toString());
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
    }

    public void renameFile(User user, String oldFilename, String newFilename) {
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
}