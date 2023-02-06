package ru.yandex.market.checkout.pushapi.service.shop.settings;

public class SettingsServiceException extends RuntimeException {
    public SettingsServiceException() {
    }

    public SettingsServiceException(String message) {
        super(message);
    }

    public SettingsServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public SettingsServiceException(Throwable cause) {
        super(cause);
    }

    public SettingsServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
