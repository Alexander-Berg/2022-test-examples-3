package ru.yandex.direct.grid.processing.service.campaign.cpmbanner;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.campaign.CpmBannerCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdDayBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignSmsEvent;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignEmailSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignNotificationRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignSmsSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdShowsFrequencyLimitRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCpmBannerCampaign;
import ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdAttributionModel;
import static ru.yandex.direct.grid.model.utils.GridTimeUtils.toGdTimeInterval;
import static ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategy.DIFFERENT_PLACES;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationGraphQlServiceUpdateCpmBannerDisabledVideoPlacementsTest {

    private static final List<String> DISABLED_VIDEO_PLACEMENTS = List.of("vk.com", "music.yandex.ru");
    private static final String MUTATION_NAME = "updateCampaigns";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedCampaigns {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCampaigns, GdUpdateCampaignPayload> UPDATE_CAMPAIGN_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdUpdateCampaigns.class, GdUpdateCampaignPayload.class);

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignRepository campaignRepository;

    private User operator;
    private ClientInfo clientInfo;
    private CpmBannerCampaignInfo cpmBannerCampaignInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        cpmBannerCampaignInfo = steps.cpmBannerCampaignSteps().createDefaultCampaign(clientInfo);

        steps.dbQueueSteps().registerJobType(RECALC_BRAND_LIFT_CAMPAIGNS);
    }

    @Test
    public void update() {
        GdUpdateCampaignUnion campaignUnion =
                getCpmBannerCampaignUnion(defaultGdUpdateCpmBannerCampaign(cpmBannerCampaignInfo)
                        .withDisabledVideoAdsPlaces(DISABLED_VIDEO_PLACEMENTS));

        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = singletonList(
                new GdUpdateCampaignPayloadItem().withId(cpmBannerCampaignInfo.getId()));

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(gdUpdateCampaignPayload.getValidationResult()).isNull();

        Campaign actualCampaign = campaignRepository.getCampaigns(clientInfo.getShard(),
                List.of(gdUpdateCampaignPayload.getUpdatedCampaigns().get(0).getId())).get(0);
        assertThat(actualCampaign.getDisabledVideoPlacements()).containsOnlyElementsOf(DISABLED_VIDEO_PLACEMENTS);

    }


    @Test
    public void update_withCpmBannerDisableFeature() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true);
        GdUpdateCampaignUnion campaignUnion =
                getCpmBannerCampaignUnion(defaultGdUpdateCpmBannerCampaign(cpmBannerCampaignInfo)
                        .withDisabledVideoAdsPlaces(DISABLED_VIDEO_PLACEMENTS));

        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        GdDefect defect = new GdDefect()
                .withCode("CampaignDefectIds.Gen.CAMPAIGN_NO_WRITE_RIGHTS")
                .withPath("campaignUpdateItems[0].id");
        assertThat(gdUpdateCampaignPayload.getValidationResult()).isNotNull();
        assertThat(gdUpdateCampaignPayload.getValidationResult().getErrors().get(0)).isEqualTo(defect);
    }

    private static GdUpdateCpmBannerCampaign defaultGdUpdateCpmBannerCampaign(CpmBannerCampaignInfo cpmBannerCampaignInfo) {
        Set<GdCampaignSmsEvent> enableEvents = Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN);
        CpmBannerCampaign cpmBannerCampaign = cpmBannerCampaignInfo.getTypedCampaign();
        return new GdUpdateCpmBannerCampaign()
                .withId(cpmBannerCampaign.getId())
                .withName(cpmBannerCampaign.getName())
                .withEndDate(cpmBannerCampaign.getEndDate())
                .withStartDate(cpmBannerCampaign.getStartDate())
                .withTimeTarget(defaultGdTimeTarget())
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.CONTEXT)
                        .withStrategy(DIFFERENT_PLACES)
                        .withStrategyName(GdCampaignStrategyName.CPM_DEFAULT)
                        .withStrategyData(new GdCampaignStrategyData())
                )
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru")
                                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                                .withSendAccountNews(true)
                                .withStopByReachDailyBudget(true)
                                .withXlsReady(true)
                                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                        )
                        .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                                .withEnableEvents(enableEvents)))
                .withContentLanguage(null)
                .withHasExtendedGeoTargeting(cpmBannerCampaign.getHasExtendedGeoTargeting())
                .withAttributionModel(toGdAttributionModel(cpmBannerCampaign.getAttributionModel()))
                .withHasAddOpenstatTagToUrl(cpmBannerCampaign.getHasAddOpenstatTagToUrl())
                .withHasAddMetrikaTagToUrl(cpmBannerCampaign.getHasAddMetrikaTagToUrl())
                .withHasSiteMonitoring(cpmBannerCampaign.getHasSiteMonitoring())
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withSectionIds(emptyList())
                .withShowsFrequencyLimit(new GdShowsFrequencyLimitRequest()
                        .withLimit(1)
                        .withDays(1)
                        .withIsForCampaignTime(false)
                );
    }

    private static GdUpdateCampaignUnion getCpmBannerCampaignUnion(GdUpdateCpmBannerCampaign cpmBannerCampaign) {
        return new GdUpdateCampaignUnion()
                .withCpmBannerCampaign(cpmBannerCampaign);
    }

}
