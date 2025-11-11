package com.udea.innosistemas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitario para RateLimitingService utilizando el patrón AAA (Arrange-Act-Assert).
 * Verifica el control de tasa de peticiones usando algoritmo Token Bucket.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingService - Test unitario con patrón AAA")
class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService();

        // Configurar valores de prueba usando ReflectionTestUtils
        ReflectionTestUtils.setField(rateLimitingService, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(rateLimitingService, "defaultCapacity", 100L);
        ReflectionTestUtils.setField(rateLimitingService, "defaultRefillTokens", 100L);
        ReflectionTestUtils.setField(rateLimitingService, "defaultRefillPeriodMinutes", 1L);
        ReflectionTestUtils.setField(rateLimitingService, "authCapacity", 10L);
        ReflectionTestUtils.setField(rateLimitingService, "authRefillTokens", 10L);
        ReflectionTestUtils.setField(rateLimitingService, "authRefillPeriodMinutes", 1L);
    }

    // ==================== ALLOW REQUEST TESTS ====================

    @Test
    @DisplayName("Permitir request primera vez - Debe retornar true")
    void allowRequest_WhenFirstRequest_ShouldReturnTrue() {
        // Arrange
        String key = "user1";

        // Act
        boolean result = rateLimitingService.allowRequest(key);

        // Assert
        assertTrue(result, "La primera request debe ser permitida");
    }

    @Test
    @DisplayName("Permitir múltiples requests bajo el límite - Debe retornar true")
    void allowRequest_WhenUnderLimit_ShouldReturnTrue() {
        // Arrange
        String key = "user2";

        // Act & Assert
        for (int i = 0; i < 50; i++) {
            boolean result = rateLimitingService.allowRequest(key);
            assertTrue(result, "Request " + i + " debe ser permitida");
        }
    }

    @Test
    @DisplayName("Exceder límite de requests - Debe retornar false")
    void allowRequest_WhenOverLimit_ShouldReturnFalse() {
        // Arrange
        String key = "user3";

        // Act
        // Consumir todos los tokens (100)
        for (int i = 0; i < 100; i++) {
            rateLimitingService.allowRequest(key);
        }

        // Intentar una request más
        boolean result = rateLimitingService.allowRequest(key);

        // Assert
        assertFalse(result, "Request que excede el límite debe ser rechazada");
    }

    @Test
    @DisplayName("Permitir request con múltiples tokens - Debe consumir tokens correctamente")
    void allowRequest_WhenMultipleTokens_ShouldConsumeCorrectly() {
        // Arrange
        String key = "user4";
        long tokensToConsume = 50;

        // Act
        boolean result = rateLimitingService.allowRequest(key, tokensToConsume);

        // Assert
        assertTrue(result, "Request debe ser permitida");

        // Verificar que se consumieron los tokens
        long availableTokens = rateLimitingService.getAvailableTokens(key);
        assertEquals(50, availableTokens, "Deben quedar 50 tokens disponibles");
    }

    @Test
    @DisplayName("Consumir más tokens de los disponibles - Debe retornar false")
    void allowRequest_WhenNotEnoughTokens_ShouldReturnFalse() {
        // Arrange
        String key = "user5";

        // Consumir 95 tokens
        rateLimitingService.allowRequest(key, 95);

        // Act
        // Intentar consumir 10 tokens cuando solo quedan 5
        boolean result = rateLimitingService.allowRequest(key, 10);

        // Assert
        assertFalse(result, "No debe permitir consumir más tokens de los disponibles");
    }

    @Test
    @DisplayName("Rate limit deshabilitado - Debe siempre retornar true")
    void allowRequest_WhenRateLimitDisabled_ShouldAlwaysReturnTrue() {
        // Arrange
        ReflectionTestUtils.setField(rateLimitingService, "rateLimitEnabled", false);
        String key = "user6";

        // Act & Assert
        for (int i = 0; i < 200; i++) {
            boolean result = rateLimitingService.allowRequest(key);
            assertTrue(result, "Con rate limit deshabilitado, todas las requests deben ser permitidas");
        }
    }

    @Test
    @DisplayName("Múltiples usuarios diferentes - Cada uno debe tener su propio bucket")
    void allowRequest_WhenMultipleUsers_ShouldHaveIndependentBuckets() {
        // Arrange
        String user1 = "user7";
        String user2 = "user8";

        // Act
        // Consumir todos los tokens del usuario 1
        for (int i = 0; i < 100; i++) {
            rateLimitingService.allowRequest(user1);
        }

        // Usuario 2 aún debe tener tokens disponibles
        boolean user2Result = rateLimitingService.allowRequest(user2);
        boolean user1Result = rateLimitingService.allowRequest(user1);

        // Assert
        assertTrue(user2Result, "Usuario 2 debe tener tokens disponibles");
        assertFalse(user1Result, "Usuario 1 debe haber agotado sus tokens");
    }

    // ==================== AUTH REQUEST TESTS ====================

    @Test
    @DisplayName("Permitir auth request primera vez - Debe retornar true")
    void allowAuthRequest_WhenFirstRequest_ShouldReturnTrue() {
        // Arrange
        String key = "auth_user1";

        // Act
        boolean result = rateLimitingService.allowAuthRequest(key);

        // Assert
        assertTrue(result, "Primera auth request debe ser permitida");
    }

    @Test
    @DisplayName("Exceder límite de auth requests - Debe retornar false")
    void allowAuthRequest_WhenOverLimit_ShouldReturnFalse() {
        // Arrange
        String key = "auth_user2";

        // Act
        // Consumir todos los tokens de auth (10)
        for (int i = 0; i < 10; i++) {
            rateLimitingService.allowAuthRequest(key);
        }

        // Intentar una request más
        boolean result = rateLimitingService.allowAuthRequest(key);

        // Assert
        assertFalse(result, "Auth request que excede el límite debe ser rechazada");
    }

    @Test
    @DisplayName("Auth request con rate limit deshabilitado - Debe retornar true")
    void allowAuthRequest_WhenRateLimitDisabled_ShouldReturnTrue() {
        // Arrange
        ReflectionTestUtils.setField(rateLimitingService, "rateLimitEnabled", false);
        String key = "auth_user3";

        // Act
        boolean result = rateLimitingService.allowAuthRequest(key);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Auth bucket separado de bucket normal - Deben ser independientes")
    void allowAuthRequest_ShouldHaveSeparateBucketFromNormal() {
        // Arrange
        String key = "user9";

        // Act
        // Agotar tokens de auth
        for (int i = 0; i < 10; i++) {
            rateLimitingService.allowAuthRequest(key);
        }

        // Verificar que bucket normal aún tiene tokens
        boolean normalResult = rateLimitingService.allowRequest(key);
        boolean authResult = rateLimitingService.allowAuthRequest(key);

        // Assert
        assertTrue(normalResult, "Bucket normal debe tener tokens");
        assertFalse(authResult, "Bucket de auth debe estar agotado");
    }

    // ==================== GET AVAILABLE TOKENS TESTS ====================

    @Test
    @DisplayName("Obtener tokens disponibles de bucket nuevo - Debe retornar capacidad completa")
    void getAvailableTokens_WhenNewBucket_ShouldReturnFullCapacity() {
        // Arrange
        String key = "user10";

        // Act
        long availableTokens = rateLimitingService.getAvailableTokens(key);

        // Assert
        assertEquals(100, availableTokens, "Bucket nuevo debe tener capacidad completa");
    }

    @Test
    @DisplayName("Obtener tokens disponibles después de consumir - Debe retornar cantidad correcta")
    void getAvailableTokens_AfterConsuming_ShouldReturnCorrectAmount() {
        // Arrange
        String key = "user11";
        rateLimitingService.allowRequest(key, 30);

        // Act
        long availableTokens = rateLimitingService.getAvailableTokens(key);

        // Assert
        assertEquals(70, availableTokens, "Deben quedar 70 tokens después de consumir 30");
    }

    @Test
    @DisplayName("Obtener tokens de bucket inexistente - Debe retornar capacidad por defecto")
    void getAvailableTokens_WhenBucketNotExists_ShouldReturnDefaultCapacity() {
        // Arrange
        String key = "nonexistent_user";

        // Act
        long availableTokens = rateLimitingService.getAvailableTokens(key);

        // Assert
        assertEquals(100, availableTokens, "Bucket inexistente debe retornar capacidad por defecto");
    }

    // ==================== RESET BUCKET TESTS ====================

    @Test
    @DisplayName("Resetear bucket - Debe eliminar bucket del cache")
    void resetBucket_WhenCalled_ShouldRemoveBucket() {
        // Arrange
        String key = "user12";
        rateLimitingService.allowRequest(key, 50);

        // Act
        rateLimitingService.resetBucket(key);
        long availableTokens = rateLimitingService.getAvailableTokens(key);

        // Assert
        assertEquals(100, availableTokens, "Bucket reseteado debe tener capacidad completa");
    }

    @Test
    @DisplayName("Resetear bucket inexistente - Debe ejecutar sin error")
    void resetBucket_WhenBucketNotExists_ShouldNotThrowException() {
        // Arrange
        String key = "nonexistent_user";

        // Act & Assert
        assertDoesNotThrow(() -> rateLimitingService.resetBucket(key));
    }

    @Test
    @DisplayName("Resetear bucket null - Debe manejar gracefully")
    void resetBucket_WhenKeyNull_ShouldHandleGracefully() {
        // Act & Assert
        assertDoesNotThrow(() -> rateLimitingService.resetBucket(null));
    }

    // ==================== CLEAR ALL BUCKETS TESTS ====================

    @Test
    @DisplayName("Limpiar todos los buckets - Debe resetear todos los usuarios")
    void clearAllBuckets_ShouldResetAllUsers() {
        // Arrange
        rateLimitingService.allowRequest("user13", 50);
        rateLimitingService.allowRequest("user14", 60);
        rateLimitingService.allowRequest("user15", 70);

        // Act
        rateLimitingService.clearAllBuckets();

        // Verificar que todos tienen capacidad completa nuevamente
        long tokens1 = rateLimitingService.getAvailableTokens("user13");
        long tokens2 = rateLimitingService.getAvailableTokens("user14");
        long tokens3 = rateLimitingService.getAvailableTokens("user15");

        // Assert
        assertEquals(100, tokens1);
        assertEquals(100, tokens2);
        assertEquals(100, tokens3);
    }

    @Test
    @DisplayName("Limpiar buckets vacíos - Debe ejecutar sin error")
    void clearAllBuckets_WhenNoBuckets_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> rateLimitingService.clearAllBuckets());
    }

    // ==================== GET BUCKET STATS TESTS ====================

    @Test
    @DisplayName("Obtener estadísticas de bucket - Debe retornar información correcta")
    void getBucketStats_WhenBucketExists_ShouldReturnStats() {
        // Arrange
        String key = "user16";
        rateLimitingService.allowRequest(key, 40);

        // Act
        String stats = rateLimitingService.getBucketStats(key);

        // Assert
        assertNotNull(stats);
        assertTrue(stats.contains(key), "Stats deben incluir la key");
        assertTrue(stats.contains("60"), "Stats deben mostrar 60 tokens disponibles");
    }

    @Test
    @DisplayName("Obtener estadísticas de bucket inexistente - Debe retornar mensaje apropiado")
    void getBucketStats_WhenBucketNotExists_ShouldReturnNoDataMessage() {
        // Arrange
        String key = "nonexistent_user";

        // Act
        String stats = rateLimitingService.getBucketStats(key);

        // Assert
        assertNotNull(stats);
        assertTrue(stats.contains("No data"), "Debe indicar que no hay datos");
    }

    @Test
    @DisplayName("Obtener estadísticas con key null - Debe manejar gracefully")
    void getBucketStats_WhenKeyNull_ShouldHandleGracefully() {
        // Act
        String stats = rateLimitingService.getBucketStats(null);

        // Assert
        assertNotNull(stats);
    }

    // ==================== IS RATE LIMIT ENABLED TESTS ====================

    @Test
    @DisplayName("Verificar si rate limit está habilitado - Debe retornar true")
    void isRateLimitEnabled_WhenEnabled_ShouldReturnTrue() {
        // Arrange
        ReflectionTestUtils.setField(rateLimitingService, "rateLimitEnabled", true);

        // Act
        boolean result = rateLimitingService.isRateLimitEnabled();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Verificar si rate limit está deshabilitado - Debe retornar false")
    void isRateLimitEnabled_WhenDisabled_ShouldReturnFalse() {
        // Arrange
        ReflectionTestUtils.setField(rateLimitingService, "rateLimitEnabled", false);

        // Act
        boolean result = rateLimitingService.isRateLimitEnabled();

        // Assert
        assertFalse(result);
    }

    // ==================== INTEGRATION-LIKE TESTS ====================

    @Test
    @DisplayName("Escenario completo: Consumir, verificar, resetear y verificar de nuevo")
    void completeScenario_ConsumeCheckResetCheck_ShouldWorkCorrectly() {
        // Arrange
        String key = "user17";

        // Act & Assert
        // 1. Consumir tokens
        rateLimitingService.allowRequest(key, 80);
        assertEquals(20, rateLimitingService.getAvailableTokens(key));

        // 2. Intentar consumir más de los disponibles
        boolean overLimit = rateLimitingService.allowRequest(key, 30);
        assertFalse(overLimit, "No debe permitir consumir más de lo disponible");

        // 3. Resetear bucket
        rateLimitingService.resetBucket(key);

        // 4. Verificar que tiene capacidad completa
        assertEquals(100, rateLimitingService.getAvailableTokens(key));

        // 5. Debe permitir request ahora
        boolean afterReset = rateLimitingService.allowRequest(key, 30);
        assertTrue(afterReset, "Después de reset debe permitir requests");
    }

    @Test
    @DisplayName("Múltiples usuarios con auth y normal - Deben funcionar independientemente")
    void multipleUsersWithAuthAndNormal_ShouldWorkIndependently() {
        // Arrange
        String user1 = "user18";
        String user2 = "user19";

        // Act
        // Usuario 1: consumir auth y normal
        rateLimitingService.allowAuthRequest(user1);
        rateLimitingService.allowRequest(user1, 50);

        // Usuario 2: solo auth
        for (int i = 0; i < 10; i++) {
            rateLimitingService.allowAuthRequest(user2);
        }

        // Assert
        // Usuario 1 debe tener tokens disponibles en ambos
        assertTrue(rateLimitingService.allowAuthRequest(user1));
        assertTrue(rateLimitingService.allowRequest(user1));

        // Usuario 2: auth agotado, normal disponible
        assertFalse(rateLimitingService.allowAuthRequest(user2));
        assertTrue(rateLimitingService.allowRequest(user2));
    }

    @Test
    @DisplayName("Consumo gradual hasta el límite - Debe trackear correctamente")
    void gradualConsumptionToLimit_ShouldTrackCorrectly() {
        // Arrange
        String key = "user20";

        // Act & Assert
        for (int i = 100; i > 0; i -= 10) {
            boolean allowed = rateLimitingService.allowRequest(key, 10);
            assertTrue(allowed, "Request debe ser permitida mientras haya tokens");

            long available = rateLimitingService.getAvailableTokens(key);
            assertEquals(i - 10, available, "Tokens disponibles deben disminuir correctamente");
        }

        // Último intento debe fallar
        boolean lastAttempt = rateLimitingService.allowRequest(key);
        assertFalse(lastAttempt, "No debe permitir requests cuando no hay tokens");
    }
}

