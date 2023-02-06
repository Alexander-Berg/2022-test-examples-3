package ru.yandex.direct.core.entity.moderation.service.sending;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.moderation.ModerationOperationModeProvider;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.NewImageBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5;
import static ru.yandex.direct.core.testing.data.TestNewImageBanners.fullImageBannerWithCreative;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CpcHtml5SenderTest extends AbstractHtml5CreativeBannerSenderTest {

    @Autowired
    private Steps steps;
    @Autowired
    private TestModerationRepository testModerationRepository;
    @Autowired
    private ModerationOperationModeProvider moderationOperationModeProvider;
    @Autowired
    private BannerTypedRepository bannerRepository;
    @Autowired
    private CpcHtml5BannerSender html5BannerSender;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Override
    @Before
    public void before() {
        init(steps, testModerationRepository, bannerRepository, html5BannerSender, ppcPropertiesSupport,
                moderationOperationModeProvider);
        disableRestrictedMode();

        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        var creative = defaultHtml5(clientInfo.getClientId(), null);
        var turbolanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientInfo.getClientId());
        var b = fullImageBannerWithCreative(null)
                .withTurboLandingId(turbolanding.getId())
                .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.READY)
                .withStatusModerate(BannerStatusModerate.READY)
                .withFlags(BannerFlags.fromSource("age:6,annoying,plus18,suspicious_goods,baby_food:0"));
        var b2 = fullImageBannerWithCreative(null)
                .withTurboLandingId(turbolanding.getId())
                .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.READY)
                .withStatusModerate(BannerStatusModerate.READY)
                .withFlags(BannerFlags.fromSource("age:6,annoying,plus18,suspicious_goods,baby_food:0"));

        var bannerInfo = steps.imageBannerSteps().createImageBanner(
                new NewImageBannerInfo().withClientInfo(clientInfo).withBanner(b).withCreative(creative));
        var bannerInfo2 = steps.imageBannerSteps().createImageBanner(
                new NewImageBannerInfo().withClientInfo(clientInfo).withBanner(b2).withCreative(creative));
        campaignInfo = bannerInfo.getCampaignInfo();
        banner = bannerInfo.getBanner();
        banner2 = bannerInfo2.getBanner();
        creativeId = bannerInfo.getCreative().getId();
    }
}
