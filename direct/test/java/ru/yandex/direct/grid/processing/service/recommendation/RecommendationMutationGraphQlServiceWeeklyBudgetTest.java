package ru.yandex.direct.grid.processing.service.recommendation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.recommendation.RecommendationType;
import ru.yandex.direct.core.entity.recommendation.model.KpiWeeklyBudget;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.grid.core.entity.recommendation.model.GdiRecommendation;
import ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService;
import ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.recommendation.GdExecuteRecommedations;
import ru.yandex.direct.grid.processing.model.recommendation.GdExecuteRecommedationsPayload;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationKey;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.ytcore.entity.recommendation.service.RecommendationService;

import static com.google.common.collect.Sets.cartesianProduct;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.decreaseStrategyTargetROI;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.increaseStrategyTargetCPA;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.increaseStrategyWeeklyBudget;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.weeklyBudget;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCampaignWithoutType;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetRoiStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpaStrategy;

@GridProcessingTest
@RunWith(Parameterized.class)
public class RecommendationMutationGraphQlServiceWeeklyBudgetTest {
    private static final int SHARD = 1;
    public static final Integer COUNTER_ID = 1;
    private static final Long VALID_GOAL_ID = 55L;
    private static final Gson GSON = new Gson();

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private CampaignModifyRepository campaignModifyRepository;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private MetrikaClientStub metrikaClientStub;

    @Autowired
    private FeatureService featureService;

    @Mock
    private GridRecommendationService gridRecommendationService;

    private RecommendationMutationGraphQlService recommendationMutationGraphQlService;
    private GridGraphQLContext gridGraphQLContext;
    private Long campaignId;

    @Parameterized.Parameter()
    public String description;

    @Parameterized.Parameter(1)
    public DbStrategy dbStrategy;

    @Parameterized.Parameter(2)
    public RecommendationType recommendationType;


    //Недельный бюджет в базе
    @Parameterized.Parameter(3)
    public BigDecimal dbWeeklyBudget;

    //Текущий недельный бюджет в рекомендации (для применения должен совпадать с данными в базе)
    @Parameterized.Parameter(4)
    public BigDecimal currentWeeklyBudget;

    //Рекомендуемый недельный бюджет
    @Parameterized.Parameter(5)
    public BigDecimal recommendedWeeklyBudget;

    //Недельный бюджет после применения
    @Parameterized.Parameter(6)
    public BigDecimal finalWeeklyBudget;


    @Parameterized.Parameter(7)
    public BigDecimal dbTargetCPA;

    @Parameterized.Parameter(8)
    public BigDecimal currentTargetCPA;

    @Parameterized.Parameter(9)
    public BigDecimal recommendedTargetCPA;

    @Parameterized.Parameter(10)
    public BigDecimal finalTargetCPA;


    @Parameterized.Parameter(11)
    public BigDecimal dbTargetROI;

    @Parameterized.Parameter(12)
    public BigDecimal currentTargetROI;

    @Parameterized.Parameter(13)
    public BigDecimal recommendedTargetROI;

    @Parameterized.Parameter(14)
    public BigDecimal finalTargetROI;

