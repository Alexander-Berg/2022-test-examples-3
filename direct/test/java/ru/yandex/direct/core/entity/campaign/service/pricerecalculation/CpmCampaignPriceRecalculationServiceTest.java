package ru.yandex.direct.core.entity.campaign.service.pricerecalculation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.bids.model.Bid;
import ru.yandex.direct.core.entity.bids.repository.BidRepository;
import ru.yandex.direct.core.entity.bids.service.BidBsStatisticFacade;
import ru.yandex.direct.core.entity.campaign.container.CampaignStrategyChangingSettings;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmCampaignWithPriceRecalculation;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.CampaignStrategyUtils.getCpmCampaignStrategyChangingSettings;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmStrategy;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.dbschema.ppc.Tables.AUTOBUDGET_FORECAST;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверка пересчёта ставок при смене стратегии для cpm кампаний")
public class CpmCampaignPriceRecalculationServiceTest {
    @Autowired
    private Steps steps;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private CpmCampaignPriceRecalculationService cpmCampaignPriceRecalculationService;
    @Autowired
    protected DslContextProvider dslContextProvider;
    @Autowired
    public CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    BidBsStatisticFacade bidBsStatisticFacadeMock;
    @Autowired
    private BidRepository bidRepository;
    @Autowired
    private KeywordRepository keywordRepository;
    @Autowired
    private RetargetingRepository retargetingRepository;

    private CampaignInfo manualCampaignInfo;
    private CampaignInfo autoBudgetCampaignInfo;
    private LocalDateTime now;

    @Before
    public void before() {
        now = LocalDateTime.now();
        manualCampaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign();
        prepareStrategy(manualCampaignInfo, defaultCpmStrategy());
        autoBudgetCampaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign();
        prepareStrategy(autoBudgetCampaignInfo, defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(now));
    }

    private AppliedChanges<CpmCampaignWithPriceRecalculation> prepareStrategy(CampaignInfo info, DbStrategy strategy) {

        CpmBannerCampaign newCpmBannerCampaign =
                (CpmBannerCampaign) campaignTypedRepository.getTypedCampaigns(info.getShard(),
                        List.of(info.getCampaignId())).get(0);

        ModelChanges<CpmCampaignWithPriceRecalculation> cpmBannerCampaignModelChanges =
                new ModelChanges<>(newCpmBannerCampaign.getId(),
                        CpmCampaignWithPriceRecalculation.class);
        cpmBannerCampaignModelChanges.process(LocalDate.now().plusDays(1), CpmBannerCampaign.START_DATE);
        cpmBannerCampaignModelChanges.process(LocalDate.now().plusDays(7), CpmBannerCampaign.END_DATE);
        cpmBannerCampaignModelChanges.process(strategy, CpmBannerCampaign.STRATEGY);
        AppliedChanges<CpmCampaignWithPriceRecalculation> cpmBannerCampaignAppliedChanges =
                cpmBannerCampaignModelChanges.applyTo(newCpmBannerCampaign);
        RestrictedCampaignsUpdateOperationContainer updateParameters = RestrictedCampaignsUpdateOperationContainer.create(
                info.getClientInfo().getShard(),
                info.getClientInfo().getUid(),
                info.getClientInfo().getClientId(),
                info.getClientInfo().getUid(),
                info.getClientInfo().getUid()
        );

        campaignModifyRepository.updateCampaigns(updateParameters,
                Collections.singletonList(cpmBannerCampaignAppliedChanges));

        return cpmBannerCampaignAppliedChanges;
    }

    /**
     * Переходим с ручной стратегии, для cpm есть только контекст
     */
    @Test
    public void changeManualDefaultCpmToAutoBudget() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(manualCampaignInfo);
        BigDecimal price = BigDecimal.valueOf(1);
        createKeyword(price, price, manualCampaignInfo);
        Retargeting retargeting = createRetargeting(price, manualCampaignInfo);

        AppliedChanges<CpmCampaignWithPriceRecalculation> cpmBannerCampaignAppliedChanges =
                prepareStrategy(manualCampaignInfo,
                        defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(now));
        CampaignStrategyChangingSettings settings =
                getCpmCampaignStrategyChangingSettings(List.of(cpmBannerCampaignAppliedChanges));

        cpmCampaignPriceRecalculationService.afterCampaignsStrategyChanged(List.of(cpmBannerCampaignAppliedChanges),
                settings, manualCampaignInfo.getUid(), UidClientIdShard.of(manualCampaignInfo.getUid(),
                        manualCampaignInfo.getClientId(), manualCampaignInfo.getShard()));

        Bid bid = getFirstBid(manualCampaignInfo);
        checkBidIfAutobudgetEnabled(bid);

