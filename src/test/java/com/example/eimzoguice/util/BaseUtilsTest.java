package com.example.eimzoguice.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseUtilsTest {
    @Test
    void getClientIpNormalizesIpv6LoopbackRemoteAddress() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");

        assertThat(BaseUtils.getClientIp(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void getClientIpUsesRealIpHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.10");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThat(BaseUtils.getClientIp(request)).isEqualTo("192.168.1.10");
    }

    @Test
    void getClientIpUsesFirstForwardedIpWhenRealIpIsMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.5, 10.0.0.6");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThat(BaseUtils.getClientIp(request)).isEqualTo("10.0.0.5");
    }

    @Test
    void getClientIpRejectsInvalidHeaderValue() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Real-IP")).thenReturn("not-an-ip");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThatThrownBy(() -> BaseUtils.getClientIp(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Client IP is not valid");
    }
}
