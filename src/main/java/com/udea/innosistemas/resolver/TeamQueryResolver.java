package com.udea.innosistemas.resolver;

import com.udea.innosistemas.dto.TeamDTO;
import com.udea.innosistemas.dto.TeamMember;
import com.udea.innosistemas.entity.Team;
import com.udea.innosistemas.entity.User;
import com.udea.innosistemas.service.TeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolver GraphQL para consultas (Queries) de equipos
 * Maneja todas las operaciones de lectura de equipos
 *
 * Refactorizado usando patrón Template Method (BaseResolver)
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 2.0.0
 */
@Controller
public class TeamQueryResolver extends BaseResolver {

    private static final Logger logger = LoggerFactory.getLogger(TeamQueryResolver.class);

    @Autowired
    private TeamService teamService;

    /**
     * Obtiene un equipo por su ID
     * Estudiantes solo pueden ver su propio equipo
     * Profesores y admins pueden ver cualquier equipo
     * Usa BaseResolver para validar permisos
     *
     * @param id ID del equipo
     * @return TeamDTO
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public TeamDTO getTeamById(@Argument Long id) {
        logger.info("Obteniendo equipo por ID: {}", id);

        // Validar acceso al equipo usando BaseResolver
        validateTeamAccess(id);

        Team team = teamService.obtenerTeamPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));

        // Obtener miembros del equipo
        List<User> miembros = userRepository.findByTeamId(id);

        return new TeamDTO(team, miembros);
    }

    /**
     * Obtiene todos los equipos de un curso
     * Solo accesible para profesores y admins
     *
     * @param courseId ID del curso
     * @return Lista de TeamDTO
     */
    @QueryMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN', 'TA')")
    public List<TeamDTO> getTeamsByCourse(@Argument Long courseId) {
        logger.info("Obteniendo equipos del curso: {}", courseId);

        List<Team> teams = teamService.obtenerTeamsPorCurso(courseId);

        return teams.stream()
                .map(team -> {
                    List<User> miembros = userRepository.findByTeamId(team.getId());
                    return new TeamDTO(team, miembros);
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el equipo del usuario autenticado
     *
     * @return TeamDTO o null si el usuario no tiene equipo
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public TeamDTO getMyTeam() {
        logger.info("Obteniendo equipo del usuario autenticado");

        User currentUser = getCurrentUser();

        if (currentUser.getTeamId() == null) {
            logger.info("Usuario {} no tiene equipo asignado", currentUser.getEmail());
            return null;
        }

        Team team = teamService.obtenerTeamPorId(currentUser.getTeamId())
                .orElse(null);

        if (team == null) {
            return null;
        }

        List<User> miembros = userRepository.findByTeamId(team.getId());
        return new TeamDTO(team, miembros);
    }

    /**
     * Obtiene equipos próximos a vencer (fecha límite cercana)
     * Solo accesible para profesores y admins
     *
     * @param days Número de días antes de la fecha límite (default: 3)
     * @return Lista de TeamDTO
     */
    @QueryMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN', 'TA')")
    public List<TeamDTO> getTeamsNearDeadline(@Argument(name = "days") Integer days) {
        int diasLimite = (days != null) ? days : 3;
        logger.info("Obteniendo equipos próximos a vencer en {} días", diasLimite);

        List<Team> teams = teamService.obtenerTeamsProximosAVencer(diasLimite);

        return teams.stream()
                .map(team -> {
                    List<User> miembros = userRepository.findByTeamId(team.getId());
                    return new TeamDTO(team, miembros);
                })
                .collect(Collectors.toList());
    }

    /**
     * Schema mapping para resolver el campo miembros de Team
     * Esto se ejecuta cuando GraphQL solicita el campo miembros
     */
    @SchemaMapping(typeName = "Team", field = "miembros")
    public List<TeamMember> miembros(TeamDTO team) {
        if (team.getMiembros() != null) {
            return team.getMiembros();
        }

        List<User> miembros = userRepository.findByTeamId(team.getId());
        return miembros.stream()
                .map(TeamMember::new)
                .collect(Collectors.toList());
    }

}
