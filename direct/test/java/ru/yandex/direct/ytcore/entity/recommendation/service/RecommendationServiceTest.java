package ru.yandex.direct.ytcore.entity.recommendation.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.ads.bsyeti.libs.communications.EChannel;
import ru.yandex.ads.bsyeti.libs.communications.ESourceType;
import ru.yandex.ads.bsyeti.libs.communications.EWebUIButtonActionType;
import ru.yandex.ads.bsyeti.libs.communications.EWebUIButtonStyle;
import ru.yandex.ads.bsyeti.libs.communications.TEventSource;
import ru.yandex.ads.bsyeti.libs.communications.proto.TMessageData;
import ru.yandex.direct.communication.CommunicationChannelRepository;
import ru.yandex.direct.communication.container.CommunicationMessageData;
import ru.yandex.direct.communication.facade.impl.actions.ApplyDailyBudgetRecommendation;
import ru.yandex.direct.communication.inventory.CommunicationInventoryClient;
import ru.yandex.direct.communication.model.inventory.ObjectEventData;
import ru.yandex.direct.communication.model.inventory.Response;
import ru.yandex.direct.communication.model.inventory.SlotResponse;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.communication.model.ButtonConfig;
import ru.yandex.direct.core.entity.communication.model.CommunicationEventVersion;
import ru.yandex.direct.core.entity.communication.model.CommunicationEventVersionStatus;
import ru.yandex.direct.core.entity.communication.model.TargetEntityType;
import ru.yandex.direct.core.entity.communication.repository.CommunicationEventVersionsRepository;
import ru.yandex.direct.core.entity.recommendation.model.RecommendationQueueInfo;
import ru.yandex.direct.core.entity.recommendation.model.RecommendationStatusInfo;
import ru.yandex.direct.core.entity.recommendation.repository.RecommendationQueueRepository;
import ru.yandex.direct.core.entity.recommendation.repository.RecommendationStatusRepository;
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.ytcore.configuration.YtCoreTest;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.recommendation.model.RecommendationStatus.DONE;
import static ru.yandex.direct.core.entity.recommendation.model.RecommendationStatus.IN_PROGRESS;

@YtCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RecommendationServiceTest {
    private static final int SHARD = 1;
    private static final int PAR_ID = 11;
    private static final int NEW_BUDGET = 48000;

    private static final long EVENT_ID = 15;
    private static final long EVENT_VERSION_ID = 9;
    public static final long ITER = 9L;
    public static final long SLOT = 13L;
    public static final int REQUEST_ID = 5;

    public static final int MINOR_VERSION = 0;
    public static final int MAJOR_VERSION = 1;

    public static final String DUMMY = "DUMMY";
    public static final String RECOMMENDATION_DASHBOARD = "recommendation_dashboard";

    @Autowired
    private RecommendationQueueRepository recommendationQueueRepository;

    @Autowired
    private RecommendationStatusRepository recommendationStatusRepository;

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private CommunicationInventoryClient communicationInventoryClient;

    @Autowired
    private CommunicationChannelRepository communicationChannelRepository;

    @Autowired
    private CommunicationEventVersionsRepository communicationEventVersionsRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private RecommendationService recommendationService;

    private RecommendationQueueInfo recommendation;
    private RecommendationStatusInfo recommendationStatus;
    private Long campaignId;

    @Before
    public void before() {
        CampaignInfo campaignInfo = steps.textCampaignSteps().createDefaultCampaign();
        campaignId = campaignInfo.getCampaignId();
        steps.featureSteps().setCurrentClient(campaignInfo.getClientId());


        recommendation = getEmptyRecommendation()
                .withUid(campaignInfo.getUid())
                .withClientId(campaignInfo.getClientId().asLong())
                .withCampaignId(campaignId)
                .withType(1L)
                .withJsonData("{\"recommendedDailyBudget\": " + NEW_BUDGET + "}");

        recommendationStatus = RecommendationService.convertToRecommendationStatusInfo(recommendation)
                .withStatus(IN_PROGRESS);
        recommendationService.add(SHARD, singleton(recommendation));
    }

    @After
    public void after() {
        recommendationQueueRepository.delete(SHARD, singleton(recommendation.getId()));
        recommendationStatusRepository.delete(SHARD, singleton(recommendationStatus));
    }

    private RecommendationQueueInfo getEmptyRecommendation() {
        return new RecommendationQueueInfo()
                .withId(RandomUtils.nextLong(1, Integer.MAX_VALUE))
                .withClientId(0L)
                .withType(0L)
                .withCampaignId(0L)
                .withAdGroupId(0L)
                .withBannerId(0L)
                .withUserKey1(EVENT_ID + "-" + EVENT_VERSION_ID)
                .withUserKey2(MAJOR_VERSION + "-" + MINOR_VERSION)
                .withUserKey3(REQUEST_ID + "-" + RECOMMENDATION_DASHBOARD)
                .withTimestamp(0L)
                .withSecTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .withQueueTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .withUid(0L)
                .withParId(null);
    }

    @Test
    @Description("При добавлении рекомендация добавлена в обе таблицы")
    public void add() {
        Set<RecommendationQueueInfo> recommendationsSet
                = new HashSet<>(recommendationQueueRepository.get(SHARD, singleton(recommendation.getId())));
        assertThat(recommendationsSet).isEqualTo(singleton(recommendation));

        Set<RecommendationStatusInfo> statusesSet
                = new HashSet<>(recommendationStatusRepository.get(SHARD, singleton(recommendationStatus)));

        assertThat(statusesSet).isEqualTo(singleton(recommendationStatus));
    }

    @Test
    @Description("Выполнение рекомендации, функциональный тест")
    public void process() {
        prepareMocks();

        //фича включена на 100%
        steps.featureSteps().enableClientFeature(FeatureName.DISABLE_OLD_DAILY_BUDGET_RECOMMENDATION);
        recommendationService.processFromQueue(SHARD, PAR_ID);

        TextCampaign campaign = campaignTypedRepository.getIdToModelSafely(SHARD, List.of(campaignId),
                TextCampaign.class).get(campaignId);
        DefaultManualStrategy strategy = strategyTypedRepository.getIdToModelSafely(SHARD,
                List.of(campaign.getStrategyId()), DefaultManualStrategy.class).get(campaign.getStrategyId());

        int expectedDayBudget = 4000;
        assertThat(strategy.getDayBudget().intValue()).isEqualTo(expectedDayBudget);
        assertThat(campaign.getDayBudget().intValue()).isEqualTo(expectedDayBudget);


        Set<RecommendationQueueInfo> recommendationsSet
                = new HashSet<>(recommendationQueueRepository.get(SHARD, singleton(recommendation.getId())));
        assertThat(recommendationsSet).isEmpty();

        Set<RecommendationStatusInfo> statusesSet
                = new HashSet<>(recommendationStatusRepository.get(SHARD, singleton(recommendationStatus)));
        assertThat(statusesSet).isEqualTo(singleton(recommendationStatus.withStatus(DONE)));
    }

    private void prepareMocks() {
        CommunicationEventVersion communicationEventVersion = new CommunicationEventVersion()
                .withEventId(EVENT_ID)
                .withIter(ITER)
                .withTargetEntityType(TargetEntityType.CAMPAIGN)
                .withSlots(List.of(SLOT))
                .withCheckActual(DUMMY)
                .withButtonConfigs(List.of(new ButtonConfig()
                        .withHandlerName(ApplyDailyBudgetRecommendation.APPLY_DAILY_BUDGET)
                        .withAction(EWebUIButtonActionType.BUTTON_ACTION_TYPE_ACTION.name())
                        .withStyle(EWebUIButtonStyle.BUTTON_STYLE_APPLY.name())))
                .withStatus(CommunicationEventVersionStatus.ACTIVE);
        when(communicationEventVersionsRepository.getVersions(any())).thenReturn(List.of(communicationEventVersion));
        when(communicationEventVersionsRepository.getVersion(any(), any())).thenReturn(communicationEventVersion);
        int dayBudget = 48000000;
        when(communicationInventoryClient.getRecommendations(any())).thenReturn(Response.newBuilder()
                .addSlotResponses(SlotResponse.newBuilder()
                        .setSlotId(13L)
                        .addObjectEventData(ObjectEventData.newBuilder()
                                .setEventId(EVENT_ID)
                                .setData("{\"new_daily_budget\": " + dayBudget + "}")
                                .setEventVersionId(EVENT_VERSION_ID)
                                .setObjectId(1L)
                                .build())
                        .build())
                .build());

        when(communicationChannelRepository.getCommunicationMessageByIds(any()))
                .thenReturn(List.of(createCommunicationMessageData(1L, 1L, 1L, 1L, 1L)));
    }

    private CommunicationMessageData createCommunicationMessageData(
            long messageId,
            long userId,
            long targetEntityId,
            long eventId,
            long versionId
    ) {
        TEventSource eventSource = TEventSource.newBuilder()
                .setType(ESourceType.DIRECT_OFFLINE_REGULAR)
                .setId(versionId)
                .build();
        return new CommunicationMessageData(EChannel.DIRECT_WEB_UI, messageId, userId, targetEntityId, eventId, null,
                eventSource, TMessageData.getDefaultInstance());
    }
}


