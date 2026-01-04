package araslanov.ruslan.cloudserver.repository;

import araslanov.ruslan.cloudserver.entity.User;
import araslanov.ruslan.cloudserver.entity.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserFileRepository extends JpaRepository<UserFile, Long> {
    List<UserFile> findByUserOrderByUploadDateDesc(User user);
    Optional<UserFile> findByUserAndFilename(User user, String filename);
    boolean existsByUserAndFilename(User user, String filename);
    void deleteByUserAndFilename(User user, String filename);
}