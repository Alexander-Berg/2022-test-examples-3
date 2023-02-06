package ru.yandex.direct.core.entity.bids.service;

import java.math.BigDecimal;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.auction.container.AdGroupForAuction;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.bids.container.KeywordBidPokazometerData;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.currency.service.CurrencyRateService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.pokazometer.GroupRequest;
import ru.yandex.direct.pokazometer.GroupResponse;
import ru.yandex.direct.pokazometer.PhraseRequest;
import ru.yandex.direct.pokazometer.PhraseResponse;
import ru.yandex.direct.pokazometer.PokazometerClient;

import static com.google.common.primitives.Longs.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy.DIFFERENT_PLACES;

public class PokazometerServiceTest {

    private static final Money DEFAULT_MONEY = Money.valueOf(BigDecimal.valueOf(0.1), CurrencyCode.RUB);

    private static final Long[] POKAZOMETER_PRICES = {100_000L, 400_000L, 1_000_000L};
    private static final Integer[] POKAZOMETER_CLICKS = {9, 21, 42};
    private static final Money[] EXPECTED_CONVERTED_PRICES = {Money.valueOf(1, CurrencyCode.RUB),
            Money.valueOf(3, CurrencyCode.RUB), Money.valueOf(5, CurrencyCode.RUB),};
    private static final Currency CURRENCY = CurrencyCode.YND_FIXED.getCurrency();
    private static final long KEYWORD_ID = 1L;
    private static final Keyword DEFAULT_KEYWORD = new Keyword()
            .withId(KEYWORD_ID)
            .withPhrase("phrase")
            .withPrice(BigDecimal.ONE)
            .withPriceContext(BigDecimal.ONE);
    private static final List<Long> DEFAULT_GEO = asList(1, 2, 3);
    private static final AdGroup DEFAULT_AD_GROUP = new TextAdGroup().withGeo(DEFAULT_GEO);
    private static final TextBanner DEFAULT_MAIN_BANNER = new TextBanner();
    @SuppressWarnings("FieldCanBeLocal")
    private CurrencyRateService currencyRateService;
    private PokazometerClient pokazometerClient;
    private PokazometerService serviceUnderTest;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        currencyRateService = mock(CurrencyRateService.class);
        when(currencyRateService.convertMoney(any(), any())).thenReturn(DEFAULT_MONEY);
        pokazometerClient = mock(PokazometerClient.class);

        when(pokazometerClient.get(any())).thenAnswer(
                invocation -> {
                    List<GroupRequest> requests = (List<GroupRequest>) invocation.getArguments()[0];

                    Function<GroupRequest, GroupResponse> responseFunction = (groupRequest) -> {
                        List<PhraseResponse> phraseResponses = StreamEx.of(groupRequest.getPhrases())
                                .map(PhraseResponse::on)
                                .peek(phrase -> phrase.setContextCoverage(1_000_000L))
                                .peek(phrase -> phrase
                                        .setPriceByCoverage(PhraseResponse.Coverage.LOW, POKAZOMETER_PRICES[0]))
                                .peek(phrase -> phrase
                                        .setPriceByCoverage(PhraseResponse.Coverage.MEDIUM, POKAZOMETER_PRICES[1]))
                                .peek(phrase -> phrase
                                        .setPriceByCoverage(PhraseResponse.Coverage.HIGH, POKAZOMETER_PRICES[2]))
                                .peek(phrase -> phrase.setClicksByCost(
                                        ImmutableMap
                                                .of(POKAZOMETER_PRICES[0], POKAZOMETER_CLICKS[0], POKAZOMETER_PRICES[1],
                                                        POKAZOMETER_CLICKS[1],
                                                        POKAZOMETER_PRICES[2], POKAZOMETER_CLICKS[2])))
                                .toList();
                        return GroupResponse.success(phraseResponses);
                    };

                    return StreamEx.of(requests)
                            .mapToEntry(responseFunction)
                            .toCustomMap(IdentityHashMap::new);
                }
        );

