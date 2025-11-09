package com.udea.innosistemas.service;

import com.udea.innosistemas.dto.AuthResponse;
import com.udea.innosistemas.dto.LoginRequest;
import com.udea.innosistemas.dto.LogoutResponse;
import com.udea.innosistemas.entity.User;
import com.udea.innosistemas.entity.UserRole;
import com.udea.innosistemas.exception.AuthenticationException;
import com.udea.innosistemas.repository.UserRepository;
import com.udea.innosistemas.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitario para AuthenticationService utilizando el patrón AAA (Arrange-Act-Assert).
 * Verifica la lógica de autenticación, generación de tokens, logout y refresh tokens.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService - Test unitario con patrón AAA")
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private SessionManagementService sessionManagementService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private LoginRequest loginRequest;
    private String testToken;
    private String testRefreshToken;

    @BeforeEach
    void setUp() {
        // Arrange - Configuración común para todos los tests
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("estudiante@udea.edu.co");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setRole(UserRole.STUDENT);
        testUser.setFirstName("Juan");
        testUser.setLastName("Pérez");
        testUser.setTeamId(1L);
        testUser.setCourseId(1L);
        testUser.setEnabled(true);
        testUser.setAccountNonExpired(true);
        testUser.setAccountNonLocked(true);
        testUser.setCredentialsNonExpired(true);
        testUser.setCreatedAt(LocalDateTime.now());

        loginRequest = new LoginRequest("estudiante@udea.edu.co", "password123");
        testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        testRefreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.token";
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Login exitoso - Debe retornar AuthResponse con tokens válidos")
    void login_WhenValidCredentials_ShouldReturnAuthResponse() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn(testToken);
        when(tokenProvider.generateRefreshToken(authentication)).thenReturn(testRefreshToken);
        when(sessionManagementService.registerSession(anyString(), anyString())).thenReturn(true);

        // Act
        AuthResponse response = authenticationService.login(loginRequest);

        // Assert
        assertNotNull(response, "La respuesta de autenticación no debe ser null");
        assertEquals(testToken, response.getToken(), "El token debe coincidir");
        assertEquals(testRefreshToken, response.getRefreshToken(), "El refresh token debe coincidir");
        assertNotNull(response.getUserInfo(), "UserInfo no debe ser null");
        assertEquals(testUser.getEmail(), response.getUserInfo().getEmail(), "El email debe coincidir");
        assertEquals(testUser.getRole(), response.getUserInfo().getRole(), "El role debe coincidir");
        assertEquals(testUser.getId(), response.getUserInfo().getId(), "El ID debe coincidir");

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).generateToken(authentication);
        verify(tokenProvider).generateRefreshToken(authentication);
        verify(sessionManagementService).registerSession(eq(testUser.getEmail()), anyString());
    }

    @Test
    @DisplayName("Login con credenciales inválidas - Debe lanzar AuthenticationException")
    void login_WhenInvalidCredentials_ShouldThrowAuthenticationException() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authenticationService.login(loginRequest);
        });

        assertEquals("Credenciales inválidas", exception.getMessage());
        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider, never()).generateToken(any());
        verify(sessionManagementService, never()).registerSession(anyString(), anyString());
    }

    @Test
    @DisplayName("Login con usuario no encontrado - Debe lanzar AuthenticationException")
    void login_WhenUserNotFound_ShouldThrowAuthenticationException() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authenticationService.login(loginRequest);
        });

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Login con excepción inesperada - Debe lanzar AuthenticationException genérica")
    void login_WhenUnexpectedException_ShouldThrowAuthenticationException() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authenticationService.login(loginRequest);
        });

        assertEquals("Error durante la autenticación", exception.getMessage());
        verify(userRepository).findByEmail(loginRequest.getEmail());
    }

    // ==================== VALIDATE CREDENTIALS TESTS ====================

    @Test
    @DisplayName("Validar credenciales correctas - Debe retornar true")
    void validateCredentials_WhenValidCredentials_ShouldReturnTrue() {
        // Arrange
        String email = "estudiante@udea.edu.co";
        String password = "password123";
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // Act
        boolean result = authenticationService.validateCredentials(email, password);

        // Assert
        assertTrue(result, "Las credenciales válidas deben retornar true");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Validar credenciales incorrectas - Debe retornar false")
    void validateCredentials_WhenInvalidCredentials_ShouldReturnFalse() {
        // Arrange
        String email = "estudiante@udea.edu.co";
        String password = "wrongPassword";
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act
        boolean result = authenticationService.validateCredentials(email, password);

        // Assert
        assertFalse(result, "Las credenciales inválidas deben retornar false");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    // ==================== REFRESH TOKEN TESTS ====================

    @Test
    @DisplayName("Refresh token exitoso - Debe retornar nuevos tokens")
    void refreshToken_WhenValidRefreshToken_ShouldReturnNewTokens() {
        // Arrange
        String newAccessToken = "new.access.token";
        String newRefreshToken = "new.refresh.token";
        Date expirationDate = new Date(System.currentTimeMillis() + 86400000);

        when(tokenBlacklistService.isTokenBlacklisted(testRefreshToken)).thenReturn(false);
        when(tokenProvider.validateToken(testRefreshToken)).thenReturn(true);
        when(tokenProvider.isRefreshToken(testRefreshToken)).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT(testRefreshToken)).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionManagementService.hasActiveSessions(testUser.getEmail())).thenReturn(true);
        when(tokenProvider.generateTokenFromUser(testUser)).thenReturn(newAccessToken);
        when(tokenProvider.generateRefreshTokenFromUser(testUser)).thenReturn(newRefreshToken);
        when(tokenProvider.getExpirationDateFromJWT(testRefreshToken)).thenReturn(expirationDate);
        doNothing().when(tokenBlacklistService).blacklistToken(testRefreshToken, expirationDate);

        // Act
        AuthResponse response = authenticationService.refreshToken(testRefreshToken);

        // Assert
        assertNotNull(response, "La respuesta no debe ser null");
        assertEquals(newAccessToken, response.getToken(), "El nuevo access token debe coincidir");
        assertEquals(newRefreshToken, response.getRefreshToken(), "El nuevo refresh token debe coincidir");
        assertNotNull(response.getUserInfo(), "UserInfo no debe ser null");
        assertEquals(testUser.getEmail(), response.getUserInfo().getEmail(), "El email debe coincidir");

        verify(tokenBlacklistService).isTokenBlacklisted(testRefreshToken);
        verify(tokenProvider).validateToken(testRefreshToken);
        verify(tokenProvider).isRefreshToken(testRefreshToken);
        verify(tokenProvider).getUsernameFromJWT(testRefreshToken);
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(sessionManagementService).hasActiveSessions(testUser.getEmail());
        verify(tokenProvider).generateTokenFromUser(testUser);
        verify(tokenProvider).generateRefreshTokenFromUser(testUser);
        verify(tokenBlacklistService).blacklistToken(testRefreshToken, expirationDate);
    }

    @Test
    @DisplayName("Refresh token en blacklist - Debe lanzar AuthenticationException")
    void refreshToken_WhenTokenBlacklisted_ShouldThrowAuthenticationException() {
        // Arrange
        when(tokenBlacklistService.isTokenBlacklisted(testRefreshToken)).thenReturn(true);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authenticationService.refreshToken(testRefreshToken);
        });

        assertEquals("Token inválido o revocado", exception.getMessage());
        verify(tokenBlacklistService).isTokenBlacklisted(testRefreshToken);
        verify(tokenProvider, never()).validateToken(any());
    }

    @Test
    @DisplayName("Refresh token inválido - Debe lanzar AuthenticationException")
    void refreshToken_WhenInvalidToken_ShouldThrowAuthenticationException() {
        // Arrange
        when(tokenBlacklistService.isTokenBlacklisted(testRefreshToken)).thenReturn(false);
        when(tokenProvider.validateToken(testRefreshToken)).thenReturn(false);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authenticationService.refreshToken(testRefreshToken);
        });

        assertEquals("Token inválido", exception.getMessage());
        verify(tokenBlacklistService).isTokenBlacklisted(testRefreshToken);
        verify(tokenProvider).validateToken(testRefreshToken);
    }

    @Test
    @DisplayName("Refresh token con tipo incorrecto - Debe lanzar AuthenticationException")
    void refreshToken_WhenNotRefreshTokenType_ShouldThrowAuthenticationException() {
        // Arrange
        when(tokenBlacklistService.isTokenBlacklisted(testRefreshToken)).thenReturn(false);
        when(tokenProvider.validateToken(testRefreshToken)).thenReturn(true);
        when(tokenProvider.isRefreshToken(testRefreshToken)).thenReturn(false);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authenticationService.refreshToken(testRefreshToken);
        });

        assertEquals("Token no es un refresh token", exception.getMessage());
        verify(tokenProvider).isRefreshToken(testRefreshToken);
    }

    @Test
    @DisplayName("Refresh token con usuario no encontrado - Debe lanzar AuthenticationException")
    void refreshToken_WhenUserNotFound_ShouldThrowAuthenticationException() {
        // Arrange
        when(tokenBlacklistService.isTokenBlacklisted(testRefreshToken)).thenReturn(false);
        when(tokenProvider.validateToken(testRefreshToken)).thenReturn(true);
        when(tokenProvider.isRefreshToken(testRefreshToken)).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT(testRefreshToken)).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authenticationService.refreshToken(testRefreshToken);
        });

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    @DisplayName("Refresh token sin sesiones activas - Debe lanzar AuthenticationException")
    void refreshToken_WhenNoActiveSessions_ShouldThrowAuthenticationException() {
        // Arrange
        when(tokenBlacklistService.isTokenBlacklisted(testRefreshToken)).thenReturn(false);
        when(tokenProvider.validateToken(testRefreshToken)).thenReturn(true);
        when(tokenProvider.isRefreshToken(testRefreshToken)).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT(testRefreshToken)).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionManagementService.hasActiveSessions(testUser.getEmail())).thenReturn(false);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authenticationService.refreshToken(testRefreshToken);
        });

        assertEquals("No hay sesiones activas", exception.getMessage());
        verify(sessionManagementService).hasActiveSessions(testUser.getEmail());
    }

    // ==================== LOGOUT TESTS ====================

    @Test
    @DisplayName("Logout exitoso - Debe retornar LogoutResponse exitoso")
    void logout_WhenValidToken_ShouldReturnSuccessResponse() {
        // Arrange
        Date expirationDate = new Date(System.currentTimeMillis() + 86400000);
        when(tokenProvider.validateToken(testToken)).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT(testToken)).thenReturn(testUser.getEmail());
        when(tokenProvider.getExpirationDateFromJWT(testToken)).thenReturn(expirationDate);
        doNothing().when(tokenBlacklistService).blacklistToken(testToken, expirationDate);
        when(sessionManagementService.invalidateAllUserSessions(testUser.getEmail())).thenReturn(3L);

        // Act
        LogoutResponse response = authenticationService.logout(testToken);

        // Assert
        assertNotNull(response, "La respuesta no debe ser null");
        assertTrue(response.isSuccess(), "El logout debe ser exitoso");
        assertEquals("Logout exitoso", response.getMessage(), "El mensaje debe coincidir");

        verify(tokenProvider).validateToken(testToken);
        verify(tokenProvider).getUsernameFromJWT(testToken);
        verify(tokenBlacklistService).blacklistToken(testToken, expirationDate);
        verify(sessionManagementService).invalidateAllUserSessions(testUser.getEmail());
    }

    @Test
    @DisplayName("Logout con token inválido - Debe retornar LogoutResponse fallido")
    void logout_WhenInvalidToken_ShouldReturnFailureResponse() {
        // Arrange
        when(tokenProvider.validateToken(testToken)).thenReturn(false);

        // Act
        LogoutResponse response = authenticationService.logout(testToken);

        // Assert
        assertNotNull(response, "La respuesta no debe ser null");
        assertFalse(response.isSuccess(), "El logout no debe ser exitoso");
        assertEquals("Token inválido", response.getMessage(), "El mensaje debe coincidir");

        verify(tokenProvider).validateToken(testToken);
        verify(tokenBlacklistService, never()).blacklistToken(anyString(), any());
        verify(sessionManagementService, never()).invalidateAllUserSessions(anyString());
    }

    @Test
    @DisplayName("Logout con excepción - Debe retornar LogoutResponse fallido")
    void logout_WhenExceptionOccurs_ShouldReturnFailureResponse() {
        // Arrange
        when(tokenProvider.validateToken(testToken)).thenThrow(new RuntimeException("Database error"));

        // Act
        LogoutResponse response = authenticationService.logout(testToken);

        // Assert
        assertNotNull(response, "La respuesta no debe ser null");
        assertFalse(response.isSuccess(), "El logout no debe ser exitoso");
        assertEquals("Error durante el logout", response.getMessage(), "El mensaje debe coincidir");

        verify(tokenProvider).validateToken(testToken);
    }

    // ==================== LOGOUT FROM ALL DEVICES TESTS ====================

    @Test
    @DisplayName("Logout de todos los dispositivos exitoso - Debe retornar LogoutResponse exitoso")
    void logoutFromAllDevices_WhenValidUsername_ShouldReturnSuccessResponse() {
        // Arrange
        String username = testUser.getEmail();
        when(sessionManagementService.invalidateAllUserSessions(username)).thenReturn(5L);

        // Act
        LogoutResponse response = authenticationService.logoutFromAllDevices(username);

        // Assert
        assertNotNull(response, "La respuesta no debe ser null");
        assertTrue(response.isSuccess(), "El logout debe ser exitoso");
        assertEquals("Logout exitoso de todos los dispositivos", response.getMessage(),
                "El mensaje debe coincidir");

        verify(sessionManagementService).invalidateAllUserSessions(username);
    }

    @Test
    @DisplayName("Logout de todos los dispositivos con excepción - Debe retornar LogoutResponse fallido")
    void logoutFromAllDevices_WhenExceptionOccurs_ShouldReturnFailureResponse() {
        // Arrange
        String username = testUser.getEmail();
        when(sessionManagementService.invalidateAllUserSessions(username))
                .thenThrow(new RuntimeException("Redis connection error"));

        // Act
        LogoutResponse response = authenticationService.logoutFromAllDevices(username);

        // Assert
        assertNotNull(response, "La respuesta no debe ser null");
        assertFalse(response.isSuccess(), "El logout no debe ser exitoso");
        assertEquals("Error durante el logout", response.getMessage(), "El mensaje debe coincidir");

        verify(sessionManagementService).invalidateAllUserSessions(username);
    }

    @Test
    @DisplayName("Logout de todos los dispositivos sin sesiones - Debe retornar LogoutResponse exitoso")
    void logoutFromAllDevices_WhenNoActiveSessions_ShouldReturnSuccessResponse() {
        // Arrange
        String username = testUser.getEmail();
        when(sessionManagementService.invalidateAllUserSessions(username)).thenReturn(0L);

        // Act
        LogoutResponse response = authenticationService.logoutFromAllDevices(username);

        // Assert
        assertNotNull(response, "La respuesta no debe ser null");
        assertTrue(response.isSuccess(), "El logout debe ser exitoso incluso sin sesiones");
        assertEquals("Logout exitoso de todos los dispositivos", response.getMessage(),
                "El mensaje debe coincidir");

        verify(sessionManagementService).invalidateAllUserSessions(username);
    }

    // ==================== INTEGRATION SCENARIO TESTS ====================

    @Test
    @DisplayName("Escenario completo: Login, Refresh y Logout - Debe funcionar correctamente")
    void completeAuthenticationFlow_ShouldWorkCorrectly() {
        // Arrange
        Date expirationDate = new Date(System.currentTimeMillis() + 86400000);
        String newAccessToken = "new.access.token";
        String newRefreshToken = "new.refresh.token";

        // Setup para login
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn(testToken);
        when(tokenProvider.generateRefreshToken(authentication)).thenReturn(testRefreshToken);
        when(sessionManagementService.registerSession(anyString(), anyString())).thenReturn(true);

        // Setup para refresh token
        when(tokenBlacklistService.isTokenBlacklisted(testRefreshToken)).thenReturn(false);
        when(tokenProvider.validateToken(testRefreshToken)).thenReturn(true);
        when(tokenProvider.isRefreshToken(testRefreshToken)).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT(testRefreshToken)).thenReturn(testUser.getEmail());
        when(sessionManagementService.hasActiveSessions(testUser.getEmail())).thenReturn(true);
        when(tokenProvider.generateTokenFromUser(testUser)).thenReturn(newAccessToken);
        when(tokenProvider.generateRefreshTokenFromUser(testUser)).thenReturn(newRefreshToken);
        when(tokenProvider.getExpirationDateFromJWT(testRefreshToken)).thenReturn(expirationDate);

        // Setup para logout
        when(tokenProvider.validateToken(newAccessToken)).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT(newAccessToken)).thenReturn(testUser.getEmail());
        when(tokenProvider.getExpirationDateFromJWT(newAccessToken)).thenReturn(expirationDate);
        when(sessionManagementService.invalidateAllUserSessions(testUser.getEmail())).thenReturn(1L);

        // Act
        // 1. Login
        AuthResponse loginResponse = authenticationService.login(loginRequest);

        // 2. Refresh Token
        AuthResponse refreshResponse = authenticationService.refreshToken(testRefreshToken);

        // 3. Logout
        LogoutResponse logoutResponse = authenticationService.logout(newAccessToken);

        // Assert
        // Verificar login
        assertNotNull(loginResponse);
        assertEquals(testToken, loginResponse.getToken());

        // Verificar refresh
        assertNotNull(refreshResponse);
        assertEquals(newAccessToken, refreshResponse.getToken());

        // Verificar logout
        assertNotNull(logoutResponse);
        assertTrue(logoutResponse.isSuccess());

        // Verificar interacciones
        verify(userRepository, times(2)).findByEmail(testUser.getEmail());
        verify(sessionManagementService).registerSession(anyString(), anyString());
        verify(sessionManagementService).hasActiveSessions(testUser.getEmail());
        verify(sessionManagementService).invalidateAllUserSessions(testUser.getEmail());
        verify(tokenBlacklistService).blacklistToken(eq(testRefreshToken), any());
        verify(tokenBlacklistService).blacklistToken(eq(newAccessToken), any());
    }
}

