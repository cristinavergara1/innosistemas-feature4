package com.udea.innosistemas.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador personalizado de errores de acceso denegado (403 Forbidden).
 * Se activa cuando un usuario autenticado no tiene permisos suficientes.
 * Proporciona respuestas JSON estructuradas con información detallada del error.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "anonymous";

        LOG.warn("Access denied for user '{}' to resource '{}': {}",
                username, request.getRequestURI(), accessDeniedException.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", Instant.now().toString());
        errorDetails.put("status", HttpServletResponse.SC_FORBIDDEN);
        errorDetails.put("error", "Forbidden");
        errorDetails.put("message", "No tiene permisos suficientes para acceder a este recurso");
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("user", username);

        // Agregar información adicional en modo development
        if (isDevelopmentMode()) {
            errorDetails.put("exceptionMessage", accessDeniedException.getMessage());
            errorDetails.put("requiredAuthorities", "Consulte la documentación de la API");
        }

        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
    }

    /**
     * Verifica si la aplicación está en modo desarrollo
     *
     * @return true si está en development
     */
    private boolean isDevelopmentMode() {
        String profile = System.getProperty("spring.profiles.active", "dev");
        return "dev".equals(profile) || "test".equals(profile);
    }
}
