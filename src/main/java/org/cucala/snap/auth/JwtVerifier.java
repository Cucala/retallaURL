package org.cucala.snap.auth;

import com.sun.net.httpserver.HttpExchange;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class JwtVerifier {

    private final SecretKey signingKey;

    public JwtVerifier(String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<String> extractEmail(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return Optional.empty();
        String token = header.substring(7).strip();
        return token.isEmpty() ? Optional.empty() : verify(token);
    }

    public Optional<String> verify(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject());
        } catch (JwtException e) {
            return Optional.empty();
        }
    }
}
