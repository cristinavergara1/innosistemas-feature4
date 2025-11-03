package com.udea.innosistemas.security;

import com.udea.innosistemas.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

//clase para generar y validar tokens JWT utilizados en la autenticación y autorización de usuarios.
//Gestiona la creación de tokens, extracción de información y validación de su integridad y expiración.
// Autor: Fábrica-Escuela de Software UdeA  
// Versión: 1.0.0


@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${innosistemas.auth.jwt.secret}")
    private String jwtSecret;

    @Value("${innosistemas.auth.jwt.expiration}")
    private long jwtExpirationInMs;

    @Value("${innosistemas.auth.jwt.refresh-expiration}")
    private long refreshExpirationInMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return generateTokenWithClaims(user);
    }

    private String generateTokenWithClaims(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs * 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());

        // Agregar teamId solo si existe
        if (user.getTeamId() != null) {
            claims.put("teamId", user.getTeamId());
        }

        // Agregar courseId solo si existe
        if (user.getCourseId() != null) {
            claims.put("courseId", user.getCourseId());
        }

        // Agregar nombre completo si existe
        if (user.getFirstName() != null && user.getLastName() != null) {
            claims.put("name", user.getFullName());
        }

        // Agregar authorities
        String authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        claims.put("authorities", authorities);

        return Jwts.builder()
                .subject(user.getUsername())
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateTokenFromUser(User user) {
        return generateTokenWithClaims(user);
    }

    // Deprecated: Usar generateTokenFromUser en su lugar
    @Deprecated
    public String generateTokenFromUsername(String username) {
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs * 1000);

        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("JWT token is expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("JWT token is unsupported: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            logger.error("JWT token validation error: {}", ex.getMessage());
        }
        return false;
    }

    public Date getExpirationDateFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getExpiration();
    }

    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromJWT(token);
        return expiration.before(new Date());
    }

    public String generateRefreshToken(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return generateRefreshTokenFromUser(user);
    }

    public String generateRefreshTokenFromUser(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationInMs * 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("type", "refresh");

        return Jwts.builder()
                .subject(user.getUsername())
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    // Deprecated: Usar generateRefreshTokenFromUser en su lugar
    @Deprecated
    public String generateRefreshTokenFromUsername(String username) {
        Date expiryDate = new Date(System.currentTimeMillis() + refreshExpirationInMs * 1000);

        return Jwts.builder()
                .subject(username)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception e) {
            logger.error("Error checking if token is refresh token: {}", e.getMessage());
            return false;
        }
    }

    public String getTokenId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getId();
        } catch (Exception e) {
            logger.error("Error extracting token ID: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserIdFromJWT(String token) {
        try {
            Claims claims = getClaims(token);
            Object userIdObj = claims.get("userId");
            if (userIdObj instanceof Integer) {
                return ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            }
            return null;
        } catch (Exception e) {
            logger.error("Error extracting userId from token: {}", e.getMessage());
            return null;
        }
    }

    public String getRoleFromJWT(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.get("role", String.class);
        } catch (Exception e) {
            logger.error("Error extracting role from token: {}", e.getMessage());
            return null;
        }
    }

    public Long getTeamIdFromJWT(String token) {
        try {
            Claims claims = getClaims(token);
            Object teamIdObj = claims.get("teamId");
            if (teamIdObj instanceof Integer) {
                return ((Integer) teamIdObj).longValue();
            } else if (teamIdObj instanceof Long) {
                return (Long) teamIdObj;
            }
            return null;
        } catch (Exception e) {
            logger.debug("No teamId in token or error extracting: {}", e.getMessage());
            return null;
        }
    }

    public Long getCourseIdFromJWT(String token) {
        try {
            Claims claims = getClaims(token);
            Object courseIdObj = claims.get("courseId");
            if (courseIdObj instanceof Integer) {
                return ((Integer) courseIdObj).longValue();
            } else if (courseIdObj instanceof Long) {
                return (Long) courseIdObj;
            }
            return null;
        } catch (Exception e) {
            logger.debug("No courseId in token or error extracting: {}", e.getMessage());
            return null;
        }
    }

    public Map<String, Object> getAllClaims(String token) {
        try {
            Claims claims = getClaims(token);
            Map<String, Object> result = new HashMap<>();
            result.put("userId", getUserIdFromJWT(token));
            result.put("email", getUsernameFromJWT(token));
            result.put("role", getRoleFromJWT(token));
            result.put("teamId", getTeamIdFromJWT(token));
            result.put("courseId", getCourseIdFromJWT(token));
            result.put("issuedAt", claims.getIssuedAt());
            result.put("expiration", claims.getExpiration());
            return result;
        } catch (Exception e) {
            logger.error("Error extracting all claims: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}