    @Parameterized.Parameter(15)
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        return cartesianProduct(
            ImmutableSet.of(
                new Object[]
                    {"Меняем недельный бюджет у автобюджетной стратегии", defaultAutobudgetStrategy(),
                        weeklyBudget,
                        new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(2000), new BigDecimal(2000),
                        null, null, null, null,
                        null, null, null, null},

                new Object[]
                    {"Недельный бюджет уже был изменен, его не меняем", defaultAutobudgetStrategy(),
                        weeklyBudget,
                        new BigDecimal(1500), new BigDecimal(1000), new BigDecimal(2000), new BigDecimal(1500),
                        null, null, null, null,
                        null, null, null, null},

                new Object[]
                    {"Меняем недельный бюджет у стратегии средний CPA", defaultAverageCpaStrategy(VALID_GOAL_ID),
                        increaseStrategyWeeklyBudget,
                        new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(2000), new BigDecimal(2000),
                        new BigDecimal(100), new BigDecimal(100), new BigDecimal(100), new BigDecimal(100),
                        null, null, null, null},

                new Object[]
                    {"Меняем недельный бюджет и CPA", defaultAverageCpaStrategy(VALID_GOAL_ID),
                        increaseStrategyTargetCPA,
                        new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(2000), new BigDecimal(2000),
                        new BigDecimal(100), new BigDecimal(100), new BigDecimal(200), new BigDecimal(200),
                        null, null, null, null},

                new Object[]
                    {"Меняем только CPA", defaultAverageCpaStrategy(VALID_GOAL_ID),
                        increaseStrategyTargetCPA,
                        new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(1000),
                        new BigDecimal(100), new BigDecimal(100), new BigDecimal(200), new BigDecimal(200),
                        null, null, null, null},

                new Object[]
                    {"Недельный бюджет уже был изменен, ничего не меняем", defaultAverageCpaStrategy(VALID_GOAL_ID),
                        increaseStrategyTargetCPA,
                        new BigDecimal(1500), new BigDecimal(1000), new BigDecimal(2000), new BigDecimal(1500),
                        new BigDecimal(100), new BigDecimal(100), new BigDecimal(200), new BigDecimal(100),
                        null, null, null, null},

                new Object[]
                    {"Меняем недельный бюджет у стратегии ROI", defaultAutobudgetRoiStrategy(VALID_GOAL_ID),
                        increaseStrategyWeeklyBudget,
                        new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(2000), new BigDecimal(2000),
                        null, null, null, null,
                        new BigDecimal(1), new BigDecimal(1), new BigDecimal(1), new BigDecimal(1)},

                new Object[]
                    {"Меняем недельный бюджет и рентабельность", defaultAutobudgetRoiStrategy(VALID_GOAL_ID),
                        decreaseStrategyTargetROI,
                        new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(2000), new BigDecimal(2000),
                        null, null, null, null,
                        new BigDecimal(2), new BigDecimal(2), new BigDecimal(1), new BigDecimal(1)},

                new Object[]
                    {"Рентабельность уже была изменена, ничего не меняем",
                        defaultAutobudgetRoiStrategy(VALID_GOAL_ID), decreaseStrategyTargetROI,
                        new BigDecimal(2000), new BigDecimal(1000), new BigDecimal(2000), new BigDecimal(2000),
                        null, null, null, null,
                        new BigDecimal(0), new BigDecimal(2), new BigDecimal(1), new BigDecimal(0)},

                new Object[]
                    {"Рентабельность можно уменьшить до -0.99",
                        defaultAutobudgetRoiStrategy(VALID_GOAL_ID), decreaseStrategyTargetROI,
                        new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(1000),
                        null, null, null, null,
                        new BigDecimal(0), new BigDecimal(0), new BigDecimal(-0.99), new BigDecimal(-0.99)},

                new Object[]
                    {"Рентабельность нельзя уменьшать меньше -0.99",
                        defaultAutobudgetRoiStrategy(VALID_GOAL_ID), decreaseStrategyTargetROI,
                        new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(1000),
                        null, null, null, null,
                        new BigDecimal(0), new BigDecimal(0), new BigDecimal(-1), new BigDecimal(0)}
                ),
            ImmutableSet.of(
                new Object[]{CampaignType.TEXT},
                new Object[]{CampaignType.DYNAMIC}
            )).stream().map(e -> ArrayUtils.addAll(e.get(0), e.get(1))).collect(toList());
    }

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        Long clientId = clientInfo.getClientId().asLong();
        steps.featureSteps().setCurrentClient(clientInfo.getClientId());

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
            activeCampaignWithoutType(null, null)
                .withType(campaignType)
                .withEmail("aa@bb.ru")
                .withStrategy(averageCpaStrategy())
                .withMetrikaCounters(singletonList(COUNTER_ID.longValue())),
            clientInfo
        );

        campaignId = campaignInfo.getCampaignId();
        CampaignWithCustomStrategy campaign =
            (CampaignWithCustomStrategy) campaignTypedRepository.getTypedCampaigns(SHARD, List.of(campaignId)).get(0);

        dbStrategy.withAutobudget(CampaignsAutobudget.YES);
        dbStrategy.getStrategyData()
            .withSum(dbWeeklyBudget)
            .withAvgCpa(dbTargetCPA)
            .withRoiCoef(dbTargetROI);

        ModelChanges<CampaignWithCustomStrategy> campModelChanges
            = ModelChanges.build(campaign, CampaignWithCustomStrategy.STRATEGY, dbStrategy);

        var appliedChangesList = List.of(campModelChanges.applyTo(campaign));
        RestrictedCampaignsUpdateOperationContainer updateParameters = RestrictedCampaignsUpdateOperationContainer.create(SHARD,
            clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid());
        campaignModifyRepository.updateCampaigns(updateParameters, appliedChangesList);

        metrikaClientStub.addUserCounter(campaignInfo.getUid(), COUNTER_ID);
        metrikaClientStub.addCounterGoal(COUNTER_ID, VALID_GOAL_ID.intValue());

        User operator = UserHelper.getUser(campaignInfo.getClientInfo().getClient());
        gridGraphQLContext = ContextHelper.buildContext(operator);

        MockitoAnnotations.initMocks(this);
        recommendationMutationGraphQlService
            = new RecommendationMutationGraphQlService(gridRecommendationService, recommendationService, featureService);

        doReturn(singletonList(new GdiRecommendation()
            .withKpi(GSON.toJson(
                new KpiWeeklyBudget()
                    .withCurrentWeeklyBudget(currentWeeklyBudget)
                    .withRecommendedWeeklyBudget(recommendedWeeklyBudget)
                    .withCurrentTargetCPA(currentTargetCPA)
                    .withRecommendedTargetCPA(recommendedTargetCPA)
                    .withCurrentTargetROI(currentTargetROI)
                    .withRecommendedTargetROI(recommendedTargetROI)))))
            .when(gridRecommendationService)
            .getAvailableRecommendations(eq(clientId), any(), any(), eq(singleton(campaignId)), any(), any(), any(),
                any(), any());
    }

    @Test
    public void executeRecommendation() {
        GdiRecommendationType gdiRecommendationType = GdiRecommendationType.fromType(recommendationType);
        GdExecuteRecommedationsPayload result = recommendationMutationGraphQlService
            .executeRecommendation(gridGraphQLContext, new GdExecuteRecommedations()
                .withItems(singletonList(new GdRecommendationKey()
                    .withType(gdiRecommendationType)
                    .withCid(campaignId))));

        assertEquals("ответ правильный", new GdExecuteRecommedationsPayload().withItems(singletonList(
            new GdRecommendationKey().withType(gdiRecommendationType).withCid(campaignId))), result);

        Campaign campaign = campaignRepository.getCampaigns(SHARD, singletonList(campaignId)).get(0);
        StrategyData strategyData = campaign.getStrategy().getStrategyData();

        BigDecimal roundedFinalTargetROI =
                finalTargetROI != null ? finalTargetROI.setScale(2, RoundingMode.HALF_UP) : null;
        BigDecimal roundedTargetROI =
                strategyData.getRoiCoef() != null ? strategyData.getRoiCoef().setScale(2, RoundingMode.HALF_UP) : null;

        assertEquals(finalWeeklyBudget, strategyData.getSum());
        assertEquals(finalTargetCPA, strategyData.getAvgCpa());
        assertEquals(roundedFinalTargetROI, roundedTargetROI);
    }
}
