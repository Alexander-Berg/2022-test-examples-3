package ru.yandex.market.checkout.checkouter.json.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.auth.AuthInfo;
import ru.yandex.market.checkout.checkouter.json.AbstractJsonHandlerTestBase;
import ru.yandex.market.checkout.checkouter.json.Names;

public class AuthInfoJsonHandlerTest extends AbstractJsonHandlerTestBase {

    private static final long MUID = 123L;
    private static final String COOKIE = "cookie";

    @Test
    public void serialize() throws Exception {
        AuthInfo authInfo = new AuthInfo(MUID, COOKIE);

        String json = write(authInfo);

        checkJson(json, "$." + Names.AuthInfo.MUID, String.valueOf(MUID));
        checkJson(json, "$." + Names.AuthInfo.COOKIE, COOKIE);
    }

    @Test
    public void deserialize() throws Exception {
        String json = "{ \"muid\": \"123\", \"cookie\": \"cookie\" }";

        AuthInfo authInfo = read(AuthInfo.class, json);

        Assertions.assertEquals(123L, authInfo.getMuid().longValue());
        Assertions.assertEquals("cookie", authInfo.getCookie());
    }
}
