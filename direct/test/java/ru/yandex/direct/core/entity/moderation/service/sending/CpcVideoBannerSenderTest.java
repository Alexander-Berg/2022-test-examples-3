package ru.yandex.direct.core.entity.moderation.service.sending;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;

import static ru.yandex.direct.core.testing.data.TestBanners.activeCpcVideoBanner;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CpcVideoBannerSenderTest extends AbstractCpmVideoBannerSenderTest {

    @Autowired
    private CpcVideoBannerSender bannerSender;

    @Override
    protected void init() {
        super.bannerSender = bannerSender;

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultVideoAdditionCreative(clientInfo, steps.creativeSteps().getNextCreativeId());

        banner = steps.bannerSteps().createActiveCpcVideoBanner(
                activeCpcVideoBanner(
                        adGroupInfo.getCampaignId(),
                        adGroupInfo.getAdGroupId(),
                        creativeInfo.getCreativeId()
                ).withStatusModerate(OldBannerStatusModerate.READY),
                adGroupInfo
        ).getBanner();

        banner2 = steps.bannerSteps().createActiveCpcVideoBanner(
                activeCpcVideoBanner(
                        adGroupInfo.getCampaignId(),
                        adGroupInfo.getAdGroupId(),
                        creativeInfo.getCreativeId()
                ).withStatusModerate(OldBannerStatusModerate.READY),
                adGroupInfo
        ).getBanner();
    }
}
