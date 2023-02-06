package ru.yandex.vendor.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static ru.yandex.vendor.util.VendorValidators.*;

public class VendorValidatorsTest {

    @Rule
    public final ExpectedException expectedExceptionRule = ExpectedException.none();

    @Test
    public void non_null_validator_does_not_throw_on_value() throws Exception {
        nonNullValidator("qwe").accept(new Object());
    }

    @Test
    public void non_null_validator_throws_exception_on_null() throws Exception {
        String message = "some error message";
        expectedExceptionRule.expect(NullPointerException.class);
        expectedExceptionRule.expectMessage(message);
        nonNullValidator(message).accept(null);
    }
}