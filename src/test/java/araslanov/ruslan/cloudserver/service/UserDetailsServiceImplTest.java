package araslanov.ruslan.cloudserver.service;

import araslanov.ruslan.cloudserver.entity.User;
import araslanov.ruslan.cloudserver.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsernameReturnsUserDetailsWhenUserExists() {
        User user = new User("testuser", "encodedPassword");
        user.setId(1L);

        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().isEmpty());
        assertTrue(userDetails.isEnabled());

        verify(userRepository).findByLogin("testuser");
    }

    @Test
    void loadUserByUsernameThrowsExceptionWhenUserNotFound() {
        when(userRepository.findByLogin("nonexistent")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("nonexistent")
        );

        assertTrue(exception.getMessage().contains("User not found: nonexistent"));
        verify(userRepository).findByLogin("nonexistent");
    }
}