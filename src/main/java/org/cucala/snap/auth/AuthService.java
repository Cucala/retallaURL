package org.cucala.snap.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class AuthService {

    private static final long TOKEN_TTL_MS = 24 * 60 * 60 * 1000L;

    private final UserRepository repository;
    private final SecretKey signingKey;

    public AuthService(UserRepository repository, String jwtSecret) {
        this.repository = repository;
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public AuthResult register(String email, String password, String name) {
        String normalized = email.trim().toLowerCase();
        if (repository.findByEmail(normalized).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado: " + normalized);
        }
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        User user = repository.save(normalized, hash, name);
        return new AuthResult(user, buildToken(normalized));
    }

    public AuthResult login(String email, String password) {
        String normalized = email.trim().toLowerCase();
        User user = repository.findByEmail(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas"));
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash().toCharArray());
        if (!result.verified) {
            throw new IllegalArgumentException("Credenciales incorrectas");
        }
        return new AuthResult(user, buildToken(normalized));
    }

    private String buildToken(String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + TOKEN_TTL_MS))
                .signWith(signingKey)
                .compact();
    }
}
