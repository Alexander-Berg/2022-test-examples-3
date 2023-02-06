package ru.yandex.direct.logicprocessor.processors.bsexport.bids;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.adv.direct.showcondition.DynamicConditionData;
import ru.yandex.adv.direct.showcondition.FeedFilterData;
import ru.yandex.adv.direct.showcondition.HrefParam;
import ru.yandex.adv.direct.showcondition.KeywordData;
import ru.yandex.adv.direct.showcondition.RelevanceMatchData;
import ru.yandex.adv.direct.showcondition.RetargetingData;
import ru.yandex.adv.direct.showconditions.BiddableShowCondition;
import ru.yandex.direct.bstransport.yt.utils.CaesarIterIdGenerator;
import ru.yandex.direct.core.bsexport.model.BidsStatusModerate;
import ru.yandex.direct.core.bsexport.model.BsExportBidDynamic;
import ru.yandex.direct.core.bsexport.model.BsExportBidKeyword;
import ru.yandex.direct.core.bsexport.model.BsExportBidOfferRetargeting;
import ru.yandex.direct.core.bsexport.model.BsExportBidPerformance;
import ru.yandex.direct.core.bsexport.model.BsExportBidRetargeting;
import ru.yandex.direct.core.bsexport.model.BsExportRelevanceMatch;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.client.service.ClientNdsService;
import ru.yandex.direct.core.entity.keyword.processing.bsexport.BsExportTextProcessor;
import ru.yandex.direct.core.entity.performancefilter.model.TargetFunnel;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.currency.currencies.CurrencyRub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.bstransport.yt.utils.ToBsConversionUtils.bsPreparePrice;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.TEXT;

class BiddableShowConditionsYtRecordMapperMinimalTest {
    public static final String TEST_PHRASE = "Test Phrase";
    public static final Currency RUB = CurrencyRub.getInstance();
    public static final Map<Integer, String> HREF_PARAMS = Map.of(1, "42");
    public static final HrefParam HREF_PARAM_PROTO = HrefParam.newBuilder()
            .setName("")
            .setValue("42")
            .setParamNo(1)
            .build();
    private BiddableShowConditionsYtRecordMapper mapper;
    private final long iterId = 1L;
    private Clock fixedClock;
    private long fixedTime;
    private BiddableShowConditionsYtRecordMapper.Now now;

    @BeforeEach
    void init() {
        fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        fixedTime = fixedClock.instant().getEpochSecond();

        var stopWordsService = mock(StopWordService.class);
        when(stopWordsService.isStopWord(any())).thenReturn(false);

        mapper = new BiddableShowConditionsYtRecordMapper(
                new BsExportTextProcessor(stopWordsService),
                mock(ClientNdsService.class),
                mock(CaesarIterIdGenerator.class),
                fixedClock);
        now = new BiddableShowConditionsYtRecordMapper.Now(iterId, fixedTime);
    }

    @Test
    void keywordBidToYt() {
        BigDecimal bid = BigDecimal.valueOf(10L);
        BigDecimal bidContext = BigDecimal.valueOf(0);
        BsExportBidKeyword keyword = new BsExportBidKeyword()
                .withId(1L)
                .withAdGroupId(2L)
                .withOrderId(3L)
                .withCampaignId(33L)
                .withPrice(bid)
                .withPriceContext(bidContext)
                .withIsSuspended(false)
                .withStatusModerate(BidsStatusModerate.YES)
                .withCurrency(RUB)
                .withCampaignType(TEXT)
                .withPhrase(TEST_PHRASE)
                .withParams(HREF_PARAMS);

        BiddableShowCondition cond = getConditionBuilder(BiddableShowConditionsYtRecordMapper.contextTypeOf(keyword))
                .setBid(bsPreparePrice(bid, RUB, TEXT))
                .setBidContext(bsPreparePrice(bidContext, RUB, TEXT))
                .setKeywordData(KeywordData.newBuilder().setText(TEST_PHRASE).build())
                .setSuspended(false)
                .build();

        assertEquals(cond, mapper.keywordBidToYt(keyword, now));
    }

