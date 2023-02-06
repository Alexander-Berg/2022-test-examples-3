package ru.yandex.direct.core.entity.campaign.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.bids.model.Bid;
import ru.yandex.direct.core.entity.bids.repository.BidRepository;
import ru.yandex.direct.core.entity.bids.service.BidBsStatisticFacade;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestCampaigns.contextAverageClickStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageClickStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualBothDifferentStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualBothStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualSearchStrategy;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.dbschema.ppc.Tables.AUTOBUDGET_FORECAST;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверка пересчёта ставок при смене стратегии")
public class CampaignStrategyServicePriceChangeTest {
    @Autowired
    public Steps steps;
    @Autowired
    CampaignRepository campaignRepository;
    @Autowired
    private CampaignStrategyService campaignStrategyService;
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

    private CampaignInfo manualCampaignInfo;
    private CampaignInfo autoCampaignInfo;

    @Before
    public void before() {
        manualCampaignInfo = steps.campaignSteps().createActiveTextCampaign();
        prepareStrategy(manualCampaignInfo, manualSearchStrategy());
        autoCampaignInfo =
                steps.campaignSteps().createActiveTextCampaignAutoStrategy(manualCampaignInfo.getClientInfo());
        prepareStrategy(autoCampaignInfo, defaultAverageClickStrategy());
    }

