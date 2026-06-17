package com.example.eimzoguice.util;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class BaseUtils {
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final String LOCALHOST_IPV6_SHORT = "::1";

    private BaseUtils() {
    }

    public static String getClientIp(HttpServletRequest req) {
        String ip = firstHeaderValue(req.getHeader("X-Real-IP"));
        if (isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = firstHeaderValue(req.getHeader("X-Forwarded-For"));
        }
        if (isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = getRemoteAddr(req);
        }
        return normalizeIp(ip);
    }

    private static String getRemoteAddr(HttpServletRequest req) {
        String remoteAddr = req.getRemoteAddr();
        if (remoteAddr == null) {
            throw new IllegalArgumentException("IP Not Found");
        }
        return remoteAddr;
    }

    private static String firstHeaderValue(String headerValue) {
        if (isEmpty(headerValue)) {
            return headerValue;
        }
        int commaIndex = headerValue.indexOf(",");
        return commaIndex >= 0 ? headerValue.substring(0, commaIndex).trim() : headerValue.trim();
    }

    private static String normalizeIp(String ip) {
        if (LOCALHOST_IPV6.equals(ip) || LOCALHOST_IPV6_SHORT.equals(ip)) {
            return LOCALHOST_IPV4;
        }
        if (!isValidIp(ip)) {
            throw new IllegalArgumentException("Client IP is not valid: " + ip);
        }
        return ip;
    }

    private static boolean isValidIp(String ip) {
        if (isEmpty(ip)) {
            return false;
        }
        if (ip.matches("^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.|$)){4}$")) {
            return true;
        }
        if (!ip.contains(":") || !ip.matches("^[0-9a-fA-F:.]+$")) {
            return false;
        }
        try {
            return InetAddress.getByName(ip).getHostAddress().contains(":");
        } catch (UnknownHostException e) {
            return false;
        }
    }

    public static boolean isEmpty(String value) {
        return value == null || value.isBlank();
    }
}
