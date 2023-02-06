package ru.yandex.market.robot.utils;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.robot.shared.utils.ProxyUtils;

/**
 * @author Tatiana Goncharova <a href="mailto:tanlit@yandex-team.ru"/>
 * @date 12.07.2017
 */
public class ProxyUtilsTest extends Assert {
    @Test
    public void testProxyUtils() throws Exception {

        assertEquals(
            "https://kupi-kolyasku.ru/market",
            ProxyUtils.getOriginalUrl(
                "https://kupi-kolyasku_rud_ru_rud_https.proxy-front.mbo-testing.market.yandex-team.ru/market"
            )
        );
        assertEquals(
            "http://static.220-volt.ru",
            ProxyUtils.getOriginalUrl("https://static_rud_220-volt_rud_ru_rud_http.proxy-front.mbo.yandex.ru")
        );


        assertEquals(
            "https://static_rud_220-volt_rud_ru_rud_http.proxy-front.mbo.yandex.ru",
            ProxyUtils.getProxyUrl("http://static.220-volt.ru", ".proxy-front.mbo.yandex.ru", false, false)
        );
        assertEquals(
            "https://static_rud_220-volt_rud_ru_rud_jsinvhttps.proxy-front.mbo.yandex.ru",
            ProxyUtils.getProxyUrl("https://static.220-volt.ru", ".proxy-front.mbo.yandex.ru", true, true)
        );
    }
}