        checkAutobudgetForecastDateWasResetIfAutobudgetEnabled(manualCampaignInfo);

        checkAutobudgetForecastWasInsertIfAutobudgetEnabled(manualCampaignInfo);

        checkAdgroupStatusAutobudgetShowIfAutobudgetEnabledIfAutobudgetEnabled(adGroupInfo);

        List<Bid> bidsBase = bidRepository.getRelevanceMatchByIds(manualCampaignInfo.getShard(), List.of(bid.getId()));
        MatcherAssert.assertThat("по добавленному keyword ничего не получаем из bids_base", bidsBase, empty());

        List<Retargeting> retargetings =
                retargetingRepository.getRetargetingsByIds(manualCampaignInfo.getShard(), List.of(retargeting.getId()),
                LimitOffset.maxLimited());
        assertThat(retargetings.get(0).getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat(retargetings.get(0).getAutobudgetPriority(), equalTo(3));
    }

    private void checkAdgroupStatusAutobudgetShowIfAutobudgetEnabledIfAutobudgetEnabled(AdGroupInfo adGroupInfo) {
        assertThat("Поле statusAutobudgetShow должно быть true после смены стратегии",
                adGroupRepository.getAdGroups(manualCampaignInfo.getShard(),
                        List.of(adGroupInfo.getAdGroupId())).get(0).getStatusAutobudgetShow(), is(true));
    }

    private void checkAutobudgetForecastWasInsertIfAutobudgetEnabled(CampaignInfo campaignInfo) {
        int count = dslContextProvider.ppc(campaignInfo.getShard())
                .selectCount()
                .from(AUTOBUDGET_FORECAST)
                .where(AUTOBUDGET_FORECAST.CID.eq(campaignInfo.getCampaignId()))
                .fetchOne()
                .value1();
        assertThat("autobudget_forecast должен был дополниться", count, greaterThan(0));
    }

    private void checkAutobudgetForecastDateWasResetIfAutobudgetEnabled(CampaignInfo campaignInfo) {
        LocalDateTime sqlTimestamp = dslContextProvider.ppc(campaignInfo.getShard())
                .select(CAMPAIGNS.AUTOBUDGET_FORECAST_DATE)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                .fetchOne()
                .value1();

        assertThat("campaigns.autobudget_forecast_date должен был сброситься", sqlTimestamp, nullValue());
    }

