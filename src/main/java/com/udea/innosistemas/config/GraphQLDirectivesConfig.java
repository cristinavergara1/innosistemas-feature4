package com.udea.innosistemas.config;

import com.udea.innosistemas.security.directive.AuthDirective;
import com.udea.innosistemas.security.directive.RequiresCourseDirective;
import com.udea.innosistemas.security.directive.RequiresTeamDirective;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Configuración para registrar las directivas personalizadas de GraphQL.
 * Registra las directivas @auth, @requiresTeam y @requiresCourse para
 * validación de permisos a nivel de campo.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Configuration
public class GraphQLDirectivesConfig {

    @Autowired
    private AuthDirective authDirective;

    @Autowired
    private RequiresTeamDirective requiresTeamDirective;

    @Autowired
    private RequiresCourseDirective requiresCourseDirective;

    /**
     * Configura el RuntimeWiring de GraphQL para registrar las directivas personalizadas.
     *
     * @return RuntimeWiringConfigurer con las directivas registradas
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .directive("auth", authDirective)
                .directive("requiresTeam", requiresTeamDirective)
                .directive("requiresCourse", requiresCourseDirective);
    }
}
