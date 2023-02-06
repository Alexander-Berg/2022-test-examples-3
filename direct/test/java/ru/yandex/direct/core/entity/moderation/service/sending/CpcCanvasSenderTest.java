package ru.yandex.direct.core.entity.moderation.service.sending;


import java.io.IOException;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.moderation.ModerationOperationModeProvider;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static ru.yandex.direct.core.testing.data.TestBanners.activeImageCreativeBanner;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CpcCanvasSenderTest extends AbstractCanvasCreativeBannerSenderTest {

    @Autowired
    private Steps steps;
    @Autowired
    private TestModerationRepository testModerationRepository;
    @Autowired
    private ModerationOperationModeProvider moderationOperationModeProvider;
    @Autowired
    private BannerTypedRepository bannerRepository;
    @Autowired
    private CpcCanvasBannerSender canvasBannerSender;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Override
    @Before
    public void before() throws IOException {
        init(steps, testModerationRepository, bannerRepository, canvasBannerSender, ppcPropertiesSupport,
                moderationOperationModeProvider);
        disableRestrictedMode();

        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        shard = clientInfo.getShard();

        creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCanvasCreative(clientInfo, creativeId);
        var turbolanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientInfo.getClientId());
        banner = steps.bannerSteps()
                .createBanner(
                        activeImageCreativeBanner(campaignInfo.getCampaignId(), null, creativeId)
                                .withTurboLandingId(turbolanding.getId())
                                .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.READY)
                                .withStatusModerate(OldBannerStatusModerate.READY)
                                .withFlags(BannerFlags.fromSource("age:6,annoying,plus18,suspicious_goods,baby_food:0")),
                        clientInfo
                )
                .getBanner();

        banner2 = steps.bannerSteps()
                .createBanner(
                        activeImageCreativeBanner(campaignInfo.getCampaignId(), null, creativeId)
                                .withTurboLandingId(turbolanding.getId())
                                .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.READY)
                                .withStatusModerate(OldBannerStatusModerate.READY)
                                .withFlags(BannerFlags.fromSource("age:6,annoying,plus18,suspicious_goods,baby_food:0")),
                        clientInfo
                )
                .getBanner();

    }
}
