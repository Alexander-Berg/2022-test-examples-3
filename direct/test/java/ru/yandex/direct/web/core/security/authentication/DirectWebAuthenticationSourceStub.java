package ru.yandex.direct.web.core.security.authentication;

import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;

import static com.google.common.base.Preconditions.checkNotNull;

public class DirectWebAuthenticationSourceStub extends DirectWebAuthenticationSource {
    private final DirectAuthentication auth;

    public DirectWebAuthenticationSourceStub(DirectAuthentication auth) {
        this.auth = auth;
    }

    @Override
    public DirectAuthentication getAuthentication() {
        return checkNotNull(auth);
    }

    @Override
    public boolean isAuthenticated() {
        return auth != null;
    }
}
