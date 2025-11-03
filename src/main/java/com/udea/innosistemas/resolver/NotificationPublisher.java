package com.udea.innosistemas.resolver;

import com.udea.innosistemas.dto.NotificationDTO;
import com.udea.innosistemas.entity.Notification;
import com.udea.innosistemas.event.NotificationEvent;
import com.udea.innosistemas.event.TeamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publisher para gestionar suscripciones GraphQL en tiempo real
 * Escucha eventos de notificaciones y equipos y los emite a los suscriptores correspondientes
 *
 * Utiliza Reactor Sinks para gestionar múltiples suscriptores de manera eficiente
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Component
public class NotificationPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPublisher.class);

    // Sinks para notificaciones por usuario
    private final Map<Long, Sinks.Many<NotificationDTO>> notificationSinks = new ConcurrentHashMap<>();

    // Sinks para eventos de equipo
    private final Map<Long, Sinks.Many<Map<String, Object>>> teamEventSinks = new ConcurrentHashMap<>();

    // Sinks para contador de no leídas por usuario
    private final Map<Long, Sinks.Many<Map<String, Object>>> unreadCountSinks = new ConcurrentHashMap<>();

    /**
     * Escucha eventos de notificación y los emite a los suscriptores
     *
     * @param event Evento de notificación
     */
    @EventListener
    public void onNotificationEvent(NotificationEvent event) {
        Notification notification = event.getNotification();
        Long userId = notification.getUserId();

        logger.info("Emitiendo notificación para usuario {}: {}", userId, notification.getTipo());

        // Emitir notificación al sink del usuario
        Sinks.Many<NotificationDTO> sink = notificationSinks.get(userId);
        if (sink != null) {
            NotificationDTO dto = new NotificationDTO(notification);
            sink.tryEmitNext(dto);
            logger.debug("Notificación emitida a suscriptor del usuario {}", userId);
        } else {
            logger.debug("No hay suscriptores activos para usuario {}", userId);
        }

        // Actualizar contador de no leídas
        updateUnreadCount(userId);
    }

    /**
     * Escucha eventos de equipo y los emite a los suscriptores
     *
     * @param event Evento de equipo
     */
    @EventListener
    public void onTeamEvent(TeamEvent event) {
        Long teamId = event.getTeamId();

        logger.info("Emitiendo evento de equipo {}: {}", teamId, event.getTipoEvento());

        // Emitir evento al sink del equipo
        Sinks.Many<Map<String, Object>> sink = teamEventSinks.get(teamId);
        if (sink != null) {
            Map<String, Object> payload = Map.of(
                    "teamId", event.getTeamId(),
                    "tipoEvento", event.getTipoEvento().name(),
                    "usuarioOrigenId", event.getUsuarioOrigenId() != null ? event.getUsuarioOrigenId() : 0,
                    "detalles", event.getDetalles(),
                    "timestamp", event.getEventTimestamp().toString(),
                    "metadata", event.getMetadata() != null ? event.getMetadata() : ""
            );

            sink.tryEmitNext(payload);
            logger.debug("Evento de equipo emitido a suscriptores del equipo {}", teamId);
        } else {
            logger.debug("No hay suscriptores activos para equipo {}", teamId);
        }
    }

    /**
     * Obtiene el Flux de notificaciones para un usuario
     * Crea un sink si no existe
     *
     * @param userId ID del usuario
     * @return Flux de NotificationDTO
     */
    public Flux<NotificationDTO> getNotificationFlux(Long userId) {
        logger.info("Creando suscripción de notificaciones para usuario {}", userId);

        Sinks.Many<NotificationDTO> sink = notificationSinks.computeIfAbsent(userId, id -> {
            Sinks.Many<NotificationDTO> newSink = Sinks.many().multicast().onBackpressureBuffer();
            logger.debug("Nuevo sink creado para usuario {}", id);
            return newSink;
        });

        return sink.asFlux()
                .doOnCancel(() -> logger.info("Usuario {} canceló su suscripción de contador", userId))
                .doOnTerminate(() -> logger.debug("Suscripción de contador terminada para usuario {}", userId));
    }

    /**
     * Obtiene el Flux de eventos de equipo
     * Crea un sink si no existe
     *
     * @param teamId ID del equipo
     * @return Flux de TeamEventPayload (como Map)
     */
    public Flux<Map<String, Object>> getTeamEventFlux(Long teamId) {
        logger.info("Creando suscripción de eventos para equipo {}", teamId);

        Sinks.Many<Map<String, Object>> sink = teamEventSinks.computeIfAbsent(teamId, id -> {
            Sinks.Many<Map<String, Object>> newSink = Sinks.many().multicast().onBackpressureBuffer();
            logger.debug("Nuevo sink de eventos creado para equipo {}", id);
            return newSink;
        });

        return sink.asFlux()
                .doOnCancel(() -> {
                    logger.info("Equipo {} canceló su suscripción de eventos", teamId);
                })
                .doOnTerminate(() -> {
                    logger.debug("Suscripción de eventos terminada para equipo {}", teamId);
                });
    }

    /**
     * Obtiene el Flux de contador de no leídas para un usuario
     * Crea un sink si no existe y emite el valor inicial
     *
     * @param userId ID del usuario
     * @return Flux de UnreadCountPayload (como Map)
     */
    public Flux<Map<String, Object>> getUnreadCountFlux(Long userId) {
        logger.info("Creando suscripción de contador de no leídas para usuario {}", userId);

        Sinks.Many<Map<String, Object>> sink = unreadCountSinks.computeIfAbsent(userId, id -> {
            Sinks.Many<Map<String, Object>> newSink = Sinks.many().multicast().onBackpressureBuffer();
            logger.debug("Nuevo sink de contador creado para usuario {}", id);
            return newSink;
        });

        // Emitir valor inicial inmediatamente
        Map<String, Object> initialCount = Map.of(
                "userId", userId,
                "count", 0, // Debería obtener el valor real del servicio
                "timestamp", LocalDateTime.now().toString()
        );
        sink.tryEmitNext(initialCount);

        return sink.asFlux()
                .doOnCancel(() -> {
                    logger.info("Usuario {} canceló su suscripción de contador", userId);
                })
                .doOnTerminate(() -> {
                    logger.debug("Suscripción de contador terminada para usuario {}", userId);
                });
    }

    /**
     * Actualiza el contador de notificaciones no leídas para un usuario
     *
     * @param userId ID del usuario
     */
    private void updateUnreadCount(Long userId) {
        Sinks.Many<Map<String, Object>> sink = unreadCountSinks.get(userId);
        if (sink != null) {
            // Aquí deberías obtener el contador real desde el servicio
            // Por ahora usamos un placeholder
            Map<String, Object> countPayload = Map.of(
                    "userId", userId,
                    "count", 1, // Debería ser el valor real
                    "timestamp", LocalDateTime.now().toString()
            );

            sink.tryEmitNext(countPayload);
            logger.debug("Contador de no leídas actualizado para usuario {}", userId);
        }
    }

    /**
     * Limpia los sinks inactivos (opcional, para evitar memory leaks)
     */
    public void cleanupInactiveSinks() {
        logger.info("Limpiando sinks inactivos");

        // Implementar lógica de limpieza si es necesario
        // Por ejemplo, remover sinks que no tienen suscriptores
    }
}
