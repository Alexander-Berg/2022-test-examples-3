package ru.yandex.direct.core.entity.retargeting.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionFixedAutoPrices;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsAutobudget;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingUpdateOperationTest {

    private static final CompareStrategy STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath("lastChangeTime")).useMatcher(approximatelyNow())
            .forFields(newPath("priceContext")).useDiffer(new BigDecimalDiffer());

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private RetargetingRepository retargetingRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private RetargetingService retargetingService;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private int shard;
    private RetargetingInfo defaultRetargetingInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        shard = clientInfo.getShard();

        defaultRetargetingInfo = steps.retargetingSteps().createDefaultRetargetingInActiveTextAdGroup(clientInfo);
    }

    @Test
    public void prepareAndApply_SetSuspendedAndUpdatePriceContext_SuccessfullUpdate() {
        ModelChanges<Retargeting> mc = suspendedModelChange(defaultRetargetingInfo.getRetargetingId(), true)
                .process(BigDecimal.valueOf(100), Retargeting.PRICE_CONTEXT);

        MassResult<Long> result = createUpdateOperation(singletonList(mc), clientInfo).prepareAndApply();
        assumeThat(result, isFullySuccessful());

        Retargeting savedRetargeting = getModels(result, shard).get(0);

        Retargeting expected = defaultRetargetingInfo.getRetargeting()
                .withPriceContext(BigDecimal.valueOf(100))
                .withIsSuspended(true);

        assertThat(savedRetargeting, beanDiffer(expected).useCompareStrategy(STRATEGY));

        AdGroup updatedAdGroup =
                adGroupRepository.getAdGroups(shard, singletonList(savedRetargeting.getAdGroupId())).get(0);
        assertThat(updatedAdGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void prepareAndApply_SetPriceContextOnly_AdGroupStatusBsSyncedNotChanged() {
        ModelChanges<Retargeting> mc = ModelChanges.build(defaultRetargetingInfo.getRetargetingId(),
                Retargeting.class, Retargeting.PRICE_CONTEXT, BigDecimal.TEN);

        MassResult<Long> result = createUpdateOperation(singletonList(mc), clientInfo).prepareAndApply();
        assumeThat(result, isFullySuccessful());

        AdGroup updatedAdGroup =
                adGroupRepository.getAdGroups(shard, singletonList(defaultRetargetingInfo.getAdGroupId())).get(0);
        assertThat(updatedAdGroup.getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    public void prepareAndApply_Suspend_AdGroupStatusBsSyncedNo() {
        Retargeting retargeting = defaultRetargetingInfo.getRetargeting();

        ModelChanges<Retargeting> mc = retargetingModelChanges(retargeting.getId())
                .process(true, Retargeting.IS_SUSPENDED);

        MassResult<Long> result = createUpdateOperation(singletonList(mc), clientInfo).prepareAndApply();
        assumeThat(result.isSuccessful(), is(true));

        AdGroup updatedAdGroup =
                adGroupRepository.getAdGroups(shard, singletonList(defaultRetargetingInfo.getAdGroupId())).get(0);
        assertThat(updatedAdGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void prepareAndApply_ChangePriceContext_HasActualChanges() {
        Retargeting retargeting = defaultRetargetingInfo.getRetargeting();

        ModelChanges<Retargeting> mc = retargetingModelChanges(retargeting.getId())
                .process(retargeting.getPriceContext().add(BigDecimal.ONE), Retargeting.PRICE_CONTEXT);

        MassResult<Long> result = createUpdateOperation(singletonList(mc), clientInfo).prepareAndApply();
        assumeThat(result.isSuccessful(), is(true));

        assertThat(result.getSuccessfulCount(), is(1));
    }

    /**
     * Проверка, что на атобюджетной стратегии, когда price_context == 0,
     * можно успешно сменить приоритет автобюджета, и при этом валидация
     * не будет ругаться на price_context == 0
     */
    @Test
    public void prepareAndApply_DontChangePriceContextOnAutoStrategy_WorksFine() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        Retargeting retargeting = defaultRetargeting()
                .withAutobudgetPriority(3)
                .withPriceContext(BigDecimal.valueOf(0));
        makeCampaignAutobudget(adGroupInfo.getCampaignInfo());

        steps.retargetingSteps().createRetargeting(retargeting, adGroupInfo);

        ModelChanges<Retargeting> mc = retargetingModelChanges(retargeting.getId())
                .process(1, Retargeting.AUTOBUDGET_PRIORITY);

        MassResult<Long> result = createUpdateOperation(singletonList(mc), clientInfo).prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void prepareAndApply_SetSamePriceContext_NoActualChanges() {
        Retargeting retargeting = defaultRetargetingInfo.getRetargeting();

        ModelChanges<Retargeting> mc = retargetingModelChanges(retargeting.getId())
                .process(retargeting.getPriceContext(), Retargeting.PRICE_CONTEXT);

        MassResult<Long> result = createUpdateOperation(singletonList(mc), clientInfo).prepareAndApply();
        assumeThat(result.isSuccessful(), is(true));

        assertThat(result.getSuccessfulCount(), is(0));
    }

    @Test
    public void prepareAndApply_SetPriceContextAndPassFixedPrice_PriceFromChangesSet() {
        Retargeting retargeting = defaultRetargetingInfo.getRetargeting();

        BigDecimal newPrice = retargeting.getPriceContext().add(BigDecimal.ONE);
        ModelChanges<Retargeting> mc = retargetingModelChanges(retargeting.getId())
                .process(newPrice, Retargeting.PRICE_CONTEXT);

        BigDecimal fixedPrice = retargeting.getPriceContext().add(BigDecimal.TEN);
        RetargetingUpdateOperation retargetingUpdateOperation =
                retargetingService.createUpdateOperation(Applicability.FULL, singletonList(mc),
                        true, ShowConditionFixedAutoPrices.ofGlobalFixedPrice(fixedPrice), false,
                        clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid());

        MassResult<Long> result = retargetingUpdateOperation.prepareAndApply();

        List<Retargeting> retargetings = retargetingRepository
                .getRetargetingsByIds(shard, singletonList(result.get(0).getResult()), maxLimited());
        assertThat(retargetings.get(0).getPriceContext().longValue(), is(newPrice.longValue()));
    }

    private RetargetingUpdateOperation createUpdateOperation(List<ModelChanges<Retargeting>> modelChanges,
                                                             ClientInfo clientInfo) {
        long clientUid = rbacService.getChiefByClientId(clientInfo.getClientId());
        return retargetingService.createUpdateOperation(Applicability.FULL, modelChanges,
                clientInfo.getUid(), clientInfo.getClientId(), clientUid);
    }

    private List<Retargeting> getModels(MassResult<Long> massResult, int shard) {
        List<Long> ids = mapList(massResult.getResult(), Result::getResult);
        return retargetingRepository.getRetargetingsByIds(shard, ids, maxLimited());
    }

    private ModelChanges<Retargeting> suspendedModelChange(Long retargetingId, boolean isSuspended) {
        return retargetingModelChanges(retargetingId).process(isSuspended, Retargeting.IS_SUSPENDED);
    }

    private static ModelChanges<Retargeting> retargetingModelChanges(long id) {
        return new ModelChanges<>(id, Retargeting.class);
    }


    private void makeCampaignAutobudget(CampaignInfo campaignInfo) {
        StrategyData strategyData = new StrategyData()
                .withSum(new BigDecimal("100000"))
                .withLimitClicks(1000L)
                .withVersion(1L);
        dslContextProvider.ppc(campaignInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.AUTOBUDGET, CampaignsAutobudget.Yes)
                .set(CAMPAIGNS.STRATEGY_NAME, CampaignsStrategyName.autobudget_avg_click)
                .set(CAMPAIGNS.STRATEGY_DATA, toJson(strategyData))
                .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                .execute();
    }
}
