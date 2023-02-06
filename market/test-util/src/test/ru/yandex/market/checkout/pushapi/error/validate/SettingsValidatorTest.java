package ru.yandex.market.checkout.pushapi.error.validate;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.checkout.pushapi.client.entity.shop.SettingsBuilder;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

public class SettingsValidatorTest {

    private SettingsValidator validator = new SettingsValidator();

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testOk() throws Exception {
        validator.validate(new SettingsBuilder().build());
    }

    @Test(expected = ValidationException.class)
    public void testSettingsIsNull() throws Exception {
        validator.validate(null);
    }

    @Test(expected = ValidationException.class)
    public void testUrlPrefixIsNull() throws Exception {
        validator.validate(new SettingsBuilder().build());
    }

    @Test(expected = ValidationException.class)
    public void testAuthTokenIsNull() throws Exception {
        validator.validate(new SettingsBuilder().build());
    }

    @Test(expected = ValidationException.class)
    public void testDataTypeIsNull() throws Exception {
        validator.validate(new SettingsBuilder().build());
    }

    @Test(expected = ValidationException.class)
    public void testAuthTypeIsNull() throws Exception {
        validator.validate(new SettingsBuilder().build());
    }
}
