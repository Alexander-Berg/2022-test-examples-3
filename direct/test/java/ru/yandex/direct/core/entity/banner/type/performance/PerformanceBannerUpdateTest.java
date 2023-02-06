package ru.yandex.direct.core.entity.banner.type.performance;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.NewPerformanceBannerInfo;

import static ru.yandex.direct.core.testing.data.TestNewPerformanceBanners.fullPerformanceBanner;

@ParametersAreNonnullByDefault
public class PerformanceBannerUpdateTest extends BannerNewBannerInfoUpdateOperationTestBase {

    protected static final ru.yandex.direct.core.entity.creative.model.StatusModerate CREATIVE_MODERATE_NEW =
            ru.yandex.direct.core.entity.creative.model.StatusModerate.NEW;
    protected static final ru.yandex.direct.core.entity.creative.model.StatusModerate CREATIVE_MODERATE_YES =
            ru.yandex.direct.core.entity.creative.model.StatusModerate.YES;
    protected static final ru.yandex.direct.core.entity.creative.model.StatusModerate CREATIVE_MODERATE_READY =
            ru.yandex.direct.core.entity.creative.model.StatusModerate.READY;


    protected static NewPerformanceBannerInfo performanceBanner(AdGroupInfo adGroupInfo,
                                                                Creative creative,
                                                                BannerStatusModerate statusModerate,
                                                                Long bsBannerId) {
        return new NewPerformanceBannerInfo()
                .withCampaignInfo(adGroupInfo.getCampaignInfo())
                .withAdGroupInfo(adGroupInfo)
                .withCreativeInfo(new CreativeInfo().withCreative(creative))
                .withBanner(fullPerformanceBanner(adGroupInfo.getCampaignId(),
                        adGroupInfo.getAdGroupId(),
                        creative.getId())
                        .withStatusModerate(statusModerate)
                        .withBsBannerId(bsBannerId));
    }

}
