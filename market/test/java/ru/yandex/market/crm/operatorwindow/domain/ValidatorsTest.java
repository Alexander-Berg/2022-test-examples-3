package ru.yandex.market.crm.operatorwindow.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.operatorwindow.domain.validation.Validator;
import ru.yandex.market.crm.operatorwindow.domain.validation.Validators;

public class ValidatorsTest {

    @Test
    public void intRangeTest() {
        final Validator<Integer> integerValidator = Validators.intRange(1, 10);
        Assertions.assertTrue(integerValidator.validate(0).hasError());
        Assertions.assertTrue(integerValidator.validate(1).isOk());
        Assertions.assertTrue(integerValidator.validate(10).isOk());
        Assertions.assertTrue(integerValidator.validate(11).hasError());
    }

}
