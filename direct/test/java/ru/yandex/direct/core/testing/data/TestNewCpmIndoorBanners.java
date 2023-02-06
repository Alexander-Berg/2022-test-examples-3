package ru.yandex.direct.core.testing.data;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.model.Language;

public class TestNewCpmIndoorBanners {

    private TestNewCpmIndoorBanners() {
        // only for static methods
    }

    public static CpmIndoorBanner clientCpmIndoorBanner(Long creativeId) {
        return clientCpmIndoorBanner(null, null, creativeId);
    }

    public static CpmIndoorBanner clientCpmIndoorBanner(Long campaignId, Long adGroupId, Long creativeId) {
        CpmIndoorBanner banner = new CpmIndoorBanner()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId);
        fillCpmIndoorBannerClientFields(banner, creativeId);
        return banner;
    }

    public static CpmIndoorBanner fullCpmIndoorBanner(Long creativeId) {
        return fullCpmIndoorBanner(null, null, creativeId);
    }

    public static CpmIndoorBanner fullCpmIndoorBanner(Long campaignId, Long adGroupId, Long creativeId) {
        CpmIndoorBanner banner = new CpmIndoorBanner()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId);
        fillCpmIndoorBannerClientFields(banner, creativeId);
        fillCpmIndoorBannerSystemFields(banner);
        return banner;
    }

    private static void fillCpmIndoorBannerClientFields(
            CpmIndoorBanner banner, Long creativeId) {
        banner.withHref("https://www.yandex.ru")
                .withCreativeId(creativeId);
    }

    private static void fillCpmIndoorBannerSystemFields(CpmIndoorBanner banner) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        banner
                .withCreativeStatusModerate(BannerCreativeStatusModerate.YES)
                .withLanguage(Language.UNKNOWN)
                .withGeoFlag(false);
    }
}
