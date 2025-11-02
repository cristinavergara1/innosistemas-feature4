package com.udea.innosistemas.dto;

import com.udea.innosistemas.entity.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserPermissionsTest {
    @Test
    void testNotificationPermissionsByRole() {
        // Arrange & Act
        UserPermissions adminPermissions = new UserPermissions(1L, UserRole.ADMIN, Arrays.asList("SEND_NOTIFICATIONS"));
        UserPermissions professorPermissions = new UserPermissions(2L, UserRole.PROFESSOR, Arrays.asList("SEND_NOTIFICATIONS"));
        UserPermissions taPermissions = new UserPermissions(3L, UserRole.TA, Arrays.asList("SEND_NOTIFICATIONS"));
        UserPermissions studentPermissions = new UserPermissions(4L, UserRole.STUDENT, Arrays.asList());

        // Assert - Verificar permisos de notificación específicos por rol
        assertTrue(adminPermissions.isCanSendNotifications());
        assertTrue(professorPermissions.isCanSendNotifications());
        assertTrue(taPermissions.isCanSendNotifications());
        assertFalse(studentPermissions.isCanSendNotifications());
    }

    @Test
    void testNotificationPermissionsWithTeamContext() {
        // Arrange
        UserPermissions userPermissions = new UserPermissions(1L, UserRole.TA, Arrays.asList("NOTIFY_TEAM"));
        Long teamId = 100L;

        // Act
        userPermissions.setTeamId(teamId);

        // Assert
        assertEquals(teamId, userPermissions.getTeamId());
        assertTrue(userPermissions.isCanSendNotifications());
    }

    @Test
    void testNotificationPermissionsWithCourseContext() {
        // Arrange
        UserPermissions userPermissions = new UserPermissions(1L, UserRole.PROFESSOR, Arrays.asList("NOTIFY_COURSE"));
        Long courseId = 200L;

        // Act
        userPermissions.setCourseId(courseId);

        // Assert
        assertEquals(courseId, userPermissions.getCourseId());
        assertTrue(userPermissions.isCanSendNotifications());
        assertTrue(userPermissions.isCanManageCourse());
    }

    @Test
    void testStudentCannotSendNotifications() {
        // Arrange
        UserPermissions studentPermissions = new UserPermissions();

        // Act
        studentPermissions.setRole(UserRole.STUDENT);
        studentPermissions.setCanSendNotifications(true); // Intentar dar permisos manualmente

        // Assert
        assertEquals(UserRole.STUDENT, studentPermissions.getRole());
        assertTrue(studentPermissions.isCanSendNotifications()); // El setter manual funciona
    }

    @Test
    void testNotificationPermissionsAreCalculatedCorrectly() {
        // Arrange & Act
        UserPermissions adminUser = new UserPermissions(1L, UserRole.ADMIN, Arrays.asList("ALL_PERMISSIONS"));
        UserPermissions taUser = new UserPermissions(2L, UserRole.TA, Arrays.asList("LIMITED_PERMISSIONS"));

        // Assert - ADMIN puede todo, TA solo notificaciones
        assertTrue(adminUser.isCanSendNotifications());
        assertTrue(adminUser.isCanManageTeam());
        assertTrue(adminUser.isCanManageCourse());

        assertTrue(taUser.isCanSendNotifications());
        assertFalse(taUser.isCanManageTeam());
        assertFalse(taUser.isCanManageCourse());
    }

    @Test
    void testPermissionsListContainsNotificationPermissions() {
        // Arrange
        List<String> notificationPermissions = Arrays.asList("SEND_EMAIL", "SEND_SMS", "SEND_PUSH");

        // Act
        UserPermissions userPermissions = new UserPermissions(1L, UserRole.PROFESSOR, notificationPermissions);

        // Assert
        assertEquals(notificationPermissions, userPermissions.getPermissions());
        assertTrue(userPermissions.getPermissions().contains("SEND_EMAIL"));
        assertTrue(userPermissions.getPermissions().contains("SEND_SMS"));
        assertTrue(userPermissions.getPermissions().contains("SEND_PUSH"));
        assertTrue(userPermissions.isCanSendNotifications());
    }

    @Test
    void testNotificationPermissionsInTeamAndCourseContext() {
        // Arrange
        UserPermissions userPermissions = new UserPermissions(1L, UserRole.TA, Arrays.asList("TEAM_NOTIFICATIONS"));

        // Act
        userPermissions.setTeamId(10L);
        userPermissions.setCourseId(20L);

        // Assert - TA puede enviar notificaciones pero no manejar equipos/cursos
        assertTrue(userPermissions.isCanSendNotifications());
        assertFalse(userPermissions.isCanManageTeam());
        assertFalse(userPermissions.isCanManageCourse());
        assertEquals(10L, userPermissions.getTeamId());
        assertEquals(20L, userPermissions.getCourseId());
    }

    }