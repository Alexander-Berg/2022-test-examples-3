package ru.yandex.direct.core.testing.data;

import java.util.Collections;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerDisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.TextBanner;

import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

public class TestNewTextBanners {

    private TestNewTextBanners() {
        // only for static methods
    }

    public static TextBanner clientTextBanner() {
        TextBanner banner = new TextBanner();
        fillTextBannerClientFields(banner);
        return banner;
    }

    public static TextBanner fullTextBanner() {
        return fullTextBanner(null, null);
    }

    public static TextBanner fullTextBanner(Long campaignId, Long adGroupId) {
        var banner = clientTextBanner()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId);
        fillTextBannerSystemFields(banner);
        return banner;
    }

    private static void fillTextBannerSystemFields(TextBanner banner) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        banner
                .withDomain("www.yandex.ru")
                .withGeoFlag(false)
                .withLanguage(Language.EN)
                .withStatusSitelinksModerate(banner.getSitelinksSetId() == null ?
                        BannerStatusSitelinksModerate.NEW : BannerStatusSitelinksModerate.YES)
                .withVcardStatusModerate(banner.getVcardId() == null ?
                        BannerVcardStatusModerate.NEW : BannerVcardStatusModerate.YES)
                .withDisplayHrefStatusModerate(ifNotNull(banner.getDisplayHref(),
                        b -> BannerDisplayHrefStatusModerate.YES))
                .withCreativeStatusModerate(ifNotNull(banner.getCreativeId(), b -> BannerCreativeStatusModerate.YES))
                .withTurboLandingStatusModerate(ifNotNull(banner.getTurboLandingId(),
                        b -> BannerTurboLandingStatusModerate.YES));
    }

    private static void fillTextBannerClientFields(TextBanner banner) {
        banner.withTitle("TextBanner Title")
                .withBody("TextBanner Body")
                .withHref("https://www.yandex.ru")
                .withCalloutIds(Collections.emptyList())
                .withIsMobile(false);
    }
}
