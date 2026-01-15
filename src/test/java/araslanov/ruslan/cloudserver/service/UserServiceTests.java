package araslanov.ruslan.cloudserver.service;

import araslanov.ruslan.cloudserver.dto.RegisterRequest;
import araslanov.ruslan.cloudserver.entity.User;
import araslanov.ruslan.cloudserver.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUserReturnsUserWhenSuccessful() {
        RegisterRequest request = new RegisterRequest();
        request.setLogin("newuser");
        request.setPassword("password123");

        User savedUser = new User("newuser", "encodedPassword");
        savedUser.setId(1L);

        when(userRepository.existsByLogin("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.registerUser(request);

        assertNotNull(result);
        assertEquals("newuser", result.getLogin());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(1L, result.getId());

        verify(userRepository).existsByLogin("newuser");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUserThrowsExceptionForDuplicateLogin() {
        RegisterRequest request = new RegisterRequest();
        request.setLogin("existinguser");
        request.setPassword("password123");

        when(userRepository.existsByLogin("existinguser")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(request)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findByLoginReturnsUserWhenExists() {
        User expectedUser = new User("testuser", "password");
        expectedUser.setId(1L);

        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(expectedUser));

        User result = userService.findByLogin("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getLogin());
        assertEquals(1L, result.getId());
    }

    @Test
    void findByLoginThrowsExceptionWhenNotFound() {
        when(userRepository.findByLogin("nonexistent")).thenReturn(Optional.empty());

        org.springframework.security.core.userdetails.UsernameNotFoundException exception =
                assertThrows(
                        org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                        () -> userService.findByLogin("nonexistent")
                );

        assertTrue(exception.getMessage().contains("User not found"));
    }
}