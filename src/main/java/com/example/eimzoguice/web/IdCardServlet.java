package com.example.eimzoguice.web;

import com.example.eimzoguice.service.MobileEImzoService;
import com.google.gson.Gson;
import com.google.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

public class IdCardServlet extends ServletSupport {
    private static final Pattern DOCUMENT_ID = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private final MobileEImzoService mobileEImzoService;
    private final Gson gson;

    @Inject
    public IdCardServlet(MobileEImzoService mobileEImzoService, Gson gson) {
        this.mobileEImzoService = mobileEImzoService;
        this.gson = gson;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        switch (routePath(request)) {
            case "/demo/eimzoidcard", "/demo/eimzoidcard/" -> forward(request, response, "/demo/eimzoidcard/index.html");
            case "/demo/eimzoidcard/user_auth_result" -> authResult(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            switch (routePath(request)) {
                case "/demo/eimzoidcard/doc_verify_result" -> verifyResult(request, response);
                default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            json(response, gson, errorBody(e));
        }
    }

    private void authResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String documentId = requiredParameter(request, "documentId");
            if (!DOCUMENT_ID.matcher(documentId).matches()) {
                throw new IllegalArgumentException("Invalid documentId");
            }
            String backendResponse = mobileEImzoService.authenticate(request, documentId);
            resultPage(response, backendResponse, false);
        } catch (Exception e) {
            resultPage(response, gson.toJson(errorBody(e)), false);
        }
    }

    private void verifyResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String document = request.getParameter("Document");
            String document64 = request.getParameter("Document64");
            if ((document == null || document.isBlank()) && (document64 == null || document64.isBlank())) {
                throw new IllegalArgumentException("POST parameter Document or Document64 was not provided");
            }
            String payload = document64 == null || document64.isBlank()
                    ? Base64.getEncoder().encodeToString(document.getBytes(StandardCharsets.UTF_8))
                    : document64;
            if ((document == null ? payload : document).length() > 128) {
                throw new IllegalArgumentException("Size must not exceed 128 because this is a test");
            }
            String backendResponse = mobileEImzoService.verify(request, requiredParameter(request, "documentId"), payload);
            resultPage(response, backendResponse, true);
        } catch (Exception e) {
            resultPage(response, gson.toJson(errorBody(e)), true);
        }
    }

    private void resultPage(HttpServletResponse response, String backendResponse, boolean includeVerificationInfo)
            throws IOException {
        html(response, "/demo/eimzoidcard/result.html", Map.of(
                "backendResponse", backendResponse,
                "includeVerificationInfo", String.valueOf(includeVerificationInfo)
        ));
    }

}
