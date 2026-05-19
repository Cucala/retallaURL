package org.cucala.snap.urls;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class UrlShortener {

    private static final String ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UrlRepository repository;

    public UrlShortener(UrlRepository repository) {
        this.repository = repository;
    }

    public ShortUrl shorten(String longUrl, String ownerEmail) {
        return shorten(longUrl, ownerEmail, generateCode());
    }

    public ShortUrl shorten(String longUrl, String ownerEmail, String code) {
        ShortUrl url = new ShortUrl(code, longUrl, Instant.now(), ownerEmail);
        repository.save(url);
        return url;
    }

    public DeleteResult delete(String code, String requesterEmail) {
        Optional<ShortUrl> url = repository.findByCode(code);
        if (url.isEmpty()) return DeleteResult.NOT_FOUND;
        if (!url.get().ownerEmail().equals(requesterEmail)) return DeleteResult.FORBIDDEN;
        repository.deleteByCode(code);
        return DeleteResult.OK;
    }

    public Optional<ShortUrl> resolve(String code) {
        return repository.findByCode(code);
    }

    public List<ShortUrl> listAll() {
        return repository.findAll();
    }

    public List<ShortUrl> listByOwner(String ownerEmail) {
        return repository.findByOwner(ownerEmail);
    }

    private static String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
