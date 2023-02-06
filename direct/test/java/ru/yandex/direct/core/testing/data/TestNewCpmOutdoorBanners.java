package ru.yandex.direct.core.testing.data;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.model.Language;

public class TestNewCpmOutdoorBanners {

    private TestNewCpmOutdoorBanners() {
        // only for static methods
    }

    public static CpmOutdoorBanner clientCpmOutdoorBanner(Long creativeId) {
        CpmOutdoorBanner banner = new CpmOutdoorBanner();
        fillCpmOutdoorBannerClientFields(banner, creativeId);
        return banner;
    }

    public static CpmOutdoorBanner fullCpmOutdoorBanner(Long creativeId) {
        return fullCpmOutdoorBanner(null, null, creativeId);
    }

    public static CpmOutdoorBanner fullCpmOutdoorBanner(Long campaignId, Long adGroupId, Long creativeId) {
        CpmOutdoorBanner banner = new CpmOutdoorBanner()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId);
        fillCpmOutdoorBannerClientFields(banner, creativeId);
        fillCpmOutdoorBannerSystemFields(banner);
        return banner;
    }

    private static void fillCpmOutdoorBannerClientFields(
            CpmOutdoorBanner banner, Long creativeId) {
        banner.withHref("https://www.yandex.ru")
                .withCreativeId(creativeId);
    }

    private static void fillCpmOutdoorBannerSystemFields(CpmOutdoorBanner banner) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        banner
                .withCreativeStatusModerate(BannerCreativeStatusModerate.YES)
                .withLanguage(Language.UNKNOWN)
                .withGeoFlag(false);
    }
}
