package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.BrandSurveyStopReason;
import ru.yandex.direct.core.entity.campaign.model.CampaignBudgetReachDaily;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.repository.CampaignBudgetReachDailyRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.model.campaign.GdBrandSurveyStatus;
import ru.yandex.direct.grid.model.campaign.GdBrandSurveyStopReason;
import ru.yandex.direct.grid.model.campaign.GdSurveyStatus;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.feature.FeatureName.SHOW_CPM_BANNER_CAMPAIGNS_IN_GRID;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class CampaignGraphQlServiceGetBrandSurveyStatusTest {
    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    campaigns(input: %s) {\n"
            + "      rowset {\n"
            + "        ... on GdCpmBannerCampaign {\n"
            + "          id\n"
            + "          brandSurveyId\n"
            + "          brandSurveyStatus {\n"
            + "            surveyStatusDaily\n"
            + "            reasonIds\n"
            + "            brandSurveyStopReasonsDaily\n"
            + "            sumSpentByDay\n"
            + "            sumSpentByTotalPeriod\n"
            + "          }\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private CampaignBudgetReachDailyRepository campaignBudgetReachDailyRepository;

    @Before
    public void before() {
    }

    @Test
    public void getGdBrandSurveyStatusTest() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        steps.dbQueueSteps().registerJobType(RECALC_BRAND_LIFT_CAMPAIGNS);

        String brandSurveyId0 = "brandSurveyId0";
        String brandSurveyId1 = "brandSurveyId1";
        String brandSurveyId2 = "brandSurveyId2";

        var startDate = LocalDate.of(2020, 2, 22);
        CampaignInfo campaign0 = steps.campaignSteps()
                .createActiveCpmBannerCampaignWithBrandLift(userInfo.getClientInfo(), brandSurveyId0,
                        startDate, startDate.plusDays(1L));
        CampaignInfo campaign1 = steps.campaignSteps()
                .createActiveCpmBannerCampaignWithBrandLift(userInfo.getClientInfo(), brandSurveyId1,
                        startDate, startDate.plusDays(1L));
        CampaignInfo campaign2 = steps.campaignSteps()
                .createActiveCpmBannerCampaignWithBrandLift(userInfo.getClientInfo(), brandSurveyId2,
                        startDate, startDate.plusDays(1L));
        CampaignInfo campaign3 = steps.campaignSteps()
                .createActiveCpmBannerCampaign(userInfo.getClientInfo());

        List<CampaignInfo> allCampaigns = List.of(campaign0, campaign1, campaign2, campaign3);
        allCampaigns.forEach(campaignInfo -> steps.campaignSteps().setPlatform(campaignInfo, CampaignsPlatform.BOTH));
        LinkedHashSet<Long> allCampaignIds = allCampaigns.stream().map(CampaignInfo::getCampaignId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Long defaultValue = 10L;
        Long largeValue = 30L;
        BigDecimal defaultBigDecimalValue = new BigDecimal(defaultValue);
        BigDecimal largeBigDecimalValue = new BigDecimal(largeValue);
        LocalDate smallDate = LocalDate.of(2017, 6, 6);
        LocalDate largeDate = LocalDate.of(2018, 6, 6);

        CampaignBudgetReachDaily campaignBudgetReach0 = new CampaignBudgetReachDaily()
                .withCampaignId(campaign0.getCampaignId())
                .withBudgetThreshold(defaultBigDecimalValue)
                .withBudgetSpent(defaultBigDecimalValue)
                .withBudgetEstimated(defaultBigDecimalValue)
                .withTargetForecast(defaultValue)
                .withTargetThreshold(defaultValue)
                .withDate(largeDate)
                .withTrafficLightColour(1L)
                .withBrandSurveyStopReasons(EnumSet.of(BrandSurveyStopReason.LOW_DAILY_BUDGET,
                        BrandSurveyStopReason.LOW_REACH));

        CampaignBudgetReachDaily campaignBudgetReach1 = new CampaignBudgetReachDaily()
                .withCampaignId(campaign1.getCampaignId())
                .withBudgetThreshold(largeBigDecimalValue)
                .withBudgetSpent(defaultBigDecimalValue)
                .withBudgetEstimated(defaultBigDecimalValue)
                .withTargetForecast(defaultValue)
                .withTargetThreshold(largeValue)
                .withDate(largeDate)
                .withTrafficLightColour(1L)
                .withBrandSurveyStopReasons(EnumSet.noneOf(BrandSurveyStopReason.class));

        CampaignBudgetReachDaily campaignBudgetReach2 = new CampaignBudgetReachDaily()
                .withCampaignId(campaign1.getCampaignId())
                .withBudgetThreshold(defaultBigDecimalValue)
                .withBudgetSpent(defaultBigDecimalValue)
                .withBudgetEstimated(defaultBigDecimalValue)
                .withTargetForecast(defaultValue)
                .withTargetThreshold(defaultValue)
                .withDate(smallDate)
                .withTrafficLightColour(1L)
                .withBrandSurveyStopReasons(null);

        campaignBudgetReachDailyRepository.addCampaignBudgetReaches(userInfo.getShard(),
                asList(campaignBudgetReach0, campaignBudgetReach1, campaignBudgetReach2));

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(allCampaignIds, userInfo);
        MatcherAssert.assertThat(rowset, hasSize(4));

        Map<Long, String> brandSurveyIdByCid = new HashMap<>();
        Map<Long, GdBrandSurveyStatus> brandSurveyStatusByCid = new HashMap<>();
        rowset.forEach(camp -> {
            Long campaignId = Long.parseLong(camp.get("id").toString());
            String brandSurveyId = ifNotNull(camp.get("brandSurveyId"), t -> t.toString());
            GdBrandSurveyStatus brandSurveyStatus = ifNotNull(camp.get("brandSurveyStatus"),
                    t -> JsonUtils.getObjectMapper().convertValue(t, GdBrandSurveyStatus.class));
            brandSurveyIdByCid.put(campaignId, brandSurveyId);
            brandSurveyStatusByCid.put(campaignId, brandSurveyStatus);
        });


        GdBrandSurveyStatus expectedBrandSurveyStatus = new GdBrandSurveyStatus()
                .withSurveyStatusDaily(GdSurveyStatus.DRAFT)
                .withReasonIds(Collections.emptyList())
                .withBrandSurveyStopReasonsDaily(EnumSet.noneOf(GdBrandSurveyStopReason.class))
                .withSumSpentByTotalPeriod(new BigDecimal("20.00"))
                .withSumSpentByDay(new BigDecimal("10.00"));


        MatcherAssert.assertThat(brandSurveyIdByCid.get(campaign0.getCampaignId()), equalTo(brandSurveyId0));
        MatcherAssert.assertThat(brandSurveyStatusByCid.get(campaign0.getCampaignId()),
                beanDiffer(expectedBrandSurveyStatus));

        MatcherAssert.assertThat(brandSurveyIdByCid.get(campaign1.getCampaignId()), equalTo(brandSurveyId1));
        MatcherAssert.assertThat(brandSurveyStatusByCid.get(campaign1.getCampaignId()),
                beanDiffer(expectedBrandSurveyStatus));

        expectedBrandSurveyStatus.setSumSpentByDay(BigDecimal.ZERO);
        expectedBrandSurveyStatus.withSumSpentByTotalPeriod(BigDecimal.ZERO);
        MatcherAssert.assertThat(brandSurveyIdByCid.get(campaign2.getCampaignId()), equalTo(brandSurveyId2));
        MatcherAssert.assertThat(brandSurveyStatusByCid.get(campaign2.getCampaignId()),
                beanDiffer(expectedBrandSurveyStatus));

        MatcherAssert.assertThat(brandSurveyIdByCid.get(campaign3.getCampaignId()), nullValue());
        MatcherAssert.assertThat(brandSurveyStatusByCid.get(campaign3.getCampaignId()), nullValue());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> sendRequestAndGetRowset(Set<Long> campaignIds, UserInfo userInfo) {
        GridGraphQLContext context = ContextHelper.buildContext(userInfo.getUser()).withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
        steps.featureSteps().addClientFeature(userInfo.getClientId(), SHOW_CPM_BANNER_CAMPAIGNS_IN_GRID, true);
        GdCampaignsContainer campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        campaignsContainer.getFilter().setCampaignIdIn(campaignIds);

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));

        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());

        Map<String, Object> clientData = (Map<String, Object>) ((Map<String, Object>) result.getData()).get("client");
        Map<String, Object> campaignsData = (Map<String, Object>) clientData.get("campaigns");
        return (List<Map<String, Object>>) campaignsData.get("rowset");
    }
}
