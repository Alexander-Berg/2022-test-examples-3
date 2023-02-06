package ru.yandex.market.hrms.e2etests.tools.exception;

public class ClientLevelException extends RuntimeException {
    private static final String MESSAGE = "ClientLevelException: ";

    public ClientLevelException(Throwable cause) {
        super(MESSAGE + cause.getMessage(), cause);
    }
}
