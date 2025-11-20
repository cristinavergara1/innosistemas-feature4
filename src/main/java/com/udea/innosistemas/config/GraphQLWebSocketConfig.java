package com.udea.innosistemas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import reactor.core.publisher.Mono;

/**
 * Configuración de WebSocket para GraphQL Subscriptions
 * Habilita soporte para subscripciones en tiempo real vía WebSocket
 *
 * Spring Boot GraphQL incluye soporte automático para WebSocket usando
 * el protocolo graphql-ws (graphql-transport-ws)
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Configuration
public class GraphQLWebSocketConfig {

    /**
     * Interceptor para agregar contexto de seguridad a las peticiones WebSocket
     * Esto permite que las subscriptions tengan acceso al usuario autenticado
     */
    @Bean
    public WebGraphQlInterceptor securityContextInterceptor() {
        return new WebGraphQlInterceptor() {
            @Override
            public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
                // El contexto de seguridad ya está establecido por el filtro JWT
                // Este interceptor permite agregar lógica adicional si es necesario
                return chain.next(request);
            }
        };
    }
}
