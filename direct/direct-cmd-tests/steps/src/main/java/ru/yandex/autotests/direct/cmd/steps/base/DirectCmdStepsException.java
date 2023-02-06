package ru.yandex.autotests.direct.cmd.steps.base;

public class DirectCmdStepsException extends RuntimeException {

    public DirectCmdStepsException(String message) {
        super(message);
    }

    public DirectCmdStepsException(String message, Throwable cause) {
        super(message, cause);
    }
}
