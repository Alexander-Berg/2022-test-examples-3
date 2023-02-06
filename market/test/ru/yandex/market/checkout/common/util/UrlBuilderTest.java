package ru.yandex.market.checkout.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UrlBuilderTest {

    @Test
    public void shouldHandleIpv6Address() {
        String address = "http://[2001:cdba:0000:0000:0000:0000:3257:9652]:39001/ping";
        UrlBuilder urlBuilder = UrlBuilder.fromString(address);
        Assertions.assertEquals("http", urlBuilder.scheme);
        Assertions.assertEquals("[2001:cdba:0000:0000:0000:0000:3257:9652]", urlBuilder.hostName);
        Assertions.assertNotNull(urlBuilder.port);
        Assertions.assertEquals(39001, urlBuilder.port.intValue());
        Assertions.assertEquals("/ping", urlBuilder.path);
    }

    @Test
    public void shouldStillHandleIpv4Address() {
        String address = "http://timursha@192.168.10.112:8080/ping";
        UrlBuilder urlBuilder = UrlBuilder.fromString(address);
        Assertions.assertEquals("192.168.10.112", urlBuilder.hostName);
    }

    @Test
    public void shouldStillHandleHosts() {
        String addrerss = "http://yandex.ru/ping";
        UrlBuilder urlBuilder = UrlBuilder.fromString(addrerss);
        Assertions.assertEquals("yandex.ru", urlBuilder.hostName);
    }
}
