package araslanov.ruslan.cloudserver.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTests {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        userDetails = new User("testuser", "password", Collections.emptyList());

        ReflectionTestUtils.setField(jwtUtil, "secret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    @Test
    void generateTokenReturnsValidJwt() {
        String token = jwtUtil.generateToken("testuser");

        assertNotNull(token, "Токен не должен быть null");
        assertFalse(token.isEmpty(), "Токен не должен быть пустым");
        assertEquals(3, token.split("\\.").length, "JWT токен должен содержать 3 части");
    }

    @Test
    void extractUsernameReturnsCorrectUsername() {
        String token = jwtUtil.generateToken("testuser");
        String extractedUsername = jwtUtil.extractUsername(token);

        assertEquals("testuser", extractedUsername, "Извлеченное имя пользователя должно соответствовать исходному");
    }

    @Test
    void validateTokenReturnsTrueForValidToken() {
        String token = jwtUtil.generateToken("testuser");
        boolean isValid = jwtUtil.validateToken(token, userDetails);

        assertTrue(isValid, "Валидный токен должен проходить проверку");
    }

    @Test
    void validateTokenReturnsFalseForDifferentUser() {
        String token = jwtUtil.generateToken("testuser");
        UserDetails differentUser = new User("otheruser", "password", Collections.emptyList());
        boolean isValid = jwtUtil.validateToken(token, differentUser);

        assertFalse(isValid, "Токен не должен быть валидным для другого пользователя");
    }

    @Test
    void extractExpirationReturnsFutureDate() {
        String token = jwtUtil.generateToken("testuser");
        java.util.Date expiration = jwtUtil.extractExpiration(token);

        assertTrue(expiration.after(new java.util.Date()),
                "Дата истечения токена должна быть в будущем");
    }
}