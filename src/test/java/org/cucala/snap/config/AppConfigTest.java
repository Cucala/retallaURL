package org.cucala.snap.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @Test
    void valoresPorDefectoConEntornoVacio() {
        AppConfig config = AppConfig.from(Map.of());

        assertEquals(3000, config.getPort());
        assertEquals("development", config.getEnv());
        assertEquals("snap.db", config.getDbName());
        assertTrue(config.isDevelopment());
        assertFalse(config.isProduction());
    }

    @Test
    void leeTodasLasVariablesDeEntorno() {
        AppConfig config = AppConfig.from(Map.of(
                "PORT", "9090",
                "APP_ENV", "production",
                "DB_NAME", "myapp.db",
                "JWT_SECRET", "prod-jwt-secret-must-be-32-chars!!"
        ));

        assertEquals(9090, config.getPort());
        assertEquals("production", config.getEnv());
        assertEquals("myapp.db", config.getDbName());
        assertEquals("prod-jwt-secret-must-be-32-chars!!", config.getJwtSecret());
        assertTrue(config.isProduction());
        assertFalse(config.isDevelopment());
    }

    @Test
    void jwtSecretTieneValorPorDefectoEnDesarrollo() {
        AppConfig config = AppConfig.from(Map.of());

        assertNotNull(config.getJwtSecret());
        assertFalse(config.getJwtSecret().isBlank());
    }

    @Test
    void lanzaErrorEnProduccionSinJwtSecret() {
        var ex = assertThrows(IllegalStateException.class, () ->
                AppConfig.from(Map.of("APP_ENV", "production", "DB_NAME", "app.db")));

        assertTrue(ex.getMessage().contains("JWT_SECRET"),
                "El mensaje debe mencionar la variable que falta");
    }

    @Test
    void lanzaErrorEnProduccionSinDbName() {
        var ex = assertThrows(IllegalStateException.class, () ->
                AppConfig.from(Map.of("APP_ENV", "production")));

        assertTrue(ex.getMessage().contains("DB_NAME"),
                "El mensaje debe mencionar la variable que falta");
    }

    @Test
    void noLanzaErrorEnDesarrolloSinDbName() {
        assertDoesNotThrow(() -> AppConfig.from(Map.of("APP_ENV", "development")));
    }

    @Test
    void lanzaErrorConPuertoNoNumerico() {
        var ex = assertThrows(IllegalStateException.class, () ->
                AppConfig.from(Map.of("PORT", "abc")));

        assertTrue(ex.getMessage().contains("PORT") || ex.getMessage().contains("número"));
    }

    @Test
    void lanzaErrorConPuertoFueraDeRango() {
        assertThrows(IllegalStateException.class, () ->
                AppConfig.from(Map.of("PORT", "99999")));
    }
}
