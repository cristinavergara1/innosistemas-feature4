# InnoSistemas Backend - API GraphQL

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=cristinavergara1_innosistemas-feature4&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=cristinavergara1_innosistemas-feature4)[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=cristinavergara1_innosistemas-feature4&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=cristinavergara1_innosistemas-feature4)
(https://sonarcloud.io/api/project_badges/measure?project=cristinavergara1_innosistemas-feature4&metric=coverage)](https://sonarcloud.io/summary/new_code?id=cristinavergara1_innosistemas-feature4)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=cristinavergara1_innosistemas-feature4&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=cristinavergara1_innosistemas-feature4)[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=cristinavergara1_innosistemas-feature4&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=cristinavergara1_innosistemas-feature4)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=cristinavergara1_innosistemas-feature4&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=cristinavergara1_innosistemas-feature4)


Plataforma de Integración y Desarrollo de Software para Estudiantes de Ingeniería de Sistemas - Universidad de Antioquia

## Descripción

Backend del sistema InnoSistemas que proporciona una API GraphQL para la gestión de usuarios, autenticación, autorización basada en roles, equipos y cursos. Este proyecto implementa las funcionalidades principales de autenticación y seguridad con JWT, control de acceso basado en roles y directivas GraphQL personalizadas.

## Stack Tecnológico

### Backend
- **Framework**: Spring Boot 3.2.0
- **Java**: 17
- **API**: GraphQL (Spring for GraphQL)
- **Base de datos**: PostgreSQL (producción) / H2 (desarrollo y pruebas)
- **Seguridad**: Spring Security + JWT (JJWT 0.12.3)
- **Caché**: Redis (opcional, configuración incluida)
- **Migraciones**: Flyway
- **Build Tool**: Maven
- **ORM**: Hibernate/JPA

### Dependencias Principales
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter GraphQL
- Spring Boot Starter Validation
- Spring Boot Starter Actuator (monitoreo)
- Spring Boot Starter Cache
- Spring Boot Starter Mail
- Spring Boot Starter Data Redis
- PostgreSQL Driver
- Flyway Core
- JWT (jjwt-api, jjwt-impl, jjwt-jackson)
- Bucket4j (rate limiting)
- Prometheus (métricas)
- Lombok

## Características Implementadas

### Autenticación y Autorización
- ✅ Autenticación JWT con tokens de acceso y refresh
- ✅ Gestión de sesiones de usuario
- ✅ Blacklist de tokens para logout
- ✅ Control de acceso basado en roles (RBAC)
- ✅ Directivas GraphQL personalizadas (@auth, @requiresTeam, @requiresCourse)

### Roles de Usuario
- **STUDENT**: Usuario estándar con acceso a sus equipos y proyectos
- **PROFESSOR**: Puede gestionar cursos, equipos y enviar notificaciones
- **ADMIN**: Acceso completo al sistema
- **TA**: Asistente de enseñanza con permisos limitados

### Seguridad
- ✅ Rate Limiting (limitación de tasa de peticiones)
- ✅ Security Headers (CSP, HSTS, X-Frame-Options)
- ✅ CORS configurado
- ✅ Validación de entrada
- ✅ Manejo centralizado de excepciones

### GraphQL API
- ✅ Queries: getCurrentUser, getUserPermissions, getTeamMembers
- ✅ Mutations: login, refreshToken, logout, logoutFromAllDevices
- ✅ GraphiQL habilitado para pruebas

### Monitoreo y Observabilidad
- ✅ Actuator endpoints (health, info, metrics, prometheus)
- ✅ Métricas de Prometheus
- ✅ Logging estructurado

## Requisitos Previos

- **Java 17** (JDK 17 o superior)
- **Maven 3.6+**
- **PostgreSQL 12+** (para producción)
- **Redis** (opcional, para caché distribuido y rate limiting)

## Instalación y Configuración

### 1. Clonar el repositorio

```bash
git clone <repository-url>
cd innosistemas-feature4
```



### 3. Crear base de datos

```bash
# Conectar a PostgreSQL
psql -U postgres

# Crear base de datos
CREATE DATABASE innosistemas;
```

### 4. Compilar el proyecto

```bash
mvn clean install
```

### 5. Ejecutar migraciones de base de datos

```bash
mvn flyway:migrate
```

### 6. Ejecutar la aplicación

#### Modo desarrollo (con H2 en memoria):
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Modo producción (con PostgreSQL):
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

#### Alternativa - JAR ejecutable:
```bash
# Compilar
mvn clean package

# Ejecutar
java -jar target/innosistemas-1.0.0.jar
```

## Endpoints de la API

### Base URL
```
http://localhost:8080/api/v1
```

**IMPORTANTE:** La aplicación usa el context-path `/api/v1`, todas las URLs deben incluirlo.

### GraphQL
- **GraphQL Endpoint**: `http://localhost:8080/api/v1/graphql`
- **GraphiQL Interface**: `http://localhost:8080/api/v1/graphiql`

### Base de datos (Desarrollo)
- **H2 Console**: `http://localhost:8080/api/v1/h2-console`

### Actuator (Monitoreo)
- **Health**: `http://localhost:8080/api/v1/actuator/health`
- **Info**: `http://localhost:8080/api/v1/actuator/info`
- **Metrics**: `http://localhost:8080/api/v1/actuator/metrics` (requiere ADMIN)
- **Prometheus**: `http://localhost:8080/api/v1/actuator/prometheus` (requiere ADMIN)

## Ejemplos de Uso GraphQL

### Login (Mutation)

```graphql
mutation Login {
  login(email: "estudiante@udea.edu.co", password: "password123") {
    token
    refreshToken
    userInfo {
      id
      email
      role
      firstName
      lastName
      fullName
      teamId
      courseId
    }
  }
}
```

### Obtener Usuario Actual (Query con autenticación)

```graphql
query GetCurrentUser {
  getCurrentUser {
    id
    email
    role
    firstName
    lastName
    fullName
    teamId
    courseId
  }
}
```

**Headers requeridos:**
```
Authorization: Bearer <your-jwt-token>
```

### Obtener Permisos de Usuario (Query con autenticación)

```graphql
query GetUserPermissions {
  getUserPermissions {
    userId
    role
    permissions
    teamId
    courseId
    canManageTeam
    canManageCourse
    canViewAllTeams
    canSendNotifications
  }
}
```

### Refresh Token (Mutation)

```graphql
mutation RefreshToken {
  refreshToken(refreshToken: "<your-refresh-token>") {
    token
    refreshToken
    userInfo {
      id
      email
      role
    }
  }
}
```

### Logout (Mutation con autenticación)

```graphql
mutation Logout {
  logout(token: "<your-current-token>") {
    success
    message
  }
}
```

### Obtener Miembros de Equipo (Query con autenticación y permisos)

```graphql
query GetTeamMembers {
  getTeamMembers(teamId: "1") {
    id
    email
    firstName
    lastName
    fullName
    role
    teamId
    courseId
  }
}
```

## Estructura del Proyecto

```
innosistemas-feature4/
├── src/
│   ├── main/
│   │   ├── java/com/udea/innosistemas/
│   │   │   ├── config/                 # Configuraciones de Spring
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   ├── GraphQLDirectivesConfig.java
│   │   │   │   ├── GatewayConfiguration.java
│   │   │   │   └── WebMvcConfig.java
│   │   │   ├── controller/             # Controllers REST
│   │   │   ├── dto/                    # Data Transfer Objects
│   │   │   ├── entity/                 # Entidades JPA
│   │   │   ├── exception/              # Manejo de excepciones
│   │   │   ├── repository/             # Repositorios JPA
│   │   │   ├── resolver/               # GraphQL Resolvers
│   │   │   ├── security/               # Componentes de seguridad
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── RateLimitFilter.java
│   │   │   │   ├── SecurityHeadersFilter.java
│   │   │   │   ├── GraphQLSecurityInterceptor.java
│   │   │   │   └── directive/          # Directivas GraphQL
│   │   │   ├── service/                # Servicios de negocio
│   │   │   │   ├── AuthenticationService.java
│   │   │   │   ├── TokenBlacklistService.java
│   │   │   │   ├── SessionManagementService.java
│   │   │   │   ├── RateLimitingService.java
│   │   │   │   └── UserQueryService.java
│   │   │   └── InnoSistemasApplication.java
│   │   └── resources/
│   │       ├── application.yml         # Configuración principal
│   │       ├── db/migration/           # Scripts Flyway
│   │       │   ├── V1__Create_users_table.sql
│   │       │   └── V2__Add_user_team_course_fields.sql
│   │       └── graphql/
│   │           └── schema.graphqls     # Schema GraphQL
│   └── test/                           # Tests unitarios e integración
├── pom.xml                             # Configuración Maven
└── README.md
```

## Perfiles de Ejecución

### Development (dev)
- Base de datos H2 en memoria
- Console H2 habilitada en `/h2-console`
- DDL auto: create-drop
- Logging: INFO/DEBUG
- CORS: localhost:3000, localhost:8080

### Test (test)
- Base de datos H2 en memoria
- DDL auto: create-drop
- Logging: DEBUG

### Production (prod)
- Base de datos PostgreSQL
- DDL auto: validate
- Redis cache habilitado
- Logging: INFO/WARN
- CORS: configurado por variable de entorno

## Configuración de Rate Limiting

El sistema incluye rate limiting configurable para proteger contra abusos:

- **Endpoints normales**: 100 peticiones por minuto
- **Endpoints de autenticación**: 10 peticiones por minuto

Configuración en `application.yml`:

```yaml
innosistemas:
  ratelimit:
    enabled: true
    default:
      capacity: 100
      refill-tokens: 100
      refill-period-minutes: 1
    auth:
      capacity: 10
      refill-tokens: 10
      refill-period-minutes: 1
```

## Seguridad

### Headers de Seguridad Configurados
- **Content-Security-Policy (CSP)**
- **Strict-Transport-Security (HSTS)**: 1 año
- **X-Frame-Options**: DENY
- **X-Content-Type-Options**: nosniff
- **X-XSS-Protection**: 1; mode=block

### Configuración JWT
- **Token de acceso**: 24 horas (86400 segundos)
- **Refresh token**: 7 días (604800 segundos)
- **Algoritmo**: HS512

## Migraciones de Base de Datos

### V1: Crear tabla de usuarios
- Tabla `users` con campos básicos
- Índices en email, role, enabled
- Trigger para actualización automática de `updated_at`

### V2: Agregar campos de equipo y curso
- Campos: `team_id`, `course_id`, `first_name`, `last_name`
- Índices en team_id y course_id

## Testing

### Ejecutar todos los tests
```bash
mvn test
```

### Ejecutar tests de integración
```bash
mvn verify
```

### Generar reporte de cobertura
```bash
mvn clean test jacoco:report
```

## Troubleshooting

### Error: Puerto 8080 ya en uso
```bash
# Cambiar puerto en application.yml o usar variable de entorno
export PORT=8081
mvn spring-boot:run
```

### Error: Conexión a base de datos rechazada
- Verificar que PostgreSQL esté ejecutándose
- Verificar credenciales en variables de entorno
- Verificar que la base de datos `innosistemas` exista

### Error: JWT Secret no configurado
- Configurar variable de entorno `JWT_SECRET`
- En producción, usar un secret fuerte de al menos 256 bits

### Error: Redis no disponible
- Redis es opcional, el sistema puede funcionar sin él
- Para deshabilitarlo, usar perfil `dev` o configurar `CACHE_TYPE=simple`

## Licencia

Universidad de Antioquia - Facultad de Ingeniería - Ingeniería de Sistemas

## Contacto

Fábrica-Escuela de Software - Universidad de Antioquia
