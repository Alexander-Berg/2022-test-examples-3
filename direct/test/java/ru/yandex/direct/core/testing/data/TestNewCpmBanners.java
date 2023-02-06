package ru.yandex.direct.core.testing.data;

import java.util.Collections;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;

import static ru.yandex.direct.core.testing.data.TestNewBanners.fillSystemFieldsForActiveBanner;

public class TestNewCpmBanners {

    private TestNewCpmBanners() {
        // only for static methods
    }

    public static CpmBanner clientCpmBanner(Long creativeId) {
        CpmBanner banner = new CpmBanner();
        fillCpmBannerClientFields(banner, creativeId);
        return banner;
    }

    public static CpmBanner fullCpmBanner(Long creativeId) {
        return fullCpmBanner(null, null, creativeId);
    }

    public static CpmBanner fullCpmBanner(Long campaignId, Long adGroupId, Long creativeId) {
        CpmBanner banner = new CpmBanner()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId);
        fillSystemFieldsForActiveBanner(banner);
        fillSystemFieldsForActiveCpmBanner(banner);
        fillCpmBannerClientFields(banner, creativeId);
        return banner;
    }

    private static void fillCpmBannerClientFields(CpmBanner banner, Long creativeId) {
        banner.withHref("https://www.yandex.ru")
                .withCreativeId(creativeId)
                .withPixels(Collections.emptyList());
    }

    private static void fillSystemFieldsForActiveCpmBanner(CpmBanner banner) {
        banner.withCreativeStatusModerate(BannerCreativeStatusModerate.YES)
                .withGeoFlag(false);
    }
}
