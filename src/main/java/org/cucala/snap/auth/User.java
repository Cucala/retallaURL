package org.cucala.snap.auth;

import java.time.Instant;

public record User(long id, String email, String passwordHash, String name, Instant createdAt) {}
