package ru.yandex.market.mboc.common.utils;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextAuthenticationHelper {

    private static final String DEFAULT_TEST_USER = "test-user";

    private SecurityContextAuthenticationHelper() {

    }

    public static void setAuthenticationToken() {
        setAuthenticationToken(DEFAULT_TEST_USER);
    }

    public static void setAuthenticationToken(String username) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, null));
    }

    public static void clearAuthenticationToken() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }
}
