package ru.yandex.direct.core.entity.moderation.service.sending;

import java.io.IOException;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;

import static ru.yandex.direct.core.testing.data.TestBanners.activeDynamicBanner;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicBannerSenderTest extends AbstractTextBannerSenderTest {

    @Autowired
    private DynamicBannerSender dynamicBannerSender;

    @Override
    protected DynamicBannerSender getBannerSender() {
        return dynamicBannerSender;
    }

    @Before
    public void before() throws IOException {
        moderationOperationModeProvider.disableForcedMode();
        moderationOperationModeProvider.disableImmutableVersionMode();

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);
        CampaignInfo campaignInfo = adGroupInfo.getCampaignInfo();

        shard = clientInfo.getShard();

        banner = steps.bannerSteps().createBanner(activeDynamicBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withBody("TestBody")
                        .withLanguage(Language.RU_)
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withFlags(BannerFlags.fromSource("age:6,annoying,plus18,suspicious_goods,baby_food:0")),
                adGroupInfo
        ).getBanner();
        banner2 = steps.bannerSteps().createBanner(activeDynamicBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withBody("TestBody2")
                        .withLanguage(Language.RU_)
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withFlags(BannerFlags.fromSource("age:6,annoying,plus18,suspicious_goods,baby_food:0")),
                adGroupInfo
        ).getBanner();
    }
}
