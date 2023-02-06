package ru.yandex.direct.grid.processing.service.campaign;

import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsPerformanceNowOptimizingBy;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.data.TestGdAddCampaigns;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddSmartCampaign;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.validation.result.DefectIds;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetRoiStrategy;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.feature.FeatureName.TURBO_SMARTS;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.ADD_CAMPAIGNS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Тесты на добавление смарт кампании
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class CampaignMutationAddGraphqlServiceSmartCampaignTest {

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
            new GraphQlTestExecutor.TemplateMutation<>(ADD_CAMPAIGNS, MUTATION_TEMPLATE,
                    GdAddCampaigns.class, GdAddCampaignPayload.class);
    private static final int COUNTER_ID = 5;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private GraphQlTestExecutor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private MetrikaClientStub metrikaClientStub;
    @Autowired
    private CampaignConstantsService campaignConstantsService;

    private User operator;
    private ClientInfo clientInfo;
    private int shard;

    private static CampaignAttributionModel defaultAttributionModel;

    @Before
    public void before() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();
        shard = userInfo.getShard();
        operator = userInfo.getUser();

        TestAuthHelper.setDirectAuthentication(operator);

        metrikaClientStub.addUserCounter(operator.getUid(), COUNTER_ID);

        defaultAttributionModel = campaignConstantsService.getDefaultAttributionModel();
    }

    public static Object[] parametersForCheckTurboSmarts() {
        return new Object[][]{
                {false, null, null},
                {false, true, true},
                {false, false, false},
                {true, true, true},
                {true, false, false},
        };
    }

    public static Object[] parametersForCheckWithExtendedGeoTargeting() {
        return new Object[][]{
                {null, true},
                {false, false},
                {true, true}
        };
    }

    @Test
    @Parameters(method = "parametersForCheckTurboSmarts")
    @TestCaseName("feature enabled: {0}, has turbo smarts: {1}, expect: {2}")
    public void addSmartCampaign_CheckTurboSmarts(boolean featureEnabled,
                                                  Boolean hasTurboSmarts,
                                                  Boolean expectHasTurboSmarts) {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), TURBO_SMARTS, featureEnabled);

        GdAddSmartCampaign campaign = TestGdAddCampaigns.defaultSmartCampaign(hasTurboSmarts, List.of(COUNTER_ID),
                defaultAttributionModel);

        GdAddCampaignPayload response = sendRequest(campaign);
        checkState(response.getValidationResult() == null && response.getAddedCampaigns().size() == 1,
                "Unexpected error or list of campaign data is empty in response");

        List<? extends BaseCampaign> campaigns = campaignTypedRepository
                .getTypedCampaigns(shard, mapList(response.getAddedCampaigns(), GdAddCampaignPayloadItem::getId));

        SmartCampaign expect = TestCampaigns.defaultSmartCampaign()
                .withStrategy(defaultAutobudgetRoiStrategy(0, false))
                .withHasTurboSmarts(expectHasTurboSmarts)
                .withContextLimit(0)
                .withHasExtendedGeoTargeting(true)
                .withEnableOfflineStatNotice(true)
                .withEnablePausedByDayBudgetEvent(true)
                .withContextPriceCoef(100)
                .withMetrikaCounters(List.of((long) COUNTER_ID));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(campaigns).as("list of campaign data")
                    .hasSize(1);
            soft.assertThat(campaigns.get(0)).as("smart campaign")
                    .is(matchedBy(beanDiffer(expect).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void addSmartCampaign_WithFeatureWhenTurboSmartsIsNull_CannotBeNull() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), TURBO_SMARTS, true);

        GdAddSmartCampaign campaign = TestGdAddCampaigns.defaultSmartCampaign(null, List.of(COUNTER_ID),
                defaultAttributionModel);

        GdAddCampaignPayload response = sendRequest(campaign);
        GdValidationResult vr = response.getValidationResult();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getAddedCampaigns()).as("list of campaign data")
                    .isEqualTo(singletonList(null));
            soft.assertThat(vr).as("validation results")
                    .isNotNull();
            soft.assertThat(vr.getErrors().get(0)).as("validation error")
                    .isEqualTo(new GdDefect()
                            .withCode(DefectIds.CANNOT_BE_NULL.getCode())
                            .withPath("campaignAddItems[0].hasTurboSmarts"));
        });
    }

    @Test
    public void addSmartCampaign_CheckThatAddingDataToCampaignsPerformanceTable() {
        GdAddSmartCampaign campaign = TestGdAddCampaigns.defaultSmartCampaign(List.of(COUNTER_ID),
                defaultAttributionModel);

        GdAddCampaignPayload response = sendRequest(campaign);
        checkState(response.getValidationResult() == null && response.getAddedCampaigns().size() == 1,
                "Unexpected error or list of campaign data is empty in response");

        List<Long> campaignIds = mapList(response.getAddedCampaigns(), GdAddCampaignPayloadItem::getId);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(campaignIds).as("size of campaign list")
                .hasSize(1);

        CampaignsPerformanceNowOptimizingBy nowOptimizingBy =
                steps.campaignsPerformanceSteps().getNowOptimizingByCid(shard, campaignIds.get(0));
        soft.assertThat(nowOptimizingBy).as("now optimizing by")
                .isEqualTo(CampaignsPerformanceNowOptimizingBy.CPC);
        soft.assertAll();
    }

    @Test
    @Parameters(method = "parametersForCheckWithExtendedGeoTargeting")
    @TestCaseName("hasExtendedGetTargeting: {0}, expect: {2}")
    public void addSmartCampaign_CheckWithExtendedGeoTargeting(Boolean hasExtendedGetTargeting,
                                                               Boolean expectedHasExtendedGetTargeting) {
        GdAddSmartCampaign campaign = TestGdAddCampaigns.defaultSmartCampaign(List.of(COUNTER_ID),
                        defaultAttributionModel)
                .withHasExtendedGeoTargeting(hasExtendedGetTargeting);

        GdAddCampaignPayload response = sendRequest(campaign);
        checkState(response.getValidationResult() == null && response.getAddedCampaigns().size() == 1,
                "Unexpected error or list of campaign data is empty in response");

        List<? extends BaseCampaign> campaigns = campaignTypedRepository
                .getTypedCampaigns(shard, mapList(response.getAddedCampaigns(), GdAddCampaignPayloadItem::getId));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(campaigns).as("size of campaign list")
                .hasSize(1);

        SmartCampaign expect = new SmartCampaign()
                .withHasExtendedGeoTargeting(expectedHasExtendedGetTargeting);

        soft.assertThat(campaigns.get(0)).as("smart campaign")
                .is(matchedBy(beanDiffer(expect).useCompareStrategy(onlyExpectedFields())));

        soft.assertAll();
    }

    private GdAddCampaignPayload sendRequest(GdAddSmartCampaign campaign) {
        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion().withSmartCampaign(campaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));
        return processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION, input, operator);
    }
}
