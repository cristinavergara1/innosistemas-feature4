package com.udea.innosistemas.service;

import com.udea.innosistemas.dto.TeamMember;
import com.udea.innosistemas.dto.UserInfo;
import com.udea.innosistemas.dto.UserPermissions;
import com.udea.innosistemas.entity.User;
import com.udea.innosistemas.entity.UserRole;
import com.udea.innosistemas.exception.AuthenticationException;
import com.udea.innosistemas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test unitario para UserQueryService utilizando el patrón AAA (Arrange-Act-Assert).
 * Verifica queries de usuarios, permisos y miembros de equipos.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserQueryService - Test unitario con patrón AAA")
class UserQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserQueryService userQueryService;

    private User testStudent;
    private User testProfessor;
    private User testAdmin;
    private User testTA;

    @BeforeEach
    void setUp() {
        // Setup test student
        testStudent = new User();
        testStudent.setId(1L);
        testStudent.setEmail("estudiante@udea.edu.co");
        testStudent.setPassword("hashedPassword");
        testStudent.setRole(UserRole.STUDENT);
        testStudent.setFirstName("Juan");
        testStudent.setLastName("Pérez");
        testStudent.setTeamId(10L);
        testStudent.setCourseId(100L);
        testStudent.setEnabled(true);
        testStudent.setAccountNonLocked(true);
        testStudent.setCreatedAt(LocalDateTime.now());
        testStudent.setUpdatedAt(LocalDateTime.now());

        // Setup test professor
        testProfessor = new User();
        testProfessor.setId(2L);
        testProfessor.setEmail("profesor@udea.edu.co");
        testProfessor.setPassword("hashedPassword");
        testProfessor.setRole(UserRole.PROFESSOR);
        testProfessor.setFirstName("María");
        testProfessor.setLastName("García");
        testProfessor.setCourseId(100L);
        testProfessor.setEnabled(true);
        testProfessor.setAccountNonLocked(true);
        testProfessor.setCreatedAt(LocalDateTime.now());
        testProfessor.setUpdatedAt(LocalDateTime.now());

        // Setup test admin
        testAdmin = new User();
        testAdmin.setId(3L);
        testAdmin.setEmail("admin@udea.edu.co");
        testAdmin.setPassword("hashedPassword");
        testAdmin.setRole(UserRole.ADMIN);
        testAdmin.setFirstName("Carlos");
        testAdmin.setLastName("Admin");
        testAdmin.setEnabled(true);
        testAdmin.setAccountNonLocked(true);
        testAdmin.setCreatedAt(LocalDateTime.now());
        testAdmin.setUpdatedAt(LocalDateTime.now());

        // Setup test TA
        testTA = new User();
        testTA.setId(4L);
        testTA.setEmail("ta@udea.edu.co");
        testTA.setPassword("hashedPassword");
        testTA.setRole(UserRole.TA);
        testTA.setFirstName("Ana");
        testTA.setLastName("López");
        testTA.setCourseId(100L);
        testTA.setEnabled(true);
        testTA.setAccountNonLocked(true);
        testTA.setCreatedAt(LocalDateTime.now());
        testTA.setUpdatedAt(LocalDateTime.now());

        SecurityContextHolder.setContext(securityContext);
    }

    // ==================== GET CURRENT USER TESTS ====================

    @Test
    @DisplayName("Obtener usuario actual estudiante - Debe retornar UserInfo correctamente")
    void getCurrentUser_WhenStudentAuthenticated_ShouldReturnUserInfo() {
        // Arrange
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testStudent.getEmail(), null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail(testStudent.getEmail())).thenReturn(Optional.of(testStudent));

        // Act
        UserInfo result = userQueryService.getCurrentUser();

        // Assert
        assertNotNull(result);
        assertEquals(testStudent.getId(), result.getId());
        assertEquals(testStudent.getEmail(), result.getEmail());
        assertEquals(testStudent.getRole(), result.getRole());
        assertEquals(testStudent.getFirstName(), result.getFirstName());
        assertEquals(testStudent.getLastName(), result.getLastName());
        assertEquals(testStudent.getTeamId(), result.getTeamId());
        assertEquals(testStudent.getCourseId(), result.getCourseId());
        verify(userRepository).findByEmail(testStudent.getEmail());
    }

    @Test
    @DisplayName("Obtener usuario actual sin autenticación - Debe lanzar AuthenticationException")
    void getCurrentUser_WhenNotAuthenticated_ShouldThrowAuthenticationException() {
        // Arrange
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "anonymousUser", null);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userQueryService.getCurrentUser();
        });

        assertEquals("Error al obtener información del usuario", exception.getMessage());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Obtener usuario actual no existente - Debe lanzar AuthenticationException")
    void getCurrentUser_WhenUserNotFound_ShouldThrowAuthenticationException() {
        // Arrange
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "noexiste@udea.edu.co", null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail("noexiste@udea.edu.co")).thenReturn(Optional.empty());

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userQueryService.getCurrentUser();
        });

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userRepository).findByEmail("noexiste@udea.edu.co");
    }

    @Test
    @DisplayName("Obtener usuario actual con null username - Debe lanzar AuthenticationException")
    void getCurrentUser_WhenUsernameNull_ShouldThrowAuthenticationException() {
        // Arrange
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(null, null);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> {
            userQueryService.getCurrentUser();
        });
    }

    @Test
    @DisplayName("Obtener usuario actual profesor - Debe retornar UserInfo con role PROFESSOR")
    void getCurrentUser_WhenProfessor_ShouldReturnCorrectRole() {
        // Arrange
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testProfessor.getEmail(), null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail(testProfessor.getEmail())).thenReturn(Optional.of(testProfessor));

        // Act
        UserInfo result = userQueryService.getCurrentUser();

        // Assert
        assertNotNull(result);
        assertEquals(UserRole.PROFESSOR, result.getRole());
        assertEquals(testProfessor.getEmail(), result.getEmail());
    }

    // ==================== GET USER PERMISSIONS TESTS ====================

    @Test
    @DisplayName("Obtener permisos de estudiante - Debe retornar permisos correctos")
    void getUserPermissions_WhenStudent_ShouldReturnStudentPermissions() {
        // Arrange
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testStudent.getEmail(), null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail(testStudent.getEmail())).thenReturn(Optional.of(testStudent));

        // Act
        UserPermissions result = userQueryService.getUserPermissions();

        // Assert
        assertNotNull(result);
        assertEquals(testStudent.getId(), result.getUserId());
        assertEquals(UserRole.STUDENT, result.getRole());
        assertEquals(testStudent.getTeamId(), result.getTeamId());
        assertEquals(testStudent.getCourseId(), result.getCourseId());

        // Verificar permisos de estudiante
        assertTrue(result.getPermissions().contains("team:read"));
        assertTrue(result.getPermissions().contains("course:read"));
        assertTrue(result.getPermissions().contains("project:submit"));
        assertTrue(result.getPermissions().contains("grade:view"));
        assertFalse(result.getPermissions().contains("user:create"));
    }

    @Test
    @DisplayName("Obtener permisos de profesor - Debe retornar permisos correctos")
    void getUserPermissions_WhenProfessor_ShouldReturnProfessorPermissions() {
        // Arrange
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testProfessor.getEmail(), null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail(testProfessor.getEmail())).thenReturn(Optional.of(testProfessor));

        // Act
        UserPermissions result = userQueryService.getUserPermissions();

        // Assert
        assertNotNull(result);
        assertEquals(UserRole.PROFESSOR, result.getRole());

        // Verificar permisos de profesor
        assertTrue(result.getPermissions().contains("user:read"));
        assertTrue(result.getPermissions().contains("team:read"));
        assertTrue(result.getPermissions().contains("team:update"));
        assertTrue(result.getPermissions().contains("course:create"));
        assertTrue(result.getPermissions().contains("course:read"));
        assertTrue(result.getPermissions().contains("course:update"));
        assertTrue(result.getPermissions().contains("notification:send"));
        assertTrue(result.getPermissions().contains("grade:assign"));
        assertFalse(result.getPermissions().contains("user:delete"));
    }

    @Test
    @DisplayName("Obtener permisos de admin - Debe retornar todos los permisos")
    void getUserPermissions_WhenAdmin_ShouldReturnAllPermissions() {
        // Arrange
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testAdmin.getEmail(), null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail(testAdmin.getEmail())).thenReturn(Optional.of(testAdmin));

        // Act
        UserPermissions result = userQueryService.getUserPermissions();

        // Assert
        assertNotNull(result);
        assertEquals(UserRole.ADMIN, result.getRole());

        // Verificar permisos de admin
        assertTrue(result.getPermissions().contains("user:create"));
        assertTrue(result.getPermissions().contains("user:read"));
        assertTrue(result.getPermissions().contains("user:update"));
        assertTrue(result.getPermissions().contains("user:delete"));
        assertTrue(result.getPermissions().contains("team:create"));
        assertTrue(result.getPermissions().contains("team:delete"));
        assertTrue(result.getPermissions().contains("course:create"));
        assertTrue(result.getPermissions().contains("course:delete"));
        assertTrue(result.getPermissions().contains("notification:send"));
        assertTrue(result.getPermissions().contains("system:configure"));
    }

    @Test
    @DisplayName("Obtener permisos de TA - Debe retornar permisos de asistente")
    void getUserPermissions_WhenTA_ShouldReturnTAPermissions() {
        // Arrange
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testTA.getEmail(), null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail(testTA.getEmail())).thenReturn(Optional.of(testTA));

        // Act
        UserPermissions result = userQueryService.getUserPermissions();

        // Assert
        assertNotNull(result);
        assertEquals(UserRole.TA, result.getRole());

        // Verificar permisos de TA
        assertTrue(result.getPermissions().contains("user:read"));
        assertTrue(result.getPermissions().contains("team:read"));
        assertTrue(result.getPermissions().contains("course:read"));
        assertTrue(result.getPermissions().contains("notification:send"));
        assertTrue(result.getPermissions().contains("grade:view"));
        assertFalse(result.getPermissions().contains("grade:assign"));
    }

    @Test
    @DisplayName("Obtener permisos sin autenticación - Debe lanzar AuthenticationException")
    void getUserPermissions_WhenNotAuthenticated_ShouldThrowAuthenticationException() {
        // Arrange
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "anonymousUser", null);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userQueryService.getUserPermissions();
        });

        assertEquals("Error al obtener permisos del usuario", exception.getMessage());
    }

    // ==================== GET TEAM MEMBERS TESTS ====================

    @Test
    @DisplayName("Estudiante obtiene miembros de su equipo - Debe retornar lista de miembros")
    void getTeamMembers_WhenStudentOwnTeam_ShouldReturnMembers() {
        // Arrange
        Long teamId = 10L;
        User teammate1 = new User();
        teammate1.setId(5L);
        teammate1.setEmail("compañero1@udea.edu.co");
        teammate1.setFirstName("Pedro");
        teammate1.setLastName("Sánchez");
        teammate1.setRole(UserRole.STUDENT);
        teammate1.setTeamId(teamId);

        User teammate2 = new User();
        teammate2.setId(6L);
        teammate2.setEmail("compañera2@udea.edu.co");
        teammate2.setFirstName("Laura");
        teammate2.setLastName("Martínez");
        teammate2.setRole(UserRole.STUDENT);
        teammate2.setTeamId(teamId);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testStudent.getEmail(), null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail(testStudent.getEmail())).thenReturn(Optional.of(testStudent));
        when(userRepository.findByTeamId(teamId)).thenReturn(Arrays.asList(testStudent, teammate1, teammate2));

        // Act
        List<TeamMember> result = userQueryService.getTeamMembers(teamId);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(userRepository).findByTeamId(teamId);
    }

    @Test
    @DisplayName("Estudiante intenta acceder a otro equipo - Debe lanzar AuthenticationException")
    void getTeamMembers_WhenStudentDifferentTeam_ShouldThrowAuthenticationException() {
        // Arrange
        Long otherTeamId = 20L;
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testStudent.getEmail(), null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail(testStudent.getEmail())).thenReturn(Optional.of(testStudent));

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userQueryService.getTeamMembers(otherTeamId);
        });

        assertEquals("No tienes permiso para ver este equipo", exception.getMessage());
        verify(userRepository, never()).findByTeamId(otherTeamId);
    }

    @Test
    @DisplayName("Estudiante sin equipo intenta acceder - Debe lanzar AuthenticationException")
    void getTeamMembers_WhenStudentNoTeam_ShouldThrowAuthenticationException() {
        // Arrange
        testStudent.setTeamId(null);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testStudent.getEmail(), null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail(testStudent.getEmail())).thenReturn(Optional.of(testStudent));

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userQueryService.getTeamMembers(10L);
        });

        assertEquals("No perteneces a ningún equipo", exception.getMessage());
    }

    @Test
    @DisplayName("Profesor obtiene miembros de cualquier equipo - Debe retornar lista")
    void getTeamMembers_WhenProfessor_ShouldReturnMembers() {
        // Arrange
        Long teamId = 10L;
        List<User> members = Arrays.asList(testStudent);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testProfessor.getEmail(), null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail(testProfessor.getEmail())).thenReturn(Optional.of(testProfessor));
        when(userRepository.findByTeamId(teamId)).thenReturn(members);

        // Act
        List<TeamMember> result = userQueryService.getTeamMembers(teamId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findByTeamId(teamId);
    }

    @Test
    @DisplayName("Admin obtiene miembros de cualquier equipo - Debe retornar lista")
    void getTeamMembers_WhenAdmin_ShouldReturnMembers() {
        // Arrange
        Long teamId = 15L;
        List<User> members = Arrays.asList(testStudent);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testAdmin.getEmail(), null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail(testAdmin.getEmail())).thenReturn(Optional.of(testAdmin));
        when(userRepository.findByTeamId(teamId)).thenReturn(members);

        // Act
        List<TeamMember> result = userQueryService.getTeamMembers(teamId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findByTeamId(teamId);
    }

    @Test
    @DisplayName("Obtener miembros de equipo vacío - Debe retornar lista vacía")
    void getTeamMembers_WhenTeamEmpty_ShouldReturnEmptyList() {
        // Arrange
        Long teamId = 10L;
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testProfessor.getEmail(), null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail(testProfessor.getEmail())).thenReturn(Optional.of(testProfessor));
        when(userRepository.findByTeamId(teamId)).thenReturn(Arrays.asList());

        // Act
        List<TeamMember> result = userQueryService.getTeamMembers(teamId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Obtener miembros sin autenticación - Debe lanzar AuthenticationException")
    void getTeamMembers_WhenNotAuthenticated_ShouldThrowAuthenticationException() {
        // Arrange
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "anonymousUser", null);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userQueryService.getTeamMembers(10L);
        });

        assertEquals("No hay usuario autenticado", exception.getMessage());
    }

    @Test
    @DisplayName("TA obtiene miembros de equipo - Debe retornar lista")
    void getTeamMembers_WhenTA_ShouldReturnMembers() {
        // Arrange
        Long teamId = 10L;
        List<User> members = Arrays.asList(testStudent);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testTA.getEmail(), null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail(testTA.getEmail())).thenReturn(Optional.of(testTA));
        when(userRepository.findByTeamId(teamId)).thenReturn(members);

        // Act
        List<TeamMember> result = userQueryService.getTeamMembers(teamId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Usuario no encontrado al obtener miembros - Debe lanzar AuthenticationException")
    void getTeamMembers_WhenUserNotFound_ShouldThrowAuthenticationException() {
        // Arrange
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "noexiste@udea.edu.co", null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail("noexiste@udea.edu.co")).thenReturn(Optional.empty());

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userQueryService.getTeamMembers(10L);
        });

        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    @Test
    @DisplayName("Error de base de datos al obtener miembros - Debe lanzar AuthenticationException")
    void getTeamMembers_WhenDatabaseError_ShouldThrowAuthenticationException() {
        // Arrange
        Long teamId = 10L;
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testProfessor.getEmail(), null);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findByEmail(testProfessor.getEmail())).thenReturn(Optional.of(testProfessor));
        when(userRepository.findByTeamId(teamId)).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userQueryService.getTeamMembers(teamId);
        });

        assertEquals("Error al obtener miembros del equipo", exception.getMessage());
    }
}

