package ru.yandex.direct.web.entity.cookie.banneraiming.service;

import java.util.List;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.web.entity.cookie.banneraiming.service.BannerAimingServiceTestHelper.getDefaultBannerAimingServiceWithSecretKeyValue;
import static ru.yandex.direct.web.entity.cookie.banneraiming.service.BannerAimingServiceTestHelper.getDefaultTimestamp;

public class BannerAimingServiceSecretKeyTest {

    private static BannerAimingService bannerAimingService;

    private long timestamp = getDefaultTimestamp();

    @Test
    public void correctSecretKeyResourceTest() {
        String secretKeyValue =
                "[{\"t\":1533889007, \"d\":\"ZyTB2F+0zdb19i5+h6gASJwCKG8=\", \"f\":1533889003}, {\"t\":1533889003, " +
                        "\"d\":\"FupNBYznQEt7twbTVnhQ/Wu8WJQ=\", \"f\":1533889002}, {\"t\":1533889007, " +
                        "\"d\":\"nAHY+CpOo+p1KNMQNhtz3VxzrLE=\", \"f\":1533889012}, {\"t\":1539510777, " +
                        "\"d\":\"HNXqC3tT/WKa0BZ6lfsHbshRENg=\", \"f\":1539424377}, {\"t\":1539424377, " +
                        "\"d\":\"oWOjT/ab2dfuoWZs11r1KcUJ3PU=\", \"f\":1539337977}, {\"t\":1539337977, " +
                        "\"d\":\"EVhMMcBY7aY4GC4ggC5bb2bdxuo=\", \"f\":1539251577}, {\"t\":1539251577, " +
                        "\"d\":\"JZcXJ7nHxSoRe43n8WUR97xar3A=\", \"f\":1539165177}]";
        bannerAimingService = getDefaultBannerAimingServiceWithSecretKeyValue(secretKeyValue);
        String expected = "ZyTB2F+0zdb19i5+h6gASJwCKG8=";
        String got = bannerAimingService.getSecretKeyForSign(timestamp);
        assertThat(got).isEqualTo(expected);
    }

    @Test
    public void secretKeyResourceWithNoSuitableTest() {
        String secretKeyValue =
                "[{\"t\":1533889003, \"d\":\"FupNBYznQEt7twbTVnhQ/Wu8WJQ=\", \"f\":1533889002}, {\"t\":1533889007, " +
                        "\"d\":\"nAHY+CpOo+p1KNMQNhtz3VxzrLE=\", \"f\":1533889012}, {\"t\":1539510777, " +
                        "\"d\":\"HNXqC3tT/WKa0BZ6lfsHbshRENg=\", \"f\":1539424377}, {\"t\":1539424377, " +
                        "\"d\":\"oWOjT/ab2dfuoWZs11r1KcUJ3PU=\", \"f\":1539337977}, {\"t\":1539337977, " +
                        "\"d\":\"EVhMMcBY7aY4GC4ggC5bb2bdxuo=\", \"f\":1539251577}, {\"t\":1539251577, " +
                        "\"d\":\"JZcXJ7nHxSoRe43n8WUR97xar3A=\", \"f\":1539165177}]";
        bannerAimingService = getDefaultBannerAimingServiceWithSecretKeyValue(secretKeyValue);
        assertThatThrownBy(() -> bannerAimingService.getSecretKeyForSign(timestamp))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void secretKeyResourceWithSeveralSuitableTest() {
        String secretKeyValue =
                "[{\"t\":1533889007, \"d\":\"ZyTB2F+0zdb19i5+h6gASJwCKG2=\", \"f\":1533889003}, {\"t\":1533889006, " +
                        "\"d\":\"ZyTB2F+0zdb19i5+h6gASJwCKG2=\", \"f\":1533889004}, {\"t\":1533889003, " +
                        "\"d\":\"FupNBYznQEt7twbTVnhQ/Wu8WJQ=\", \"f\":1533889002}, {\"t\":1533889007, " +
                        "\"d\":\"nAHY+CpOo+p1KNMQNhtz3VxzrLE=\", \"f\":1533889012}, {\"t\":1539510777, " +
                        "\"d\":\"HNXqC3tT/WKa0BZ6lfsHbshRENg=\", \"f\":1539424377}, {\"t\":1539424377, " +
                        "\"d\":\"oWOjT/ab2dfuoWZs11r1KcUJ3PU=\", \"f\":1539337977}, {\"t\":1539337977, " +
                        "\"d\":\"EVhMMcBY7aY4GC4ggC5bb2bdxuo=\", \"f\":1539251577}, {\"t\":1539251577, " +
                        "\"d\":\"JZcXJ7nHxSoRe43n8WUR97xar3A=\", \"f\":1539165177}]";
        bannerAimingService = getDefaultBannerAimingServiceWithSecretKeyValue(secretKeyValue);
        List<String> expected = asList("ZyTB2F+0zdb19i5+h6gASJwCKG1=", "ZyTB2F+0zdb19i5+h6gASJwCKG2=");
        String got = bannerAimingService.getSecretKeyForSign(timestamp);

        assertThat(got).isIn(expected);
    }
}
