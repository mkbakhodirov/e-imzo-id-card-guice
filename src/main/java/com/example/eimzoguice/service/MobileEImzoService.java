package com.example.eimzoguice.service;

import com.example.eimzoguice.config.AppConfig;
import com.example.eimzoguice.util.BaseUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

@Singleton
public class MobileEImzoService {
    private final URL baseUrl;

    @Inject
    public MobileEImzoService(AppConfig config) {
        try {
            this.baseUrl = new URL(config.required("eimzo.rest.service.host.base"));
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid E-IMZO base URL", e);
        }
    }

    public String mobileAuth(HttpServletRequest request) throws IOException {
        return post("/frontend/mobile/auth", request, Map.of());
    }

    public String mobileSign(HttpServletRequest request) throws IOException {
        return post("/frontend/mobile/sign", request, Map.of());
    }

    public String mobileStatus(HttpServletRequest request, String documentId) throws IOException {
        return post("/frontend/mobile/status", request, Map.of("documentId", documentId));
    }

    public String authenticate(HttpServletRequest request, String documentId) throws IOException {
        return get("/backend/mobile/authenticate/" + encodePath(documentId), request);
    }

    public String verify(HttpServletRequest request, String documentId, String document64) throws IOException {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("documentId", documentId);
        form.put("document", document64);
        return post("/backend/mobile/verify", request, form);
    }

    private String get(String path, HttpServletRequest request) throws IOException {
        HttpURLConnection connection = open(path, request);
        connection.setRequestMethod("GET");
        return read(connection);
    }

    private String post(String path, HttpServletRequest request, Map<String, String> form) throws IOException {
        byte[] body = formBody(form).getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = open(path, request);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", String.valueOf(body.length));
        connection.setDoOutput(true);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body);
        }
        return read(connection);
    }

    private HttpURLConnection open(String path, HttpServletRequest request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl, path).openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Host", host(request));
        connection.setRequestProperty("X-Real-IP", BaseUtils.getClientIp(request));
        connection.setRequestProperty("X-Forwarded-Host", host(request));
        connection.setRequestProperty("X-Real-Host", host(request));
        return connection;
    }

    private String read(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        InputStreamReader reader = new InputStreamReader(
                status >= 200 && status < 400 ? connection.getInputStream() : connection.getErrorStream(),
                StandardCharsets.UTF_8
        );
        StringBuilder body = new StringBuilder();
        try (BufferedReader buffered = new BufferedReader(reader)) {
            String line;
            while ((line = buffered.readLine()) != null) {
                body.append(line).append('\n');
            }
        } finally {
            connection.disconnect();
        }
        if (status != 200) {
            throw new IOException(body.toString());
        }
        return body.toString();
    }

    private String formBody(Map<String, String> form) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : form.entrySet()) {
            joiner.add(encode(entry.getKey()) + "=" + encode(entry.getValue()));
        }
        return joiner.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String encodePath(String value) {
        return encode(value).replace("+", "%20");
    }

    private String host(HttpServletRequest request) {
        String host = request.getHeader("Host");
        return host == null || host.isBlank() ? request.getServerName() : host;
    }
}
