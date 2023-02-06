package ru.yandex.direct.grid.processing.service.recommendation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.recommendation.model.RecommendationStatus;
import ru.yandex.direct.core.entity.recommendation.model.RecommendationStatusInfo;
import ru.yandex.direct.core.entity.recommendation.repository.RecommendationStatusRepository;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.recommendation.model.GdiRecommendation;
import ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService;
import ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.recommendation.GdExecuteRecommedations;
import ru.yandex.direct.grid.processing.model.recommendation.GdExecuteRecommedationsPayload;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationKey;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.ytcore.entity.recommendation.service.RecommendationService;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType.switchOnAutotargeting;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RecommendationMutationGraphQlServiceAutotargetingTest {
    private static final int SHARD = 1;
    private static final GdiRecommendationType RECOMMENDATION_TYPE = switchOnAutotargeting;

    @Mock
    private GridRecommendationService gridRecommendationService;

    @Autowired
    private AdGroupSteps groupSteps;

    @Autowired
    private RecommendationStatusRepository recommendationStatusRepository;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private RelevanceMatchRepository relevanceMatchRepository;

    @Autowired
    private FeatureService featureService;

    private RecommendationMutationGraphQlService recommendationMutationGraphQlService;
    private GridGraphQLContext gridGraphQLContext;
    private Long clientId;
    private Long campaignId;
    private Long adGroupId;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        recommendationMutationGraphQlService
                = new RecommendationMutationGraphQlService(gridRecommendationService, recommendationService, featureService);

        AdGroupInfo activeTextAdGroup = groupSteps.createActiveTextAdGroup();
        adGroupId = activeTextAdGroup.getAdGroupId();
        CampaignInfo campaignInfo = activeTextAdGroup.getCampaignInfo();
        campaignId = campaignInfo.getCampaignId();
        clientId = campaignInfo.getClientId().asLong();
        User operator = campaignInfo.getClientInfo().getChiefUserInfo().getUser();
        gridGraphQLContext = ContextHelper.buildContext(operator);

        doReturn(singletonList(new GdiRecommendation()))
                .when(gridRecommendationService)
                .getAvailableRecommendations(eq(clientId), any(), any(), eq(singleton(campaignId)), any(), any(),
                        any(), any(), any());
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
                        .withItems(singletonList(new GdRecommendationKey().withType(RECOMMENDATION_TYPE)
                                .withCid(campaignId).withPid(adGroupId))));

        assertEquals("ответ правильный", new GdExecuteRecommedationsPayload().withItems(
                singletonList(new GdRecommendationKey().withType(RECOMMENDATION_TYPE).withCid(campaignId)
                        .withPid(adGroupId))), result);

        Collection<RelevanceMatch> relevanceMatches = relevanceMatchRepository
                .getRelevanceMatchesByAdGroupIds(SHARD, ClientId.fromLong(clientId), singleton(adGroupId)).values();
        assertEquals("автотаргетинг установлен", 1, relevanceMatches.size());

        RecommendationStatusInfo recommendationStatus = getEmptyRecommendationStatus()
                .withType(RECOMMENDATION_TYPE.getId())
                .withClientId(clientId).withCampaignId(campaignId).withAdGroupId(adGroupId);
        Set<RecommendationStatusInfo> statusesSet
                = new HashSet<>(recommendationStatusRepository.get(SHARD, singleton(recommendationStatus)));
        assertEquals("статус рекомендации изменен",
                singleton(recommendationStatus.withStatus(RecommendationStatus.DONE)), statusesSet);
    }
}
