package com.udea.innosistemas.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreateNotificationRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testCreateTareaVencimientoAlert() {
        LocalDateTime deadline = LocalDateTime.now().plusHours(24);

        CreateNotificationRequest notification = CreateNotificationRequest.builder()
                .userId(123L)
                .tipo("ALERTA_VENCIMIENTO")
                .mensaje(" Tarea 'Entrega Final Proyecto' vence en 24 horas")
                .teamId(15L)
                .cursoId(456L)
                .prioridad("ALTA")
                .enlace("https://innosistemas.udea.edu.co/tareas/789")
                .expiraEn(deadline)
                .metadata("{\"tareaId\": 789, \"horasRestantes\": 24}")
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations =
            validator.validate(notification);

        assertTrue(violations.isEmpty());
        assertEquals("ALERTA_VENCIMIENTO", notification.getTipo());
        assertEquals("ALTA", notification.getPrioridad());
        assertTrue(notification.getMensaje().contains("24 horas"));
        assertNotNull(notification.getTeamId());
        assertTrue(notification.getMetadata().contains("horasRestantes"));
    }

    @Test
    void testCreateNuevaEvaluacionNotification() {
        CreateNotificationRequest notification = CreateNotificationRequest.builder()
                .userId(456L)
                .tipo("NUEVA_EVALUACION")
                .mensaje("Nueva evaluación disponible: 'Quiz Módulo 3 - Arquitectura de Software'")
                .cursoId(101L)
                .prioridad("MEDIA")
                .enlace("https://innosistemas.udea.edu.co/evaluaciones/quiz-modulo3")
                .expiraEn(LocalDateTime.now().plusWeeks(1))
                .metadata("{\"evaluacionId\": 301, \"modulo\": 3, \"tipo\": \"quiz\"}")
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations =
            validator.validate(notification);

        assertTrue(violations.isEmpty());
        assertEquals("NUEVA_EVALUACION", notification.getTipo());
        assertTrue(notification.getMensaje().contains("Quiz Módulo 3"));
        assertEquals(101L, notification.getCursoId());
        assertTrue(notification.getMetadata().contains("evaluacionId"));
    }

    @Test
    void testCreateComunicacionMasivaAdministrador() {
        CreateNotificationRequest notification = CreateNotificationRequest.builder()
                .userId(null) // Comunicación masiva no tiene usuario específico
                .tipo("COMUNICACION_MASIVA")
                .mensaje(" Recordatorio: Matrícula académica abierta del 15 al 30 de enero")
                .prioridad("ALTA")
                .enlace("https://innosistemas.udea.edu.co/matricula")
                .expiraEn(LocalDateTime.of(2024, 1, 30, 23, 59))
                .metadata("{\"tipo\": \"matricula\", \"fechaInicio\": \"2024-01-15\", \"fechaFin\": \"2024-01-30\"}")
                .build();

        // Para comunicaciones masivas, userId puede ser null
        assertNull(notification.getUserId());
        assertEquals("COMUNICACION_MASIVA", notification.getTipo());
        assertTrue(notification.getMensaje().contains("Matrícula"));
        assertEquals("ALTA", notification.getPrioridad());
    }

    @Test
    void testCreateActualizacionEquipoNotification() {
        CreateNotificationRequest notification = CreateNotificationRequest.builder()
                .userId(789L)
                .tipo("ACTUALIZACION_EQUIPO")
                .mensaje(" Tu equipo 'Los Desarrolladores' tiene una nueva entrega pendiente")
                .teamId(25L)
                .cursoId(202L)
                .prioridad("MEDIA")
                .metadata("{\"equipoNombre\": \"Los Desarrolladores\", \"accion\": \"nueva_entrega\"}")
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations =
            validator.validate(notification);

        assertTrue(violations.isEmpty());
        assertEquals("ACTUALIZACION_EQUIPO", notification.getTipo());
        assertEquals(25L, notification.getTeamId());
        assertTrue(notification.getMensaje().contains("Los Desarrolladores"));
        assertTrue(notification.getMetadata().contains("nueva_entrega"));
    }

    @Test
    void testCreateRecordatorioClaseNotification() {
        LocalDateTime claseInicio = LocalDateTime.now().plusMinutes(15);

        CreateNotificationRequest notification = CreateNotificationRequest.builder()
                .userId(555L)
                .tipo("RECORDATORIO_CLASE")
                .mensaje("Recordatorio: Clase de 'Ingeniería de Software' comienza en 15 minutos")
                .cursoId(303L)
                .prioridad("BAJA")
                .enlace("https://meet.google.com/abc-defg-hij")
                .expiraEn(claseInicio)
                .metadata("{\"aula\": \"Virtual\", \"profesor\": \"Dr. García\", \"duracion\": 120}")
                .build();

        assertTrue(notification.getExpiraEn().isAfter(LocalDateTime.now()));
        assertEquals("RECORDATORIO_CLASE", notification.getTipo());
        assertTrue(notification.getMensaje().contains("15 minutos"));
        assertTrue(notification.getEnlace().contains("meet.google.com"));
    }

    @Test
    void testValidationFailsForNotificacionSinUsuarioEspecifico() {
        CreateNotificationRequest notification = CreateNotificationRequest.builder()
                .tipo("ALERTA_VENCIMIENTO") // Este tipo requiere usuario específico
                .mensaje("Tarea pendiente")
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations =
            validator.validate(notification);

        // Debería fallar porque las alertas personales requieren userId
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidationFailsForTipoVacio() {
        CreateNotificationRequest notification = CreateNotificationRequest.builder()
                .userId(123L)
                .tipo("") // Tipo vacío
                .mensaje("Mensaje válido")
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations =
            validator.validate(notification);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("tipo de notificación es obligatorio")));
    }


    @Test
    void testCreateNotificacionSeguridadSistema() {
        CreateNotificationRequest notification = CreateNotificationRequest.builder()
                .userId(999L)
                .tipo("SEGURIDAD_ALERTA")
                .mensaje(" Detectamos un acceso inusual a tu cuenta. Verifica tu actividad reciente")
                .prioridad("CRITICA")
                .enlace("https://innosistemas.udea.edu.co/seguridad/actividad")
                .expiraEn(LocalDateTime.now().plusDays(1))
                .metadata("{\"tipoAlerta\": \"acceso_inusual\", \"ip\": \"192.168.1.100\"}")
                .build();

        assertEquals("SEGURIDAD_ALERTA", notification.getTipo());
        assertEquals("CRITICA", notification.getPrioridad());
        assertTrue(notification.getMensaje().contains("acceso inusual"));
        assertTrue(notification.getMetadata().contains("acceso_inusual"));
    }

    @Test
    void testBuilderChainingParaNotificacionCompleja() {
        CreateNotificationRequest notification = CreateNotificationRequest.builder()
                .userId(111L)
                .tipo("DASHBOARD_ACTUALIZACION")
                .mensaje("El dashboard de comunicaciones se ha actualizado con nuevas métricas")
                .teamId(5L)
                .cursoId(150L)
                .prioridad("BAJA")
                .enlace("https://innosistemas.udea.edu.co/dashboard")
                .expiraEn(LocalDateTime.now().plusMonths(1))
                .metadata("{\"version\": \"2.1.0\", \"nuevas_metricas\": true}")
                .build();

        // Verificar que todos los campos se asignaron correctamente
        assertEquals(111L, notification.getUserId());
        assertEquals("DASHBOARD_ACTUALIZACION", notification.getTipo());
        assertEquals(5L, notification.getTeamId());
        assertEquals(150L, notification.getCursoId());
        assertTrue(notification.getMetadata().contains("version"));
    }

    @Test
    void testConstructorSimpleParaNotificacionBasica() {
        CreateNotificationRequest notification = new CreateNotificationRequest(
                666L,
                "INFO_GENERAL",
                "Bienvenido al sistema InnoSistemas UdeA",
                null,
                "{\"tipo\": \"bienvenida\"}"
        );

        Set<ConstraintViolation<CreateNotificationRequest>> violations =
            validator.validate(notification);

        assertTrue(violations.isEmpty());
        assertEquals("INFO_GENERAL", notification.getTipo());
        assertTrue(notification.getMensaje().contains("InnoSistemas"));
        assertNull(notification.getTeamId());
    }

    @Test
    void testNotificacionConExpiracionEnElPasado() {
        LocalDateTime fechaPasada = LocalDateTime.now().minusHours(2);

        CreateNotificationRequest notification = CreateNotificationRequest.builder()
                .userId(777L)
                .tipo("EVENTO_PASADO")
                .mensaje("Esta notificación ya expiró")
                .expiraEn(fechaPasada)
                .build();

        // El sistema debe permitir crear la notificación, pero será filtrada al enviar
        assertTrue(notification.getExpiraEn().isBefore(LocalDateTime.now()));
        assertEquals("EVENTO_PASADO", notification.getTipo());
    }
}