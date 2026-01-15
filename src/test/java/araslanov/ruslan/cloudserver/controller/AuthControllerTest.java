// src/test/java/araslanov/ruslan/cloudserver/controller/AuthControllerTest.java
package araslanov.ruslan.cloudserver.controller;

import araslanov.ruslan.cloudserver.dto.LoginRequest;
import araslanov.ruslan.cloudserver.dto.LoginResponse;
import araslanov.ruslan.cloudserver.dto.RegisterRequest;
import araslanov.ruslan.cloudserver.entity.User;
import araslanov.ruslan.cloudserver.security.JwtUtil;
import araslanov.ruslan.cloudserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setLogin("testuser");
        loginRequest.setPassword("password123");

        registerRequest = new RegisterRequest();
        registerRequest.setLogin("newuser");
        registerRequest.setPassword("password123");

        testUser = new User("newuser", "encodedPassword");
        testUser.setId(1L);
    }

    @Test
    void loginReturnsTokenWhenCredentialsAreValid() {
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                "testuser", "password", Collections.emptyList());

        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.generateToken("testuser")).thenReturn("test-jwt-token");

        ResponseEntity<LoginResponse> response = authController.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("test-jwt-token", response.getBody().getAuthToken());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(jwtUtil).generateToken("testuser");
    }

    @Test
    void registerReturnsCreatedStatus() {
        when(userService.registerUser(any(RegisterRequest.class))).thenReturn(testUser);

        ResponseEntity<Void> response = authController.register(registerRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(userService).registerUser(registerRequest);
    }

    @Test
    void logoutReturnsOkStatus() {
        ResponseEntity<Void> response = authController.logout("test-token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}