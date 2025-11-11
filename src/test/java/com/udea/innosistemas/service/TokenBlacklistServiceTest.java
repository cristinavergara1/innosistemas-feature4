package com.udea.innosistemas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitario para TokenBlacklistService utilizando el patrón AAA (Arrange-Act-Assert).
 * Verifica la gestión de tokens revocados en blacklist usando Redis.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService - Test unitario con patrón AAA")
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    private String testToken;
    private Date futureExpirationDate;
    private Date pastExpirationDate;
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    @BeforeEach
    void setUp() {
        testToken = "eyJhbGciOiJIUzUxMiJ9.test.token";
        futureExpirationDate = new Date(System.currentTimeMillis() + 86400000); // +24 horas
        pastExpirationDate = new Date(System.currentTimeMillis() - 3600000); // -1 hora
    }

    // ==================== BLACKLIST TOKEN TESTS ====================

    @Test
    @DisplayName("Blacklist token válido - Debe agregar token a blacklist con TTL correcto")
    void blacklistToken_WhenValidToken_ShouldAddToBlacklist() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // Act
        tokenBlacklistService.blacklistToken(testToken, futureExpirationDate);

        // Assert
        verify(valueOperations).set(
                eq(BLACKLIST_PREFIX + testToken),
                eq("revoked"),
                anyLong(),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("Blacklist token expirado - No debe agregar a blacklist")
    void blacklistToken_WhenExpiredToken_ShouldNotAddToBlacklist() {
        // Arrange
        // No mock needed - token is already expired

        // Act
        tokenBlacklistService.blacklistToken(testToken, pastExpirationDate);

        // Assert
        // Verify no interaction with Redis since token is expired
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("Blacklist token con excepción Redis - Debe manejar error gracefully")
    void blacklistToken_WhenRedisException_ShouldHandleGracefully() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis connection error"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // Act & Assert - No debe lanzar excepción
        assertDoesNotThrow(() -> tokenBlacklistService.blacklistToken(testToken, futureExpirationDate));
    }

    @Test
    @DisplayName("Blacklist token null - Debe manejar token null")
    void blacklistToken_WhenTokenNull_ShouldHandleGracefully() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // Act & Assert
        assertDoesNotThrow(() -> tokenBlacklistService.blacklistToken(null, futureExpirationDate));
    }

    @Test
    @DisplayName("Blacklist token con fecha null - Debe manejar error")
    void blacklistToken_WhenExpirationDateNull_ShouldHandleGracefully() {
        // No mock needed - will throw NullPointerException internally and be caught


        // Act & Assert

        // Verify no interaction with Redis since there was an error
        verify(redisTemplate, never()).opsForValue();
        assertDoesNotThrow(() -> tokenBlacklistService.blacklistToken(testToken, null));
    }

    // ==================== IS TOKEN BLACKLISTED TESTS ====================

    @Test
    @DisplayName("Verificar token en blacklist - Debe retornar true si existe")
    void isTokenBlacklisted_WhenTokenExists_ShouldReturnTrue() {
        // Arrange
        when(redisTemplate.hasKey(BLACKLIST_PREFIX + testToken)).thenReturn(true);

        // Act
        boolean result = tokenBlacklistService.isTokenBlacklisted(testToken);

        // Assert
        assertTrue(result, "Debe retornar true para token en blacklist");
        verify(redisTemplate).hasKey(BLACKLIST_PREFIX + testToken);
    }

    @Test
    @DisplayName("Verificar token no en blacklist - Debe retornar false")
    void isTokenBlacklisted_WhenTokenNotExists_ShouldReturnFalse() {
        // Arrange
        when(redisTemplate.hasKey(BLACKLIST_PREFIX + testToken)).thenReturn(false);

        // Act
        boolean result = tokenBlacklistService.isTokenBlacklisted(testToken);

        // Assert
        assertFalse(result, "Debe retornar false para token no en blacklist");
        verify(redisTemplate).hasKey(BLACKLIST_PREFIX + testToken);
    }

    @Test
    @DisplayName("Verificar token con Redis null - Debe retornar false por seguridad")
    void isTokenBlacklisted_WhenRedisReturnsNull_ShouldReturnFalse() {
        // Arrange
        when(redisTemplate.hasKey(BLACKLIST_PREFIX + testToken)).thenReturn(null);

        // Act
        boolean result = tokenBlacklistService.isTokenBlacklisted(testToken);

        // Assert
        assertFalse(result, "Debe retornar false cuando Redis retorna null");
    }

    @Test
    @DisplayName("Verificar token con excepción Redis - Debe retornar true por seguridad")
    void isTokenBlacklisted_WhenRedisException_ShouldReturnTrueForSecurity() {
        // Arrange
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis error"));

        // Act
        boolean result = tokenBlacklistService.isTokenBlacklisted(testToken);

        // Assert
        assertTrue(result, "Debe retornar true por seguridad en caso de error Redis");
    }

    @Test
    @DisplayName("Verificar token null - Debe manejar gracefully")
    void isTokenBlacklisted_WhenTokenNull_ShouldHandleGracefully() {
        // Arrange
        when(redisTemplate.hasKey(BLACKLIST_PREFIX + null)).thenReturn(false);

        // Act
        boolean result = tokenBlacklistService.isTokenBlacklisted(null);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Verificar token vacío - Debe retornar false")
    void isTokenBlacklisted_WhenTokenEmpty_ShouldReturnFalse() {
        // Arrange
        String emptyToken = "";
        when(redisTemplate.hasKey(BLACKLIST_PREFIX + emptyToken)).thenReturn(false);

        // Act
        boolean result = tokenBlacklistService.isTokenBlacklisted(emptyToken);

        // Assert
        assertFalse(result);
    }

    // ==================== REMOVE TOKEN TESTS ====================

    @Test
    @DisplayName("Remover token de blacklist - Debe eliminar token exitosamente")
    void removeTokenFromBlacklist_WhenValidToken_ShouldRemoveSuccessfully() {
        // Arrange
        when(redisTemplate.delete(BLACKLIST_PREFIX + testToken)).thenReturn(true);

        // Act
        tokenBlacklistService.removeTokenFromBlacklist(testToken);

        // Assert
        verify(redisTemplate).delete(BLACKLIST_PREFIX + testToken);
    }

    @Test
    @DisplayName("Remover token no existente - Debe ejecutar sin error")
    void removeTokenFromBlacklist_WhenTokenNotExists_ShouldExecuteWithoutError() {
        // Arrange
        when(redisTemplate.delete(BLACKLIST_PREFIX + testToken)).thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() -> tokenBlacklistService.removeTokenFromBlacklist(testToken));
        verify(redisTemplate).delete(BLACKLIST_PREFIX + testToken);
    }

    @Test
    @DisplayName("Remover token con excepción Redis - Debe manejar error")
    void removeTokenFromBlacklist_WhenRedisException_ShouldHandleError() {
        // Arrange
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis error"));

        // Act & Assert
        assertDoesNotThrow(() -> tokenBlacklistService.removeTokenFromBlacklist(testToken));
    }

    @Test
    @DisplayName("Remover token null - Debe manejar gracefully")
    void removeTokenFromBlacklist_WhenTokenNull_ShouldHandleGracefully() {
        // Arrange
        when(redisTemplate.delete(BLACKLIST_PREFIX + null)).thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() -> tokenBlacklistService.removeTokenFromBlacklist(null));
    }

    // ==================== CLEAR BLACKLIST TESTS ====================

    @Test
    @DisplayName("Limpiar blacklist con tokens - Debe eliminar todos los tokens")
    void clearBlacklist_WhenTokensExist_ShouldRemoveAll() {
        // Arrange
        Set<String> keys = Set.of(
                BLACKLIST_PREFIX + "token1",
                BLACKLIST_PREFIX + "token2",
                BLACKLIST_PREFIX + "token3"
        );
        when(redisTemplate.keys(BLACKLIST_PREFIX + "*")).thenReturn(keys);
        when(redisTemplate.delete(keys)).thenReturn(3L);

        // Act
        tokenBlacklistService.clearBlacklist();

        // Assert
        verify(redisTemplate).keys(BLACKLIST_PREFIX + "*");
        verify(redisTemplate).delete(keys);
    }

    @Test
    @DisplayName("Limpiar blacklist vacía - Debe ejecutar sin error")
    void clearBlacklist_WhenNoTokens_ShouldExecuteWithoutError() {
        // Arrange
        Set<String> emptyKeys = Set.of();
        when(redisTemplate.keys(BLACKLIST_PREFIX + "*")).thenReturn(emptyKeys);

        // Act
        tokenBlacklistService.clearBlacklist();

        // Assert
        verify(redisTemplate).keys(BLACKLIST_PREFIX + "*");
        verify(redisTemplate, never()).delete(anySet());
    }

    @Test
    @DisplayName("Limpiar blacklist con keys null - Debe manejar gracefully")
    void clearBlacklist_WhenKeysNull_ShouldHandleGracefully() {
        // Arrange
        when(redisTemplate.keys(BLACKLIST_PREFIX + "*")).thenReturn(null);

        // Act & Assert
        assertDoesNotThrow(() -> tokenBlacklistService.clearBlacklist());
        verify(redisTemplate, never()).delete(anySet());
    }

    @Test
    @DisplayName("Limpiar blacklist con excepción Redis - Debe manejar error")
    void clearBlacklist_WhenRedisException_ShouldHandleError() {
        // Arrange
        when(redisTemplate.keys(anyString())).thenThrow(new RuntimeException("Redis error"));

        // Act & Assert
        assertDoesNotThrow(() -> tokenBlacklistService.clearBlacklist());
    }

    // ==================== INTEGRATION-LIKE TESTS ====================

    @Test
    @DisplayName("Flujo completo: Blacklist, verificar y remover token")
    void completeFlow_BlacklistCheckAndRemove_ShouldWorkCorrectly() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        when(redisTemplate.hasKey(BLACKLIST_PREFIX + testToken)).thenReturn(true);
        when(redisTemplate.delete(BLACKLIST_PREFIX + testToken)).thenReturn(true);

        // Act
        // 1. Blacklist token
        tokenBlacklistService.blacklistToken(testToken, futureExpirationDate);

        // 2. Verificar que está en blacklist
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(testToken);

        // 3. Remover token
        tokenBlacklistService.removeTokenFromBlacklist(testToken);

        // Assert
        assertTrue(isBlacklisted, "Token debe estar en blacklist después de agregarlo");
        verify(valueOperations).set(anyString(), eq("revoked"), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(redisTemplate).hasKey(BLACKLIST_PREFIX + testToken);
        verify(redisTemplate).delete(BLACKLIST_PREFIX + testToken);
    }

    @Test
    @DisplayName("Blacklist múltiples tokens - Debe agregar todos correctamente")
    void blacklistMultipleTokens_ShouldAddAllCorrectly() {
        // Arrange
        String token1 = "token1";
        String token2 = "token2";
        String token3 = "token3";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // Act
        tokenBlacklistService.blacklistToken(token1, futureExpirationDate);
        tokenBlacklistService.blacklistToken(token2, futureExpirationDate);
        tokenBlacklistService.blacklistToken(token3, futureExpirationDate);

        // Assert
        verify(valueOperations, times(3)).set(anyString(), eq("revoked"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("Verificar token con caracteres especiales - Debe funcionar correctamente")
    void isTokenBlacklisted_WhenTokenHasSpecialCharacters_ShouldWork() {
        // Arrange
        String specialToken = "eyJ@#$%.token+with/special=chars";
        when(redisTemplate.hasKey(BLACKLIST_PREFIX + specialToken)).thenReturn(true);

        // Act
        boolean result = tokenBlacklistService.isTokenBlacklisted(specialToken);

        // Assert
        assertTrue(result);
        verify(redisTemplate).hasKey(BLACKLIST_PREFIX + specialToken);
    }

    @Test
    @DisplayName("Blacklist token con TTL muy corto - Debe agregarlo correctamente")
    void blacklistToken_WhenVeryShortTTL_ShouldAddCorrectly() {
        // Arrange
        Date nearFutureDate = new Date(System.currentTimeMillis() + 100); // +100ms
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // Act
        tokenBlacklistService.blacklistToken(testToken, nearFutureDate);

        // Assert
        verify(valueOperations).set(
                eq(BLACKLIST_PREFIX + testToken),
                eq("revoked"),
                anyLong(),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("Limpiar blacklist con muchos tokens - Debe eliminar todos")
    void clearBlacklist_WhenManyTokens_ShouldRemoveAll() {
        // Arrange
        Set<String> manyKeys = Set.of(
                BLACKLIST_PREFIX + "token1",
                BLACKLIST_PREFIX + "token2",
                BLACKLIST_PREFIX + "token3",
                BLACKLIST_PREFIX + "token4",
                BLACKLIST_PREFIX + "token5"
        );
        when(redisTemplate.keys(BLACKLIST_PREFIX + "*")).thenReturn(manyKeys);
        when(redisTemplate.delete(manyKeys)).thenReturn(5L);

        // Act
        tokenBlacklistService.clearBlacklist();

        // Assert
        verify(redisTemplate).delete(manyKeys);
    }
}

