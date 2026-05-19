package org.cucala.snap.urls;

import java.time.Instant;

public record ShortUrl(String code, String longUrl, Instant createdAt, String ownerEmail) {}
