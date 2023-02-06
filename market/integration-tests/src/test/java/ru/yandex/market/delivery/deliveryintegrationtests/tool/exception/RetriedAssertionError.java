package ru.yandex.market.delivery.deliveryintegrationtests.tool.exception;

/**
 * Ошибка валидации, которую уже ретраили и повторно ретраить не нужно
 */
public class RetriedAssertionError extends AssertionError {
    public RetriedAssertionError(Throwable cause) {
        super(cause);
    }
}
