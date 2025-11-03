package com.udea.innosistemas.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;



/**
 * Interceptor para autenticación JWT en conexiones WebSocket
 * Se ejecuta antes de establecer la conexión WebSocket para GraphQL Subscriptions
 *
 * Extrae y valida el token JWT del header Authorization o de los parámetros
 * de conexión y establece el contexto de seguridad
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Component
public class GraphQLWebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(GraphQLWebSocketAuthInterceptor.class);

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    public GraphQLWebSocketAuthInterceptor(JwtTokenProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    @Nullable
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            logger.debug("Procesando conexión WebSocket");

            // Intentar obtener token del header Authorization
            String token = extractTokenFromHeaders(accessor);

            // Si no hay token en headers, intentar obtenerlo de los parámetros nativos
            if (token == null) {
                token = extractTokenFromNativeHeaders(accessor);
            }

            if (token != null && tokenProvider.validateToken(token)) {
                String username = tokenProvider.getUsernameFromJWT(token);
                logger.info("Autenticando usuario en WebSocket: {}", username);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
                accessor.setUser(authentication);

                logger.debug("Usuario autenticado exitosamente en WebSocket: {}", username);
            } else {
                logger.warn("Token JWT inválido o no proporcionado para conexión WebSocket");
                // Nota: No rechazamos la conexión aquí, dejamos que las subscriptions
                // individuales manejen la autorización con @PreAuthorize
            }
        }

        return message;
    }

    /**
     * Extrae el token JWT de los headers estándar
     */
    private String extractTokenFromHeaders(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Extrae el token JWT de los headers nativos (puede venir en diferentes formatos)
     */
    private String extractTokenFromNativeHeaders(StompHeaderAccessor accessor) {
        // Intentar obtener de diferentes posibles headers
        String token = accessor.getFirstNativeHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }

        // Intentar obtener directamente como "token"
        token = accessor.getFirstNativeHeader("token");
        if (StringUtils.hasText(token)) {
            return token;
        }

        // Intentar obtener de "Auth-Token" (algunas librerías cliente lo usan)
        token = accessor.getFirstNativeHeader("Auth-Token");
        if (StringUtils.hasText(token)) {
            return token;
        }

        return null;
    }
}