    private void checkBidIfAutobudgetEnabled(Bid bid) {
        assertThat("Поле priceContext должно подняться до минимально допустимой",
                bid.getPriceContext(), comparesEqualTo(CurrencyCode.RUB.getCurrency().getMinCpmPrice()));
        assertThat("Поле price должна быть 0",
                bid.getPrice(), comparesEqualTo(BigDecimal.ZERO));
        assertThat("Поле autobudgetPriority должно быть 3", bid.getAutobudgetPriority(), equalTo(3));
        assertThat(bid.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void changeAutobudgetToManual() {
        BigDecimal price = BigDecimal.valueOf(11);
        Keyword keyword = createKeyword(price, price, autoBudgetCampaignInfo);
        bidRepository.insertBidsToBidsManualPrices(autoBudgetCampaignInfo.getShard(),
                List.of(new Bid()
                        .withId(keyword.getId())
                        .withCampaignId(autoBudgetCampaignInfo.getCampaignId())
                        .withPrice(BigDecimal.valueOf(1L))
                        .withPriceContext(BigDecimal.valueOf(3001))));

        AppliedChanges<CpmCampaignWithPriceRecalculation> cpmBannerCampaignAppliedChanges =
                prepareStrategy(autoBudgetCampaignInfo, defaultCpmStrategy());
        CampaignStrategyChangingSettings settings =
                getCpmCampaignStrategyChangingSettings(List.of(cpmBannerCampaignAppliedChanges));

        cpmCampaignPriceRecalculationService.afterCampaignsStrategyChanged(List.of(cpmBannerCampaignAppliedChanges),
                settings, autoBudgetCampaignInfo.getUid(), UidClientIdShard.of(autoBudgetCampaignInfo.getUid(),
                        autoBudgetCampaignInfo.getClientId(), autoBudgetCampaignInfo.getShard()));

        int count = dslContextProvider.ppc(autoBudgetCampaignInfo.getShard())
                .selectCount()
                .from(AUTOBUDGET_FORECAST)
                .where(AUTOBUDGET_FORECAST.CID.eq(autoBudgetCampaignInfo.getCampaignId()))
                .fetchOne()
                .value1();
        assertThat("autobudget_forecast должен был очиститься", count, is(0));

        List<Bid> bids = bidRepository.getBidsByCampaignIds(autoBudgetCampaignInfo.getShard(),
                singletonList(autoBudgetCampaignInfo.getCampaignId()));
        assertThat(bids, hasSize(1));
        assertThat(bids.get(0).getPriceContext(), comparesEqualTo(CurrencyCode.RUB.getCurrency().getMaxCpmPrice()));
        List<Bid> remainedInBidManual =
                bidRepository.getBidsFromBidsManualPricesByCampaignIds(autoBudgetCampaignInfo.getShard(),
                        singletonList(autoBudgetCampaignInfo.getCampaignId()));
        assertThat(remainedInBidManual, hasSize(0));
    }

    @Test
    public void changeAutobudgetToManual_PriceContextUpdatesFromZeroToAvgPriceFor1000Shows() {
        BigDecimal price = BigDecimal.valueOf(11);
        Retargeting retargeting = createRetargeting(BigDecimal.ZERO, autoBudgetCampaignInfo);
        Keyword keyword = createKeyword(price, BigDecimal.ZERO, autoBudgetCampaignInfo);
        bidRepository.insertBidsToBidsManualPrices(autoBudgetCampaignInfo.getShard(),
                List.of(new Bid()
                        .withId(keyword.getId())
                        .withCampaignId(autoBudgetCampaignInfo.getCampaignId())
                        .withPrice(BigDecimal.valueOf(1L))
                        .withPriceContext(BigDecimal.ZERO)));

        DbStrategy strategy = defaultCpmStrategy();
        AppliedChanges<CpmCampaignWithPriceRecalculation> cpmBannerCampaignAppliedChanges =
                prepareStrategy(autoBudgetCampaignInfo, strategy);
        CampaignStrategyChangingSettings settings =
                getCpmCampaignStrategyChangingSettings(List.of(cpmBannerCampaignAppliedChanges));

        cpmCampaignPriceRecalculationService.afterCampaignsStrategyChanged(List.of(cpmBannerCampaignAppliedChanges),
                settings, autoBudgetCampaignInfo.getUid(), UidClientIdShard.of(autoBudgetCampaignInfo.getUid(),
                        autoBudgetCampaignInfo.getClientId(), autoBudgetCampaignInfo.getShard()));

        BigDecimal oldStrategyAvgCpm = new BigDecimal("100");
        List<Bid> bids = bidRepository.getBidsByCampaignIds(autoBudgetCampaignInfo.getShard(),
                singletonList(autoBudgetCampaignInfo.getCampaignId()));
        assertThat(bids, hasSize(1));
        assertThat(bids.get(0).getPriceContext(), comparesEqualTo(oldStrategyAvgCpm));
        assertThat(bids.get(0).getStatusBsSynced(), equalTo(StatusBsSynced.NO));


        List<Retargeting> retargetings =
                retargetingRepository.getRetargetingsByIds(autoBudgetCampaignInfo.getShard(),
                        List.of(retargeting.getId()),
                        LimitOffset.maxLimited());
        assertThat(retargetings, hasSize(1));
        assertThat(retargetings.get(0).getPriceContext(), comparesEqualTo(oldStrategyAvgCpm));
        assertThat(retargetings.get(0).getStatusBsSynced(), equalTo(StatusBsSynced.NO));

    }

    private Retargeting createRetargeting(BigDecimal priceContext, CampaignInfo campaignInfo) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo);

        Retargeting retargeting = new Retargeting()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(campaignInfo.getCampaignId())
                .withRetargetingConditionId(2489948L)
                .withPriceContext(priceContext)
                .withLastChangeTime(LocalDateTime.now().withNano(0))
                .withStatusBsSynced(StatusBsSynced.YES)
                .withAutobudgetPriority(5)
                .withIsSuspended(false);
        steps.retargetingSteps().createRetargeting(retargeting, adGroupInfo);
        return retargeting;
    }

    private Keyword createKeyword(BigDecimal price, BigDecimal priceContext, CampaignInfo campaignInfo) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo);
        Keyword keyword = defaultKeyword()
                .withAutobudgetPriority(null)
                .withStatusBsSynced(StatusBsSynced.YES)
                .withPrice(price)
                .withPriceContext(priceContext)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(campaignInfo.getCampaignId());
        keywordRepository.addKeywords(dslContextProvider.ppc(campaignInfo.getShard()).configuration(),
                singletonList(keyword));
        return keyword;
    }

    private Bid getFirstBid(CampaignInfo campaignInfo) {
        List<Bid> bids = bidRepository.getBidsByCampaignIds(campaignInfo.getShard(),
                singletonList(campaignInfo.getCampaignId()));
        return bids.get(0);
    }
}
