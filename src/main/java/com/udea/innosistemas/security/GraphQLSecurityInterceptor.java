package com.udea.innosistemas.security;


import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Interceptor GraphQL para validación de permisos a nivel de operación.
 * Se ejecuta antes de cada operación GraphQL y valida que el usuario
 * tenga los permisos necesarios basados en su rol.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Component
public class GraphQLSecurityInterceptor implements WebGraphQlInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(GraphQLSecurityInterceptor.class);

    @Value("${spring.graphql.schema.introspection.enabled:false}")
    private boolean introspectionEnabled;

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Obtener el nombre de la operación
        String operationName = request.getOperationName();
        String document = request.getDocument();

        LOG.debug("GraphQL operation: {}, document: {}", operationName, document);

        // Si no hay autenticación y la operación no es login, denegar acceso
        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getName())) {

            // Permitir operaciones de autenticación
            if (document != null && (document.contains("mutation login") ||
                                    document.contains("mutation refreshToken"))) {
                return chain.next(request);
            }

            // Permitir introspección SOLO si está habilitada (perfil dev)
            if (introspectionEnabled && document != null &&
                (document.contains("IntrospectionQuery") ||
                 document.contains("__schema") ||
                 document.contains("__type"))) {
                LOG.debug("Allowing introspection query in development mode");
                return chain.next(request);
            }

            LOG.warn("Unauthenticated GraphQL request blocked: {}", operationName);
            return Mono.error(new AccessDeniedException("Autenticación requerida"));
        }

        // Log de la operación autenticada
        LOG.info("GraphQL operation '{}' by user: {}", operationName, authentication.getName());

        return chain.next(request);
    }

    /**
     * Resolver de excepciones para GraphQL que convierte excepciones de Spring Security
     * en errores GraphQL apropiados.
     */
    @Component
    public static class GraphQLExceptionResolver extends DataFetcherExceptionResolverAdapter {

        private static final Logger LOG = LoggerFactory.getLogger(GraphQLExceptionResolver.class);

        @Override
        protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
            LOG.error("GraphQL error in field {}: {}", env.getField().getName(), ex.getMessage());

            if (ex instanceof AccessDeniedException) {
                return GraphqlErrorBuilder.newError()
                        .message("Acceso denegado: " + ex.getMessage())
                        .path(env.getExecutionStepInfo().getPath())
                        .location(env.getField().getSourceLocation())
                        .build();
            }

            if (ex instanceof com.udea.innosistemas.exception.AuthenticationException) {
                return GraphqlErrorBuilder.newError()
                        .message("Error de autenticación: " + ex.getMessage())
                        .path(env.getExecutionStepInfo().getPath())
                        .location(env.getField().getSourceLocation())
                        .build();
            }

            return null; // Dejar que otros resolvers manejen otras excepciones
        }
    }
}
