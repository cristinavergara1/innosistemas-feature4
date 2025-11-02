package com.udea.innosistemas.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthResponseTest {

    @Test
    void testDefaultConstructor() {
        AuthResponse response = new AuthResponse();

        assertNull(response.getToken());
        assertNull(response.getRefreshToken());
        assertNull(response.getUserInfo());
    }

    @Test
    void testConstructorWithTokenAndUserInfo() {
        UserInfo userInfo = new UserInfo(); // Asume que UserInfo existe
        AuthResponse response = new AuthResponse("test-token", userInfo);

        assertEquals("test-token", response.getToken());
        assertEquals(userInfo, response.getUserInfo());
        assertNull(response.getRefreshToken());
    }

    @Test
    void testConstructorWithAllParameters() {
        UserInfo userInfo = new UserInfo();
        AuthResponse response = new AuthResponse("test-token", "refresh-token", userInfo);

        assertEquals("test-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(userInfo, response.getUserInfo());
    }

    @Test
    void testSettersAndGetters() {
        AuthResponse response = new AuthResponse();
        UserInfo userInfo = new UserInfo();

        response.setToken("new-token");
        response.setRefreshToken("new-refresh-token");
        response.setUserInfo(userInfo);

        assertEquals("new-token", response.getToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertEquals(userInfo, response.getUserInfo());
    }

    @Test
    void testSetTokenToNull() {
        AuthResponse response = new AuthResponse("initial-token", new UserInfo());
        response.setToken(null);

        assertNull(response.getToken());
    }

    @Test
    void testSetRefreshTokenToNull() {
        AuthResponse response = new AuthResponse("token", "refresh", new UserInfo());
        response.setRefreshToken(null);

        assertNull(response.getRefreshToken());
    }

    @Test
    void testSetUserInfoToNull() {
        AuthResponse response = new AuthResponse("token", new UserInfo());
        response.setUserInfo(null);

        assertNull(response.getUserInfo());
    }}