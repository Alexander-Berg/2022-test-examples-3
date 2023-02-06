package ru.yandex.direct.grid.processing.service.recommendation;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.recommendation.model.RecommendationStatus;
import ru.yandex.direct.core.entity.recommendation.model.RecommendationStatusInfo;
import ru.yandex.direct.core.entity.recommendation.repository.RecommendationStatusRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
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
import ru.yandex.direct.ytcore.entity.recommendation.service.RecommendationService;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType.removePagesFromBlackListOfACampaign;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RecommendationMutationGraphQlServiceBackListTest {
    private static final int SHARD = 1;
    private static final String DOMAIN1 = "ya.ru";
    private static final String DOMAIN_FOR_REMOVAL = "wrong.com";
    private static final GdiRecommendationType RECOMMENDATION_TYPE = removePagesFromBlackListOfACampaign;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private RecommendationStatusRepository recommendationStatusRepository;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private FeatureService featureService;

    @Mock
    private GridRecommendationService gridRecommendationService;

    private RecommendationMutationGraphQlService recommendationMutationGraphQlService;
    private GridGraphQLContext gridGraphQLContext;
    private Long campaignId;
    private Long clientId;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        recommendationMutationGraphQlService
                = new RecommendationMutationGraphQlService(gridRecommendationService, recommendationService, featureService);

        CampaignInfo campaignInfo = campaignSteps.createCampaign(activeTextCampaign(null, null)
                .withDisabledDomains(ImmutableSet.of(DOMAIN1, DOMAIN_FOR_REMOVAL)));

        campaignId = campaignInfo.getCampaignId();
        clientId = campaignInfo.getClientId().asLong();

        User operator = UserHelper.getUser(campaignInfo.getClientInfo().getClient());
        gridGraphQLContext = ContextHelper.buildContext(operator);

        doReturn(singletonList(new GdiRecommendation().withKpi("{\"campaigns\": [{\"cid\": " + campaignId + "}]}")))
                .when(gridRecommendationService)
                .getAvailableRecommendations(eq(clientId), any(), any(), eq(singleton(campaignId)), any(), any(), any(), any(), any());
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
        GdExecuteRecommedationsPayload expectedResult = new GdExecuteRecommedationsPayload().withItems(singletonList(
                new GdRecommendationKey().withType(RECOMMENDATION_TYPE).withCid(campaignId)
                        .withUserKey1(DOMAIN_FOR_REMOVAL)));
        GdExecuteRecommedationsPayload result = recommendationMutationGraphQlService
                .executeRecommendation(gridGraphQLContext, new GdExecuteRecommedations()
                        .withItems(singletonList(new GdRecommendationKey().withType(RECOMMENDATION_TYPE)
                                .withCid(campaignId).withUserKey1(DOMAIN_FOR_REMOVAL))));
        assertEquals("ответ правильный", expectedResult, result);

        Campaign campaign = campaignRepository.getCampaigns(SHARD, singletonList(campaignId)).get(0);
        assertEquals("домен из списка удален", ImmutableSet.of(DOMAIN1), campaign.getDisabledDomains());

        RecommendationStatusInfo recommendationStatus = getEmptyRecommendationStatus().withType(
                RECOMMENDATION_TYPE.getId())
                .withClientId(clientId).withCampaignId(campaignId).withUserKey1(DOMAIN_FOR_REMOVAL);
        Set<RecommendationStatusInfo> statusesSet
                = new HashSet<>(recommendationStatusRepository.get(SHARD, singleton(recommendationStatus)));
        assertEquals("статус рекомендации изменен",
                singleton(recommendationStatus.withStatus(RecommendationStatus.DONE)), statusesSet);
    }
}
