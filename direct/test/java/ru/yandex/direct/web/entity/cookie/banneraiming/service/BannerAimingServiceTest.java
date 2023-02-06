package ru.yandex.direct.web.entity.cookie.banneraiming.service;

import java.time.Clock;
import java.time.Duration;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.web.entity.cookie.banneraiming.service.BannerAimingServiceTestHelper.getDefaultBannerAimingService;
import static ru.yandex.direct.web.entity.cookie.banneraiming.service.BannerAimingServiceTestHelper.getDefaultClock;
import static ru.yandex.direct.web.entity.cookie.banneraiming.service.BannerAimingServiceTestHelper.getDefaultTimestamp;

public class BannerAimingServiceTest {

    private static final Duration COOKIE_TTL = Duration.ofMinutes(10);

    private static BannerAimingService bannerAimingService;

    private long timestamp = getDefaultTimestamp();

    private Clock clock = getDefaultClock();

    @BeforeClass
    public static void setUp() {
        bannerAimingService = getDefaultBannerAimingService();
    }

    @Test
    public void calculateSignTest() {
        String yandexUid = "1234567";
        long bannerId = 12345;
        long expire = 1533899005L;
        String expected = "2f5f19295f1192113d84cfc4b07653095a96ac72";
        String got = bannerAimingService.calculateSign(expire, timestamp, yandexUid, bannerId);
        assertThat(got).isEqualTo(expected);
    }

    @Test
    public void calculateCookieSecretKeyTest() {
        String expected = "uc0a51ad8933e2397281aa1b1e189f156";
        String got = bannerAimingService.calculateCookieSecretKey(clock.instant());
        assertThat(got).isEqualTo(expected);
    }

    @Test
    public void calculateCookieValueTest() {
        String yandexUid = "1234567";
        long bannerId = 12345;
        String expected = "1533889605.12345_1533889005_e6f593367af5ee75f94ac45114df31321eefe961";
        String got = bannerAimingService.calculateCookieValue(yandexUid, bannerId, clock.instant(), COOKIE_TTL);
        assertThat(got).isEqualTo(expected);
    }
}

