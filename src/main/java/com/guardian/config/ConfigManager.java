package com.guardian.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized Configuration Manager.
 * Loads defaults from config.properties and allows CI/CD override via -D flags.
 *
 * Usage in CI:  mvn test -Dbrowser=chrome -Dheadless=true -Dbase.url=http://localhost:8080
 */
public final class ConfigManager {

    private static final Properties props = new Properties();

    static {
        try (InputStream is = ConfigManager.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            System.err.println("⚠ config.properties not found — using defaults + system props.");
        }
    }

    private ConfigManager() { /* utility class */ }

    /**
     * Get a config value. System properties (-D flags) always win over file values.
     */
    public static String get(String key) {
        return System.getProperty(key, props.getProperty(key, ""));
    }

    public static String get(String key, String defaultValue) {
        String val = System.getProperty(key, props.getProperty(key));
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key);
        return val.isBlank() ? defaultValue : Boolean.parseBoolean(val);
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key);
        try {
            return val.isBlank() ? defaultValue : Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ─── Convenience Accessors ─────────────────────────────────
    public static String browser()   { return get("browser", "chrome"); }
    public static boolean headless() { return getBoolean("headless", false); }
    public static String baseUrl()   { return get("base.url", "file:///mock-app/index.html"); }
    public static String apiBaseUrl(){ return get("api.base.url", "http://localhost:3000"); }
    public static int retryCount()   { return getInt("retry.count", 1); }
}
