package com.example.eimzoguice.web;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class ServletSupport extends HttpServlet {
    protected String routePath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    protected void forward(HttpServletRequest request, HttpServletResponse response, String path)
            throws ServletException, IOException {
        request.getRequestDispatcher(path).forward(request, response);
    }

    protected void json(HttpServletResponse response, Gson gson, Object body) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(body));
    }

    protected void html(HttpServletResponse response, String path, Map<String, String> values) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html");
        response.getWriter().write(render(path, values));
    }

    private String render(String path, Map<String, String> values) throws IOException {
        try (InputStream input = getServletContext().getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("View not found: " + path);
            }
            String html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : values.entrySet()) {
                html = html.replace("{{" + entry.getKey() + "}}", escapeHtml(entry.getValue()));
            }
            return html;
        }
    }

    protected String requiredParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing request parameter: " + name);
        }
        return value;
    }

    protected Map<String, Object> errorBody(Exception e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 0);
        body.put("message", e.getMessage());
        return body;
    }

    protected String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
