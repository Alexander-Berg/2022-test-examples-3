package ru.yandex.direct.web.entity.cookie.banneraiming.service;

import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.web.entity.cookie.banneraiming.service.BannerAimingServiceTestHelper.getDefaultBannerAimingService;

@RunWith(Parameterized.class)
public class BannerAimingServiceGetSetCookieUrlTest {

    private static final Duration COOKIE_TTL = Duration.ofMinutes(10);

    private static BannerAimingService bannerAimingService;

    @Before
    public void setup() {
        bannerAimingService = getDefaultBannerAimingService();
    }

    @Parameterized.Parameter
    public String hostTDL;

    @Parameterized.Parameter(1)
    public String expectedURL;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] parameters() {
        return new Object[][]{
                {"yandex.ru",
                        "https://yandex.ru/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=1533889605.12345_1533889005_e6f593367af5ee75f94ac45114df31321eefe961&retpath=https%3A%2F%2Fyandex.ru"},
                {"yandex.kz",
                        "https://yandex.kz/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=1533889605.12345_1533889005_e6f593367af5ee75f94ac45114df31321eefe961&retpath=https%3A%2F%2Fyandex.ru"},
                {"yandex.com",
                        "https://yandex.com/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=1533889605.12345_1533889005_e6f593367af5ee75f94ac45114df31321eefe961&retpath=https%3A%2F%2Fyandex.ru"},
                {"yandex.ua",
                        "https://yandex.ua/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=1533889605.12345_1533889005_e6f593367af5ee75f94ac45114df31321eefe961&retpath=https%3A%2F%2Fyandex.ru"},
                {"yandex.com.tr",
                        "https://yandex.com.tr/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=1533889605.12345_1533889005_e6f593367af5ee75f94ac45114df31321eefe961&retpath=https%3A%2F%2Fyandex.ru"},
                {"yandex.ru.tr",
                        "https://yandex.ru/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=1533889605.12345_1533889005_e6f593367af5ee75f94ac45114df31321eefe961&retpath=https%3A%2F%2Fyandex.ru"},
                {"google.com",
                        "https://yandex.ru/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=1533889605.12345_1533889005_e6f593367af5ee75f94ac45114df31321eefe961&retpath=https%3A%2F%2Fyandex.ru"},
                {"yandex.by",
                        "https://yandex.by/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=1533889605.12345_1533889005_e6f593367af5ee75f94ac45114df31321eefe961&retpath=https%3A%2F%2Fyandex.ru"},
        };
    }

    @Test
    public void getRedirectUrlTest() {
        String yandexUid = "1234567";
        long bannerId = 12345;
        String retpath = "https://yandex.ru";
        String gotUrl = bannerAimingService.calculateSetCookieUrl(hostTDL, yandexUid, bannerId, retpath, COOKIE_TTL);
        assertThat(gotUrl).isEqualTo(expectedURL);

    }
}
