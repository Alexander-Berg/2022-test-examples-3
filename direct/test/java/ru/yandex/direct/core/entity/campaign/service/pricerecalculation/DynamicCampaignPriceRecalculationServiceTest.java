package ru.yandex.direct.core.entity.campaign.service.pricerecalculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.log.service.LogPriceService;
import ru.yandex.direct.core.entity.bids.repository.BidRepository;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithDefaultPriceRecalculation;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaignWithPriceRecalculation;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.DynamicsSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbschema.ppc.enums.BidsDynamicStatusbssynced;
import ru.yandex.direct.dbschema.ppc.tables.records.BidsDynamicRecord;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.qatools.allure.annotations.Description;

import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_DYNAMIC;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверка пересчёта ставок при смене стратегии для dynamic кампаний")
public class DynamicCampaignPriceRecalculationServiceTest {
    private static final BigDecimal PRICE = BigDecimal.valueOf(1);
    private static final BigDecimal PRICE_CONTEXT = BigDecimal.valueOf(2);
    private static final long CONTEXT_PRICE_COEF = 100L;
    private static final Currency RUB = Currencies.getCurrency(CurrencyCode.RUB);

    @Autowired
    public Steps steps;
    @Autowired
    public CommonCampaignPriceRecalculationService commonCampaignPriceRecalculationService;
    @Autowired
    public BidRepository bidRepository;
    @Autowired
    public ShardHelper shardHelper;
    @Autowired
    public CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private DynamicsSteps dynamicsSteps;
    @Mock
    private LogPriceService logPriceService;

