package ru.yandex.market.hrms.e2etests.tools.exception;

/**
 * Ошибка валидации, которую уже ретраили и повторно ретраить не нужно
 */
public class RetriedAssertionError extends AssertionError {
    public RetriedAssertionError(Throwable cause) {
        super(cause);
    }
}
