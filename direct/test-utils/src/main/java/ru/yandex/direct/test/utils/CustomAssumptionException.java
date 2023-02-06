package ru.yandex.direct.test.utils;

public class CustomAssumptionException extends RuntimeException {

    public CustomAssumptionException(AssertionError cause) {
        super(cause.getMessage());
    }
}
