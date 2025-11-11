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
        doNothing().when(tokenBlacklistService).blacklistToken(eq(testRefreshToken), any());

        // Setup para logout
        when(tokenProvider.validateToken(newAccessToken)).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT(newAccessToken)).thenReturn(testUser.getEmail());
        when(tokenProvider.getExpirationDateFromJWT(newAccessToken)).thenReturn(expirationDate);
        doNothing().when(tokenBlacklistService).blacklistToken(eq(newAccessToken), any());
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

    // ==================== EDGE CASES AND ADDITIONAL COVERAGE ====================

    @Test
    @DisplayName("Login con sesión no registrada - Debe lanzar excepción si registerSession falla")
    void login_WhenSessionRegistrationFails_ShouldStillReturnAuthResponse() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn(testToken);
        when(tokenProvider.generateRefreshToken(authentication)).thenReturn(testRefreshToken);
        when(sessionManagementService.registerSession(anyString(), anyString())).thenReturn(false);

        // Act
        AuthResponse response = authenticationService.login(loginRequest);

        // Assert
        assertNotNull(response, "La respuesta no debe ser null aunque falle el registro de sesión");
        assertEquals(testToken, response.getToken());
        assertEquals(testRefreshToken, response.getRefreshToken());
        verify(sessionManagementService).registerSession(eq(testUser.getEmail()), anyString());
    }

    @Test
    @DisplayName("Login con usuario deshabilitado - Debe lanzar AuthenticationException")
    void login_WhenUserDisabled_ShouldThrowAuthenticationException() {
        // Arrange
        testUser.setEnabled(false);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("User is disabled"));

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authenticationService.login(loginRequest);
        });

        assertEquals("Credenciales inválidas", exception.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Login con cuenta bloqueada - Debe lanzar AuthenticationException")
    void login_WhenAccountLocked_ShouldThrowAuthenticationException() {
        // Arrange
        testUser.setAccountNonLocked(false);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Account is locked"));

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authenticationService.login(loginRequest);
        });

        assertEquals("Credenciales inválidas", exception.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Validar credenciales con email null - Debe retornar false")
    void validateCredentials_WhenEmailNull_ShouldReturnFalse() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Email cannot be null"));

        // Act
        boolean result = authenticationService.validateCredentials(null, "password123");

        // Assert
        assertFalse(result, "Debe retornar false cuando el email es null");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Validar credenciales con password null - Debe retornar false")
    void validateCredentials_WhenPasswordNull_ShouldReturnFalse() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Password cannot be null"));

        // Act
        boolean result = authenticationService.validateCredentials("test@udea.edu.co", null);

        // Assert
        assertFalse(result, "Debe retornar false cuando el password es null");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Validar credenciales con email vacío - Debe retornar false")
    void validateCredentials_WhenEmailEmpty_ShouldReturnFalse() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Email cannot be empty"));

        // Act
        boolean result = authenticationService.validateCredentials("", "password123");

        // Assert
        assertFalse(result, "Debe retornar false cuando el email está vacío");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Refresh token con excepción inesperada - Debe lanzar AuthenticationException")
    void refreshToken_WhenUnexpectedException_ShouldThrowAuthenticationException() {
        // Arrange
        when(tokenBlacklistService.isTokenBlacklisted(testRefreshToken)).thenReturn(false);
        when(tokenProvider.validateToken(testRefreshToken)).thenReturn(true);
        when(tokenProvider.isRefreshToken(testRefreshToken)).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT(testRefreshToken)).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(sessionManagementService.hasActiveSessions(testUser.getEmail())).thenReturn(true);
        when(tokenProvider.generateTokenFromUser(testUser)).thenThrow(new RuntimeException("Token generation failed"));

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authenticationService.refreshToken(testRefreshToken);
        });

        assertEquals("Error al renovar el token", exception.getMessage());
        verify(tokenProvider).generateTokenFromUser(testUser);
    }

    @Test
    @DisplayName("Refresh token null - Debe lanzar AuthenticationException")
    void refreshToken_WhenTokenNull_ShouldThrowAuthenticationException() {
        // Arrange
        when(tokenBlacklistService.isTokenBlacklisted(null)).thenReturn(false);
        when(tokenProvider.validateToken(null)).thenReturn(false);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authenticationService.refreshToken(null);
        });

        assertEquals("Token inválido", exception.getMessage());
    }

    @Test
    @DisplayName("Refresh token vacío - Debe lanzar AuthenticationException")
    void refreshToken_WhenTokenEmpty_ShouldThrowAuthenticationException() {
        // Arrange
        String emptyToken = "";
        when(tokenBlacklistService.isTokenBlacklisted(emptyToken)).thenReturn(false);
        when(tokenProvider.validateToken(emptyToken)).thenReturn(false);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authenticationService.refreshToken(emptyToken);
        });

        assertEquals("Token inválido", exception.getMessage());
    }

    @Test
    @DisplayName("Logout con token null - Debe retornar LogoutResponse fallido")
    void logout_WhenTokenNull_ShouldReturnFailureResponse() {
        // Arrange
        when(tokenProvider.validateToken(null)).thenReturn(false);

        // Act
        LogoutResponse response = authenticationService.logout(null);

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Token inválido", response.getMessage());
        verify(tokenProvider).validateToken(null);
    }

    @Test
    @DisplayName("Logout con token vacío - Debe retornar LogoutResponse fallido")
    void logout_WhenTokenEmpty_ShouldReturnFailureResponse() {
        // Arrange
        String emptyToken = "";
        when(tokenProvider.validateToken(emptyToken)).thenReturn(false);

        // Act
        LogoutResponse response = authenticationService.logout(emptyToken);

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Token inválido", response.getMessage());
    }

    @Test
    @DisplayName("Logout con excepción en blacklist - Debe retornar LogoutResponse fallido")
    void logout_WhenBlacklistFails_ShouldReturnFailureResponse() {
        // Arrange
        when(tokenProvider.validateToken(testToken)).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT(testToken)).thenReturn(testUser.getEmail());
        when(tokenProvider.getExpirationDateFromJWT(testToken))
                .thenThrow(new RuntimeException("Redis connection error"));

        // Act
        LogoutResponse response = authenticationService.logout(testToken);

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Error durante el logout", response.getMessage());
    }

    @Test
    @DisplayName("Logout exitoso sin sesiones activas - Debe retornar LogoutResponse exitoso")
    void logout_WhenNoActiveSessions_ShouldStillReturnSuccessResponse() {
        // Arrange
        Date expirationDate = new Date(System.currentTimeMillis() + 86400000);
        when(tokenProvider.validateToken(testToken)).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT(testToken)).thenReturn(testUser.getEmail());
        when(tokenProvider.getExpirationDateFromJWT(testToken)).thenReturn(expirationDate);
        doNothing().when(tokenBlacklistService).blacklistToken(testToken, expirationDate);
        when(sessionManagementService.invalidateAllUserSessions(testUser.getEmail())).thenReturn(0L);

        // Act
        LogoutResponse response = authenticationService.logout(testToken);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Logout exitoso", response.getMessage());
        verify(sessionManagementService).invalidateAllUserSessions(testUser.getEmail());
    }

    @Test
    @DisplayName("LogoutFromAllDevices con username null - Debe retornar LogoutResponse fallido")
    void logoutFromAllDevices_WhenUsernameNull_ShouldReturnFailureResponse() {
        // Arrange
        when(sessionManagementService.invalidateAllUserSessions(null))
                .thenThrow(new IllegalArgumentException("Username cannot be null"));

        // Act
        LogoutResponse response = authenticationService.logoutFromAllDevices(null);

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Error durante el logout", response.getMessage());
    }

    @Test
    @DisplayName("LogoutFromAllDevices con username vacío - Debe retornar LogoutResponse exitoso")
    void logoutFromAllDevices_WhenUsernameEmpty_ShouldReturnSuccessResponse() {
        // Arrange
        String emptyUsername = "";
        when(sessionManagementService.invalidateAllUserSessions(emptyUsername)).thenReturn(0L);

        // Act
        LogoutResponse response = authenticationService.logoutFromAllDevices(emptyUsername);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Logout exitoso de todos los dispositivos", response.getMessage());
    }

    @Test
    @DisplayName("LogoutFromAllDevices con múltiples sesiones - Debe retornar LogoutResponse exitoso")
    void logoutFromAllDevices_WhenMultipleSessions_ShouldReturnSuccessResponse() {
        // Arrange
        String username = testUser.getEmail();
        when(sessionManagementService.invalidateAllUserSessions(username)).thenReturn(10L);

        // Act
        LogoutResponse response = authenticationService.logoutFromAllDevices(username);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Logout exitoso de todos los dispositivos", response.getMessage());
        verify(sessionManagementService).invalidateAllUserSessions(username);
    }

    @Test
    @DisplayName("Login con diferentes roles - Debe retornar AuthResponse con role correcto")
    void login_WithDifferentRoles_ShouldReturnCorrectRoleInResponse() {
        // Arrange - Probar con PROFESSOR
        testUser.setRole(UserRole.PROFESSOR);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn(testToken);
        when(tokenProvider.generateRefreshToken(authentication)).thenReturn(testRefreshToken);
        when(sessionManagementService.registerSession(anyString(), anyString())).thenReturn(true);

        // Act
        AuthResponse response = authenticationService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(UserRole.PROFESSOR, response.getUserInfo().getRole());
        assertEquals(testUser.getEmail(), response.getUserInfo().getEmail());
    }

    @Test
    @DisplayName("Refresh token verifica UserInfo completo - Debe incluir todos los campos")
    void refreshToken_ShouldReturnCompleteUserInfo() {
        // Arrange
        Date expirationDate = new Date(System.currentTimeMillis() + 86400000);
        String newAccessToken = "new.access.token";
        String newRefreshToken = "new.refresh.token";

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
        assertNotNull(response.getUserInfo());
        assertEquals(testUser.getId(), response.getUserInfo().getId());
        assertEquals(testUser.getEmail(), response.getUserInfo().getEmail());
        assertEquals(testUser.getRole(), response.getUserInfo().getRole());
        assertEquals(testUser.getFirstName(), response.getUserInfo().getFirstName());
        assertEquals(testUser.getLastName(), response.getUserInfo().getLastName());
        assertEquals(testUser.getTeamId(), response.getUserInfo().getTeamId());
        assertEquals(testUser.getCourseId(), response.getUserInfo().getCourseId());
    }

    @Test
    @DisplayName("Login verifica UserInfo completo - Debe incluir todos los campos del usuario")
    void login_ShouldReturnCompleteUserInfo() {
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
        assertNotNull(response.getUserInfo());
        assertEquals(testUser.getId(), response.getUserInfo().getId());
        assertEquals(testUser.getEmail(), response.getUserInfo().getEmail());
        assertEquals(testUser.getRole(), response.getUserInfo().getRole());
        assertEquals(testUser.getFirstName(), response.getUserInfo().getFirstName());
        assertEquals(testUser.getLastName(), response.getUserInfo().getLastName());
        assertEquals(testUser.getTeamId(), response.getUserInfo().getTeamId());
        assertEquals(testUser.getCourseId(), response.getUserInfo().getCourseId());
    }

    @Test
    @DisplayName("Refresh token con usuario sin teamId - Debe funcionar correctamente")
    void refreshToken_WhenUserHasNoTeam_ShouldStillWork() {
        // Arrange
        testUser.setTeamId(null);
        Date expirationDate = new Date(System.currentTimeMillis() + 86400000);
        String newAccessToken = "new.access.token";
        String newRefreshToken = "new.refresh.token";

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
        assertNotNull(response);
        assertNotNull(response.getUserInfo());
        assertNull(response.getUserInfo().getTeamId(), "TeamId debe ser null");
        assertEquals(newAccessToken, response.getToken());
    }

    @Test
    @DisplayName("Login con usuario sin courseId - Debe funcionar correctamente")
    void login_WhenUserHasNoCourse_ShouldStillWork() {
        // Arrange
        testUser.setCourseId(null);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn(testToken);
        when(tokenProvider.generateRefreshToken(authentication)).thenReturn(testRefreshToken);
        when(sessionManagementService.registerSession(anyString(), anyString())).thenReturn(true);

        // Act
        AuthResponse response = authenticationService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getUserInfo());
        assertNull(response.getUserInfo().getCourseId(), "CourseId debe ser null");
        assertEquals(testToken, response.getToken());
    }

    @Test
    @DisplayName("Login con role ADMIN - Debe retornar AuthResponse con role ADMIN")
    void login_WithAdminRole_ShouldReturnAdminRoleInResponse() {
        // Arrange
        testUser.setRole(UserRole.ADMIN);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn(testToken);
        when(tokenProvider.generateRefreshToken(authentication)).thenReturn(testRefreshToken);
        when(sessionManagementService.registerSession(anyString(), anyString())).thenReturn(true);

        // Act
        AuthResponse response = authenticationService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(UserRole.ADMIN, response.getUserInfo().getRole());
        assertEquals(testUser.getEmail(), response.getUserInfo().getEmail());
    }

    @Test
    @DisplayName("Login con role TA - Debe retornar AuthResponse con role TA")
    void login_WithTARole_ShouldReturnTARoleInResponse() {
        // Arrange
        testUser.setRole(UserRole.TA);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn(testToken);
        when(tokenProvider.generateRefreshToken(authentication)).thenReturn(testRefreshToken);
        when(sessionManagementService.registerSession(anyString(), anyString())).thenReturn(true);

        // Act
        AuthResponse response = authenticationService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(UserRole.TA, response.getUserInfo().getRole());
        assertEquals(testUser.getEmail(), response.getUserInfo().getEmail());
    }

    @Test
    @DisplayName("Refresh token con usuario recién actualizado - Debe retornar información actualizada")
    void refreshToken_WhenUserDataUpdated_ShouldReturnUpdatedInfo() {
        // Arrange
        testUser.setFirstName("NuevoNombre");
        testUser.setLastName("NuevoApellido");
        Date expirationDate = new Date(System.currentTimeMillis() + 86400000);
        String newAccessToken = "new.access.token";
        String newRefreshToken = "new.refresh.token";

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
        assertNotNull(response);
        assertEquals("NuevoNombre", response.getUserInfo().getFirstName());
        assertEquals("NuevoApellido", response.getUserInfo().getLastName());
    }

    @Test
    @DisplayName("Logout con múltiples sesiones - Debe invalidar todas las sesiones")
    void logout_WhenMultipleSessions_ShouldInvalidateAllSessions() {
        // Arrange
        Date expirationDate = new Date(System.currentTimeMillis() + 86400000);
        when(tokenProvider.validateToken(testToken)).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT(testToken)).thenReturn(testUser.getEmail());
        when(tokenProvider.getExpirationDateFromJWT(testToken)).thenReturn(expirationDate);
        doNothing().when(tokenBlacklistService).blacklistToken(testToken, expirationDate);
        when(sessionManagementService.invalidateAllUserSessions(testUser.getEmail())).thenReturn(5L);

        // Act
        LogoutResponse response = authenticationService.logout(testToken);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Logout exitoso", response.getMessage());
        verify(sessionManagementService).invalidateAllUserSessions(testUser.getEmail());
    }

    @Test
    @DisplayName("Validar credenciales con email especial - Debe funcionar correctamente")
    void validateCredentials_WhenSpecialCharactersInEmail_ShouldWork() {
        // Arrange
        String specialEmail = "test+tag@udea.edu.co";
        String password = "password123";
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // Act
        boolean result = authenticationService.validateCredentials(specialEmail, password);

        // Assert
        assertTrue(result);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Login con password especial - Debe funcionar correctamente")
    void login_WhenPasswordHasSpecialCharacters_ShouldWork() {
        // Arrange
        LoginRequest specialPasswordRequest = new LoginRequest(
                "estudiante@udea.edu.co",
                "P@ssw0rd!#$%&*"
        );
        when(userRepository.findByEmail(specialPasswordRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn(testToken);
        when(tokenProvider.generateRefreshToken(authentication)).thenReturn(testRefreshToken);
        when(sessionManagementService.registerSession(anyString(), anyString())).thenReturn(true);

        // Act
        AuthResponse response = authenticationService.login(specialPasswordRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testToken, response.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Refresh token con sesión única - Debe funcionar correctamente")
    void refreshToken_WhenSingleSession_ShouldWork() {
        // Arrange
        Date expirationDate = new Date(System.currentTimeMillis() + 86400000);
        String newAccessToken = "new.access.token";
        String newRefreshToken = "new.refresh.token";

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
        assertNotNull(response);
        assertEquals(newAccessToken, response.getToken());
        assertEquals(newRefreshToken, response.getRefreshToken());
        verify(tokenBlacklistService).blacklistToken(testRefreshToken, expirationDate);
    }

    @Test
    @DisplayName("Login con usuario con teamId y courseId - Debe incluir ambos en respuesta")
    void login_WhenUserHasTeamAndCourse_ShouldIncludeBothInResponse() {
        // Arrange
        testUser.setTeamId(100L);
        testUser.setCourseId(200L);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn(testToken);
        when(tokenProvider.generateRefreshToken(authentication)).thenReturn(testRefreshToken);
        when(sessionManagementService.registerSession(anyString(), anyString())).thenReturn(true);

        // Act
        AuthResponse response = authenticationService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getUserInfo());
        assertEquals(100L, response.getUserInfo().getTeamId());
        assertEquals(200L, response.getUserInfo().getCourseId());
    }

    @Test
    @DisplayName("Validar credenciales múltiples veces - Debe funcionar consistentemente")
    void validateCredentials_WhenCalledMultipleTimes_ShouldWorkConsistently() {
        // Arrange
        String email = "estudiante@udea.edu.co";
        String password = "password123";
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // Act
        boolean result1 = authenticationService.validateCredentials(email, password);
        boolean result2 = authenticationService.validateCredentials(email, password);
        boolean result3 = authenticationService.validateCredentials(email, password);

        // Assert
        assertTrue(result1);
        assertTrue(result2);
        assertTrue(result3);
        verify(authenticationManager, times(3)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
