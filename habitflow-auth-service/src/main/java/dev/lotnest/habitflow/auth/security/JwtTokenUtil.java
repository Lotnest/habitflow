package dev.lotnest.habitflow.auth.security;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Getter
@Slf4j
public class JwtTokenUtil {
    private final SecretKey jwtSecret;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final int accessTokenExpirationSeconds;
    private final int refreshTokenExpirationSeconds;

    public JwtTokenUtil() {
        Dotenv dotenv = Dotenv.load();
        String rawSecret = dotenv.get("JWT_SECRET", System.getenv("JWT_SECRET"));
        jwtSecret = Keys.hmacShaKeyFor(rawSecret.getBytes(StandardCharsets.UTF_8));

        accessTokenExpirationMs = Long.parseLong(dotenv.get("JWT_ACCESS_EXPIRATION_MS", "900000"));
        refreshTokenExpirationMs = Long.parseLong(dotenv.get("JWT_REFRESH_EXPIRATION_MS", "604800000"));

        accessTokenExpirationSeconds = (int) (accessTokenExpirationMs / 1000);
        refreshTokenExpirationSeconds = (int) (refreshTokenExpirationMs / 1000);
    }

    public String generateAccessToken(String userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenExpirationMs))
                .signWith(jwtSecret, Jwts.SIG.HS512)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshTokenExpirationMs))
                .signWith(jwtSecret, Jwts.SIG.HS512)
                .compact();
    }

    public String getUserIdFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(jwtSecret)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception exception) {
            log.debug("JWT token is invalid: {}", exception.getMessage());
            return false;
        }
    }
}
