package ru.yandex.direct.core.entity.moderation.service.receiving.operations.banners;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.Age;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.repository.bulk_update.BulkUpdateHolder;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderationdiag.service.ModerationDiagService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReason;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestNewMcBanners;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewMcBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.TrustedRedirectSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationResponseUtil.makeBannerResponse;
import static ru.yandex.direct.core.entity.uac.UacTestDataKt.TRACKING_URL;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMcBannerCampaignWithSystemFields;
import static ru.yandex.direct.feature.FeatureName.DECLINE_UNFAMILY_MCBANNER_MODERATION;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

@CoreTest
@RunWith(SpringRunner.class)
public class StopUnfamilyMCBannerOpTest {

    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private ModerationDiagService moderationDiagService;
    @Autowired
    private Steps steps;

    private final BulkUpdateHolder bulkUpdateHolder = new BulkUpdateHolder();

    @Autowired
    private ModerationReasonRepository moderationReasonRepository;

    @Autowired
    private StopUnfamilyMCBannerOp operation;

    @Autowired
    private BannerService bannerService;

    private ClientInfo clientInfo;

    private Long bannerId;

    @Before
    public void setUp() throws Exception {
        moderationDiagService.invalidateAll();
        steps.moderationDiagSteps().insertStandartDiags();
        clientInfo = steps.clientSteps().createDefaultClient();
        var campaign = steps.mcBannerCampaignSteps().createCampaign(clientInfo, defaultMcBannerCampaignWithSystemFields());
        var adGroup = steps.adGroupSteps().createActiveMcBannerAdGroup(campaign);
        var banner = steps.mcBannerSteps().createMcBanner(new NewMcBannerInfo()
                .withBanner(TestNewMcBanners
                        .fullMcBanner(campaign.getCampaignId(), adGroup.getAdGroupId(), "imageHash")
                        .withHref(TRACKING_URL)
                        .withFlags(new BannerFlags().with(BannerFlags.AGE, Age.AGE_6))
                        .withStatusModerate(BannerStatusModerate.READY)
                        .withIsMobileImage(true)
                        .withDomain(TrustedRedirectSteps.DOMAIN))
                .withAdGroupInfo(adGroup));
        bannerId = banner.getBannerId();
    }

    @Test
    public void objectAcceptedWithFeature() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), DECLINE_UNFAMILY_MCBANNER_MODERATION, true);
        BannerModerationResponse moderationResponse = makeBannerResponse(bannerId, 42L, Yes, Map.of("unfamily", "1"),
                ModerationObjectType.AD_IMAGE);
        moderationResponse.getMeta().setClientId(clientInfo.getClientId().asLong());
        operation.consume(bulkUpdateHolder, moderationResponse);
        operation.flush(dslContextProvider.ppc(clientInfo.getShard()).configuration(), bulkUpdateHolder);

        bulkUpdateHolder.execute(dslContextProvider.ppc(clientInfo.getShard()).configuration());

        Map<Long, BannerWithSystemFields> bannersByIds =
                listToMap(bannerService.getBannersByIds(List.of(bannerId)), BannerWithSystemFields::getId);
        List<ModerationReason> moderationReasons = moderationReasonRepository.fetchRejected(clientInfo.getShard(),
                ModerationReasonObjectType.BANNER, List.of(bannerId));

        assertEquals(BannerStatusModerate.NO, bannersByIds.get(bannerId).getStatusModerate());
        assertEquals(BannerStatusPostModerate.REJECTED, bannersByIds.get(bannerId).getStatusPostModerate());

        assertThat(moderationReasons).size().isEqualTo(1);
        assertThat(moderationReasons.get(0).getReasons()).size().isEqualTo(1);
        assertThat(moderationReasons.get(0).getReasons().get(0).getId()).isEqualTo(1256L);
    }

    @Test
    public void objectAcceptedWithoutFeature() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), DECLINE_UNFAMILY_MCBANNER_MODERATION, false);
        BannerModerationResponse moderationResponse = makeBannerResponse(bannerId, 42L, Yes, Map.of("unfamily", "1"),
                ModerationObjectType.AD_IMAGE);
        moderationResponse.getMeta().setClientId(clientInfo.getClientId().asLong());

        operation.consume(bulkUpdateHolder, moderationResponse);
        operation.flush(dslContextProvider.ppc(clientInfo.getShard()).configuration(), bulkUpdateHolder);

        bulkUpdateHolder.execute(dslContextProvider.ppc(clientInfo.getShard()).configuration());

        Map<Long, BannerWithSystemFields> bannersByIds =
                listToMap(bannerService.getBannersByIds(List.of(bannerId)), BannerWithSystemFields::getId);
        List<ModerationReason> moderationReasons = moderationReasonRepository.fetchRejected(clientInfo.getShard(),
                ModerationReasonObjectType.BANNER, List.of(bannerId));

        assertNotEquals(BannerStatusModerate.NO, bannersByIds.get(bannerId).getStatusModerate());
        assertNotEquals(BannerStatusPostModerate.REJECTED, bannersByIds.get(bannerId).getStatusPostModerate());

        assertThat(moderationReasons).size().isEqualTo(0);
    }

}