        serviceUnderTest = new PokazometerService(currencyRateService, pokazometerClient);
    }

    @Test
    public void getPokazometerResults_resultNotEmpty_whenStrategyEqDifferentPlaces() {
        Campaign campaign = new Campaign().withStrategy(getDbStrategy(DIFFERENT_PLACES))
                .withCurrency(CurrencyCode.RUB);
        List<AdGroupForAuction> adGroupForAuctions = singletonList(getAdGroupForAuctionWithCampaign(campaign));

        List<KeywordBidPokazometerData> actual = serviceUnderTest.getPokazometerResults(adGroupForAuctions);

        assertThat(actual.isEmpty()).isFalse();
        assertThat(actual.get(0).getKeywordId()).isSameAs(KEYWORD_ID);
        assertThat(actual.get(0).getCoverageWithPrices().values())
                .containsSequence(DEFAULT_MONEY, DEFAULT_MONEY, DEFAULT_MONEY);
    }

    @Test
    public void getPokazometerResults_resultEmpty_whenStrategyNeDifferentPlaces() {
        Campaign campaign = new Campaign().withStrategy(getDbStrategy(null));
        List<AdGroupForAuction> adGroupForAuctions = singletonList(getAdGroupForAuctionWithCampaign(campaign));

        List<KeywordBidPokazometerData> actual = serviceUnderTest.getPokazometerResults(adGroupForAuctions);

        assertThat(actual.isEmpty()).isTrue();
    }

    @Test
    public void getPokazometerResults_resultEmpty_whenErrorsInPokazometerResponse() {
        Campaign campaign = new Campaign().withStrategy(getDbStrategy(DIFFERENT_PLACES));
        List<AdGroupForAuction> adGroupForAuctions = singletonList(getAdGroupForAuctionWithCampaign(campaign));

        reset(pokazometerClient);
        when(pokazometerClient.get(any())).thenAnswer(
                invocation -> {
                    @SuppressWarnings("unchecked")
                    List<GroupRequest> requests = (List<GroupRequest>) invocation.getArguments()[0];
                    // возвращаем ответ с ошибкой из клиента
                    return StreamEx.of(requests)
                            .mapToEntry((ignored) -> GroupResponse.failure(emptyList()))
                            .toCustomMap(IdentityHashMap::new);
                }
        );

        List<KeywordBidPokazometerData> actual = serviceUnderTest.getPokazometerResults(adGroupForAuctions);
        assertThat(actual.isEmpty()).isTrue();
    }

    @Test
    public void getPokazometerResults_doesNotThrow_whenPriceContextIsNull() {
        Campaign campaign = new Campaign().withStrategy(getDbStrategy(DIFFERENT_PLACES))
                .withCurrency(CurrencyCode.RUB);
        Keyword keyword = new Keyword()
                .withId(KEYWORD_ID)
                .withPhrase("phrase")
                .withPrice(BigDecimal.ONE)
                .withPriceContext(null);
        List<AdGroupForAuction> adGroupForAuctions =
                singletonList(getAdGroupForAuctionWithCampaignKeyword(campaign, keyword));

        assertThatCode(() -> serviceUnderTest.getPokazometerResults(adGroupForAuctions))
                .doesNotThrowAnyException();
    }

    @Test
    public void getPokazometerResults_resultNotEmpty() {
        when(currencyRateService
                .convertMoney(Money.valueOfMicros(POKAZOMETER_PRICES[0], CurrencyCode.YND_FIXED), CurrencyCode.RUB))
                .thenReturn(EXPECTED_CONVERTED_PRICES[0]);
        when(currencyRateService
                .convertMoney(Money.valueOfMicros(POKAZOMETER_PRICES[1], CurrencyCode.YND_FIXED), CurrencyCode.RUB))
                .thenReturn(EXPECTED_CONVERTED_PRICES[1]);
        when(currencyRateService
                .convertMoney(Money.valueOfMicros(POKAZOMETER_PRICES[2], CurrencyCode.YND_FIXED), CurrencyCode.RUB))
                .thenReturn(EXPECTED_CONVERTED_PRICES[2]);

        List<KeywordBidPokazometerData> actual =
                serviceUnderTest.getPokazometerResults(getDefaultGroupRequest(), CurrencyCode.RUB);

        assertThat(actual.isEmpty()).isFalse();
        assertThat(actual.get(0).getKeywordId()).isSameAs(KEYWORD_ID);
        assertThat(actual.get(0).getAllCostsAndClicks().values())
                .containsExactlyInAnyOrder(POKAZOMETER_CLICKS[0], POKAZOMETER_CLICKS[1], POKAZOMETER_CLICKS[2]);

    }


    @Test
    public void getPokazometerResults_groupRequest_resultEmpty_whenErrorsInPokazometerResponse() {

        reset(pokazometerClient);
        when(pokazometerClient.get(any())).thenAnswer(
                invocation -> {
                    @SuppressWarnings("unchecked")
                    List<GroupRequest> requests = (List<GroupRequest>) invocation.getArguments()[0];
                    // возвращаем ответ с ошибкой из клиента
                    return StreamEx.of(requests)
                            .mapToEntry((ignored) -> GroupResponse.failure(emptyList()))
                            .toCustomMap(IdentityHashMap::new);
                }
        );

        List<KeywordBidPokazometerData> actual =
                serviceUnderTest.getPokazometerResults(getDefaultGroupRequest(), CurrencyCode.RUB);
        assertThat(actual).isEmpty();
    }

    @Test
    public void getPokazometerResults_groupRequests_whenPriceContextIsNull() {

        Keyword keyword = new Keyword()
                .withId(KEYWORD_ID)
                .withPhrase("phrase")
                .withPrice(BigDecimal.ONE)
                .withPriceContext(null);

        List<GroupRequest> requests = singletonList(getGroupRequest(getPhraseRequests(keyword), DEFAULT_GEO));

        assertThatCode(() -> serviceUnderTest.getPokazometerResults(requests, CurrencyCode.RUB))
                .doesNotThrowAnyException();
    }

    @Nonnull
    private DbStrategy getDbStrategy(CampOptionsStrategy strategy) {
        DbStrategy dbStrategy = new DbStrategy();
        dbStrategy.withStrategy(strategy);
        return dbStrategy;
    }

    @Nonnull
    private AdGroupForAuction getAdGroupForAuctionWithCampaign(Campaign campaign) {
        return getAdGroupForAuctionWithCampaignKeyword(campaign, DEFAULT_KEYWORD);
    }

    @Nonnull
    private AdGroupForAuction getAdGroupForAuctionWithCampaignKeyword(Campaign campaign, Keyword keyword) {
        return AdGroupForAuction.builder()
                .campaign(campaign)
                .adGroup(DEFAULT_AD_GROUP)
                .mainBanner(DEFAULT_MAIN_BANNER)
                .keywords(singletonList(keyword))
                .currency(CURRENCY)
                .bannerQuantity(1)
                .build();
    }

    @Nonnull
    private List<PhraseRequest> getPhraseRequests(Keyword... keyword) {
        return StreamEx.of(keyword)
                .map(kw -> new PhraseRequest(kw.getPhrase(),
                        (kw.getPriceContext() == null) ? null : kw.getPriceContext().longValue(),
                        kw.getId()))
                .toList();
    }

    @Nonnull
    private GroupRequest getGroupRequest(List<PhraseRequest> phraseRequests, List<Long> geo) {
        return new GroupRequest(phraseRequests, geo);
    }

    @Nonnull
    private List<GroupRequest> getDefaultGroupRequest() {
        return singletonList(getGroupRequest(getPhraseRequests(
                new Keyword()
                        .withId(KEYWORD_ID)
                        .withPhrase("phrase1")
                        .withPriceContext(BigDecimal.ONE),
                new Keyword()
                        .withId(KEYWORD_ID + 1)
                        .withPhrase("phrase2")
                        .withPriceContext(BigDecimal.TEN)),
                DEFAULT_GEO));
    }
}
