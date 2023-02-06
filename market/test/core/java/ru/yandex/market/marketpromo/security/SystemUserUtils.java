package ru.yandex.market.marketpromo.security;

import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.springframework.security.core.context.SecurityContextHolder;

import ru.yandex.market.marketpromo.core.application.security.MBOCAuthenticationRequest;

public final class SystemUserUtils {

    private SystemUserUtils() {
    }

    public static void loginAsSystem() {
        SecurityContextHolder.getContext().setAuthentication(MBOCAuthenticationRequest.builder()
                .roles(SystemUser.SYSTEM.getRoles())
                .build());
        SystemUserHolder.setCurrentUser(SystemUser.SYSTEM);
    }

    public static void runAsSystem(@Nonnull Runnable runnable) {
        SystemUserHolder.setCurrentUser(SystemUser.SYSTEM);
        SystemUserHolder.wrap(runnable).run();
    }

    public static <T> T doAsSystem(@Nonnull Callable<T> callable) throws Exception {
        SystemUserHolder.setCurrentUser(SystemUser.SYSTEM);
        return SystemUserHolder.wrap(callable).call();
    }
}
