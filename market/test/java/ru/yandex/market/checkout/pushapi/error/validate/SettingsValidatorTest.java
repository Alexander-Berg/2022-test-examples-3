package ru.yandex.market.checkout.pushapi.error.validate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Features;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

public class SettingsValidatorTest {

    private SettingsValidator validator = new SettingsValidator();

    @BeforeEach
    public void setUp() throws Exception {
    }

    @Test
    public void testOk() throws Exception {
        validator.validate(Settings.builder()
                .partnerInterface(true)
                .dataType(DataType.XML)
                .authToken("some token")
                .authType(AuthType.URL)
                .urlPrefix("prefix")
                .features(Features.builder()
                        .enabledGenericBundleSupport(true)
                        .build())
                .build());
    }

    @Test
    public void testSettingsIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(null);
        });
    }

    @Test
    public void testUrlPrefixIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(Settings.builder().partnerInterface(false).urlPrefix(null).build());
        });
    }

    @Test
    public void testAuthTokenIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(Settings.builder().partnerInterface(false).authToken(null).build());
        });
    }

    @Test
    public void testDataTypeIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(Settings.builder().partnerInterface(false).dataType(null).build());
        });
    }

    @Test
    public void testAuthTypeIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(Settings.builder().partnerInterface(false).authType(null).build());
        });
    }
}
