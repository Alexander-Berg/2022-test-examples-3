package ru.yandex.direct.core.testing.data;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;

import static java.util.Collections.emptyList;

public class TestNewPerformanceBanners {

    private TestNewPerformanceBanners() {
        // only for static methods
    }

    public static PerformanceBanner clientPerformanceBanner(Long creativeId) {
        return clientPerformanceBanner(null, creativeId);
    }

    public static PerformanceBanner clientPerformanceBanner(Long adgroupId, Long creativeId) {
        PerformanceBanner banner = new PerformanceBanner()
                .withAdGroupId(adgroupId);
        fillPerformanceBannerClientFields(banner, creativeId);
        return banner;
    }

    public static PerformanceBanner fullPerformanceBanner(Long campaignId, Long adgroupId, Long creativeId) {
        var banner = clientPerformanceBanner(creativeId)
                .withAdGroupId(adgroupId)
                .withCampaignId(campaignId);
        fillPerformanceBannerSystemFields(banner);
        return banner;
    }

    private static void fillPerformanceBannerClientFields(PerformanceBanner banner, Long creativeId) {
        banner
                .withCreativeId(creativeId);
    }

    private static void fillPerformanceBannerSystemFields(PerformanceBanner banner) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        banner
                .withCreativeStatusModerate(BannerCreativeStatusModerate.YES)
                .withVcardStatusModerate(banner.getVcardId() == null ?
                        BannerVcardStatusModerate.NEW : BannerVcardStatusModerate.YES)
                .withCalloutIds(emptyList())
                .withLanguage(Language.UNKNOWN)
                .withBody("{Перфоманс текст}")
                .withTitle("{Перфоманс заголовок}")
                .withGeoFlag(false);
    }
}
