package com.udea.innosistemas.dto;

import com.udea.innosistemas.entity.Team;
import com.udea.innosistemas.entity.User;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TeamDTOTest {

    @Test
    void testDefaultConstructor() {
        // Arrange & Act
        TeamDTO teamDTO = new TeamDTO();

        // Assert
        assertNull(teamDTO.getId());
        assertNull(teamDTO.getNombre());
        assertNull(teamDTO.getDescripcion());
        assertNull(teamDTO.getFechaCreacion());
        assertNull(teamDTO.getFechaLimite());
        assertNull(teamDTO.getCourseId());
        assertFalse(teamDTO.isActivo());
        assertNull(teamDTO.getMaxMiembros());
        assertNull(teamDTO.getMiembros());
        assertFalse(teamDTO.isVencido());
        assertFalse(teamDTO.isPuedeAgregarMiembros());
    }

    @Test
    void testConstructorFromTeamEntity() {
        // Arrange
        Team team = new Team();
        team.setId(1L);
        team.setNombre("Equipo Desarrollo Web");
        team.setDescripcion("Proyecto final de programación web");
        team.setFechaCreacion(LocalDateTime.now().minusDays(5));
        team.setFechaLimite(LocalDateTime.now().plusDays(30));
        team.setCourseId(101L);
        team.setActivo(true);
        team.setMaxMiembros(4);


        // Act
        TeamDTO dto = new TeamDTO(team);

        // Assert
        assertEquals(team.getId(), dto.getId());
        assertEquals(team.getNombre(), dto.getNombre());
        assertEquals(team.getDescripcion(), dto.getDescripcion());
        assertEquals(team.getFechaCreacion(), dto.getFechaCreacion());
        assertEquals(team.getFechaLimite(), dto.getFechaLimite());
        assertEquals(team.getCourseId(), dto.getCourseId());
        assertEquals(team.isActivo(), dto.isActivo());
        assertEquals(team.getMaxMiembros(), dto.getMaxMiembros());
    }

    @Test
    void testConstructorFromTeamWithUserList() {
        // Arrange
        Team team = new Team();
        team.setId(2L);
        team.setNombre("Equipo Base de Datos");
        team.setCourseId(102L);
        team.setMaxMiembros(3);

        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("carlos.rodriguez@udea.edu.co");

        User user2 = new User();
        user2.setId(2L);
        user2.setEmail("ana.garcia@udea.edu.co");

        List<User> miembros = Arrays.asList(user1, user2);

        // Act
        TeamDTO dto = new TeamDTO(team, miembros);

        // Assert
        assertEquals(team.getId(), dto.getId());
        assertEquals(team.getNombre(), dto.getNombre());
        assertEquals(team.getCourseId(), dto.getCourseId());
        assertNotNull(dto.getMiembros());
        assertEquals(2, dto.getMiembros().size());
    }

    @Test
    void testConstructorWithNullMembersList() {
        // Arrange
        Team team = new Team();
        team.setId(3L);
        team.setNombre("Equipo Sin Miembros");
        team.setCourseId(103L);

        // Act
        TeamDTO dto = new TeamDTO(team, null);

        // Assert
        assertEquals(team.getId(), dto.getId());
        assertEquals(team.getNombre(), dto.getNombre());
        assertNotNull(dto.getMiembros());
        assertTrue(dto.getMiembros().isEmpty());
    }

    @Test
    void testSettersAndGettersForProgrammingTeam() {
        // Arrange
        TeamDTO dto = new TeamDTO();
        Long expectedId = 5L;
        String expectedNombre = "Equipo Algoritmos Avanzados";
        String expectedDescripcion = "Implementación de algoritmos de ordenamiento";
        LocalDateTime expectedFechaCreacion = LocalDateTime.now().minusDays(2);
        LocalDateTime expectedFechaLimite = LocalDateTime.now().plusDays(45);
        Long expectedCourseId = 201L;
        boolean expectedActivo = true;
        Integer expectedMaxMiembros = 5;
        boolean expectedVencido = false;
        boolean expectedPuedeAgregar = true;

        // Act
        dto.setId(expectedId);
        dto.setNombre(expectedNombre);
        dto.setDescripcion(expectedDescripcion);
        dto.setFechaCreacion(expectedFechaCreacion);
        dto.setFechaLimite(expectedFechaLimite);
        dto.setCourseId(expectedCourseId);
        dto.setActivo(expectedActivo);
        dto.setMaxMiembros(expectedMaxMiembros);
        dto.setVencido(expectedVencido);
        dto.setPuedeAgregarMiembros(expectedPuedeAgregar);

        // Assert
        assertEquals(expectedId, dto.getId());
        assertEquals(expectedNombre, dto.getNombre());
        assertEquals(expectedDescripcion, dto.getDescripcion());
        assertEquals(expectedFechaCreacion, dto.getFechaCreacion());
        assertEquals(expectedFechaLimite, dto.getFechaLimite());
        assertEquals(expectedCourseId, dto.getCourseId());
        assertTrue(dto.isActivo());
        assertEquals(expectedMaxMiembros, dto.getMaxMiembros());
        assertFalse(dto.isVencido());
        assertTrue(dto.isPuedeAgregarMiembros());
    }

    @Test
    void testActiveTeamForSoftwareEngineering() {
        // Arrange
        TeamDTO dto = new TeamDTO();
        String teamName = "Equipo Ingeniería de Software";
        Long courseId = 301L;

        // Act
        dto.setNombre(teamName);
        dto.setCourseId(courseId);
        dto.setActivo(true);
        dto.setMaxMiembros(6);
        dto.setPuedeAgregarMiembros(true);

        // Assert
        assertEquals(teamName, dto.getNombre());
        assertEquals(courseId, dto.getCourseId());
        assertTrue(dto.isActivo());
        assertEquals(6, dto.getMaxMiembros());
        assertTrue(dto.isPuedeAgregarMiembros());
    }

    @Test
    void testExpiredTeam() {
        // Arrange
        TeamDTO dto = new TeamDTO();
        LocalDateTime pastDeadline = LocalDateTime.now().minusDays(10);

        // Act
        dto.setNombre("Equipo Proyecto Vencido");
        dto.setFechaLimite(pastDeadline);
        dto.setVencido(true);
        dto.setActivo(false);
        dto.setPuedeAgregarMiembros(false);

        // Assert
        assertEquals("Equipo Proyecto Vencido", dto.getNombre());
        assertEquals(pastDeadline, dto.getFechaLimite());
        assertTrue(dto.isVencido());
        assertFalse(dto.isActivo());
        assertFalse(dto.isPuedeAgregarMiembros());
    }

    @Test
    void testTeamWithFullCapacity() {
        // Arrange
        TeamDTO dto = new TeamDTO();
        Integer maxCapacity = 3;

        // Act
        dto.setNombre("Equipo Completo");
        dto.setMaxMiembros(maxCapacity);
        dto.setPuedeAgregarMiembros(false);
        dto.setActivo(true);

        // Assert
        assertEquals("Equipo Completo", dto.getNombre());
        assertEquals(maxCapacity, dto.getMaxMiembros());
        assertFalse(dto.isPuedeAgregarMiembros());
        assertTrue(dto.isActivo());
    }


    @Test
    void testTeamCreationTimestamp() {
        // Arrange
        TeamDTO dto = new TeamDTO();
        LocalDateTime creationTime = LocalDateTime.now().minusHours(2);
        LocalDateTime deadline = LocalDateTime.now().plusWeeks(8);

        // Act
        dto.setNombre("Equipo Nuevos Algoritmos");
        dto.setFechaCreacion(creationTime);
        dto.setFechaLimite(deadline);
        dto.setActivo(true);

        // Assert
        assertEquals("Equipo Nuevos Algoritmos", dto.getNombre());
        assertEquals(creationTime, dto.getFechaCreacion());
        assertEquals(deadline, dto.getFechaLimite());
        assertTrue(dto.isActivo());
        assertTrue(deadline.isAfter(creationTime));
    }

    @Test
    void testTeamForSpecificCourse() {
        // Arrange
        TeamDTO dto = new TeamDTO();
        Long specificCourseId = 501L;
        String courseSpecificDescription = "Proyecto integrador para Arquitectura de Software";

        // Act
        dto.setNombre("Equipo Arquitectura");
        dto.setDescripcion(courseSpecificDescription);
        dto.setCourseId(specificCourseId);
        dto.setMaxMiembros(4);

        // Assert
        assertEquals("Equipo Arquitectura", dto.getNombre());
        assertEquals(courseSpecificDescription, dto.getDescripcion());
        assertEquals(specificCourseId, dto.getCourseId());
        assertEquals(4, dto.getMaxMiembros());
    }

    @Test
    void testTeamStatusUpdate() {
        // Arrange
        TeamDTO dto = new TeamDTO();
        dto.setActivo(true);
        dto.setPuedeAgregarMiembros(true);

        // Act
        dto.setActivo(false);
        dto.setPuedeAgregarMiembros(false);
        dto.setVencido(true);

        // Assert
        assertFalse(dto.isActivo());
        assertFalse(dto.isPuedeAgregarMiembros());
        assertTrue(dto.isVencido());
    }
}