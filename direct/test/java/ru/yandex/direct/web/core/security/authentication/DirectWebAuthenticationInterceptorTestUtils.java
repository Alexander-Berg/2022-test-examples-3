package ru.yandex.direct.web.core.security.authentication;

import org.assertj.core.api.Condition;

import ru.yandex.direct.core.security.AccessDeniedException;

class DirectWebAuthenticationInterceptorTestUtils {
    static ThrowsException notOk() {
        return new ThrowsException();
    }

    static ExceptionIsAbsent ok() {
        return new ExceptionIsAbsent();
    }

    public static class ThrowsException extends Condition<Exception> {
        @Override
        public boolean matches(Exception value) {
            return value != null && value instanceof AccessDeniedException;
        }
    }

    public static class ExceptionIsAbsent extends Condition<Exception> {
        @Override
        public boolean matches(Exception value) {
            return value == null;
        }
    }
}