    private void prepareStrategy(CampaignInfo info, DbStrategy strategy) {
        TextCampaign newTextCampaign = new TextCampaign().withId(info.getCampaignId()).withName("name");
        ModelChanges<TextCampaign> textCampaignModelChanges = ModelChanges.build(newTextCampaign, TextCampaign.NAME,
                newTextCampaign.getName());
        textCampaignModelChanges.process(LocalDate.now().plusDays(1), TextCampaign.START_DATE);
        textCampaignModelChanges.process(LocalDate.now().plusDays(7), TextCampaign.END_DATE);
        textCampaignModelChanges.process(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru", TextCampaign.EMAIL);
        AppliedChanges<TextCampaign> textCampaignAppliedChanges = textCampaignModelChanges.applyTo(newTextCampaign);
        RestrictedCampaignsUpdateOperationContainer updateParameters = RestrictedCampaignsUpdateOperationContainer.create(
                info.getClientInfo().getShard(),
                info.getClientInfo().getUid(),
                info.getClientInfo().getClientId(),
                info.getClientInfo().getUid(),
                info.getClientInfo().getUid()
        );

        campaignModifyRepository.updateCampaigns(updateParameters,
                Collections.singletonList(textCampaignAppliedChanges));
        campaignStrategyService.updateTextCampaignStrategy(info.getCampaignId(), strategy, info.getUid(),
                UidAndClientId.of(info.getUid(), info.getClientId()), false);
    }

    @Test
    /**
     * Переходим с ручной стратегии с показами на поиске
     * к ручной стратегии с показами на всех площадках (не раздельное управление)
     */
    public void changeManualDefaultSearchToManualDefaultBoth() {
        BigDecimal price = BigDecimal.valueOf(11);
        // случай DIRECT-109367
//        Создаю кампанию с ручной стратегией на поиске
        prepareStrategy(manualCampaignInfo, manualSearchStrategy());
//        Создаю баннеры с фразой со ставкой 11 руб на поиске и 0 ставкой в сети
        createKeyword(price, BigDecimal.ZERO, manualCampaignInfo);
        assertThat(getFirstBid(manualCampaignInfo).getPriceContext(), comparesEqualTo(BigDecimal.ZERO));

//        Меняю стратегию на ручную с показами на всех площадках (не раздельное управление)
        campaignStrategyService.updateTextCampaignStrategy(manualCampaignInfo.getCampaignId(),
                manualBothStrategy(), manualCampaignInfo.getUid(),
                UidAndClientId.of(manualCampaignInfo.getUid(), manualCampaignInfo.getClientId()), false);

        Bid bid = getFirstBid(manualCampaignInfo);
        assertThat("Поле price должно остаться установленная цена",
                bid.getPrice(), comparesEqualTo(price));
        assertThat("Поле price_context должна быть минимальная ставка",
                bid.getPriceContext(), comparesEqualTo(Currencies.getCurrency(CurrencyCode.RUB).getMinPrice()));
        assertThat(bid.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void changeManualToAutobudget_CampaignForecastDateIsDropped() {
        //если переходим на автобюджет с неавтобюджетной стратегии
        //Запланировать перерасчёт прогноза
        dslContextProvider.ppc(manualCampaignInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.AUTOBUDGET_FORECAST_DATE, LocalDateTime.now().minusDays(5))
                .where(CAMPAIGNS.CID.eq(manualCampaignInfo.getCampaignId()))
                .execute();

        campaignStrategyService.updateTextCampaignStrategy(manualCampaignInfo.getCampaignId(),
                defaultAverageClickStrategy(), manualCampaignInfo.getUid(),
                UidAndClientId.of(manualCampaignInfo.getUid(), manualCampaignInfo.getClientId()), false);

        LocalDateTime sqlTimestamp = dslContextProvider.ppc(manualCampaignInfo.getShard())
                .select(CAMPAIGNS.AUTOBUDGET_FORECAST_DATE)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(manualCampaignInfo.getCampaignId()))
                .fetchOne()
                .value1();
        assertThat("campaigns.autobudget_forecast_date должен был сброситься", sqlTimestamp, nullValue());
    }

    @Test
    public void changeAutobudgetToAutobudget_CampaignForecastDateIsDropped() {
        //если переходим на автобюджет с неавтобюджетной стратегии
        //Запланировать перерасчёт прогноза
        dslContextProvider.ppc(autoCampaignInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.AUTOBUDGET_FORECAST_DATE, LocalDateTime.now().minusDays(5))
                .where(CAMPAIGNS.CID.eq(autoCampaignInfo.getCampaignId()))
                .execute();

        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                defaultAutobudgetStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        LocalDateTime sqlTimestamp = dslContextProvider.ppc(autoCampaignInfo.getShard())
                .select(CAMPAIGNS.AUTOBUDGET_FORECAST_DATE)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(autoCampaignInfo.getCampaignId()))
                .fetchOne()
                .value1();
        assertThat("campaigns.autobudget_forecast_date должен был сброситься", sqlTimestamp, nullValue());
    }

    @Test
    public void changeAutobudgetToManual_CampaignForecastDateIsNotNull() {
        //если переходим на ручную с автобюджет стратегии
        //перерасчёт прогноза не должен измениться
        dslContextProvider.ppc(autoCampaignInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.AUTOBUDGET_FORECAST_DATE, LocalDateTime.now().minusDays(5))
                .where(CAMPAIGNS.CID.eq(autoCampaignInfo.getCampaignId()))
                .execute();

        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                manualSearchStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        LocalDateTime sqlTimestamp = dslContextProvider.ppc(autoCampaignInfo.getShard())
                .select(CAMPAIGNS.AUTOBUDGET_FORECAST_DATE)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(autoCampaignInfo.getCampaignId()))
                .fetchOne()
                .value1();
        assertThat("campaigns.autobudget_forecast_date не должен был сброситься", sqlTimestamp, notNullValue());
    }

    @Test
    public void changeManualToAutobudget_insertAutobudgetForecast() {
        //если переходим на автобюджет с неавтобюджетной стратегии
        //Запланировать перерасчёт прогноза
        campaignStrategyService.updateTextCampaignStrategy(manualCampaignInfo.getCampaignId(),
                defaultAverageClickStrategy(), manualCampaignInfo.getUid(),
                UidAndClientId.of(manualCampaignInfo.getUid(), manualCampaignInfo.getClientId()), false);

        int count = dslContextProvider.ppc(manualCampaignInfo.getShard())
                .selectCount()
                .from(AUTOBUDGET_FORECAST)
                .where(AUTOBUDGET_FORECAST.CID.eq(manualCampaignInfo.getCampaignId()))
                .fetchOne()
                .value1();
        assertThat("autobudget_forecast должен был дополниться", count, greaterThan(0));
    }

    @Test
    public void changeAutobudgetToManual_cleanAutobudgetForecast() {
        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                manualSearchStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        int count = dslContextProvider.ppc(autoCampaignInfo.getShard())
                .selectCount()
                .from(AUTOBUDGET_FORECAST)
                .where(AUTOBUDGET_FORECAST.CID.eq(autoCampaignInfo.getCampaignId()))
                .fetchOne()
                .value1();
        assertThat("autobudget_forecast должен был очиститься", count, is(0));
    }

