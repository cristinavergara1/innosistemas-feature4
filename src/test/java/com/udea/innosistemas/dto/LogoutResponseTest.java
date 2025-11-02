package com.udea.innosistemas.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogoutResponseTest {

    @Test
    void testDefaultConstructor() {
        // Arrange & Act
        LogoutResponse logoutResponse = new LogoutResponse();

        // Assert
        assertFalse(logoutResponse.isSuccess());
        assertNull(logoutResponse.getMessage());
    }

    @Test
    void testParameterizedConstructorWithSuccessTrue() {
        // Arrange
        boolean expectedSuccess = true;
        String expectedMessage = "Logout exitoso";

        // Act
        LogoutResponse logoutResponse = new LogoutResponse(expectedSuccess, expectedMessage);

        // Assert
        assertTrue(logoutResponse.isSuccess());
        assertEquals(expectedMessage, logoutResponse.getMessage());
    }

    @Test
    void testParameterizedConstructorWithSuccessFalse() {
        // Arrange
        boolean expectedSuccess = false;
        String expectedMessage = "Error en logout";

        // Act
        LogoutResponse logoutResponse = new LogoutResponse(expectedSuccess, expectedMessage);

        // Assert
        assertFalse(logoutResponse.isSuccess());
        assertEquals(expectedMessage, logoutResponse.getMessage());
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        LogoutResponse logoutResponse = new LogoutResponse();
        boolean expectedSuccess = true;
        String expectedMessage = "Sesión cerrada correctamente";

        // Act
        logoutResponse.setSuccess(expectedSuccess);
        logoutResponse.setMessage(expectedMessage);

        // Assert
        assertEquals(expectedSuccess, logoutResponse.isSuccess());
        assertEquals(expectedMessage, logoutResponse.getMessage());
    }

    @Test
    void testSuccessfulLogoutScenario() {
        // Arrange
        boolean success = true;
        String message = "Usuario desconectado exitosamente";

        // Act
        LogoutResponse response = new LogoutResponse(success, message);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertNotNull(response.getMessage());
    }

    @Test
    void testFailedLogoutScenario() {
        // Arrange
        boolean success = false;
        String message = "Error al cerrar sesión";

        // Act
        LogoutResponse response = new LogoutResponse(success, message);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertNotNull(response.getMessage());
    }

    @Test
    void testUpdateSuccessFlag() {
        // Arrange
        LogoutResponse response = new LogoutResponse(false, "Initial message");

        // Act
        response.setSuccess(true);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Initial message", response.getMessage());
    }

    @Test
    void testUpdateMessage() {
        // Arrange
        LogoutResponse response = new LogoutResponse(true, "Original message");
        String newMessage = "Updated message";

        // Act
        response.setMessage(newMessage);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(newMessage, response.getMessage());
    }

    @Test
    void testNullMessageHandling() {
        // Arrange
        boolean success = true;
        String nullMessage = null;

        // Act
        LogoutResponse response = new LogoutResponse(success, nullMessage);

        // Assert
        assertTrue(response.isSuccess());
        assertNull(response.getMessage());
    }

    @Test
    void testEmptyMessageHandling() {
        // Arrange
        boolean success = false;
        String emptyMessage = "";

        // Act
        LogoutResponse response = new LogoutResponse(success, emptyMessage);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("", response.getMessage());
        assertTrue(response.getMessage().isEmpty());
    }
}