    private DynamicCampaignPriceRecalculationService dynamicCampaignPriceRecalculationService;
    private CampaignInfo campaignInfo;
    private Long campaignId;
    private int shard;
    private UidAndClientId uidAndClientId;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        dynamicCampaignPriceRecalculationService = new DynamicCampaignPriceRecalculationService(shardHelper,
                bidRepository, logPriceService, commonCampaignPriceRecalculationService);
        campaignInfo = steps.campaignSteps().createActiveDynamicCampaign();
        shard = campaignInfo.getShard();
        campaignId = campaignInfo.getCampaignId();
        uidAndClientId = UidAndClientId.of(campaignInfo.getUid(), campaignInfo.getClientId());
    }

    @Test
    public void getNewDynamicPrice() {
        DbStrategy strategy = new DbStrategy();
        strategy.setPlatform(CampaignsPlatform.BOTH);

        BigDecimal price = dynamicCampaignPriceRecalculationService.getNewDynamicPrice(strategy, RUB, PRICE,
                PRICE_CONTEXT);
        assertThat(price).isEqualTo(PRICE);
    }

    @Test
    public void getNewDynamicPrice_WithSearchStop() {
        DbStrategy strategy = new DbStrategy();
        strategy.setPlatform(CampaignsPlatform.CONTEXT);

        BigDecimal price = dynamicCampaignPriceRecalculationService.getNewDynamicPrice(strategy, RUB, PRICE,
                PRICE_CONTEXT);
        assertThat(price).isEqualTo(PRICE);
    }

    @Test
    public void getNewDynamicPrice_WhenPricesAreZero() {
        DbStrategy strategy = new DbStrategy();
        strategy.setPlatform(CampaignsPlatform.BOTH);

        BigDecimal price = dynamicCampaignPriceRecalculationService.getNewDynamicPrice(strategy, RUB, ZERO, ZERO);
        assertThat(price).isEqualTo(RUB.getDefaultPrice());
    }

    @Test
    public void getNewDynamicPrice_WhenPriceIsZero() {
        DbStrategy strategy = new DbStrategy();
        strategy.setPlatform(CampaignsPlatform.BOTH);

        BigDecimal price = dynamicCampaignPriceRecalculationService.getNewDynamicPrice(strategy, RUB, ZERO,
                PRICE_CONTEXT);
        assertThat(price).isEqualTo(PRICE_CONTEXT);
    }

    @Test
    public void getNewDynamicPriceContext_WhenNotDifferentPlaces() {
        DbStrategy strategy = new DbStrategy();
        strategy.setPlatform(CampaignsPlatform.BOTH);
        strategy.setStrategy(CampOptionsStrategy.AUTOBUDGET_AVG_CPC_PER_CAMP);

        BigDecimal priceContext =
                dynamicCampaignPriceRecalculationService.getNewDynamicPriceContext(strategy, RUB, PRICE, PRICE_CONTEXT,
                        CONTEXT_PRICE_COEF);
        assertThat(priceContext).isEqualTo(ZERO);
    }

    @Test
    public void getNewDynamicPriceContext_WhenNetStop() {
        DbStrategy strategy = new DbStrategy();
        strategy.setPlatform(CampaignsPlatform.SEARCH);
        strategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);

        BigDecimal priceContext =
                dynamicCampaignPriceRecalculationService.getNewDynamicPriceContext(strategy, RUB, PRICE, PRICE_CONTEXT,
                        CONTEXT_PRICE_COEF);
        assertThat(priceContext).isEqualTo(ZERO);
    }

    @Test
    public void getNewDynamicPriceContext_WhenPriceContextNotZero() {
        DbStrategy strategy = new DbStrategy();
        strategy.setPlatform(CampaignsPlatform.BOTH);
        strategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);

        BigDecimal priceContext =
                dynamicCampaignPriceRecalculationService.getNewDynamicPriceContext(strategy, RUB, ZERO, PRICE_CONTEXT,
                        CONTEXT_PRICE_COEF);
        assertThat(priceContext).isEqualTo(PRICE_CONTEXT);
    }

    @Test
    public void getNewDynamicPriceContext_WhenPricesAreZero() {
        DbStrategy strategy = new DbStrategy();
        strategy.setPlatform(CampaignsPlatform.BOTH);
        strategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);

        BigDecimal priceContext =
                dynamicCampaignPriceRecalculationService.getNewDynamicPriceContext(strategy, RUB, ZERO, ZERO,
                        CONTEXT_PRICE_COEF);
        assertThat(priceContext).isEqualTo(RUB.getDefaultPrice());
    }

    @Test
    public void getNewDynamicPriceContext_WhenPriceNotZero() {
        DbStrategy strategy = new DbStrategy();
        strategy.setPlatform(CampaignsPlatform.BOTH);
        strategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);

        BigDecimal expectPriceContext = (BigDecimal.valueOf(CONTEXT_PRICE_COEF).multiply(PRICE))
                .divide(BigDecimal.valueOf(100L), 2, RoundingMode.CEILING);

        BigDecimal priceContext =
                dynamicCampaignPriceRecalculationService.getNewDynamicPriceContext(strategy, RUB, PRICE, ZERO,
                        CONTEXT_PRICE_COEF);
        assertThat(priceContext).isEqualTo(expectPriceContext);
    }

    @Test
    public void resetBidsDynamicBsStatusAndPriority() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(campaignInfo);
        BidsDynamicRecord bidDynamic = dynamicsSteps.addDefaultBidsDynamic(adGroupInfo);

        changeBidsDynamicPriorityAndBsStatus(shard, bidDynamic.getDynId());

        DbStrategy strategy = new DbStrategy();
        strategy.setAutobudget(CampaignsAutobudget.YES);

        var changes = new ModelChanges<>(campaignId, DynamicCampaignWithPriceRecalculation.class)
                .process(strategy, DynamicCampaignWithPriceRecalculation.STRATEGY)
                .applyTo(new DynamicCampaign().withId(campaignId));

        dynamicCampaignPriceRecalculationService.resetBidsDynamicBsStatusAndPriority(singletonList(changes),
                uidAndClientId);

        List<BidsDynamicRecord> bidsDynamicRecords =
                dynamicsSteps.getBidsDynamicRecordsByCampaignIds(shard, singleton(campaignId));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(bidsDynamicRecords).hasSize(1);
            soft.assertThat(bidsDynamicRecords.get(0).getAutobudgetpriority()).isEqualTo(3);
            soft.assertThat(bidsDynamicRecords.get(0).getStatusbssynced()).isEqualTo(BidsDynamicStatusbssynced.No);
        });
    }

    @Test
    public void adjustBidsDynamicPrices() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(campaignInfo);
        dynamicsSteps.addDefaultBidsDynamic(adGroupInfo);

        DbStrategy strategy = new DbStrategy();
        strategy.withAutobudget(CampaignsAutobudget.NO)
                .withPlatform(CampaignsPlatform.BOTH)
                .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES);

        var changes = new ModelChanges<>(campaignId, DynamicCampaignWithPriceRecalculation.class)
                .process(strategy, CampaignWithDefaultPriceRecalculation.STRATEGY)
                .process(CurrencyCode.USD, CampaignWithDefaultPriceRecalculation.CURRENCY)
                .applyTo(new DynamicCampaign().withId(campaignId));

        dynamicCampaignPriceRecalculationService.adjustBidsDynamicPrices(singletonList(changes), uidAndClientId);

        BigDecimal expectPrice = CurrencyCode.USD.getCurrency().getDefaultPrice();
        BigDecimal expectPriceContext = CurrencyCode.USD.getCurrency().getDefaultPrice();

        List<BidsDynamicRecord> bidsDynamicRecords =
                dynamicsSteps.getBidsDynamicRecordsByCampaignIds(shard, singleton(campaignId));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(bidsDynamicRecords).hasSize(1);
            soft.assertThat(bidsDynamicRecords.get(0).getPrice())
                    .isEqualByComparingTo(expectPrice);
            soft.assertThat(bidsDynamicRecords.get(0).getPriceContext())
                    .isEqualByComparingTo(expectPriceContext);
        });

        verify(logPriceService).logPrice(anyList(), anyLong());
    }

    /**
     * Когда нет BidsDynamic для кампании -> код обрабатывается, новых записей не создается
     */
    @Test
    public void adjustBidsDynamicPrices_WithoutBidsDynamic() {
        steps.adGroupSteps().createActiveDynamicTextAdGroup(campaignInfo);

        var changes = new ModelChanges<>(campaignId, DynamicCampaignWithPriceRecalculation.class)
                .applyTo(new DynamicCampaign().withId(campaignId));

        dynamicCampaignPriceRecalculationService.adjustBidsDynamicPrices(singletonList(changes), uidAndClientId);

        List<BidsDynamicRecord> bidsDynamicRecords =
                dynamicsSteps.getBidsDynamicRecordsByCampaignIds(shard, singleton(campaignId));
        assertThat(bidsDynamicRecords).isEmpty();
        verify(logPriceService, never()).logPrice(anyList(), anyLong());
    }

    /**
     * Тестируем обработку для двух кампаний, при этом только для одной есть BidsDynamic
     * -> код обрабатывается, новых записей не создаетсяб
     */
    @Test
    public void adjustBidsDynamicPrices_WithBidsDynamicForDifferentCampaigns() {
        CampaignInfo differCampaignInfo = steps.campaignSteps().createActiveDynamicCampaign();
        AdGroupInfo differAdGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(differCampaignInfo);
        BidsDynamicRecord differBidsDynamic = dynamicsSteps.addDefaultBidsDynamic(differAdGroupInfo);
        Long differCampaignId = differAdGroupInfo.getCampaignId();

        DbStrategy strategy = new DbStrategy();
        strategy.withAutobudget(CampaignsAutobudget.NO)
                .withPlatform(CampaignsPlatform.BOTH)
                .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES);

        var changesCampaign = new ModelChanges<>(campaignId, DynamicCampaignWithPriceRecalculation.class)
                .applyTo(new DynamicCampaign().withId(campaignId));
        var changesDifferCampaign = new ModelChanges<>(differCampaignId, DynamicCampaignWithPriceRecalculation.class)
                .process(strategy, CampaignWithDefaultPriceRecalculation.STRATEGY)
                .process(CurrencyCode.USD, CampaignWithDefaultPriceRecalculation.CURRENCY)
                .applyTo(new DynamicCampaign().withId(differCampaignId));

        dynamicCampaignPriceRecalculationService.adjustBidsDynamicPrices(
                List.of(changesCampaign, changesDifferCampaign), uidAndClientId);

        List<BidsDynamicRecord> bidsDynamicRecords =
                dynamicsSteps.getBidsDynamicRecordsByCampaignIds(shard, List.of(campaignId, differCampaignId));
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(bidsDynamicRecords).hasSize(1);
            soft.assertThat(bidsDynamicRecords.get(0).getDynId()).isEqualTo(differBidsDynamic.getDynId());
        });

        verify(logPriceService).logPrice(anyList(), anyLong());
    }

    /**
     * Тестируем что обрабатываются только BidsDynamic от dynamic групп.
     * -> при обработке TextAdGroup ставки не изменяются
     */
    @Test
    public void adjustBidsDynamicPrices_WithBidsDynamicFromNotDynamicAdGroup() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        BidsDynamicRecord bidsDynamic = dynamicsSteps.addDefaultBidsDynamic(adGroupInfo);

        DbStrategy strategy = new DbStrategy();
        strategy.withAutobudget(CampaignsAutobudget.NO)
                .withPlatform(CampaignsPlatform.BOTH)
                .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES);

        var changesCampaign = new ModelChanges<>(campaignId, DynamicCampaignWithPriceRecalculation.class)
                .applyTo(new DynamicCampaign().withId(campaignId));

        dynamicCampaignPriceRecalculationService.adjustBidsDynamicPrices(singletonList(changesCampaign),
                uidAndClientId);

        BigDecimal expectPrice = bidsDynamic.getPrice();
        BigDecimal expectPriceContext = bidsDynamic.getPriceContext();

        List<BidsDynamicRecord> bidsDynamicRecords =
                dynamicsSteps.getBidsDynamicRecordsByCampaignIds(shard, List.of(campaignId));
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(bidsDynamicRecords).hasSize(1);
            soft.assertThat(bidsDynamicRecords.get(0).getPrice())
                    .isEqualByComparingTo(expectPrice);
            soft.assertThat(bidsDynamicRecords.get(0).getPriceContext())
                    .isEqualByComparingTo(expectPriceContext);
        });

        verify(logPriceService, never()).logPrice(anyList(), anyLong());
    }

    /**
     * Когда после пересчета новых ставок они оказываются теми же
     * -> ставки не меняются, лог не пишется
     */
    @Test
    public void adjustBidsDynamicPrices_TheSamePricesAfterCalculation() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(campaignInfo);
        BidsDynamicRecord bidsDynamicRecord = dynamicsSteps.addDefaultBidsDynamic(adGroupInfo);

        DbStrategy strategy = new DbStrategy();
        strategy.withAutobudget(CampaignsAutobudget.NO)
                .withPlatform(CampaignsPlatform.CONTEXT);

        var changes = new ModelChanges<>(campaignId, DynamicCampaignWithPriceRecalculation.class)
                .process(strategy, CampaignWithDefaultPriceRecalculation.STRATEGY)
                .process(CurrencyCode.USD, CampaignWithDefaultPriceRecalculation.CURRENCY)
                .applyTo(new DynamicCampaign().withId(campaignId));

        dynamicCampaignPriceRecalculationService.adjustBidsDynamicPrices(singletonList(changes), uidAndClientId);

        BigDecimal expectPrice = bidsDynamicRecord.getPrice();
        BigDecimal expectPriceContext = bidsDynamicRecord.getPriceContext();

        List<BidsDynamicRecord> bidsDynamicRecordResults =
                dynamicsSteps.getBidsDynamicRecordsByCampaignIds(shard, singleton(campaignId));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(bidsDynamicRecordResults).hasSize(1);
            soft.assertThat(bidsDynamicRecordResults.get(0).getPrice())
                    .isEqualByComparingTo(expectPrice);
            soft.assertThat(bidsDynamicRecordResults.get(0).getPriceContext())
                    .isEqualByComparingTo(expectPriceContext);
        });

        verify(logPriceService).logPrice(emptyList(), uidAndClientId.getUid());
    }

    private void changeBidsDynamicPriorityAndBsStatus(int shard, Long dynId) {
        dslContextProvider.ppc(shard)
                .update(BIDS_DYNAMIC)
                .set(BIDS_DYNAMIC.AUTOBUDGET_PRIORITY, (Long) null)
                .set(BIDS_DYNAMIC.STATUS_BS_SYNCED, BidsDynamicStatusbssynced.Yes)
                .where(BIDS_DYNAMIC.DYN_ID.eq(dynId))
                .execute();
    }
}
