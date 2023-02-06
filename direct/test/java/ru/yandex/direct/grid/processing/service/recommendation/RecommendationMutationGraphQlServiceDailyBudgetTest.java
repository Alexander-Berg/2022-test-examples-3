package ru.yandex.direct.grid.processing.service.recommendation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.recommendation.model.RecommendationStatus;
import ru.yandex.direct.core.entity.recommendation.model.RecommendationStatusInfo;
import ru.yandex.direct.core.entity.recommendation.repository.RecommendationStatusRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.entity.recommendation.model.GdiRecommendation;
import ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.recommendation.GdExecuteRecommedations;
import ru.yandex.direct.grid.processing.model.recommendation.GdExecuteRecommedationsPayload;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationKey;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.ytcore.entity.recommendation.service.RecommendationService;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.recommendation.model.RecommendationStatus.DONE;
import static ru.yandex.direct.core.entity.recommendation.model.RecommendationStatus.FAILED;
import static ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType.dailyBudget;

@GridProcessingTest
@RunWith(Parameterized.class)
public class RecommendationMutationGraphQlServiceDailyBudgetTest {
    private static final int SHARD = 1;
    private static final int NEW_BUDGET = 48000;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private RecommendationStatusRepository recommendationStatusRepository;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private FeatureService featureService;

    @Mock
    private GridRecommendationService gridRecommendationService;

    @Parameterized.Parameter()
    public String description;

    @Parameterized.Parameter(1)
    public boolean autostrategy;

    @Parameterized.Parameter(2)
    public boolean budgetChanged;

    @Parameterized.Parameter(3)
    public RecommendationStatus status;

    private RecommendationMutationGraphQlService recommendationMutationGraphQlService;
    private GridGraphQLContext gridGraphQLContext;
    private Long campaignId;
    private Long clientId;
    private int currentBudget;

    @Parameterized.Parameters(name = "{0} expectedCampaignStatus={4} expectedAdGroupStatus={5}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"Default campaign", false, true, DONE},
                {"Autostrategy campaign", true, false, FAILED},
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        recommendationMutationGraphQlService
                = new RecommendationMutationGraphQlService(gridRecommendationService, recommendationService, featureService);

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId().asLong();
        CampaignInfo campaignInfo = autostrategy
                ? steps.campaignSteps().createActiveTextCampaignAutoStrategy(clientInfo)
                : steps.campaignSteps().createActiveTextCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();

        List<Campaign> campaigns = campaignRepository.getCampaigns(SHARD, singletonList(campaignId));
        currentBudget = campaigns.get(0).getStrategy().getDayBudget().intValue();

        User operator = UserHelper.getUser(campaignInfo.getClientInfo().getClient());
        gridGraphQLContext = ContextHelper.buildContext(operator);

        doReturn(singletonList(new GdiRecommendation().withKpi("{\"recommendedDailyBudget\": " + NEW_BUDGET + "}")))
                .when(gridRecommendationService)
                .getAvailableRecommendations(eq(clientId), any(), any(), eq(singleton(campaignId)), any(), any(), any(),
                        any(), any());
    }

    private RecommendationStatusInfo getEmptyRecommendationStatus() {
        return new RecommendationStatusInfo()
                .withClientId(0L)
                .withType(0L)
                .withCampaignId(0L)
                .withAdGroupId(0L)
                .withBannerId(0L)
                .withUserKey1("")
                .withUserKey2("")
                .withUserKey3("")
                .withTimestamp(0L);
    }

    @Test
    public void executeRecommendation() {
        GdExecuteRecommedationsPayload result = recommendationMutationGraphQlService
                .executeRecommendation(gridGraphQLContext, new GdExecuteRecommedations()
                        .withItems(singletonList(new GdRecommendationKey().withType(dailyBudget).withCid(campaignId))));

        assertEquals("ответ правильный", new GdExecuteRecommedationsPayload().withItems(singletonList(
                new GdRecommendationKey().withType(dailyBudget).withCid(campaignId))), result);

        List<Campaign> campaigns = campaignRepository.getCampaigns(SHARD, singletonList(campaignId));
        assertEquals("бюджет кампании", budgetChanged ? NEW_BUDGET : currentBudget,
                campaigns.get(0).getStrategy().getDayBudget().intValue());

        RecommendationStatusInfo recommendationStatus = getEmptyRecommendationStatus().withType(dailyBudget.getId())
                .withClientId(clientId).withCampaignId(campaignId);
        Set<RecommendationStatusInfo> statusesSet
                = new HashSet<>(recommendationStatusRepository.get(SHARD, singleton(recommendationStatus)));
        assertEquals("статус рекомендации",
                singleton(recommendationStatus.withStatus(status)), statusesSet);
    }
}
