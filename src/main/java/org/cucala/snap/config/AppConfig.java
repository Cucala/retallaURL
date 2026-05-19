package org.cucala.snap.config;

import java.util.Map;

public final class AppConfig {

    private final int port;
    private final String env;
    private final String dbName;
    private final String jwtSecret;

    private AppConfig(int port, String env, String dbName, String jwtSecret) {
        this.port = port;
        this.env = env;
        this.dbName = dbName;
        this.jwtSecret = jwtSecret;
    }

    public static AppConfig load() {
        return from(System.getenv());
    }

    public static AppConfig from(Map<String, String> env) {
        String envName = env.getOrDefault("APP_ENV", "development");
        boolean production = "production".equalsIgnoreCase(envName);

        int port = parsePort(env.getOrDefault("PORT", "3000"));

        String dbName = env.get("DB_NAME");
        if (dbName == null || dbName.isBlank()) {
            if (production) {
                throw new IllegalStateException(
                        "La variable de entorno DB_NAME es obligatoria en producción");
            }
            dbName = "snap.db";
        }

        String jwtSecret = env.get("JWT_SECRET");
        if (jwtSecret == null || jwtSecret.isBlank()) {
            if (production) {
                throw new IllegalStateException(
                        "La variable de entorno JWT_SECRET es obligatoria en producción");
            }
            jwtSecret = "snap-dev-insecure-jwt-secret-32bytes!!!";
        }

        return new AppConfig(port, envName, dbName, jwtSecret);
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value.trim());
            if (port < 0 || port > 65535) {
                throw new IllegalStateException(
                        "Puerto inválido: " + value + ". Debe estar entre 0 y 65535");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "El valor de PORT no es un número válido: " + value);
        }
    }

    public int getPort() { return port; }
    public String getEnv() { return env; }
    public String getDbName() { return dbName; }
    public String getJwtSecret() { return jwtSecret; }
    public boolean isProduction() { return "production".equalsIgnoreCase(env); }
    public boolean isDevelopment() { return !isProduction(); }
}
