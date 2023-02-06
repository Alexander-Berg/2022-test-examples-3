package ru.yandex.direct.core.entity.moderation.service.sending;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static ru.yandex.direct.core.testing.data.TestBanners.activeCpcVideoBanner;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendCpcVideoBannerModerationRequestTest extends AbstractSendCpcVideoBannerModerationRequestTest {

    @Autowired
    private CpcVideoBannerSender cpcVideoBannerSender;

    @Override
    protected BaseCpmVideoBannerSender getSender() {
        return cpcVideoBannerSender;
    }

    @Override
    protected void init() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        cpcBannerInfo = steps.bannerSteps().createActiveCpcVideoBanner(
                activeCpcVideoBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeInfo.getCreativeId())
                        .withStatusModerate(OldBannerStatusModerate.READY),
                adGroupInfo);
    }
}