    @Test
    void dynamicBidToYt() {
        BigDecimal bid = BigDecimal.valueOf(10L);
        BigDecimal bidContext = BigDecimal.valueOf(0);
        BsExportBidDynamic dynamic = new BsExportBidDynamic()
                .withId(1L)
                .withAdGroupId(2L)
                .withOrderId(3L)
                .withCampaignId(33L)
                .withDynCondId(66L)
                .withPrice(bid)
                .withPriceContext(bidContext)
                .withCurrency(RUB)
                .withCampaignType(TEXT)
                .withParams(HREF_PARAMS)
                .withIsSuspended(false);

        BiddableShowCondition cond = getConditionBuilder(BiddableShowConditionsYtRecordMapper.contextTypeOf(dynamic))
                .setBid(bsPreparePrice(bid, RUB, TEXT))
                .setBidContext(bsPreparePrice(bidContext, RUB, TEXT))
                .setDynamicConditionData(DynamicConditionData.newBuilder().setDynCondId(66L).build())
                .setSuspended(false)
                .build();

        assertEquals(cond, mapper.dynamicBidToYt(dynamic, now));
    }

    @Test
    void performanceBidToYt() {
        BsExportBidPerformance filter = new BsExportBidPerformance()
                .withId(1L)
                .withAdGroupId(2L)
                .withOrderId(3L)
                .withCampaignId(33L)
                .withTargetFunnel(TargetFunnel.NEW_AUDITORY)
                .withCampaignStrategyData(
                        new StrategyData()
                                .withFilterAvgBid(BigDecimal.valueOf(12))
                                .withFilterAvgCpa(BigDecimal.valueOf(43))
                )
                .withCampaignStrategyName(StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP)
                .withPriceCpc(BigDecimal.valueOf(10L))
                .withPriceCpa(BigDecimal.valueOf(0))
                .withCurrency(RUB)
                .withCampaignType(TEXT)
                .withParams(HREF_PARAMS)
                .withIsSuspended(false);

        BiddableShowCondition cond = getConditionBuilder(BiddableShowConditionsYtRecordMapper.contextTypeOf(filter))
                .setFeedFilterData(
                        FeedFilterData.newBuilder()
                                .setPriceCpc(113000)
                                .setPriceCpa(485900)
                                .setOnlyNewAuditory(true)
                                .setOnlyOfferRetargeting(false)
                                .build()
                )
                .setSuspended(false)
                .build();

        assertEquals(cond, mapper.performanceBidToYt(filter, now, Percent.fromPercent(BigDecimal.valueOf(13))));
    }

    @Test
    void offerRetargetingBidToYt() {
        BsExportBidOfferRetargeting filter = new BsExportBidOfferRetargeting()
                .withId(1L)
                .withAdGroupId(2L)
                .withOrderId(3L)
                .withCampaignId(33L)
                .withCampaignStrategyData(
                        new StrategyData()
                                .withFilterAvgBid(BigDecimal.valueOf(12))
                                .withFilterAvgCpa(BigDecimal.valueOf(43))
                )
                .withCampaignStrategyName(StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP)
                .withCurrency(RUB)
                .withCampaignType(TEXT)
                .withParams(HREF_PARAMS)
                .withIsSuspended(false);

        BiddableShowCondition cond = getConditionBuilder(BiddableShowConditionsYtRecordMapper.contextTypeOf(filter))
                .setFeedFilterData(
                        FeedFilterData.newBuilder()
                                .setPriceCpc(135600) // ставка из стратегии кампании с добавлением nds
                                .setPriceCpa(485900) // ставка из стратегии кампании с добавлением nds
                                .build()
                )
                .setSuspended(false)
                .build();

        assertEquals(cond, mapper.offerRetargetingBidToYt(filter, now, Percent.fromPercent(BigDecimal.valueOf(13))));
    }

