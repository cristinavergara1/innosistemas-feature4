package com.udea.innosistemas.security;

import com.udea.innosistemas.service.RateLimitingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de Rate Limiting que controla el número de peticiones por usuario.
 * Se ejecuta después del filtro de autenticación JWT.
 * Limita peticiones basándose en usuario autenticado o IP del cliente.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitFilter.class);

    @Autowired
    private RateLimitingService rateLimitingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Obtener clave para rate limiting (usuario o IP)
        String key = getRateLimitKey(request);

        // Verificar si es un endpoint de autenticación
        boolean isAuthEndpoint = isAuthenticationEndpoint(request);

        // Aplicar rate limiting
        boolean allowed;
        if (isAuthEndpoint) {
            allowed = rateLimitingService.allowAuthRequest(key);
        } else {
            allowed = rateLimitingService.allowRequest(key);
        }

        if (!allowed) {
            // Rate limit excedido
            LOG.warn("Rate limit exceeded for key: {} on endpoint: {}", key, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}"
            );
            return;
        }

        // Agregar headers informativos
        addRateLimitHeaders(response, key);

        filterChain.doFilter(request, response);
    }

    /**
     * Obtiene la clave para rate limiting basándose en usuario autenticado o IP
     *
     * @param request HttpServletRequest
     * @return Clave única para rate limiting
     */
    private String getRateLimitKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Si el usuario está autenticado, usar su username
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "user:" + authentication.getName();
        }

        // Si no está autenticado, usar IP del cliente
        String clientIp = getClientIp(request);
        return "ip:" + clientIp;
    }

    /**
     * Obtiene la IP del cliente considerando proxies
     *
     * @param request HttpServletRequest
     * @return IP del cliente
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Tomar la primera IP de la lista
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Verifica si el endpoint es de autenticación
     *
     * @param request HttpServletRequest
     * @return true si es endpoint de auth
     */
    private boolean isAuthenticationEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // GraphQL mutations de autenticación
        if (uri.contains("/graphql") && "POST".equals(method)) {
            // Para GraphQL necesitaríamos parsear el body, por ahora aplicar límite estricto
            return true;
        }

        // REST endpoints de autenticación
        return uri.contains("/auth/login") ||
               uri.contains("/auth/refresh") ||
               uri.contains("/auth/register");
    }

    /**
     * Agrega headers de rate limiting a la respuesta
     *
     * @param response HttpServletResponse
     * @param key Clave del rate limit
     */
    private void addRateLimitHeaders(HttpServletResponse response, String key) {
        try {
            long availableTokens = rateLimitingService.getAvailableTokens(key);
            response.setHeader("X-RateLimit-Remaining", String.valueOf(availableTokens));
            response.setHeader("X-RateLimit-Limit", "100"); // Podría ser dinámico
        } catch (Exception e) {
            LOG.debug("Error adding rate limit headers: {}", e.getMessage());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // No aplicar rate limiting a actuator endpoints
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/info");
    }
}
