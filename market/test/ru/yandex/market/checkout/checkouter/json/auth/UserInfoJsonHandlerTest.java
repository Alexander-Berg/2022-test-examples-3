package ru.yandex.market.checkout.checkouter.json.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.auth.UserInfo;
import ru.yandex.market.checkout.checkouter.json.AbstractJsonHandlerTestBase;
import ru.yandex.market.checkout.checkouter.json.Names;

public class UserInfoJsonHandlerTest extends AbstractJsonHandlerTestBase {

    private static final String IP = "127.0.0.1";
    private static final String USER_AGENT = "chrome";

    @Test
    public void serialize() throws Exception {
        UserInfo userInfo = new UserInfo(IP, USER_AGENT);

        String json = write(userInfo);

        checkJson(json, "$." + Names.UserInfo.IP, IP);
        checkJson(json, "$." + Names.UserInfo.USER_AGENT, USER_AGENT);
    }

    @Test
    public void deserialize() throws Exception {
        String json = "{ \"ip\": \"127.0.0.1\", \"userAgent\": \"chrome\" }";

        UserInfo userInfo = read(UserInfo.class, json);

        Assertions.assertEquals(IP, userInfo.getIp());
        Assertions.assertEquals(USER_AGENT, userInfo.getUserAgent());
    }
}
