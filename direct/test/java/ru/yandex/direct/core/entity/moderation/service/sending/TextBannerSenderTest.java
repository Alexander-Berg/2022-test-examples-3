package ru.yandex.direct.core.entity.moderation.service.sending;

import java.io.IOException;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldStatusBannerImageModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TextBannerSenderTest extends AbstractTextBannerSenderTest {

    @Autowired
    private TextBannerSender textBannerSender;

    @Override
    protected TextBannerSender getBannerSender() {
        return textBannerSender;
    }

    @Before
    public void before() throws IOException {
        moderationOperationModeProvider.disableForcedMode();
        moderationOperationModeProvider.disableImmutableVersionMode();

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        shard = clientInfo.getShard();

        banner = steps.bannerSteps().createBanner(activeTextBanner(campaignInfo.getCampaignId(), null)
                        .withTitle("TestTitle")
                        .withTitleExtension("TestTitleExt")
                        .withBody("TestBody")
                        .withLanguage(Language.RU_)
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withFlags(BannerFlags.fromSource("age:6,annoying,plus18,suspicious_goods,baby_food:0")),
                clientInfo
        ).getBanner();
        banner2 = steps.bannerSteps().createBanner(activeTextBanner(campaignInfo.getCampaignId(), null)
                        .withTitle("TestTitle2")
                        .withTitleExtension("TestTitleExt2")
                        .withBody("TestBody2")
                        .withLanguage(Language.RU_)
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withFlags(BannerFlags.fromSource("age:6,annoying,plus18,suspicious_goods,baby_food:0")),
                clientInfo
        ).getBanner();

        bannerWithImage = steps.bannerSteps().createBanner(activeTextBanner(campaignInfo.getCampaignId(), null)
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withBody("TestBody")
                        .withTitle("TestTitle")
                        .withTitleExtension("TestTitleExt")
                        .withLanguage(Language.UNKNOWN),
                clientInfo
        );

        bannerImage = steps.bannerSteps().createBannerImage(bannerWithImage,
                steps.bannerSteps().createBannerImageFormat(clientInfo),
                defaultBannerImage(banner.getId(), randomAlphanumeric(16)).withBsBannerId(3L)
                        .withStatusModerate(OldStatusBannerImageModerate.READY)
        );
    }
}
