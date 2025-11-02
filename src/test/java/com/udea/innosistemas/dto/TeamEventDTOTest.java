package com.udea.innosistemas.dto;

import com.udea.innosistemas.enums.TipoEvento;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TeamEventDTOTest {

    @Test
    void testDefaultConstructor() {
        // Arrange & Act
        TeamEventDTO eventDTO = new TeamEventDTO();

        // Assert
        assertNull(eventDTO.getTeamId());
        assertNull(eventDTO.getTipoEvento());
        assertNull(eventDTO.getUsuarioOrigenId());
        assertNull(eventDTO.getDetalles());
        assertNotNull(eventDTO.getTimestamp());
        assertNull(eventDTO.getMetadata());
        assertTrue(eventDTO.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testParameterizedConstructor() {
        // Arrange
        Long teamId = 1L;
        TipoEvento tipoEvento = TipoEvento.CREACION_EQUIPO; // Cambiar por el valor correcto
        Long usuarioOrigenId = 100L;
        String detalles = "Usuario agregado al equipo";
        LocalDateTime timestampAntes = LocalDateTime.now();

        // Act
        TeamEventDTO eventDTO = new TeamEventDTO(teamId, tipoEvento, usuarioOrigenId, detalles);

        // Assert
        assertEquals(teamId, eventDTO.getTeamId());
        assertEquals(tipoEvento, eventDTO.getTipoEvento());
        assertEquals(usuarioOrigenId, eventDTO.getUsuarioOrigenId());
        assertEquals(detalles, eventDTO.getDetalles());
        assertNotNull(eventDTO.getTimestamp());
        assertTrue(eventDTO.getTimestamp().isAfter(timestampAntes) ||
                eventDTO.getTimestamp().isEqual(timestampAntes));
        assertNull(eventDTO.getMetadata());
    }


    @Test
    void testSettersAndGetters() {
        // Arrange
        TeamEventDTO eventDTO = new TeamEventDTO();
        Long teamId = 2L;
        TipoEvento tipoEvento = TipoEvento.CREACION_EQUIPO; // Usar un valor que exista
        Long usuarioOrigenId = 200L;
        String detalles = "Usuario removido del equipo";
        LocalDateTime timestamp = LocalDateTime.now().minusHours(1);
        String metadata = "metadata adicional";

        // Act
        eventDTO.setTeamId(teamId);
        eventDTO.setTipoEvento(tipoEvento);
        eventDTO.setUsuarioOrigenId(usuarioOrigenId);
        eventDTO.setDetalles(detalles);
        eventDTO.setTimestamp(timestamp);
        eventDTO.setMetadata(metadata);

        // Assert
        assertEquals(teamId, eventDTO.getTeamId());
        assertEquals(tipoEvento, eventDTO.getTipoEvento());
        assertEquals(usuarioOrigenId, eventDTO.getUsuarioOrigenId());
        assertEquals(detalles, eventDTO.getDetalles());
        assertEquals(timestamp, eventDTO.getTimestamp());
        assertEquals(metadata, eventDTO.getMetadata());
    }

    @Test
    void testConstructorWithNullValues() {
        // Arrange & Act
        TeamEventDTO eventDTO = new TeamEventDTO(null, null, null, null);

        // Assert
        assertNull(eventDTO.getTeamId());
        assertNull(eventDTO.getTipoEvento());
        assertNull(eventDTO.getUsuarioOrigenId());
        assertNull(eventDTO.getDetalles());
        assertNotNull(eventDTO.getTimestamp());
        assertNull(eventDTO.getMetadata());
    }

    @Test
    void testSetNullValues() {
        // Arrange
        TeamEventDTO eventDTO = new TeamEventDTO(1L, TipoEvento.CREACION_EQUIPO, 100L, "Test");

        // Act
        eventDTO.setTeamId(null);
        eventDTO.setTipoEvento(null);
        eventDTO.setUsuarioOrigenId(null);
        eventDTO.setDetalles(null);
        eventDTO.setTimestamp(null);
        eventDTO.setMetadata(null);

        // Assert
        assertNull(eventDTO.getTeamId());
        assertNull(eventDTO.getTipoEvento());
        assertNull(eventDTO.getUsuarioOrigenId());
        assertNull(eventDTO.getDetalles());
        assertNull(eventDTO.getTimestamp());
        assertNull(eventDTO.getMetadata());
    }

    @Test
    void testTimestampIsSetOnConstructorCall() {
        // Arrange
        LocalDateTime beforeCreation = LocalDateTime.now();

        // Act
        TeamEventDTO eventDTO1 = new TeamEventDTO();
        TeamEventDTO eventDTO2 = new TeamEventDTO(1L, TipoEvento.CREACION_EQUIPO, 100L, "Test");
        LocalDateTime afterCreation = LocalDateTime.now();

        // Assert
        assertTrue(eventDTO1.getTimestamp().isAfter(beforeCreation) ||
                eventDTO1.getTimestamp().isEqual(beforeCreation));
        assertTrue(eventDTO1.getTimestamp().isBefore(afterCreation) ||
                eventDTO1.getTimestamp().isEqual(afterCreation));

        assertTrue(eventDTO2.getTimestamp().isAfter(beforeCreation) ||
                eventDTO2.getTimestamp().isEqual(beforeCreation));
        assertTrue(eventDTO2.getTimestamp().isBefore(afterCreation) ||
                eventDTO2.getTimestamp().isEqual(afterCreation));
    }}
