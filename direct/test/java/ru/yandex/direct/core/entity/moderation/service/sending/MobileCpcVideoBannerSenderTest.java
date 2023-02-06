package ru.yandex.direct.core.entity.moderation.service.sending;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;

import static ru.yandex.direct.core.testing.data.TestBanners.activeCpcVideoBanner;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileCpcVideoBannerSenderTest extends AbstractCpmVideoBannerSenderTest {

    @Autowired
    private MobileCpcVideoBannerSender bannerSender;

    @Override
    protected void init() {
        super.bannerSender = bannerSender;

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultVideoAdditionCreative(clientInfo, steps.creativeSteps().getNextCreativeId());

        var turbolanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientInfo.getClientId());
        banner = steps.bannerSteps().createActiveCpcVideoBanner(
                activeCpcVideoBanner(
                        adGroupInfo.getCampaignId(),
                        adGroupInfo.getAdGroupId(),
                        creativeInfo.getCreativeId()
                ).withStatusModerate(OldBannerStatusModerate.READY)
                        .withTurboLandingId(turbolanding.getId())
                        .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.READY),
        adGroupInfo
        ).getBanner();

        banner2 = steps.bannerSteps().createActiveCpcVideoBanner(
                activeCpcVideoBanner(
                        adGroupInfo.getCampaignId(),
                        adGroupInfo.getAdGroupId(),
                        creativeInfo.getCreativeId()
                ).withStatusModerate(OldBannerStatusModerate.READY)
                        .withTurboLandingId(turbolanding.getId())
                        .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.READY),
                adGroupInfo
        ).getBanner();
    }
}
