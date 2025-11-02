package com.udea.innosistemas.dto;

import com.udea.innosistemas.entity.Notification;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class NotificationDTOTest {

    @Test
    void testDefaultConstructor() {
        // Arrange & Act
        NotificationDTO notificationDTO = new NotificationDTO();

        // Assert
        assertNull(notificationDTO.getId());
        assertNull(notificationDTO.getUserId());
        assertNull(notificationDTO.getMensaje());
        assertNull(notificationDTO.getTipo());
        assertFalse(notificationDTO.isLeida());
        assertNull(notificationDTO.getFechaCreacion());
        assertNull(notificationDTO.getFechaLectura());
        assertNull(notificationDTO.getTeamId());
        assertNull(notificationDTO.getCursoId());
        assertNull(notificationDTO.getPrioridad());
        assertNull(notificationDTO.getEnlace());
        assertNull(notificationDTO.getMetadata());
        assertNull(notificationDTO.getExpiraEn());
    }

    @Test
    void testConstructorFromNotificationEntity() {
        // Arrange
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(100L);
        notification.setMensaje("Nueva tarea asignada en Programación Web");
        notification.setTipo("TASK_ASSIGNMENT");
        notification.setLeida(false);
        notification.setFechaCreacion(LocalDateTime.now());
        notification.setTeamId(5L);
        notification.setCursoId(10L);
        notification.setPrioridad(Notification.NotificationPriority.ALTA); // Usar el enum interno
        notification.setEnlace("https://innosistemas.udea.edu.co/tasks/123");
        notification.setMetadata("{\"taskId\":123,\"course\":\"Web Programming\"}");
        notification.setExpiraEn(LocalDateTime.now().plusDays(7));

        // Act
        NotificationDTO dto = new NotificationDTO(notification);

        // Assert
        assertEquals(notification.getId(), dto.getId());
        assertEquals(notification.getUserId(), dto.getUserId());
        assertEquals(notification.getMensaje(), dto.getMensaje());
        assertEquals(notification.getTipo(), dto.getTipo());
        assertEquals(notification.isLeida(), dto.isLeida());
        assertEquals(notification.getFechaCreacion(), dto.getFechaCreacion());
        assertEquals(notification.getTeamId(), dto.getTeamId());
        assertEquals(notification.getCursoId(), dto.getCursoId());
        assertEquals("ALTA", dto.getPrioridad());
        assertEquals(notification.getEnlace(), dto.getEnlace());
        assertEquals(notification.getMetadata(), dto.getMetadata());
        assertEquals(notification.getExpiraEn(), dto.getExpiraEn());
    }

    @Test
    void testConstructorFromNotificationWithNullPriority() {
        // Arrange
        Notification notification = new Notification();
        notification.setId(2L);
        notification.setUserId(200L);
        notification.setMensaje("Recordatorio de evaluación");
        notification.setPrioridad(null);

        // Act
        NotificationDTO dto = new NotificationDTO(notification);

        // Assert
        assertEquals(notification.getId(), dto.getId());
        assertEquals(notification.getUserId(), dto.getUserId());
        assertEquals(notification.getMensaje(), dto.getMensaje());
        assertNull(dto.getPrioridad());
    }

    @Test
    void testSettersAndGettersForStudentNotification() {
        // Arrange
        NotificationDTO dto = new NotificationDTO();
        Long expectedId = 3L;
        Long expectedUserId = 300L;
        String expectedMensaje = "Su entrega ha sido evaluada";
        String expectedTipo = "GRADE_AVAILABLE";
        boolean expectedLeida = true;
        LocalDateTime expectedFechaCreacion = LocalDateTime.now().minusHours(2);
        LocalDateTime expectedFechaLectura = LocalDateTime.now();
        Long expectedTeamId = 8L;
        Long expectedCursoId = 15L;
        String expectedPrioridad = "MEDIA";
        String expectedEnlace = "https://innosistemas.udea.edu.co/grades/456";
        String expectedMetadata = "{\"gradeId\":456,\"score\":85}";
        LocalDateTime expectedExpiraEn = LocalDateTime.now().plusDays(30);

        // Act
        dto.setId(expectedId);
        dto.setUserId(expectedUserId);
        dto.setMensaje(expectedMensaje);
        dto.setTipo(expectedTipo);
        dto.setLeida(expectedLeida);
        dto.setFechaCreacion(expectedFechaCreacion);
        dto.setFechaLectura(expectedFechaLectura);
        dto.setTeamId(expectedTeamId);
        dto.setCursoId(expectedCursoId);
        dto.setPrioridad(expectedPrioridad);
        dto.setEnlace(expectedEnlace);
        dto.setMetadata(expectedMetadata);
        dto.setExpiraEn(expectedExpiraEn);

        // Assert
        assertEquals(expectedId, dto.getId());
        assertEquals(expectedUserId, dto.getUserId());
        assertEquals(expectedMensaje, dto.getMensaje());
        assertEquals(expectedTipo, dto.getTipo());
        assertTrue(dto.isLeida());
        assertEquals(expectedFechaCreacion, dto.getFechaCreacion());
        assertEquals(expectedFechaLectura, dto.getFechaLectura());
        assertEquals(expectedTeamId, dto.getTeamId());
        assertEquals(expectedCursoId, dto.getCursoId());
        assertEquals(expectedPrioridad, dto.getPrioridad());
        assertEquals(expectedEnlace, dto.getEnlace());
        assertEquals(expectedMetadata, dto.getMetadata());
        assertEquals(expectedExpiraEn, dto.getExpiraEn());
    }

    @Test
    void testTaskDeadlineNotification() {
        // Arrange
        NotificationDTO dto = new NotificationDTO();
        String taskDeadlineMessage = "Entrega de proyecto final vence en 24 horas";
        String taskType = "TASK_DEADLINE";

        // Act
        dto.setMensaje(taskDeadlineMessage);
        dto.setTipo(taskType);
        dto.setPrioridad("ALTA");
        dto.setLeida(false);

        // Assert
        assertEquals(taskDeadlineMessage, dto.getMensaje());
        assertEquals(taskType, dto.getTipo());
        assertEquals("ALTA", dto.getPrioridad());
        assertFalse(dto.isLeida());
    }

    @Test
    void testTeamNotification() {
        // Arrange
        NotificationDTO dto = new NotificationDTO();
        Long teamId = 12L;
        String teamMessage = "Nuevo miembro agregado al equipo";

        // Act
        dto.setTeamId(teamId);
        dto.setMensaje(teamMessage);
        dto.setTipo("TEAM_UPDATE");
        dto.setPrioridad("BAJA");

        // Assert
        assertEquals(teamId, dto.getTeamId());
        assertEquals(teamMessage, dto.getMensaje());
        assertEquals("TEAM_UPDATE", dto.getTipo());
        assertEquals("BAJA", dto.getPrioridad());
    }

    @Test
    void testCourseAnnouncementNotification() {
        // Arrange
        NotificationDTO dto = new NotificationDTO();
        Long cursoId = 25L;
        String announcement = "Clase cancelada para el próximo viernes";

        // Act
        dto.setCursoId(cursoId);
        dto.setMensaje(announcement);
        dto.setTipo("COURSE_ANNOUNCEMENT");
        dto.setPrioridad("MEDIA");

        // Assert
        assertEquals(cursoId, dto.getCursoId());
        assertEquals(announcement, dto.getMensaje());
        assertEquals("COURSE_ANNOUNCEMENT", dto.getTipo());
        assertEquals("MEDIA", dto.getPrioridad());
    }

    @Test
    void testNotificationWithLink() {
        // Arrange
        NotificationDTO dto = new NotificationDTO();
        String enlace = "https://innosistemas.udea.edu.co/assignment/789";
        String metadata = "{\"assignmentId\":789,\"dueDate\":\"2024-12-15\"}";

        // Act
        dto.setEnlace(enlace);
        dto.setMetadata(metadata);
        dto.setMensaje("Nueva asignación disponible");
        dto.setTipo("NEW_ASSIGNMENT");

        // Assert
        assertEquals(enlace, dto.getEnlace());
        assertEquals(metadata, dto.getMetadata());
        assertEquals("Nueva asignación disponible", dto.getMensaje());
        assertEquals("NEW_ASSIGNMENT", dto.getTipo());
    }

    @Test
    void testExpiringNotification() {
        // Arrange
        NotificationDTO dto = new NotificationDTO();
        LocalDateTime expiration = LocalDateTime.now().plusDays(3);

        // Act
        dto.setExpiraEn(expiration);
        dto.setMensaje("Recordatorio: Evaluación disponible");
        dto.setTipo("EVALUATION_REMINDER");

        // Assert
        assertEquals(expiration, dto.getExpiraEn());
        assertEquals("Recordatorio: Evaluación disponible", dto.getMensaje());
        assertEquals("EVALUATION_REMINDER", dto.getTipo());
    }

    @Test
    void testReadNotificationUpdate() {
        // Arrange
        NotificationDTO dto = new NotificationDTO();
        LocalDateTime readTime = LocalDateTime.now();

        // Act
        dto.setLeida(true);
        dto.setFechaLectura(readTime);

        // Assert
        assertTrue(dto.isLeida());
        assertEquals(readTime, dto.getFechaLectura());
    }

    @Test
    void testHighPriorityNotification() {
        // Arrange
        NotificationDTO dto = new NotificationDTO();
        String urgentMessage = "Cambio de fecha de examen final";

        // Act
        dto.setMensaje(urgentMessage);
        dto.setPrioridad("ALTA");
        dto.setTipo("URGENT_ANNOUNCEMENT");

        // Assert
        assertEquals(urgentMessage, dto.getMensaje());
        assertEquals("ALTA", dto.getPrioridad());
        assertEquals("URGENT_ANNOUNCEMENT", dto.getTipo());
    }
}