package ru.yandex.direct.core.entity.campaign.service;


import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.container.DeleteCampMetrikaCountersRequest;
import ru.yandex.direct.core.entity.campaign.container.UpdateCampMetrikaCountersRequest;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.strategy.model.StrategyWithMetrikaCounters;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbschema.ppc.enums.MetrikaCountersSource;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampMetrikaCountersServiceUpdateCountersForStrategiesTest {

    @Autowired
    private Steps steps;
    @Autowired
    private StrategyTypedRepository strategyTypedRepository;
    @Autowired
    private CampMetrikaCountersService campMetrikaCountersService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    private int shard;
    private long uid;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private Long campaignId;
    private TextCampaignInfo campaignInfo;

    private static final Long BUCKET_SIZE = 100L;

    private static final Long COUNTER_ID = randomIdFromBucket(0);
    private static final Long COUNTER_ID2 = randomIdFromBucket(1);
    private static final Long COUNTER_ID3 = randomIdFromBucket(2);
    private static final Long NONSPRAV_COUNTER_ID = randomIdFromBucket(3);
    private static final Long NONSPRAV_COUNTER_ID2 = randomIdFromBucket(4);


    @Before
    public void before() {
        clientInfo = steps.userSteps().createDefaultUser().getClientInfo();
        walletService.createWalletForNewClient(clientInfo.getClientId(), clientInfo.getUid());

        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        uid = clientInfo.getUid();

        steps.featureSteps().addClientFeature(clientId, FeatureName.TOGETHER_UPDATING_STRATEGY_AND_CAMPAIGN_METRIKA_COUNTERS, true);

        // создал компанию со стратегией
        campaignInfo = createTextCampaign();
        campaignId = campaignInfo.getCampaignId();

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

        checkCampaignCounters(campaignInfo, List.of(COUNTER_ID));

    }

    @Test
    public void addCountersToStrategy_addNothing() {
        // test
        add(List.of());

        checkCampaignCounters(campaignInfo, List.of());

    }

    @Test
    public void addCountersToStrategy_addExistingCounter() {
        // preparing
        add(List.of(COUNTER_ID, COUNTER_ID2));

        checkCampaignCounters(campaignInfo, List.of(COUNTER_ID, COUNTER_ID2));

        // test
        add(List.of(COUNTER_ID));

        checkCampaignCounters(campaignInfo, List.of(COUNTER_ID, COUNTER_ID2));
    }

    @Test
    public void addCountersToStrategy_addTogetherAddedAndNewCounter() {
        // preparing
        add(List.of(COUNTER_ID));

        checkCampaignCounters(campaignInfo, List.of(COUNTER_ID));

        // test
        add(List.of(COUNTER_ID, COUNTER_ID2));

        checkCampaignCounters(campaignInfo, List.of(COUNTER_ID, COUNTER_ID2));
    }

    @Test
    public void removeCountersToStrategy_removePartial() {
        // preparing
        add(List.of(COUNTER_ID, COUNTER_ID2));

        var firstRemove = remove(List.of(COUNTER_ID2));

        assertFalse("sprav counters can't be removed", firstRemove.isSuccessful());

        add(List.of(NONSPRAV_COUNTER_ID));

        // test
        var secondRemove = remove(List.of(NONSPRAV_COUNTER_ID));

        assertTrue("non sprav counters can be removed", secondRemove.isSuccessful());

        checkCampaignCounters(campaignInfo, List.of(COUNTER_ID, COUNTER_ID2));
    }

    @Test
    public void removeCountersToStrategy_removeAll() {
        // preparing
        var nonSpravCounters = List.of(NONSPRAV_COUNTER_ID, NONSPRAV_COUNTER_ID2);

        add(nonSpravCounters);

        // test
        remove(nonSpravCounters);

        checkCampaignCounters(campaignInfo, List.of());
    }

    @Test
    public void removeCountersToStrategy_removeAllPossible() {
        // preparing
        var spravCounters = List.of(COUNTER_ID, COUNTER_ID2);
        var nonSpravCounters = List.of(NONSPRAV_COUNTER_ID, NONSPRAV_COUNTER_ID2);

        add(
                Stream.of(
                        spravCounters,
                        nonSpravCounters)
                .flatMap(List::stream)
                .collect(Collectors.toList())
        );

        // test
        remove(nonSpravCounters);

        checkCampaignCounters(campaignInfo, spravCounters);
    }


    @Test
    public void removeCountersToStrategy_removeNonExistingCounter() {
        // preparing
        add(List.of(COUNTER_ID, COUNTER_ID2));
        // test
        remove(List.of(COUNTER_ID3));

        checkCampaignCounters(campaignInfo, List.of(COUNTER_ID, COUNTER_ID2));
    }

    @Test
    public void replaceCountersToStrategy_replaceNothing() {
        // preparing
        add(List.of(NONSPRAV_COUNTER_ID));

        checkCampaignCounters(campaignInfo, List.of(NONSPRAV_COUNTER_ID));

        // test
        Result<UpdateCampMetrikaCountersRequest> result = replace(
                List.of(NONSPRAV_COUNTER_ID));

        assertTrue(result.isSuccessful());

        checkCampaignCounters(campaignInfo, List.of(NONSPRAV_COUNTER_ID));
    }

    @Test
    public void replaceCountersToStrategy_replaceAll() {
        // preparing
        add(List.of(NONSPRAV_COUNTER_ID));

        checkCampaignCounters(campaignInfo, List.of(NONSPRAV_COUNTER_ID));

        // test
        Result<UpdateCampMetrikaCountersRequest> result = replace(
                List.of(NONSPRAV_COUNTER_ID2));

        assertTrue(result.isSuccessful());

        checkCampaignCounters(campaignInfo, List.of(NONSPRAV_COUNTER_ID2));
    }

    @Test
    public void replaceCountersToStrategy_replaceAllOnlyNonSprav() {
        // preparing
        add(List.of(COUNTER_ID, NONSPRAV_COUNTER_ID));

        checkCampaignCounters(campaignInfo, List.of(COUNTER_ID, NONSPRAV_COUNTER_ID));

        // test
        Result<UpdateCampMetrikaCountersRequest> result = replace(
                List.of(COUNTER_ID, NONSPRAV_COUNTER_ID2));

        assertTrue(result.isSuccessful());

        checkCampaignCounters(campaignInfo, List.of(COUNTER_ID, NONSPRAV_COUNTER_ID2));
    }


    @Test
    public void deleteAllCountersToStrategy_deleteAllCountersForStrategyWithOneCampaign() {
        // preparing
        add(List.of(COUNTER_ID, COUNTER_ID2));

        checkCampaignCounters(campaignInfo, List.of(COUNTER_ID, COUNTER_ID2));

        deleteAll();

        checkCampaignCounters(campaignInfo, List.of());
    }

    @Test
    public void deleteAllCountersToStrategy_deleteAllCountersForStrategyWithTwoCampaigns() {
        // preparing
        TextCampaignInfo anotherCampaign = createTextCampaign();

        Long strategyId = campaignRepository.getStrategyIdsByCampaignIds(
                shard,
                clientId,
                List.of(campaignId)
        ).get(campaignId);

        setStrategy(anotherCampaign, strategyId);

        add(List.of(campaignId), List.of(COUNTER_ID));
        add(List.of(anotherCampaign.getCampaignId()), List.of(COUNTER_ID));

        assertThat(getCountersForStrategy(strategyId))
                .containsAll(List.of(COUNTER_ID));

        // test
        deleteAll();

        assertThat(getCountersForStrategy(strategyId))
                .containsAll(List.of());
    }

    private void checkCampaignCounters(TextCampaignInfo campaignInfo, List<Long> counters) {
        assertThat(getStrategyCountersForCampaign(campaignInfo))
                .hasSameElementsAs(counters);
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

    private Result<UpdateCampMetrikaCountersRequest> remove(List<Long> cids, List<Long> counters) {
        return processUpdating(c -> c::removeCampMetrikaCounters).apply(cids, counters);
    }

    private Result<UpdateCampMetrikaCountersRequest> remove(List<Long> counters) {
        return remove(List.of(campaignId), counters);
    }

    private Result<UpdateCampMetrikaCountersRequest> replace(List<Long> cids, List<Long> counters) {
        return processUpdating(c -> c::replaceCampMetrikaCounters).apply(cids, counters);
    }

    private Result<UpdateCampMetrikaCountersRequest> replace(List<Long> counters) {
        return replace(List.of(campaignId), counters);
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

    private List<Long> getStrategyCountersForCampaign(TextCampaignInfo campaignInfo) {
        return getCountersForStrategy(campaignInfo.getTypedCampaign().getStrategyId());
    }

    private List<Long> getCountersForStrategy(Long sid) {
        var strategies =
                StreamEx.of(strategyTypedRepository.getTyped(shard, List.of(sid)))
                        .select(StrategyWithMetrikaCounters.class)
                        .toList();
        if (strategies.isEmpty()) return Collections.emptyList();
        return strategies.get(0).getMetrikaCounters();
    }

    private void setStrategy(TextCampaignInfo textCampaignInfo, Long sid) {
        dslContextProvider.ppc(textCampaignInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STRATEGY_ID, sid)
                .where(CAMPAIGNS.CID.eq(textCampaignInfo.getCampaignId()))
                .execute();
    }

    private static Long randomIdFromBucket(int bucket) {
        return bucket * BUCKET_SIZE + RandomNumberUtils.nextPositiveLong(BUCKET_SIZE);
    }

    @FunctionalInterface
    interface TriFunction<A,B,C,R> {
        R apply(A a, B b, C c);
    }
}
