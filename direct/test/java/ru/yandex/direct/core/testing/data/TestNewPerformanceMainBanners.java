package ru.yandex.direct.core.testing.data;

import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;

public class TestNewPerformanceMainBanners {
    private TestNewPerformanceMainBanners() {
        // only for static methods
    }

    public static PerformanceBannerMain clientPerformanceMainBanner() {
        return new PerformanceBannerMain();
    }

    public static PerformanceBannerMain fullPerformanceMainBanner() {
        PerformanceBannerMain banner = clientPerformanceMainBanner();
        fillPerformanceMainBannerSystemFields(banner);
        return banner;
    }

    private static void fillPerformanceMainBannerSystemFields(PerformanceBannerMain banner) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        banner
                .withBody("{Перфоманс текст}")
                .withTitle("{Перфоманс заголовок}")
                .withGeoFlag(false)
                .withLanguage(Language.UNKNOWN);
    }
}
