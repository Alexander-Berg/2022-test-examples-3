package ru.yandex.direct.web.entity.cookie.banneraiming.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.web.entity.cookie.banneraiming.service.BannerAimingServiceTestHelper.getDefaultBannerAimingService;

@RunWith(Parameterized.class)
public class BannerAimingServiceGetResetCookieUrlTest {

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
                        "https://yandex.ru/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=&retpath=https%3A%2F%2Fyandex.ru"},
                {"yandex.kz",
                        "https://yandex.kz/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=&retpath=https%3A%2F%2Fyandex.ru"},
                {"yandex.com",
                        "https://yandex.com/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=&retpath=https%3A%2F%2Fyandex.ru"},
                {"yandex.ua",
                        "https://yandex.ua/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=&retpath=https%3A%2F%2Fyandex.ru"},
                {"yandex.com.tr",
                        "https://yandex.com.tr/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=&retpath=https%3A%2F%2Fyandex.ru"},
                {"yandex.ru.tr",
                        "https://yandex.ru/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=&retpath=https%3A%2F%2Fyandex.ru"},
                {"google.com",
                        "https://yandex.ru/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=&retpath=https%3A%2F%2Fyandex.ru"},
                {"yandex.by",
                        "https://yandex.by/portal/set/any/?sk=uc0a51ad8933e2397281aa1b1e189f156&adoc=&retpath=https%3A%2F%2Fyandex.ru"},
        };
    }

    @Test
    public void getRedirectUrlTest() {
        String retpath = "https://yandex.ru";
        String gotUrl = bannerAimingService.getResetCookieUrl(hostTDL, retpath);
        assertThat(gotUrl).isEqualTo(expectedURL);

    }
}
