package ru.yandex.direct.core.testing.data;

import ru.yandex.direct.core.entity.banner.model.BannerDisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.Language;

public class TestNewDynamicBanners {

    private TestNewDynamicBanners() {
        // only for static methods
    }

    public static DynamicBanner clientDynamicBanner() {
        DynamicBanner banner = new DynamicBanner();
        fillDynamicBannerClientFields(banner);
        return banner;
    }

    public static DynamicBanner fullDynamicBanner() {
        return fullDynamicBanner(null, null);
    }

    public static DynamicBanner fullDynamicBanner(Long campaignId, Long adGroupId) {
        var banner = clientDynamicBanner()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId);
        fillDynamicBannerSystemFields(banner);
        return banner;
    }

    private static void fillDynamicBannerClientFields(DynamicBanner banner) {
        banner
                .withBody("DynamicBanner Body")
                .withHref("https://www.yandex.ru");
    }

    private static void fillDynamicBannerSystemFields(DynamicBanner banner) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        banner
                .withTitle("{Dynamic title}")
                .withDomain("www.yandex.ru")
                .withGeoFlag(false)
                .withLanguage(Language.EN)
                .withStatusSitelinksModerate(banner.getSitelinksSetId() == null ?
                        BannerStatusSitelinksModerate.NEW : BannerStatusSitelinksModerate.YES)
                .withVcardStatusModerate(banner.getVcardId() == null ?
                        BannerVcardStatusModerate.NEW : BannerVcardStatusModerate.YES)
                .withDisplayHrefStatusModerate(BannerDisplayHrefStatusModerate.YES);
    }
}
