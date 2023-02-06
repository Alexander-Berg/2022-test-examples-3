package ru.yandex.direct.core.testing.data;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.McBanner;
import ru.yandex.direct.core.entity.banner.model.NewStatusImageModerate;

import static ru.yandex.direct.core.entity.banner.type.body.BannerWithBodyConstants.MC_BANNER_BODY;
import static ru.yandex.direct.core.entity.banner.type.title.BannerConstantsService.MC_BANNER_TITLE;

public class TestNewMcBanners {

    private TestNewMcBanners() {
    }

    public static McBanner fullMcBanner(Long campaignId, Long adgroupId, String imageHash) {
        McBanner banner = clientMcBanner(imageHash)
                .withAdGroupId(adgroupId)
                .withCampaignId(campaignId);
        fillMcBannerSystemFields(banner);
        return banner;
    }

    public static McBanner clientMcBanner(String imageHash) {
        McBanner banner = new McBanner();
        fillMcBannerClientFields(banner, imageHash);
        return banner;
    }

    private static void fillMcBannerClientFields(McBanner banner, String imageHash) {
        banner.withHref("https://www.yandex.ru")
                .withImageHash(imageHash);
    }

    private static void fillMcBannerSystemFields(McBanner banner) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        banner.withTitle(MC_BANNER_TITLE)
                .withBody(MC_BANNER_BODY)
                .withDomain("www.yandex.ru")
                .withGeoFlag(false)
                .withStatusModerate(BannerStatusModerate.YES)
                .withImageStatusModerate(NewStatusImageModerate.YES);
    }

}
