package com.saucedemo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    private static final Properties properties = new Properties();

    static {
        // 1. Load base config.properties as defaults
        Properties base = loadProperties("config/config.properties");

        // 2. Resolve active environment: -Denv=<name> overrides default.env in config.properties
        String env = System.getProperty("env", base.getProperty("default.env", "dev"));

        // 3. Load env-specific env.properties
        Properties envProps = loadProperties("config/" + env + "/env.properties");

        // 4. Start with base defaults, then overlay env values
        properties.putAll(base);
        properties.putAll(envProps);

        // 5. JVM system properties (-Dkey=value) take highest priority
        //    This allows CI to override e.g. -Dexecution.headless=true -Dbrowser.name=chromium
        System.getProperties().forEach((key, value) -> {
            if (properties.containsKey(key.toString())) {
                properties.setProperty(key.toString(), value.toString());
            }
        });
    }

    private static Properties loadProperties(String resourcePath) {
        Properties props = new Properties();
        try (InputStream input = ConfigReader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input != null) props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load: " + resourcePath, e);
        }
        return props;
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    public static int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }
}
