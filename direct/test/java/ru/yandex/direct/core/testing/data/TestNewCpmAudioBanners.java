package ru.yandex.direct.core.testing.data;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmAudioBanner;
import ru.yandex.direct.core.entity.banner.model.Language;

public class TestNewCpmAudioBanners {

    private TestNewCpmAudioBanners() {
        // only for static methods
    }

    public static CpmAudioBanner clientCpmAudioBanner(Long creativeId) {
        CpmAudioBanner banner = new CpmAudioBanner();
        fillCpmAudioBannerClientFields(banner, creativeId);
        return banner;
    }

    public static CpmAudioBanner fullCpmAudioBanner(Long creativeId) {
        CpmAudioBanner banner = new CpmAudioBanner();
        fillCpmAudioBannerClientFields(banner, creativeId);
        fillCpmAudioBannerSystemFields(banner);
        return banner;
    }

    private static void fillCpmAudioBannerClientFields(
            CpmAudioBanner banner, Long creativeId) {
        banner.withHref("https://www.yandex.ru")
                .withCreativeId(creativeId);
    }

    private static void fillCpmAudioBannerSystemFields(CpmAudioBanner banner) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        banner
                .withCreativeStatusModerate(BannerCreativeStatusModerate.YES)
                .withLanguage(Language.UNKNOWN)
                .withGeoFlag(false);
    }
}
