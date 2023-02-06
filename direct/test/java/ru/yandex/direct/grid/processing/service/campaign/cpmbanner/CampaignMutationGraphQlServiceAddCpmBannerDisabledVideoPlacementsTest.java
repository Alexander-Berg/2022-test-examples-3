package ru.yandex.direct.grid.processing.service.campaign.cpmbanner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdDayBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignSmsEvent;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCmpBannerCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignEmailSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignNotificationRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignSmsSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_METRIKA_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_OPENSTAT_TAG_TO_URL;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdAttributionModel;
import static ru.yandex.direct.grid.model.utils.GridTimeUtils.toGdTimeInterval;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationGraphQlServiceAddCpmBannerDisabledVideoPlacementsTest {
    private static final List<String> DISABLED_VIDEO_PLACEMENTS = List.of("vk.com", "music.yandex.ru");
    private static final String MUTATION_NAME = "addCampaigns";
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
            + "    addedCampaigns {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.TemplateMutation<GdAddCampaigns, GdAddCampaignPayload> ADD_CAMPAIGN_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdAddCampaigns.class, GdAddCampaignPayload.class);

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignConstantsService campaignConstantsService;

    private User operator;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.DISABLE_BILLING_AGGREGATES, true);
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void addCpmBannerCampaign() {
        GdAddCmpBannerCampaign gdAddCpmBannerCampaign =
                defaultGdAddCpmBannerCampaign(campaignConstantsService.getDefaultAttributionModel())
                        .withDisabledVideoAdsPlaces(DISABLED_VIDEO_PLACEMENTS);

        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion()
                .withCpmBannerCampaign(gdAddCpmBannerCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNull();
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).hasSize(1);

        Campaign actualCampaign = campaignRepository.getCampaigns(clientInfo.getShard(),
                List.of(gdAddCampaignPayload.getAddedCampaigns().get(0).getId())).get(0);
        assertThat(actualCampaign.getDisabledVideoPlacements()).containsOnlyElementsOf(DISABLED_VIDEO_PLACEMENTS);

    }

    private static GdAddCmpBannerCampaign defaultGdAddCpmBannerCampaign(CampaignAttributionModel defaultAttributionModel) {
        Set<GdCampaignSmsEvent> enableEvents = Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN);
        return new GdAddCmpBannerCampaign()
                .withName("new Camp")
                .withStartDate(LocalDate.now().plusDays(1))
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.CONTEXT)
                        .withStrategy(null)
                        .withStrategyName(GdCampaignStrategyName.CPM_DEFAULT)
                        .withStrategyData(new GdCampaignStrategyData())
                )
                .withMetrikaCounters(emptyList())
                .withHasSiteMonitoring(false)
                .withHasExtendedGeoTargeting(false)
                .withTimeTarget(defaultGdTimeTarget())
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
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.STRETCHED)
                .withDisabledPlaces(emptyList());
    }

}
