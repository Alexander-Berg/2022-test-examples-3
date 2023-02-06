package ru.yandex.common.services.auth.blackbox;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by kudrale on 12.01.16.
 */
public class UserInfoResponseParserTest {

    @Test
    public void testNull() {
        UserInfoResponseParser userInfoResponseParser = new UserInfoResponseParser();
        userInfoResponseParser.onUserId(1000L);
        Assert.assertNull(userInfoResponseParser.getParsedModel());
    }

    @Test
    public void testLogin() throws IOException {
        UserInfo userInfo = new UserInfoResponseParser().parse(getClass().getResourceAsStream("userInfo.response.xml"));
        assertNotNull(userInfo);
        assertEquals("aleksfes.beru.3", userInfo.getLogin());
        assertEquals(1090807280l, userInfo.getUid());
    }
}
