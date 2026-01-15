package araslanov.ruslan.cloudserver.repository;

import araslanov.ruslan.cloudserver.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByLoginReturnsUserWhenExists() {
        User user = new User("testuser", "password");
        entityManager.persist(user);
        entityManager.flush();

        Optional<User> found = userRepository.findByLogin("testuser");

        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getLogin());
    }

    @Test
    void findByLoginReturnsEmptyWhenUserNotFound() {
        Optional<User> found = userRepository.findByLogin("nonexistent");

        assertFalse(found.isPresent());
    }

    @Test
    void existsByLoginReturnsTrueWhenUserExists() {
        User user = new User("existinguser", "password");
        entityManager.persist(user);
        entityManager.flush();

        boolean exists = userRepository.existsByLogin("existinguser");

        assertTrue(exists);
    }

    @Test
    void existsByLoginReturnsFalseWhenUserDoesNotExist() {
        boolean exists = userRepository.existsByLogin("nonexistent");

        assertFalse(exists);
    }
}
