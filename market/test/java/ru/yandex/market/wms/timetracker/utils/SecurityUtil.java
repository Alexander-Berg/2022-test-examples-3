package ru.yandex.market.wms.timetracker.utils;

import java.util.List;
import java.util.Objects;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.auth.core.model.InforAuthentication;

public class SecurityUtil {

    private static final String ATTR = "SPRING_SECURITY_CONTEXT";

    private SecurityUtil() {

    }

    public static void setSecurityContextAttribute(MockHttpServletRequestBuilder requestBuilder, String username) {
        requestBuilder.sessionAttr(
                ATTR,
                createSecurityContext(Objects.requireNonNullElse(username, "defaultTestUser"))
        );
    }

    private static SecurityContext createSecurityContext(String username) {
        var tokenContent = username + "-token";
        var token = new InforAuthentication(username, tokenContent, List.of());
        token.setAuthenticated(true);

        var context = SecurityContextHolder.getContext();
        context.setAuthentication(token);
        return context;
    }
}
