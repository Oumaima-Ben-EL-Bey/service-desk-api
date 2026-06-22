package de.oumaima.servicedesk.user;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!!";
    private static final long EXPIRATION_MS = 3_600_000L;

    @Test
    void generateToken_setsEmailAsSubject_andSignsToken() {
        JwtService jwtService = new JwtService(SECRET, EXPIRATION_MS);

        String token = jwtService.generateToken("jane@example.com");

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("jane@example.com");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void parseAndValidate_throwsOnBadSignature() {
        JwtService minter = new JwtService(SECRET, EXPIRATION_MS);
        String token = minter.generateToken("jane@example.com");

        JwtService impostor = new JwtService("a-totally-different-secret-32-bytes-min!!", EXPIRATION_MS);

        assertThatThrownBy(() -> impostor.parseAndValidate(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseAndValidate_throwsOnExpiredToken() {
        JwtService minter = new JwtService(SECRET, -1000L);
        String token = minter.generateToken("jane@example.com");


        assertThatThrownBy(() -> minter.parseAndValidate(token))
                .isInstanceOf(JwtException.class);
    }
    @Test
    void parseAndValidate_returnsEmail() {
        JwtService minter = new JwtService(SECRET, EXPIRATION_MS);
        String token = minter.generateToken("jane@example.com");
        String email = minter.parseAndValidate(token);

        assertThat(email).isEqualTo("jane@example.com");
    }


}
