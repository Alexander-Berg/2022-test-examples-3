package ru.yandex.direct.grid.processing.util;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.converters.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;

@ParametersAreNonnullByDefault
public class TestAuthHelper {

    public static void setDirectAuthentication(User operator) {
        setDirectAuthentication(operator, operator);
    }

    public static void setDirectAuthentication(User operator, User subjectUser) {
        DirectAuthentication directAuthentication = new DirectAuthentication(operator, subjectUser);
        setAuthentication(directAuthentication);
    }

    public static void setNullAuthentication() {
        setAuthentication(null);
    }

    private static void setAuthentication(@Nullable Authentication directAuthentication) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(ctx);
        ctx.setAuthentication(directAuthentication);
    }

}
