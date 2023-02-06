package ru.yandex.market.logistic.api;

import javax.validation.Validation;
import javax.validation.Validator;

public class TestUtils {
    public static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private TestUtils() {
        throw new UnsupportedOperationException();
    }
}
