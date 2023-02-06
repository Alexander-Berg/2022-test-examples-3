package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.Age;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.McBanner;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.receiving.AdImageBannerModerationReceivingService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReason;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestNewMcBanners;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewMcBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.TrustedRedirectSteps;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.AD_IMAGE;
import static ru.yandex.direct.core.entity.uac.UacTestDataKt.TRACKING_URL;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMcBannerCampaignWithSystemFields;
import static ru.yandex.direct.feature.FeatureName.DECLINE_UNFAMILY_MCBANNER_MODERATION;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveMcBannerUnfamilyFlagModerationResponseTest extends ReceiveModerationResponseBaseTest {
    private static final List<Long> DEFAULT_MINUS_REGION = Arrays.asList(4L, 5L, 6L);
    protected static final List<ModerationReasonDetailed> DEFAULT_REASONS = Arrays.asList(
            new ModerationReasonDetailed().withId(2L),
            new ModerationReasonDetailed().withId(3L));

    @Autowired
    private AdImageBannerModerationReceivingService adImageBannerModerationReceivingService;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private ModerationReasonRepository moderationReasonRepository;


    private int shard;
    private ClientInfo clientInfo;
    private McBanner mcbanner;

    @Before
    public void setUp() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();

        var campaignInfo = steps.mcBannerCampaignSteps().createCampaign(clientInfo, defaultMcBannerCampaignWithSystemFields());
        var adGroupInfo = steps.adGroupSteps().createActiveMcBannerAdGroup(campaignInfo);

        shard = clientInfo.getShard();

        mcbanner = steps.mcBannerSteps().createMcBanner(new NewMcBannerInfo()
                        .withBanner(TestNewMcBanners
                                .fullMcBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId(), "imageHash")
                                .withHref(TRACKING_URL)
                                .withFlags(new BannerFlags().with(BannerFlags.AGE, Age.AGE_6))
                                .withStatusModerate(BannerStatusModerate.SENDING)
                                .withIsMobileImage(true)
                                .withDomain(TrustedRedirectSteps.DOMAIN))
                        .withAdGroupInfo(adGroupInfo)).getBanner();

        testModerationRepository.createBannerVersion(shard, mcbanner.getId(), 1L);
    }

    @Test
    public void bannerDeclinedWithFeature() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), DECLINE_UNFAMILY_MCBANNER_MODERATION, true);
        steps.moderationDiagSteps().insertStandartDiags();
        BannerModerationResponse response = createResponse(mcbanner.getId(), AD_IMAGE, Yes, "en", 1L,
                Map.of("unfamily", "1"), DEFAULT_MINUS_REGION, clientInfo, DEFAULT_REASONS);

        var unknownVerdictCountAndSuccess =
                adImageBannerModerationReceivingService.processModerationResponses(shard, singletonList(response));


        Map<Long, BannerWithSystemFields> bannersByIds =
                listToMap(bannerService.getBannersByIds(List.of(mcbanner.getId())), BannerWithSystemFields::getId);
        List<ModerationReason> moderationReasons = moderationReasonRepository.fetchRejected(clientInfo.getShard(),
                ModerationReasonObjectType.BANNER, List.of(mcbanner.getId()));

        assertEquals(BannerStatusModerate.NO, bannersByIds.get(mcbanner.getId()).getStatusModerate());
        assertEquals(BannerStatusPostModerate.REJECTED, bannersByIds.get(mcbanner.getId()).getStatusPostModerate());

        Assertions.assertThat(moderationReasons).size().isEqualTo(1);
        Assertions.assertThat(moderationReasons.get(0).getReasons()).size().isEqualTo(1);
        Assertions.assertThat(moderationReasons.get(0).getReasons().get(0).getId()).isEqualTo(1256L);
    }

    @Test
    public void bannerAcceptedWithoutFeature() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), DECLINE_UNFAMILY_MCBANNER_MODERATION, true);
        steps.moderationDiagSteps().insertStandartDiags();
        BannerModerationResponse response = createResponse(mcbanner.getId(), AD_IMAGE, Yes, "en", 1L,
                Map.of("unfamily", "1"), DEFAULT_MINUS_REGION, clientInfo, DEFAULT_REASONS);

        var unknownVerdictCountAndSuccess =
                adImageBannerModerationReceivingService.processModerationResponses(shard, singletonList(response));


        Map<Long, BannerWithSystemFields> bannersByIds =
                listToMap(bannerService.getBannersByIds(List.of(mcbanner.getId())), BannerWithSystemFields::getId);
        List<ModerationReason> moderationReasons = moderationReasonRepository.fetchRejected(clientInfo.getShard(),
                ModerationReasonObjectType.BANNER, List.of(mcbanner.getId()));

        assertEquals(BannerStatusModerate.NO, bannersByIds.get(mcbanner.getId()).getStatusModerate());
        assertEquals(BannerStatusPostModerate.REJECTED, bannersByIds.get(mcbanner.getId()).getStatusPostModerate());

        Assertions.assertThat(moderationReasons).size().isEqualTo(1);
        Assertions.assertThat(moderationReasons.get(0).getReasons()).size().isEqualTo(1);
        Assertions.assertThat(moderationReasons.get(0).getReasons().get(0).getId()).isEqualTo(1256L);
    }

    @Test
    public void moderationReasonAddedToDeclinedBanner() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), DECLINE_UNFAMILY_MCBANNER_MODERATION, true);
        steps.moderationDiagSteps().insertStandartDiags();
        BannerModerationResponse response = createResponse(mcbanner.getId(), AD_IMAGE, No, "en", 1L,
                Map.of("unfamily", "1"), DEFAULT_MINUS_REGION, clientInfo, DEFAULT_REASONS);

        var unknownVerdictCountAndSuccess =
                adImageBannerModerationReceivingService.processModerationResponses(shard, singletonList(response));


        Map<Long, BannerWithSystemFields> bannersByIds =
                listToMap(bannerService.getBannersByIds(List.of(mcbanner.getId())), BannerWithSystemFields::getId);
        List<ModerationReason> moderationReasons = moderationReasonRepository.fetchRejected(clientInfo.getShard(),
                ModerationReasonObjectType.BANNER, List.of(mcbanner.getId()));

        assertEquals(BannerStatusModerate.NO, bannersByIds.get(mcbanner.getId()).getStatusModerate());
        assertEquals(BannerStatusPostModerate.REJECTED, bannersByIds.get(mcbanner.getId()).getStatusPostModerate());

        Assertions.assertThat(moderationReasons).size().isEqualTo(1);
        Assertions.assertThat(moderationReasons.get(0).getReasons()).size().isEqualTo(DEFAULT_REASONS.size() + 1);
        Assertions.assertThat(moderationReasons.get(0).getReasons()).containsAnyOf(new ModerationReasonDetailed().withId(1256L));
    }

}
