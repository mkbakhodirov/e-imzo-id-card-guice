package com.example.eimzoguice.web;

import com.example.eimzoguice.service.MobileEImzoService;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
            case "/demo/eimzoidcard/user_auth_result.php" -> authResult(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            switch (routePath(request)) {
                case "/demo/eimzoidcard/doc_verify_result.php" -> verifyResult(request, response);
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
        JsonObject json = gson.fromJson(backendResponse, JsonObject.class);
        StringBuilder body = new StringBuilder();
        body.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <link rel="stylesheet" href="css/bootstrap.min.css">
                    <script src="js/bootstrap.min.js"></script>
                </head>
                <body>
                """);

        if (json == null || intValue(json, "status") != 1) {
            body.append("<div class=\"alert alert-danger\" role=\"alert\">")
                    .append(escapeHtml(stringValue(json, "message")))
                    .append("</div>");
        } else {
            body.append("<div class=\"container\">");
            appendCertificateInfo(body, json);
            if (includeVerificationInfo) {
                appendObjectRows(body, objectValue(json, "verificationInfo"));
            }
            body.append("</div>");
        }

        body.append("""
                <form>
                <div class="form-group">
                    <label for="exampleFormControlTextarea1">JSON</label>
                    <textarea class="form-control" id="exampleFormControlTextarea1" rows="10">
                """);
        body.append(escapeHtml(backendResponse));
        body.append("""
                    </textarea>
                  </div>
                </form>
                </body>
                </html>
                """);

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html");
        response.getWriter().write(body.toString());
    }

    private void appendCertificateInfo(StringBuilder body, JsonObject json) {
        JsonObject info = objectValue(json, "subjectCertificateInfo");
        JsonObject subjectName = objectValue(info, "subjectName");
        body.append(row("Certificate SN", stringValue(info, "serialNumber")));
        body.append(row("Certificate Validity", stringValue(info, "validFrom") + " - " + stringValue(info, "validTo")));
        appendObjectRows(body, subjectName);
    }

    private void appendObjectRows(StringBuilder body, JsonObject object) {
        if (object == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            body.append(row(entry.getKey(), displayValue(entry.getValue())));
        }
    }

    private String row(String key, String value) {
        return "<div class=\"row\"><div class=\"col-sm\">"
                + escapeHtml(key)
                + "</div><div class=\"col-sm\">"
                + escapeHtml(value)
                + "</div></div>";
    }

    private String displayValue(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return "";
        }
        if (value.isJsonArray()) {
            StringBuilder joined = new StringBuilder();
            for (JsonElement element : value.getAsJsonArray()) {
                if (!joined.isEmpty()) {
                    joined.append(",");
                }
                joined.append(displayValue(element));
            }
            return joined.toString();
        }
        if (value.isJsonPrimitive()) {
            return value.getAsString();
        }
        return gson.toJson(value);
    }

    private JsonObject objectValue(JsonObject object, String member) {
        if (object == null || !object.has(member) || !object.get(member).isJsonObject()) {
            return null;
        }
        return object.getAsJsonObject(member);
    }

    private String stringValue(JsonObject object, String member) {
        if (object == null || !object.has(member) || object.get(member).isJsonNull()) {
            return "";
        }
        return object.get(member).getAsString();
    }

    private int intValue(JsonObject object, String member) {
        if (object == null || !object.has(member) || object.get(member).isJsonNull()) {
            return 0;
        }
        return object.get(member).getAsInt();
    }

}
