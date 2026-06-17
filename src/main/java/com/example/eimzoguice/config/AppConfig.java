package com.example.eimzoguice.config;

import com.google.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;

@Singleton
public class AppConfig {
    private static final String CONFIG_FILE = "application.properties";
    private static final String YAML_CONFIG_FILE = "application.yaml";

    private final Properties properties = new Properties();

    public AppConfig() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
                return;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + CONFIG_FILE, e);
        }

        try (InputStream input = classLoader.getResourceAsStream(YAML_CONFIG_FILE)) {
            if (input == null) {
                throw new IllegalStateException(CONFIG_FILE + " or " + YAML_CONFIG_FILE + " was not found on the classpath");
            }
            loadYaml(input);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + YAML_CONFIG_FILE, e);
        }
    }

    public String required(String key) {
        String value = System.getProperty(key, properties.getProperty(key));
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration: " + key);
        }
        return value;
    }

    private void loadYaml(InputStream input) throws IOException {
        Deque<String> path = new ArrayDeque<>();
        for (String line : new String(input.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
            String withoutComment = stripComment(line);
            if (withoutComment.isBlank()) {
                continue;
            }

            int indent = countLeadingSpaces(withoutComment);
            String trimmed = withoutComment.trim();
            int separator = trimmed.indexOf(':');
            if (separator < 0) {
                continue;
            }

            int depth = indent / 2;
            while (path.size() > depth) {
                path.removeLast();
            }

            String key = trimmed.substring(0, separator).trim();
            String value = trimmed.substring(separator + 1).trim();
            if (value.isEmpty()) {
                path.addLast(key);
                continue;
            }

            path.addLast(key);
            properties.setProperty(String.join(".", path), unquote(value));
            path.removeLast();
        }
    }

    private String stripComment(String line) {
        int comment = line.indexOf('#');
        return comment < 0 ? line : line.substring(0, comment);
    }

    private int countLeadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
