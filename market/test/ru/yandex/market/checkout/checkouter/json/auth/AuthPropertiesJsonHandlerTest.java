package ru.yandex.market.checkout.checkouter.json.auth;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.auth.AuthProperties;
import ru.yandex.market.checkout.checkouter.json.AbstractJsonHandlerTestBase;
import ru.yandex.market.checkout.checkouter.json.Names;

public class AuthPropertiesJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        AuthProperties authProperties = new AuthProperties(true);

        String json = write(authProperties);

        checkJson(json, "$." + Names.AuthProperties.ON, true);
    }
}
