package ru.yandex.market.crm.platform.mappers.utils;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MappersUtilsTest {

    @Test
    public void testExtractSessionId() {
        assertNull(MappersUtils.getSessionId(null));
        assertNull(MappersUtils.getSessionId("-"));
        assertNull(MappersUtils.getSessionId("kv"));
        assertNull(MappersUtils.getSessionId("k=v"));
        assertEquals("session_id_value", MappersUtils.getSessionId("k1=v1; session_id=session_id_value; k2=v2"));
    }

    @Test
    public void testSession() {
        String result = MappersUtils.getSessionId("deliveryincluded=1; L=; Session_id=; fuid01=; my=; sessionid2=; " +
                "yandex_login=; yandexuid=693655581434786841");
        Assert.assertNull("Не задано значение session_id", result);
    }

    @Test
    public void testGetPuidFromSessionId() {
        assertNull(MappersUtils.getPuid(""));
        assertNull(MappersUtils.getPuid("no_puid"));

        String sessionId =
                "3:1530606610.5.0.1525368719501:I8GRBQ:26.1|4844809.0.2|1130000027587769.14057.2.2:14057" +
                        "|646451930.325385.2.2:325385|621788919.944982.2.2:944982|621727081.1118327.2.2:1118327" +
                        "|646450762.1125883.2.2:1125883|184002.87642.XXXXXXXXXXXXXXXXXXXXXXXXXXX";
        assertEquals(Long.valueOf(4844809), MappersUtils.getPuid(sessionId));

        sessionId = "3:1530653708.5.0.1498505155509:uuYnLg:56.1|2356249.26503406.2.2:26503406" +
                "|184028.418134.XXXXXXXXXXXXXXXXXXXXXXXXXXX";
        assertEquals(Long.valueOf(2356249), MappersUtils.getPuid(sessionId));
    }
}
