package ru.yandex.direct.intapi.entity.showconditions.controller;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.bsauction.BsAuctionBidItem;
import ru.yandex.direct.bsauction.BsRequest;
import ru.yandex.direct.bsauction.BsRequestPhrase;
import ru.yandex.direct.bsauction.BsResponse;
import ru.yandex.direct.bsauction.BsTrafaretClient;
import ru.yandex.direct.bsauction.FullBsTrafaretResponsePhrase;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.client.model.ClientLimits;
import ru.yandex.direct.core.entity.currency.service.CurrencyRateService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.showconditions.model.request.KeywordAddItem;
import ru.yandex.direct.intapi.entity.showconditions.model.request.KeywordModificationContainer;
import ru.yandex.direct.intapi.entity.showconditions.model.request.KeywordUpdateItem;
import ru.yandex.direct.intapi.entity.showconditions.model.request.ShowConditionsRequest;
import ru.yandex.direct.intapi.entity.showconditions.model.response.BannerItemResponse;
import ru.yandex.direct.intapi.entity.showconditions.model.response.KeywordItemResponse;
import ru.yandex.direct.intapi.entity.showconditions.model.response.ResponseItem;
import ru.yandex.direct.intapi.entity.showconditions.model.response.ShowConditionsResponse;
import ru.yandex.direct.intapi.entity.showconditions.model.response.TrafficVolumeItem;
import ru.yandex.direct.pokazometer.GroupRequest;
import ru.yandex.direct.pokazometer.GroupResponse;
import ru.yandex.direct.pokazometer.PhraseResponse;
import ru.yandex.direct.pokazometer.PokazometerClient;
import ru.yandex.direct.ytcomponents.statistics.model.PhraseStatisticsRequest;
import ru.yandex.direct.ytcomponents.statistics.model.PhraseStatisticsResponse;
import ru.yandex.direct.ytcomponents.statistics.model.StatValueAggregator;
import ru.yandex.direct.ytcore.entity.statistics.service.RecentStatisticsService;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.bsauction.BsTrafaretClient.PLACE_TRAFARET_MAPPING;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordWithText;
import static ru.yandex.direct.intapi.entity.showconditions.model.response.ShowConditionsResponse.PRESET_TRAFFIC_VOLUME_POSITIONS;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.BS_TRAFARET_AUCTION_CLIENT;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Тесты для проверки привязки дефектов валидации обновления и удаления ключевых слов в intapi
 */
