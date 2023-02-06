package ru.yandex.direct.core.entity.campaign.service;


import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.container.DeleteCampMetrikaCountersRequest;
import ru.yandex.direct.core.entity.campaign.container.UpdateCampMetrikaCountersRequest;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.info.strategy.DefaultManualStrategyInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbschema.ppc.enums.MetrikaCountersSource;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampMetrikaCountersServiceUpdateCountersForStrategiesExampleTest {

    private static final Long BUCKET_SIZE = 100L;
    private static final Long COUNTER_ID = randomIdFromBucket(0);
    private static final Long COUNTER_ID2 = randomIdFromBucket(1);
    private static final Long NONSPRAV_COUNTER_ID = randomIdFromBucket(3);
    private static final Long NONSPRAV_COUNTER_ID2 = randomIdFromBucket(4);
    @Autowired
    private Steps steps;
    @Autowired
    private CampMetrikaCountersService campMetrikaCountersService;

    @Autowired
    private MetrikaClientStub metrikaClientStub;
    private long uid;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private Long campaignId;
    private DefaultManualStrategyInfo strategyInfo;

    private static Long randomIdFromBucket(int bucket) {
        return bucket * BUCKET_SIZE + RandomNumberUtils.nextPositiveLong(BUCKET_SIZE);
    }

    @Before
    public void before() {
        strategyInfo = steps.defaultManualStrategySteps().createDefaultStrategyWithCampaign();

        clientInfo = strategyInfo.getClientInfo();
        clientId = clientInfo.getClientId();
        uid = clientInfo.getUid();

        steps.featureSteps().addClientFeature(clientId,
                FeatureName.TOGETHER_UPDATING_STRATEGY_AND_CAMPAIGN_METRIKA_COUNTERS, true);

        // создал компанию со стратегией
        campaignId = strategyInfo.getTypedStrategy().getCids().get(0);

        CounterInfoDirect nonSpravCounter1 = new CounterInfoDirect()
                .withId(NONSPRAV_COUNTER_ID.intValue())
                .withCounterSource(MetrikaCountersSource.turbo.getName());

        CounterInfoDirect nonSpravCounter2 = new CounterInfoDirect()
                .withId(NONSPRAV_COUNTER_ID2.intValue())
                .withCounterSource(MetrikaCountersSource.turbo.getName());

        metrikaClientStub.addUserCounter(uid, nonSpravCounter1);
        metrikaClientStub.addUserCounter(uid, nonSpravCounter2);
    }

    private TextCampaignInfo createTextCampaign() {
        TextCampaign textCampaign = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo);
        return steps.textCampaignSteps().createCampaign(clientInfo, textCampaign);
    }

    @Test
    public void addCountersToStrategy_strategyWithoutCounters() {
        // test
        add(List.of(COUNTER_ID));

        steps.defaultManualStrategySteps().updateToActualStrategy(strategyInfo);

        assertThat(strategyInfo.getTypedStrategy().getMetrikaCounters())
                .hasSameElementsAs(List.of(COUNTER_ID));
    }

    @Test
    public void addCountersToStrategy_addExistingCounter() {
        // preparing
        add(List.of(COUNTER_ID, COUNTER_ID2));

        steps.defaultManualStrategySteps().updateToActualStrategy(strategyInfo);

        assertThat(strategyInfo.getTypedStrategy().getMetrikaCounters()).hasSameElementsAs(
                List.of(COUNTER_ID, COUNTER_ID2)
        );
        // test
        add(List.of(COUNTER_ID));

        steps.defaultManualStrategySteps().updateToActualStrategy(strategyInfo);

        assertThat(strategyInfo.getTypedStrategy().getMetrikaCounters())
                .hasSameElementsAs(List.of(COUNTER_ID, COUNTER_ID2));
    }

    @Test
    public void deleteAllCountersToStrategy_deleteAllCountersForStrategyWithTwoCampaigns() {
        // preparing
        TextCampaignInfo anotherCampaignInfo = createTextCampaign();

        steps.defaultManualStrategySteps().bindStrategyAndCampaign(strategyInfo, anotherCampaignInfo);

        add(List.of(campaignId), List.of(COUNTER_ID));
        add(List.of(anotherCampaignInfo.getCampaignId()), List.of(COUNTER_ID));

        steps.defaultManualStrategySteps().updateToActualStrategy(strategyInfo);

        assertThat(strategyInfo.getTypedStrategy().getMetrikaCounters())
                .hasSameElementsAs(List.of(COUNTER_ID));

        // test
        deleteAll();

        steps.defaultManualStrategySteps().updateToActualStrategy(strategyInfo);

        assertThat(strategyInfo.getTypedStrategy().getMetrikaCounters())
                .hasSameElementsAs(List.of());
    }

    @Test
    public void testStrategyCreatingWithoutCampaign() {
        DefaultManualStrategyInfo newStrategyInfo = steps.defaultManualStrategySteps().createDefaultStrategy();
        assertThat(newStrategyInfo.getTypedStrategy().getId()).isNotNull();
    }

    @Test
    public void testEmptyStrategyBinding() {
        DefaultManualStrategyInfo newStrategyInfo = steps.defaultManualStrategySteps().createDefaultStrategy();
        TextCampaignInfo newCampaignInfo = steps.textCampaignSteps().createDefaultCampaign();

        // сцепляем стратегию и кампанию
        steps.defaultManualStrategySteps().bindStrategyAndCampaign(newStrategyInfo, newCampaignInfo);

        // у стратегии обновились кампании
        assertThat(newStrategyInfo.getCampaignIds()).hasSameElementsAs(List.of(newCampaignInfo.getCampaignId()));
        // у кампании обновилась стратегия
        assertThat(newCampaignInfo.getTypedCampaign().getStrategyId()).isEqualTo(newStrategyInfo.getTypedStrategy().getId());
    }

    @Test
    public void testEmptyStrategyBindingAndUnBinding() {
        DefaultManualStrategyInfo newStrategyInfo = steps.defaultManualStrategySteps().createDefaultStrategy();
        TextCampaignInfo newCampaignInfo = steps.textCampaignSteps().createDefaultCampaign();

        // сцепляем стратегию и кампанию
        steps.defaultManualStrategySteps().bindStrategyAndCampaign(newStrategyInfo, newCampaignInfo);

        // у стратегии обновились кампании
        assertThat(newStrategyInfo.getCampaignIds()).hasSameElementsAs(List.of(newCampaignInfo.getCampaignId()));
        // у кампании обновилась стратегия
        assertThat(newCampaignInfo.getTypedCampaign().getStrategyId()).isEqualTo(newStrategyInfo.getTypedStrategy().getId());

        // разъединяем стратегию и кампанию
        steps.defaultManualStrategySteps().unbindStrategyAndCampaign(newStrategyInfo, newCampaignInfo);

        // стратегия больше не связана с этой кампанией
        assertThat(newStrategyInfo.getCampaignIds()).isEmpty();

        // кампания не связана со стратегией
        assertThat(newCampaignInfo.getTypedCampaign().getStrategyId()).isNotEqualTo(newStrategyInfo.getTypedStrategy().getId());

        var strategyForNewCampaignInfo =
                new DefaultManualStrategyInfo(
                        newCampaignInfo.getClientInfo(),
                        new DefaultManualStrategy().withId(newCampaignInfo.getTypedCampaign().getStrategyId())
                );

        // получаем стратегию, которая создалась для кампании при отвязке
        steps.defaultManualStrategySteps().updateToActualStrategy(strategyForNewCampaignInfo);

        // стратегия точно связана с кампанией, для которой она создалась
        assertThat(strategyForNewCampaignInfo.getCampaignIds()).hasSameElementsAs(List.of(newCampaignInfo.getCampaignId()));
    }

    private BiFunction<List<Long>,
            List<Long>,
            Result<UpdateCampMetrikaCountersRequest>> processUpdating(
            Function<CampMetrikaCountersService,
                    TriFunction<ClientId,
                            UpdateCampMetrikaCountersRequest,
                            Applicability,
                            Result<UpdateCampMetrikaCountersRequest>>> updating) {
        return (cids, counterIds) ->
                updating.apply(campMetrikaCountersService).apply(
                        clientId,
                        createRequest(cids, counterIds),
                        Applicability.FULL);
    }

    private Result<UpdateCampMetrikaCountersRequest> add(List<Long> cids, List<Long> counters) {
        return processUpdating(c -> c::addCampMetrikaCounters).apply(cids, counters);
    }

    private Result<UpdateCampMetrikaCountersRequest> add(List<Long> counters) {
        return add(List.of(campaignId), counters);
    }

    private Result<DeleteCampMetrikaCountersRequest> deleteAll() {
        return campMetrikaCountersService.deleteAllCampMetrikaCounters(
                clientId,
                new DeleteCampMetrikaCountersRequest()
                        .withCids(List.of(campaignId)),
                Applicability.FULL
        );
    }

    private UpdateCampMetrikaCountersRequest createRequest(
            List<Long> cids,
            List<Long> metrikaCounters
    ) {
        return new UpdateCampMetrikaCountersRequest()
                .withCids(cids)
                .withMetrikaCounters(metrikaCounters);
    }

    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }
}