    @Test
    void retargetingBidToYt() {
        long retCondId = 200L;

        BigDecimal bidContext = BigDecimal.valueOf(0);

        BsExportBidRetargeting retargeting = new BsExportBidRetargeting()
                .withId(1L)
                .withAdGroupId(2L)
                .withOrderId(3L)
                .withCampaignId(33L)
                .withPriceContext(bidContext)
                .withCurrency(RUB)
                .withCampaignType(TEXT)
                .withRetCondId(retCondId)
                .withParams(HREF_PARAMS)
                .withIsSuspended(false)
                .withIsAccessible(true);

        BiddableShowCondition cond =
                getConditionBuilder(BiddableShowConditionsYtRecordMapper.contextTypeOf(retargeting))
                        .setBidContext(bsPreparePrice(bidContext, RUB, TEXT))
                        .setRetargetingData(RetargetingData.newBuilder().setRetCondId(retCondId).build())
                        .setSuspended(false)
                        .build();

        assertEquals(cond, mapper.retargetingBidToYt(retargeting, now));
    }

    @Test
    void relevanceMatchBidToYt() {
        BigDecimal bid = BigDecimal.valueOf(10L);
        BigDecimal bidContext = BigDecimal.valueOf(0);
        BsExportRelevanceMatch relevanceMatch = relevanceMatchExport(bid, bidContext)
                .withRelevanceMatchCategories(Set.of(RelevanceMatchCategory.exact_mark,
                        RelevanceMatchCategory.competitor_mark));

        var relevanceMatchData =
                RelevanceMatchData.newBuilder()
                        .addAllRelevanceMatchCategories(Set.of(RelevanceMatchData.RelevanceMatchCategory.ExactMark,
                                RelevanceMatchData.RelevanceMatchCategory.CompetitorMark));

        BiddableShowCondition cond =
                getConditionBuilder(BiddableShowConditionsYtRecordMapper.contextTypeOf(relevanceMatch))
                        .setBid(bsPreparePrice(bid, RUB, TEXT))
                        .setBidContext(bsPreparePrice(bidContext, RUB, TEXT))
                        .setRelevanceMatchData(RelevanceMatchData.newBuilder().build())
                        .setSuspended(false)
                        .setRelevanceMatchData(relevanceMatchData)
                        .build();

        assertEquals(cond, mapper.relevanceMatchBidToYt(relevanceMatch, now));
    }

    @Test
    void relevanceMatchNoCategoriesBidToYt() {
        BigDecimal bid = BigDecimal.valueOf(10L);
        BigDecimal bidContext = BigDecimal.valueOf(0);
        BsExportRelevanceMatch relevanceMatchWithNullCategories = relevanceMatchExport(bid, bidContext);
        BsExportRelevanceMatch relevanceMatchWithEmptyCategories =
                relevanceMatchExport(bid, bidContext).withRelevanceMatchCategories(Set.of());

        BiddableShowCondition cond =
                getConditionBuilder(BiddableShowConditionsYtRecordMapper.contextTypeOf(relevanceMatchWithNullCategories))
                        .setBid(bsPreparePrice(bid, RUB, TEXT))
                        .setBidContext(bsPreparePrice(bidContext, RUB, TEXT))
                        .setRelevanceMatchData(RelevanceMatchData.newBuilder().build())
                        .setSuspended(false)
                        .build();

        assertEquals(cond, mapper.relevanceMatchBidToYt(relevanceMatchWithNullCategories, now));
        assertEquals(cond, mapper.relevanceMatchBidToYt(relevanceMatchWithEmptyCategories, now));
    }

    private BsExportRelevanceMatch relevanceMatchExport(BigDecimal price, BigDecimal priceContext) {
        return new BsExportRelevanceMatch()
                .withId(1L)
                .withAdGroupId(2L)
                .withOrderId(3L)
                .withCampaignId(33L)
                .withPrice(price)
                .withPriceContext(priceContext)
                .withCurrency(RUB)
                .withCampaignType(TEXT)
                .withParams(HREF_PARAMS)
                .withIsDeleted(false)
                .withIsSuspended(false);
    }

    private BiddableShowCondition.@NotNull Builder getConditionBuilder(int contextType) {
        return BiddableShowCondition.newBuilder()
                .setIterId(iterId)
                .setOrderId(3L)
                .setAdGroupId(2L)
                .setContextType(contextType)
                .setUpdateTime(fixedTime)
                .setDeleteTime(0)
                .setId(1L)
                .addHrefParams(HREF_PARAM_PROTO);
    }
}
