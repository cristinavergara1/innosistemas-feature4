package com.udea.innosistemas.dto;

import com.udea.innosistemas.entity.User;
import com.udea.innosistemas.entity.UserRole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserInfoTest {

    @Test
    void testDefaultConstructor() {
        // Arrange & Act
        UserInfo userInfo = new UserInfo();

        // Assert
        assertNull(userInfo.getId());
        assertNull(userInfo.getEmail());
        assertNull(userInfo.getRole());
        assertNull(userInfo.getTeamId());
        assertNull(userInfo.getCourseId());
        assertNull(userInfo.getFirstName());
        assertNull(userInfo.getLastName());
        assertNull(userInfo.getFullName());
    }

    @Test
    void testConstructorWithBasicInfo() {
        // Arrange
        Long expectedId = 1L;
        String expectedEmail = "test@udea.edu.co";
        UserRole expectedRole = UserRole.STUDENT;

        // Act
        UserInfo userInfo = new UserInfo(expectedId, expectedEmail, expectedRole);

        // Assert
        assertEquals(expectedId, userInfo.getId());
        assertEquals(expectedEmail, userInfo.getEmail());
        assertEquals(expectedRole, userInfo.getRole());
        assertNull(userInfo.getTeamId());
        assertNull(userInfo.getCourseId());
    }

    @Test
    void testConstructorWithTeamAndCourse() {
        // Arrange
        Long expectedId = 2L;
        String expectedEmail = "student@udea.edu.co";
        UserRole expectedRole = UserRole.STUDENT;
        Long expectedTeamId = 10L;
        Long expectedCourseId = 5L;

        // Act
        UserInfo userInfo = new UserInfo(expectedId, expectedEmail, expectedRole, expectedTeamId, expectedCourseId);

        // Assert
        assertEquals(expectedId, userInfo.getId());
        assertEquals(expectedEmail, userInfo.getEmail());
        assertEquals(expectedRole, userInfo.getRole());
        assertEquals(expectedTeamId, userInfo.getTeamId());
        assertEquals(expectedCourseId, userInfo.getCourseId());
    }

    @Test
    void testConstructorFromUserEntity() {
        // Arrange
        User user = new User();
        user.setId(3L);
        user.setEmail("profesor@udea.edu.co");
        user.setRole(UserRole.PROFESSOR);
        user.setTeamId(15L);
        user.setCourseId(8L);
        user.setFirstName("María");
        user.setLastName("González");


        // Act
        UserInfo userInfo = new UserInfo(user);

        // Assert
        assertEquals(user.getId(), userInfo.getId());
        assertEquals(user.getEmail(), userInfo.getEmail());
        assertEquals(user.getRole(), userInfo.getRole());
        assertEquals(user.getTeamId(), userInfo.getTeamId());
        assertEquals(user.getCourseId(), userInfo.getCourseId());
        assertEquals(user.getFirstName(), userInfo.getFirstName());
        assertEquals(user.getLastName(), userInfo.getLastName());

    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        UserInfo userInfo = new UserInfo();
        Long expectedId = 4L;
        String expectedEmail = "admin@udea.edu.co";
        UserRole expectedRole = UserRole.ADMIN;
        Long expectedTeamId = 20L;
        Long expectedCourseId = 12L;
        String expectedFirstName = "Carlos";
        String expectedLastName = "Pérez";


        // Act
        userInfo.setId(expectedId);
        userInfo.setEmail(expectedEmail);
        userInfo.setRole(expectedRole);
        userInfo.setTeamId(expectedTeamId);
        userInfo.setCourseId(expectedCourseId);
        userInfo.setFirstName(expectedFirstName);
        userInfo.setLastName(expectedLastName);


        // Assert
        assertEquals(expectedId, userInfo.getId());
        assertEquals(expectedEmail, userInfo.getEmail());
        assertEquals(expectedRole, userInfo.getRole());
        assertEquals(expectedTeamId, userInfo.getTeamId());
        assertEquals(expectedCourseId, userInfo.getCourseId());
        assertEquals(expectedFirstName, userInfo.getFirstName());
        assertEquals(expectedLastName, userInfo.getLastName());

    }

    @Test
    void testAdminRole() {
        // Arrange
        Long adminId = 100L;
        String adminEmail = "admin@sistema.com";
        UserRole adminRole = UserRole.ADMIN;

        // Act
        UserInfo adminInfo = new UserInfo(adminId, adminEmail, adminRole);

        // Assert
        assertEquals(adminRole, adminInfo.getRole());
        assertEquals(adminId, adminInfo.getId());
        assertEquals(adminEmail, adminInfo.getEmail());
    }

    @Test
    void testProfessorRole() {
        // Arrange
        Long professorId = 200L;
        String professorEmail = "profesor@udea.edu.co";
        UserRole professorRole = UserRole.PROFESSOR;

        // Act
        UserInfo professorInfo = new UserInfo(professorId, professorEmail, professorRole);

        // Assert
        assertEquals(professorRole, professorInfo.getRole());
        assertEquals(professorId, professorInfo.getId());
        assertEquals(professorEmail, professorInfo.getEmail());
    }

    @Test
    void testStudentWithTeamAndCourse() {
        // Arrange
        UserInfo studentInfo = new UserInfo();
        Long studentId = 300L;
        Long teamId = 5L;
        Long courseId = 3L;

        // Act
        studentInfo.setId(studentId);
        studentInfo.setRole(UserRole.STUDENT);
        studentInfo.setTeamId(teamId);
        studentInfo.setCourseId(courseId);

        // Assert
        assertEquals(UserRole.STUDENT, studentInfo.getRole());
        assertEquals(teamId, studentInfo.getTeamId());
        assertEquals(courseId, studentInfo.getCourseId());
        assertNotNull(studentInfo.getId());
    }
}