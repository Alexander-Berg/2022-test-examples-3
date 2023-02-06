package ru.yandex.market.mbo.db.validator;

public class ObjectConsistencyValidatorException extends RuntimeException {

    public ObjectConsistencyValidatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
