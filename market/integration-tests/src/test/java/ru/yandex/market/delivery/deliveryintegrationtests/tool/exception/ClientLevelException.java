package ru.yandex.market.delivery.deliveryintegrationtests.tool.exception;

public class ClientLevelException extends RuntimeException {
    private static final String MESSAGE = "ClientLevelException: ";

    public ClientLevelException(Throwable cause) {
        super(MESSAGE + cause.getMessage(), cause);
    }
}