@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class ShowConditionsControllerModifyKeywordTest {

    private static final CompareStrategy COMPARE_STRATEGY =
            allFieldsExcept(
                    newPath("\\d+", "keywords", "\\d+", "trafficVolume"),
                    newPath("\\d+", "banners", "\\d+", "textStatus"));

    private static final String DEFAULT_PHRASE = "default phrase";
    private static final String NEW_PHRASE = "new phrase";
    private static final BigDecimal INITIAL_PRICE = BigDecimal.valueOf(12).setScale(2, BigDecimal.ROUND_UNNECESSARY);
    private static final BigDecimal DEFAULT_PRICE = BigDecimal.valueOf(10);

    private AdGroupInfo adGroupInfo;

    private int shard;
    private long uid;
    private long clientId;
    private long adGroupId;
    private long campaignId;
    private long keywordId;
    private CurrencyCode currency;

    private Map<Integer, String> expectedPriceForCoverage;
    private Map<Long, BannerItemResponse> expectedBanners;

    @Autowired
    private ShowConditionsController showConditionsController;

    @Autowired
    private TestCampaignRepository campaignRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private RecentStatisticsService recentStatisticsService;

    @Autowired
    @Qualifier(BS_TRAFARET_AUCTION_CLIENT)
    private BsTrafaretClient bsTrafaretClient;

    @Autowired
    private PokazometerClient pokazometerClient;

    @Autowired
    private CurrencyRateService currencyRateService;

    @Before
    public void before() {
        Keyword keyword = keywordWithText(DEFAULT_PHRASE)
                .withPrice(INITIAL_PRICE)
                .withPriceContext(INITIAL_PRICE);
        KeywordInfo keywordInfo = steps.keywordSteps().createKeyword(keyword);
        adGroupInfo = keywordInfo.getAdGroupInfo();

        // для успешного ответа из торгов, должен быть задан баннер в группе, иначе результат будет пустой.
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), adGroupInfo);

        shard = keywordInfo.getShard();
        uid = adGroupInfo.getUid();
        clientId = adGroupInfo.getClientId().asLong();
        adGroupId = keywordInfo.getAdGroupId();
        campaignId = keywordInfo.getCampaignId();
        keywordId = keywordInfo.getId();

        currency = CurrencyCode.RUB;

        configureStatisticsServiceMock();
        configureTrafaretAuctionMock();
        configurePokazometerClientMock();

        Function<Long, BigDecimal> converterCurrency = v -> {
            Money converted = currencyRateService.convertMoney(Money.valueOf(v, CurrencyCode.YND_FIXED), currency);
            converted = converted.subtractNds(Percent.fromPercent(BigDecimal.valueOf(18))); // default in Russia;
            return converted.roundToAuctionStepUp().bigDecimalValue();
        };

        expectedPriceForCoverage = EntryStream.of(defaultCoverageMap())
                .mapKeys(PhraseResponse.Coverage::getPercentage)
                .mapValues(v -> v / 1_000_000) // micros to default money value
                .mapValues(converterCurrency)
                .mapValues(Object::toString)
                .toMap();

        expectedBanners = singletonMap(bannerInfo.getBannerId(),
                new BannerItemResponse(null, null)
                        .withBannerID(bannerInfo.getBanner().getBsBannerId())
                        .withStatusActive(true)
                        .withStatusModerate(BannerStatusModerate.YES)
                        .withStatusShow(true));
    }

    private void configureStatisticsServiceMock() {
        when(recentStatisticsService
                .getPhraseStatistics(anyList(), any(), any()))
                .thenAnswer(invocation -> {
                            List<PhraseStatisticsRequest> requests = invocation.getArgument(0);
                            return requests.stream().collect(Collectors.toMap(
                                    r -> new PhraseStatisticsResponse()
                                            .withCampaignId(campaignId)
                                            .withAdGroupId(adGroupId)
                                            .withPhraseId(r.getPhraseId())
                                            .showConditionStatIndex(),
                                    r -> new StatValueAggregator()
                                            .addSearchClicks(2L)
                                            .addSearchShows(100L)));
                        }
                );
    }

    private void configurePokazometerClientMock() {
        when(pokazometerClient.get(any())).thenAnswer(invocation -> {
            List<GroupRequest> groups = invocation.getArgument(0);
            if (groups == null) {
                return null;
            }

            return StreamEx.of(groups)
                    .mapToEntry(g -> GroupResponse.success(mapList(g.getPhrases(), request -> {
                        PhraseResponse response = PhraseResponse.on(request);
                        response.getPriceByCoverage().putAll(defaultCoverageMap());
                        return response;
                    })))
                    .toCustomMap(IdentityHashMap::new);
        });

    }

    private Map<PhraseResponse.Coverage, Long> defaultCoverageMap() {
        return ImmutableMap.of(
                PhraseResponse.Coverage.LOW, 10_000_000L,
                PhraseResponse.Coverage.MEDIUM, 20_000_000L,
                PhraseResponse.Coverage.HIGH, 30_000_000L);
    }

    private void configureTrafaretAuctionMock() {
        when(bsTrafaretClient.getAuctionResultsWithPositionCtrCorrection(anyList())).thenAnswer(
                invocation -> {
                    List<BsRequest<BsRequestPhrase>> requests = invocation.getArgument(0);
                    return generateDefaultBsAuctionResponse(requests, currency);
                }
        );
    }

    @SuppressWarnings("checkstyle:linelength")
    private static IdentityHashMap<BsRequest<BsRequestPhrase>, BsResponse<BsRequestPhrase,
            FullBsTrafaretResponsePhrase>> generateDefaultBsAuctionResponse(
            List<BsRequest<BsRequestPhrase>> requests, CurrencyCode currency) {
        return StreamEx.of(requests)
                .mapToEntry(r -> {
                    IdentityHashMap<BsRequestPhrase, FullBsTrafaretResponsePhrase> successResult =
                            StreamEx.of(r.getPhrases())
                                    .mapToEntry(phr -> trafaretResponsePhrase(currency))
                                    .toCustomMap(IdentityHashMap::new);
                    return BsResponse.success(successResult);
                })
                .toCustomMap(IdentityHashMap::new);
    }

    private static FullBsTrafaretResponsePhrase trafaretResponsePhrase(CurrencyCode currency) {
        FullBsTrafaretResponsePhrase response = new FullBsTrafaretResponsePhrase();
        response.withBidItems(asList(
                new BsAuctionBidItem(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM1),
                        Money.valueOf(6500, currency), Money.valueOf(6500, currency)),
                new BsAuctionBidItem(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM2),
                        Money.valueOf(40, currency), Money.valueOf(45d, currency)),
                new BsAuctionBidItem(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM3),
                        Money.valueOf(30, currency), Money.valueOf(35d, currency)),
                new BsAuctionBidItem(PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM4),
                        Money.valueOf(20, currency), Money.valueOf(25d, currency))
        ));
        return response;
    }

    @Test
    public void update_EmptyRequest() {
        ShowConditionsRequest request = new ShowConditionsRequest();

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertTrue(response.isSuccessful());
        assertThat(response.getItems().entrySet(), Matchers.empty());
    }

    @Test
    public void update_EqualsKeywordIdsOnDelete_Success() {
        ShowConditionsRequest request = buildDeleteRequest(adGroupId, keywordId);
        request.getKeywords().get(adGroupId).getDeleted().add(keywordId);

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertTrue(response.isSuccessful());
        assertThat(response.getItems(), beanDiffer(singletonMap(adGroupId, new ResponseItem()
                .withBanners(expectedBanners))).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void update_AddKeyword_Success() {
        // если баннер не задан, то в торги не ходим, поэтому явно добавляем баннер, чтобы определялся mainBannerId
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(adGroupInfo.getCampaignInfo());
        Long localAdGroupId = bannerInfo.getAdGroupId();

        expectedBanners = singletonMap(bannerInfo.getBannerId(),
                new BannerItemResponse(null, null)
                        .withBannerID(bannerInfo.getBanner().getBsBannerId())
                        .withStatusActive(true)
                        .withStatusModerate(BannerStatusModerate.YES)
                        .withStatusShow(true)
                        .withTextStatus("Черновик"));
        ShowConditionsRequest request = buildAddRequest(localAdGroupId,
                new KeywordAddItem().withPhrase(NEW_PHRASE).withPrice(DEFAULT_PRICE).withPriceContext(DEFAULT_PRICE));

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        List<Keyword> keywords = keywordRepository.getKeywordsByAdGroupId(shard, localAdGroupId);
        assumeThat("в базе дожна быть только одна фраза", keywords, hasSize(1));
        Keyword keyword = keywords.get(0);

        assertTrue(response.isSuccessful());
        assertThat(response.getItems(), beanDiffer(singletonMap(localAdGroupId, new ResponseItem()
                .withBanners(expectedBanners)
                .withPhrasesAddedQuantity(1)
                .withKeywords(singletonMap(keyword.getId(), new KeywordItemResponse()
                        .withNormPhrase(keyword.getNormPhrase())
                        .withWordsCount(2)
                        .withPhraseId(keyword.getPhraseBsId().toString())
                        .withPrice(keyword.getPrice().toString())
                        .withPriceContext(keyword.getPriceContext().toString())
                        .withPhrase(keyword.getPhrase())
                        .withDuplicate(0)
                        .withPriceForCoverage(expectedPriceForCoverage)
                        .withShows(100L)
                        .withClicks(2L)
                        .withCtr(BigDecimal.valueOf(2.00).setScale(2, BigDecimal.ROUND_UNNECESSARY).toString())))))
                .useCompareStrategy(COMPARE_STRATEGY));

        Map<Integer, TrafficVolumeItem> trafficVolumeItems =
                response.getItems().get(localAdGroupId).getKeywords().get(keyword.getId()).getTrafficVolume();
        assertInterpolatedTrafficVolume(trafficVolumeItems);
    }

    @Test
    public void update_AddKeyword_LimitExceeded() {
        steps.clientSteps().updateClientLimits(adGroupInfo.getClientInfo()
                .withClientLimits((ClientLimits) new ClientLimits().withClientId(ClientId.fromLong(clientId))
                        .withKeywordsCountLimit(1L)));
        ShowConditionsRequest request = buildAddRequest(adGroupId,
                new KeywordAddItem().withPhrase(NEW_PHRASE).withPrice(DEFAULT_PRICE).withPriceContext(DEFAULT_PRICE));

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        List<Keyword> keywords = keywordRepository.getKeywordsByAdGroupId(shard, adGroupId);
        assumeThat("в базе дожна быть только одна фраза", keywords, hasSize(1));

        assertFalse(response.isSuccessful());
        assertThat(response.getItems(),
                beanDiffer(singletonMap(adGroupId, new ResponseItem()
                        .withBanners(expectedBanners)
                        .withIsGroupOversized(1)
                        .withPhrasesExceedsLimitQuantity(1))).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void update_AddKeywordDuplicateExisting_Success() {
        String duplicatePhrase = "duplicate phrase";
        Keyword keyword = steps.keywordSteps().createKeyword(adGroupInfo, defaultKeyword().withPhrase(duplicatePhrase))
                .getKeyword();
        ShowConditionsRequest request = buildAddRequest(adGroupId,
                new KeywordAddItem().withPhrase(duplicatePhrase).withPrice(DEFAULT_PRICE)
                        .withPriceContext(DEFAULT_PRICE));

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertTrue(response.isSuccessful());
        assertThat(response.getItems(),
                beanDiffer(singletonMap(adGroupId, new ResponseItem()
                        .withBanners(expectedBanners)
                        .withKeywords(singletonMap(keyword.getId(),
                                new KeywordItemResponse()
                                        .withPhrase(keyword.getPhrase())
                                        .withDuplicate(1))))).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void update_UpdateKeyword_Success() {
        String newPhrase = "new phrase";
        ShowConditionsRequest request =
                buildEditRequest(adGroupId, keywordId, new KeywordUpdateItem().withPhrase(newPhrase));

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertTrue(response.isSuccessful());
        assertThat(response.getItems(), beanDiffer(singletonMap(adGroupId, new ResponseItem()
                .withBanners(expectedBanners)
                .withKeywords(singletonMap(keywordId, new KeywordItemResponse()
                        .withNormPhrase(newPhrase)
                        .withWordsCount(2)
                        .withPhraseId(BigInteger.ZERO.toString())
                        .withPrice(INITIAL_PRICE.toString())
                        .withPriceContext(INITIAL_PRICE.toString())
                        .withId(keywordId)
                        .withPhrase(newPhrase)
                        .withDuplicate(0)
                        .withIsSuspended(0)
                        .withPriceForCoverage(expectedPriceForCoverage)
                        .withShows(100L)
                        .withClicks(2L)
                        .withCtr(BigDecimal.valueOf(2.00).setScale(2, BigDecimal.ROUND_UNNECESSARY).toString())))))
                .useCompareStrategy(COMPARE_STRATEGY));

        Map<Integer, TrafficVolumeItem> trafficVolumeItems =
                response.getItems().get(adGroupId).getKeywords().get(keywordId).getTrafficVolume();
        assertInterpolatedTrafficVolume(trafficVolumeItems);
    }

    @Test
    public void update_UpdateKeywordDuplicateExisting_Success() {
        String duplicatePhrase = "duplicate phrase";
        Keyword keyword = steps.keywordSteps().createKeyword(adGroupInfo, defaultKeyword().withPhrase(duplicatePhrase))
                .getKeyword();
        ShowConditionsRequest request =
                buildEditRequest(adGroupId, keywordId, new KeywordUpdateItem().withPhrase(duplicatePhrase));

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertTrue(response.isSuccessful());
        assertThat(response.getItems(), beanDiffer(singletonMap(adGroupId, new ResponseItem()
                .withBanners(expectedBanners)
                .withKeywords(singletonMap(keywordId, new KeywordItemResponse()
                        .withId(keyword.getId())
                        .withPhrase(null)
                        .withDuplicate(1))))).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void update_AddInArchivedCampaign_CampaignStatusArchivedOnDelete() {
        archiveCampaign(campaignId, shard);
        ShowConditionsRequest request =
                buildAddRequest(adGroupId, new KeywordAddItem().withPhrase(NEW_PHRASE).withPrice(DEFAULT_PRICE));

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertThat(response.getItems().get(adGroupId).getErrors(), notNullValue());
        assertThat(response.getItems().get(adGroupId).getErrorsByPhrases(), not(emptyMap()));
    }

    @Test
    public void update_SuspendInArchivedCampaign_CampaignStatusArchivedOnSuspend() {
        archiveCampaign(campaignId, shard);
        ShowConditionsRequest request = buildEditRequest(adGroupId, keywordId,
                new KeywordUpdateItem().withSuspended(1));

        updateAndAssertThatHasDefect(request, adGroupId);
    }

    @Test
    public void update_AddKeywordInArchivedCampaign_RequestFailed() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(adGroupInfo.getCampaignInfo());
        adGroupId = bannerInfo.getAdGroupId();

        archiveCampaign(campaignId, shard);

        ShowConditionsRequest request = buildAddRequest(adGroupId,
                new KeywordAddItem().withPhrase(NEW_PHRASE).withPrice(DEFAULT_PRICE).withPriceContext(DEFAULT_PRICE));

        ShowConditionsResponse response =
                (ShowConditionsResponse) showConditionsController.update(request, uid, clientId);

        assertResponseHasErrors(response);
    }

    @Test
    public void update_UpdateKeywordInArchivedCampaign_RequestFailed() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(adGroupInfo.getCampaignInfo());
        adGroupId = bannerInfo.getAdGroupId();

        archiveCampaign(campaignId, shard);

        ShowConditionsRequest request =
                buildEditRequest(adGroupId, keywordId, new KeywordUpdateItem().withPrice(BigDecimal.TEN));

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertResponseHasErrors(response);
    }

    /*
    @Test
    public void update_UpdateKeywordInArchivedCampaign_RequestFailed() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(adGroupInfo.getCampaignInfo());
        Long adGroupId = bannerInfo.getAdGroupId();

        archiveCampaign(campaignId, shard);

        ShowConditionsRequest request =
                buildEditRequest(adGroupId, keywordId, new KeywordUpdateItem().withPrice(BigDecimal.TEN));

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertResponseHasErrors(response);
    }
    */

    @Test
    public void update_DeleteInArchivedCampaign_CampaignStatusArchivedOnDelete() {
        archiveCampaign(campaignId, shard);
        ShowConditionsRequest request = buildDeleteRequest(adGroupId, keywordId);

        updateAndAssertThatHasDefect(request, adGroupId);
    }

    @Test
    public void update_DeleteAndUpdateSameKeyword_RequestFailed() {
        ShowConditionsRequest request = buildEditRequest(adGroupId, keywordId, new KeywordUpdateItem());
        request.getKeywords().get(adGroupId).getDeleted().add(keywordId);

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertThat("ожидается ошибка верхнего уровня: некорректные параметры удаления и обновления КФ",
                response.getErrors(), not(empty()));
    }


    private ShowConditionsRequest buildAddRequest(Long adGroupId, KeywordAddItem item) {
        ShowConditionsRequest request = new ShowConditionsRequest();

        KeywordModificationContainer container = new KeywordModificationContainer();
        container.getAdded().add(item);
        request.getKeywords().put(adGroupId, container);

        return request;
    }

    private ShowConditionsRequest buildEditRequest(Long adGroupId, Long keywordId, KeywordUpdateItem item) {
        ShowConditionsRequest request = new ShowConditionsRequest();

        KeywordModificationContainer container = new KeywordModificationContainer();
        container.getUpdated().put(keywordId, item);
        request.getKeywords().put(adGroupId, container);

        return request;
    }

    private ShowConditionsRequest buildDeleteRequest(Long adGroupId, Long keywordId) {
        ShowConditionsRequest request = new ShowConditionsRequest();

        KeywordModificationContainer container = new KeywordModificationContainer();
        container.getDeleted().add(keywordId);
        request.getKeywords().put(adGroupId, container);

        return request;
    }

    private void updateAndAssertThatHasDefect(ShowConditionsRequest request, Long adGroupId) {
        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertThat(response.getItems().get(adGroupId).getErrors(), notNullValue());
        assertThat(response.getItems().get(adGroupId).getErrors(), not(empty()));
    }

    private void assertInterpolatedTrafficVolume(Map<Integer, TrafficVolumeItem> trafficVolumeItems) {
        assertFalse(trafficVolumeItems.isEmpty());

        int maxPosition = Collections.max(trafficVolumeItems.keySet());
        trafficVolumeItems.forEach((position, item) -> {
            if (PRESET_TRAFFIC_VOLUME_POSITIONS.contains(position) || position == maxPosition) {
                assertThat(item.getShowInTable(), is(1));
            } else {
                assertNull(item.getShowInTable());
            }

            BigDecimal bid = BigDecimal.valueOf(item.getBidPrice()).movePointLeft(6);
            if (bid.compareTo(currency.getCurrency().getMaxShowBid()) > 0) {
                assertThat(item.getDontShowBidPrice(), is(1));
            } else {
                assertNull(item.getDontShowBidPrice());
            }
        });
    }

    private void assertResponseHasErrors(ShowConditionsResponse response) {
        boolean hasErrors = !response.getErrors().isEmpty()
                || response.getItems().values().stream().anyMatch(i -> !i.getErrors().isEmpty())
                || response.getItems().values().stream().anyMatch(i -> !i.getErrorsByPhrases().isEmpty());

        assertThat(hasErrors, is(true));
    }

    private void archiveCampaign(long campaignId, int shard) {
        campaignRepository.archiveCampaign(shard, campaignId);
    }
}