    @Test
    public void changeAutobudgetToManual_restoreManualBids() {
        Keyword keywordInfo1 = createKeyword(BigDecimal.TEN, BigDecimal.TEN, autoCampaignInfo);
        bidRepository.insertBidsToBidsManualPrices(autoCampaignInfo.getShard(),
                asList(new Bid()
                        .withId(keywordInfo1.getId())
                        .withCampaignId(autoCampaignInfo.getCampaignId())
                        .withPrice(BigDecimal.valueOf(1L))
                        .withPriceContext(BigDecimal.valueOf(2L))));

        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                manualSearchStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        List<Bid> bids = bidRepository.getBidsByCampaignIds(autoCampaignInfo.getShard(),
                singletonList(autoCampaignInfo.getCampaignId()));
        MatcherAssert.assertThat(bids, hasSize(1));
        List<Bid> remainedInBidManual =
                bidRepository.getBidsFromBidsManualPricesByCampaignIds(autoCampaignInfo.getShard(),
                        singletonList(autoCampaignInfo.getCampaignId()));
        MatcherAssert.assertThat(remainedInBidManual, hasSize(0));
    }

    @Test
    public void changeManualToAutobudget_updateStatusAutoBudgetShowSetTrue() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        prepareStrategy(campaignInfo, manualSearchStrategy());
        AdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId())
                .withStatusAutobudgetShow(false);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(adGroup, campaignInfo);
        List<Long> adGroupIds = singletonList(adGroupInfo.getAdGroupId());
        assertThat("Поле statusAutobudgetShow по умолчанию было false",
                adGroupRepository.getAdGroups(campaignInfo.getShard(),
                        adGroupIds).get(0).getStatusAutobudgetShow(), is(false));

        campaignStrategyService.updateTextCampaignStrategy(campaignInfo.getCampaignId(),
                defaultAverageClickStrategy(), campaignInfo.getUid(),
                UidAndClientId.of(campaignInfo.getUid(), campaignInfo.getClientId()), false);

        assertThat("Поле statusAutobudgetShow должно быть true после смены стратегии",
                adGroupRepository.getAdGroups(campaignInfo.getShard(),
                        adGroupIds).get(0).getStatusAutobudgetShow(), is(true));
    }

    @Test
    public void changeManualToAutobudget_updateBids() {
        BigDecimal maxPrice = CurrencyCode.RUB.getCurrency().getMaxPrice();
        createKeyword(BigDecimal.TEN, maxPrice.multiply(BigDecimal.valueOf(2.7)), manualCampaignInfo);

        campaignStrategyService.updateTextCampaignStrategy(manualCampaignInfo.getCampaignId(),
                defaultAverageClickStrategy(), manualCampaignInfo.getUid(),
                UidAndClientId.of(manualCampaignInfo.getUid(), manualCampaignInfo.getClientId()), false);

        List<Bid> bids = bidRepository.getBidsByCampaignIds(manualCampaignInfo.getShard(),
                singletonList(manualCampaignInfo.getCampaignId()));
        MatcherAssert.assertThat(bids, hasSize(1));
        List<Bid> bidsBase = bidRepository.getRelevanceMatchByIds(manualCampaignInfo.getShard(),
                mapList(bids, Bid::getId));
        MatcherAssert.assertThat("по добавленному keyword ничего не получаем из bids_base", bidsBase, empty());
        Bid bid = bids.get(0);

        assertThat("Поле price не должно измениться", bid.getPrice(), comparesEqualTo(BigDecimal.TEN));
        assertThat("Поле price_context должно измениться в bids", bid.getPriceContext(), comparesEqualTo(maxPrice));
        assertThat("Поле autobudgetPriority должно измениться в bids", bid.getAutobudgetPriority(), is(3));
        assertThat(bid.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    /**Переход на поисковую стратегию со статегии "независимое управление". Сбрасываются цены на сеть*/
    public void changeAutobudgetToManual_resetPriceContext() {
        createKeyword(BigDecimal.TEN, BigDecimal.TEN, autoCampaignInfo);

        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                manualSearchStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        List<Bid> bids = bidRepository.getBidsByCampaignIds(autoCampaignInfo.getShard(),
                singletonList(autoCampaignInfo.getCampaignId()));
        MatcherAssert.assertThat(bids, hasSize(1));
        List<Bid> bidsBase = bidRepository.getRelevanceMatchByIds(autoCampaignInfo.getShard(),
                mapList(bids, Bid::getId));
        MatcherAssert.assertThat("по добавленному keyword ничего не получаем из bids_base", bidsBase, empty());

        assertThat("Поле price_context должно сброситься в bids",
                bids.get(0).getPriceContext(), comparesEqualTo(BigDecimal.ZERO));
    }

    private Keyword createKeyword(BigDecimal price, BigDecimal priceContext, CampaignInfo campaignInfo) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
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

    private void createRelevanceMatch(CampaignInfo campaignInfo, BigDecimal price, BigDecimal priceContext) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        steps.relevanceMatchSteps().addRelevanceMatchToAdGroup(adGroupInfo, price, priceContext);
    }

    @Test
    /**если кампания создаётся с отключенным поиском, то в ней нет цен на поиске. выставляем их
     * с автобюджета переходим на ручное обе платформы. Флаг Раздельно управлять ставками на поиске и в сетях
     * если на поиске нет ставки, а в сетях она есть, копируем ставку с сети*/
    public void changeAutobudgetToManual_copyContext() {
        createKeyword(BigDecimal.ZERO, BigDecimal.TEN, autoCampaignInfo);

        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                defaultStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        assertThat(bidRepository.getBidsByCampaignIds(autoCampaignInfo.getShard(),
                singletonList(autoCampaignInfo.getCampaignId())).get(0).getPrice(),
                comparesEqualTo(BigDecimal.TEN));
    }

    @Test
    /**если кампания создаётся с отключенным поиском, то в ней нет цен на поиске. выставляем их
     * с автобюджета переходим на ручное обе платформы. Флаг Раздельно управлять ставками на поиске и в сетях
     * в сети тоже нет, то Прогноз первого места в Гарантии + 30%*/
    public void changeAutobudgetToManual_priceByPlace() {
        createKeyword(BigDecimal.ZERO, BigDecimal.ZERO, autoCampaignInfo);
        //замокать BsAuctionService
        Answer<Object> stubAnswer = invocation -> {
            List<Bid> bids = invocation.getArgument(1);
            return StreamEx.of(bids)
                    .mapToEntry(Bid::getId,
                            b -> Money.valueOf(40.0, CurrencyCode.RUB))
                    .toMap();
        };
        when(bidBsStatisticFacadeMock.bidBsStatisticFirstPosition(any(), anyList()))
                .thenAnswer(stubAnswer);

        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                defaultStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        assertThat(bidRepository.getBidsByCampaignIds(autoCampaignInfo.getShard(),
                singletonList(autoCampaignInfo.getCampaignId())).get(0).getPrice(),
                comparesEqualTo(BigDecimal.valueOf(52.0)));
        assertThat(bidRepository.getBidsByCampaignIds(autoCampaignInfo.getShard(),
                singletonList(autoCampaignInfo.getCampaignId())).get(0).getStatusBsSynced(),
                is(StatusBsSynced.NO));
    }

    @Test
    /**если кампания создаётся с отключенным поиском, то в ней нет цен на поиске. выставляем их
     * с автобюджета переходим на ручное обе платформы. Флаг Раздельно управлять ставками на поиске и в сетях
     * прогноза нет, выставляется ставка по умолчанию*/
    public void changeAutobudgetToManual_defaultPrice() {
        createKeyword(BigDecimal.ZERO, BigDecimal.ZERO, autoCampaignInfo);
        when(bidBsStatisticFacadeMock.bidBsStatisticFirstPosition(eq(autoCampaignInfo.getClientId()), anyList()))
                .thenReturn(emptyMap());

        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                defaultStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        assertThat(bidRepository.getBidsByCampaignIds(autoCampaignInfo.getShard(),
                singletonList(autoCampaignInfo.getCampaignId())).get(0).getPrice(),
                comparesEqualTo(CurrencyCode.RUB.getCurrency().getDefaultPrice()));
        assertThat(bidRepository.getBidsByCampaignIds(autoCampaignInfo.getShard(),
                singletonList(autoCampaignInfo.getCampaignId())).get(0).getStatusBsSynced(),
                is(StatusBsSynced.NO));
    }

    @Test
    public void changeAutobudgetToManual_setContextPriceCoef() {
        createKeyword(BigDecimal.ZERO, BigDecimal.ZERO, autoCampaignInfo);

        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                defaultStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        assertThat(bidRepository.getBidsByCampaignIds(autoCampaignInfo.getShard(),
                singletonList(autoCampaignInfo.getCampaignId())).get(0).getPriceContext(),
                comparesEqualTo(CurrencyCode.RUB.getCurrency().getDefaultPrice()));
        assertThat(bidRepository.getBidsByCampaignIds(autoCampaignInfo.getShard(),
                singletonList(autoCampaignInfo.getCampaignId())).get(0).getStatusBsSynced(),
                is(StatusBsSynced.NO));
    }

    @Test
    public void changeManualToManual_setContextPriceCoef() {
        createKeyword(BigDecimal.ZERO, BigDecimal.ZERO, manualCampaignInfo);

        campaignStrategyService.updateTextCampaignStrategy(manualCampaignInfo.getCampaignId(),
                defaultStrategy(), manualCampaignInfo.getUid(),
                UidAndClientId.of(manualCampaignInfo.getUid(), manualCampaignInfo.getClientId()), false);

        assertThat(bidRepository.getBidsByCampaignIds(manualCampaignInfo.getShard(),
                singletonList(manualCampaignInfo.getCampaignId())).get(0).getPriceContext(),
                comparesEqualTo(CurrencyCode.RUB.getCurrency().getDefaultPrice()));
        assertThat(bidRepository.getBidsByCampaignIds(manualCampaignInfo.getShard(),
                singletonList(manualCampaignInfo.getCampaignId())).get(0).getStatusBsSynced(),
                is(StatusBsSynced.NO));
    }

    /**
     * Для ретаргетинга и автотаргетинга: поправим нулевые ставки при переходе с автобюджета на ручное управление
     */
    @Test
    public void changeAutobudgetToManualRelevance_NormalPriceNotUpdated() {
        createRelevanceMatch(autoCampaignInfo, BigDecimal.TEN, BigDecimal.ZERO);

        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                defaultStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        assertThat(bidRepository.getBidsWithRelevanceMatchByCampaignIds(autoCampaignInfo.getShard(),
                singletonList(autoCampaignInfo.getCampaignId())).get(0).getPrice(),
                comparesEqualTo(BigDecimal.TEN));
    }

    /**
     * Для ретаргетинга и автотаргетинга: поправим нулевые ставки при переходе с автобюджета на ручное управление
     */
    @Test
    public void changeAutobudgetToManualRelevance_SmallPriceUpdatedToDefaultPrice() {
        createRelevanceMatch(autoCampaignInfo, BigDecimal.ZERO, BigDecimal.ZERO);

        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                defaultStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        assertThat(bidRepository.getBidsWithRelevanceMatchByCampaignIds(autoCampaignInfo.getShard(),
                singletonList(autoCampaignInfo.getCampaignId())).get(0).getPrice(),
                comparesEqualTo(CurrencyCode.RUB.getCurrency().getDefaultPrice()));
    }

    /**
     * Для ретаргетинга и автотаргетинга: поправим нулевые ставки при переходе с автобюджета на ручное управление
     */
    @Test
    public void changeAutobudgetToManualRelevance_BigPriceUpdatedToDefaultPrice() {
        createRelevanceMatch(autoCampaignInfo, new BigDecimal(100000), BigDecimal.ZERO);

        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                defaultStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        assertThat(bidRepository.getBidsWithRelevanceMatchByCampaignIds(autoCampaignInfo.getShard(),
                singletonList(autoCampaignInfo.getCampaignId())).get(0).getPrice(),
                comparesEqualTo(CurrencyCode.RUB.getCurrency().getDefaultPrice()));
    }

    /**
     * Для ретаргетинга и автотаргетинга: поправим нулевые ставки при переходе с автобюджета на ручное управление
     */
    @Test
    public void changeAutobudgetToManualRelevance_PriceContextUpdatedToDefaultPrice_WhenFeatureOn() {
        createRelevanceMatch(autoCampaignInfo, new BigDecimal(100000), BigDecimal.ZERO);

        steps.featureSteps().addClientFeature(autoCampaignInfo.getClientInfo().getClientId(),
                FeatureName.CONTEXT_RELEVANCE_MATCH_ALLOWED, true);
        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                defaultStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        Bid bid = bidRepository.getBidsWithRelevanceMatchByCampaignIds(autoCampaignInfo.getShard(),
                singletonList(autoCampaignInfo.getCampaignId())).get(0);
        assertThat(bid.getPrice(),
                comparesEqualTo(CurrencyCode.RUB.getCurrency().getDefaultPrice()));
        assertThat(bid.getPriceContext(),
                comparesEqualTo(CurrencyCode.RUB.getCurrency().getDefaultPrice()));
    }

    @Test
    public void changeAutobudgetToManualRelevance_ZeroBidsRetargeting() {
        //DIRECT-108211: ошибка при смене стратегий в расчёте условий ретаргетинга на баннере
        createRelevanceMatch(autoCampaignInfo, new BigDecimal(100000), BigDecimal.ZERO);
        Retargeting retargeting = new Retargeting()
                .withCampaignId(autoCampaignInfo.getCampaignId())
                .withRetargetingConditionId(1489948L)
                .withPriceContext(BigDecimal.ZERO)
                .withLastChangeTime(LocalDateTime.now().withNano(0))
                .withStatusBsSynced(StatusBsSynced.YES)
                .withAutobudgetPriority(3)
                .withIsSuspended(false);
        steps.retargetingSteps().createRetargeting(retargeting, autoCampaignInfo);

        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                defaultStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        assertThat(bidRepository.getBidsWithRelevanceMatchByCampaignIds(autoCampaignInfo.getShard(),
                singletonList(autoCampaignInfo.getCampaignId())).get(0).getPrice(),
                comparesEqualTo(CurrencyCode.RUB.getCurrency().getDefaultPrice()));
    }

    @Test
    public void differentPlace_updateBids() {
        //случай DIRECT-103654
//        Создаю кампанию с ручной стратегией на всех площадках (не раздельное управление)
        prepareStrategy(manualCampaignInfo, manualBothStrategy());
//        Создаю баннеры с фразой со ставкой 11 руб
        createKeyword(BigDecimal.valueOf(11), BigDecimal.ZERO, manualCampaignInfo);
        assertThat(getFirstBid(manualCampaignInfo).getPriceContext(), comparesEqualTo(BigDecimal.ZERO));

//        Меняю стратегию на автобюджетную (Оптимизация кликов по средней цене клика), все площадки/поиск/сети
        campaignStrategyService.updateTextCampaignStrategy(manualCampaignInfo.getCampaignId(),
                defaultAverageClickStrategy(), manualCampaignInfo.getUid(),
                UidAndClientId.of(manualCampaignInfo.getUid(), manualCampaignInfo.getClientId()), false);
        assertThat("переключение на автобюджет в перле price_context остался 0",
                getFirstBid(manualCampaignInfo).getPriceContext(), comparesEqualTo(BigDecimal.ZERO));
//        Меняю стратегию на ручную с раздельным управлением
        campaignStrategyService.updateTextCampaignStrategy(manualCampaignInfo.getCampaignId(),
                defaultStrategy(), manualCampaignInfo.getUid(),
                UidAndClientId.of(manualCampaignInfo.getUid(), manualCampaignInfo.getClientId()), false);

        Bid bid = getFirstBid(manualCampaignInfo);
        assertThat("Поле price должно быть изначальное 11",
                bid.getPrice(), comparesEqualTo(BigDecimal.valueOf(11)));
        assertThat("Поле price_context должно бытть как price изначальное 11",
                bid.getPriceContext(), comparesEqualTo(BigDecimal.valueOf(11)));
        assertThat(bid.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    private Bid getFirstBid(CampaignInfo campaignInfo) {
        List<Bid> bids = bidRepository.getBidsByCampaignIds(campaignInfo.getShard(),
                singletonList(campaignInfo.getCampaignId()));
        return bids.get(0);
    }

    @Test
    public void changeAutobudgetToManual_restoreManualBidsZero() {
        Keyword keywordInfo1 = createKeyword(BigDecimal.valueOf(48.05), BigDecimal.valueOf(11.95), autoCampaignInfo);
        bidRepository.insertBidsToBidsManualPrices(autoCampaignInfo.getShard(),
                asList(new Bid()
                        .withId(keywordInfo1.getId())
                        .withCampaignId(autoCampaignInfo.getCampaignId())
                        .withPrice(BigDecimal.valueOf(14.5))
                        .withPriceContext(BigDecimal.ZERO)));

        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                manualSearchStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        assertThat(getFirstBid(autoCampaignInfo).getPrice(), comparesEqualTo(BigDecimal.valueOf(14.5)));

        var bidsBase = bidRepository.getRelevanceMatchByIds(
                autoCampaignInfo.getShard(), singletonList(getFirstBid(autoCampaignInfo).getId()));
        MatcherAssert.assertThat("по добавленному keyword ничего не получаем из bids_base", bidsBase, empty());
    }

    @Test
    public void changeAutobudgetToManual_statusBsSyncedNo() {
        createKeyword(BigDecimal.valueOf(48.05), BigDecimal.valueOf(11.95), autoCampaignInfo);

        campaignStrategyService.updateTextCampaignStrategy(autoCampaignInfo.getCampaignId(),
                manualSearchStrategy(), autoCampaignInfo.getUid(),
                UidAndClientId.of(autoCampaignInfo.getUid(), autoCampaignInfo.getClientId()), false);

        assertThat(getFirstBid(autoCampaignInfo).getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void changeManualToManual_statusBsSyncedYes() {
        createKeyword(BigDecimal.valueOf(48.05), BigDecimal.valueOf(11.95), manualCampaignInfo);
        campaignStrategyService.updateTextCampaignStrategy(manualCampaignInfo.getCampaignId(),
                manualSearchStrategy(), manualCampaignInfo.getUid(),
                UidAndClientId.of(manualCampaignInfo.getUid(), manualCampaignInfo.getClientId()), false);
        assertThat(getFirstBid(manualCampaignInfo).getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    public void changeManualToManualNotDifferentPlace_statusBsSyncedYes() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        prepareStrategy(campaignInfo, manualBothStrategy());
        createKeyword(BigDecimal.valueOf(447.98), BigDecimal.ZERO, campaignInfo);

        campaignStrategyService.updateTextCampaignStrategy(campaignInfo.getCampaignId(),
                manualBothStrategy(), campaignInfo.getUid(),
                UidAndClientId.of(campaignInfo.getUid(), campaignInfo.getClientId()), false);

        assertFalse(manualBothStrategy().isDifferentPlaces());
        assertThat(getFirstBid(campaignInfo).getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    public void changeManualToManualDifferentPlace_statusBsSyncedYes() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        prepareStrategy(campaignInfo, manualBothDifferentStrategy());
        createKeyword(BigDecimal.valueOf(48.05), BigDecimal.valueOf(11.95), campaignInfo);

        campaignStrategyService.updateTextCampaignStrategy(campaignInfo.getCampaignId(),
                manualBothDifferentStrategy(), campaignInfo.getUid(),
                UidAndClientId.of(campaignInfo.getUid(), campaignInfo.getClientId()), false);

        assertTrue(manualBothDifferentStrategy().isDifferentPlaces());
        assertThat(getFirstBid(campaignInfo).getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    public void changeAutobudgetToAutobudget_statusBsSyncedYes() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        prepareStrategy(campaignInfo, defaultAverageClickStrategy());
        createKeyword(BigDecimal.valueOf(48.05), BigDecimal.valueOf(11.95), campaignInfo);

        campaignStrategyService.updateTextCampaignStrategy(campaignInfo.getCampaignId(),
                contextAverageClickStrategy(), campaignInfo.getUid(),
                UidAndClientId.of(campaignInfo.getUid(), campaignInfo.getClientId()), false);

        assertThat(getFirstBid(campaignInfo).getStatusBsSynced(), is(StatusBsSynced.YES));
    }
}
