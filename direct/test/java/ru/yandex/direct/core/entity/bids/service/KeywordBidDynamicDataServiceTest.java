package ru.yandex.direct.core.entity.bids.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;

import one.util.streamex.StreamEx;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.advq.AdvqClient;
import ru.yandex.direct.advq.SearchKeywordResult;
import ru.yandex.direct.advq.SearchRequest;
import ru.yandex.direct.advq.search.SearchItem;
import ru.yandex.direct.advq.search.Statistics;
import ru.yandex.direct.bsauction.BsRequest;
import ru.yandex.direct.bsauction.BsRequestPhrase;
import ru.yandex.direct.bsauction.BsResponse;
import ru.yandex.direct.bsauction.BsTrafaretClient;
import ru.yandex.direct.bsauction.FullBsTrafaretResponsePhrase;
import ru.yandex.direct.bsauction.PositionalBsTrafaretResponsePhrase;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.service.MinusKeywordPreparingTool;
import ru.yandex.direct.core.entity.auction.container.BsRequestPhraseWrapper;
import ru.yandex.direct.core.entity.auction.container.bs.KeywordBidBsAuctionData;
import ru.yandex.direct.core.entity.auction.container.bs.KeywordTrafaretData;
import ru.yandex.direct.core.entity.bids.container.CompleteBidData;
import ru.yandex.direct.core.entity.bids.container.KeywordBidDynamicData;
import ru.yandex.direct.core.entity.bids.container.ShowConditionType;
import ru.yandex.direct.core.entity.bids.model.Bid;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.pokazometer.GroupRequest;
import ru.yandex.direct.pokazometer.GroupResponse;
import ru.yandex.direct.pokazometer.PhraseResponse;
import ru.yandex.direct.pokazometer.PokazometerClient;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBsResponses.defaultPhraseResponse;
import static ru.yandex.direct.core.testing.data.TestBsResponses.defaultTrafaretResponsePhrase;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.privateMinusKeywordsPack;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.BS_TRAFARET_AUCTION_CLIENT;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordBidDynamicDataServiceTest {
    private static final CurrencyCode CURRENCY_CODE = CurrencyCode.RUB;

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
    @Autowired
    private Steps steps;
    @Autowired
    private PokazometerClient pokazometerClient;
    @Autowired
    @Qualifier(BS_TRAFARET_AUCTION_CLIENT)
    private BsTrafaretClient bsTrafaretClient;
    @Autowired
    private AdvqClient advqClient;
    @Autowired
    private KeywordBidDynamicDataService keywordBidDynamicDataService;
    @Autowired
    private MinusKeywordPreparingTool minusKeywordPreparingTool;
    private KeywordInfo keywordInfo;
    private KeywordInfo keywordWithoutBannerInfo;

    private AdGroupInfo adGroupWithLibraryMinusKeywords;
    private KeywordInfo adGroupKeyword;
    private List<String> allMinusKeywords;

    private static IdentityHashMap<BsRequest<BsRequestPhrase>, BsResponse<BsRequestPhrase,
            PositionalBsTrafaretResponsePhrase>> generateDefaultBsAuctionResponse(
            List<BsRequest<BsRequestPhrase>> requests) {
        return StreamEx.of(requests)
                .mapToEntry(r -> {
                    IdentityHashMap<BsRequestPhrase, PositionalBsTrafaretResponsePhrase> successResult =
                            StreamEx.of(r.getPhrases())
                                    .mapToEntry(phr -> defaultPhraseResponse(CURRENCY_CODE, phr))
                                    .toCustomMap(IdentityHashMap::new);
                    return BsResponse.success(successResult);
                })
                .toCustomMap(IdentityHashMap::new);
    }

    private static IdentityHashMap<BsRequest<BsRequestPhrase>, BsResponse<BsRequestPhrase,
            FullBsTrafaretResponsePhrase>>
    generateDefaultBsTrafaretResponse(List<BsRequest<BsRequestPhrase>> requests) {
        return StreamEx.of(requests)
                .mapToEntry(r -> {
                    IdentityHashMap<BsRequestPhrase, FullBsTrafaretResponsePhrase> successResult =
                            StreamEx.of(r.getPhrases())
                                    .mapToEntry(phr -> defaultTrafaretResponsePhrase(CURRENCY_CODE, phr))
                                    .toCustomMap(IdentityHashMap::new);
                    return BsResponse.success(successResult);
                })
                .toCustomMap(IdentityHashMap::new);
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        keywordInfo = steps.keywordSteps().createDefaultKeyword();
        steps.bannerSteps().createBanner(activeTextBanner(keywordInfo.getCampaignId(), keywordInfo.getAdGroupId()),
                keywordInfo.getAdGroupInfo());

        keywordWithoutBannerInfo = steps.keywordSteps().createDefaultKeyword();

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        MinusKeywordsPackInfo libraryMinusKeywordsPack =
                steps.minusKeywordsPackSteps().createMinusKeywordsPack(libraryMinusKeywordsPack()
                        .withMinusKeywords(asList("abracadabra", "kkkkk", "word 2")), clientInfo);
        MinusKeywordsPackInfo privateMinusKeywordsPack =
                steps.minusKeywordsPackSteps().createMinusKeywordsPack(privateMinusKeywordsPack()
                        .withMinusKeywords(asList("bararara", "cdcdcd", "word 1")), clientInfo);

        adGroupWithLibraryMinusKeywords = steps.adGroupSteps().createAdGroup(TestGroups.activeTextAdGroup()
                        .withMinusKeywordsId(privateMinusKeywordsPack.getMinusKeywordPackId())
                        .withMinusKeywords(privateMinusKeywordsPack.getMinusKeywordsPack().getMinusKeywords())
                        .withLibraryMinusKeywordsIds(singletonList(libraryMinusKeywordsPack.getMinusKeywordPackId())),
                clientInfo);
        steps.bannerSteps().createActiveTextBanner(adGroupWithLibraryMinusKeywords);
        adGroupKeyword = steps.keywordSteps().createKeyword(adGroupWithLibraryMinusKeywords);

        allMinusKeywords = minusKeywordPreparingTool
                .mergePrivateAndLibrary(privateMinusKeywordsPack.getMinusKeywordsPack().getMinusKeywords(),
                        singletonList(libraryMinusKeywordsPack.getMinusKeywordsPack().getMinusKeywords()));

        when(pokazometerClient.get(anyList())).thenAnswer(
                invocation -> {
                    List<GroupRequest> groups = invocation.getArgument(0);
                    return StreamEx.of(groups)
                            .mapToEntry(g -> GroupResponse.success(mapList(g.getPhrases(), PhraseResponse::on)))
                            .toCustomMap(IdentityHashMap::new);
                }
        );
        when(bsTrafaretClient.getAuctionResults(anyList())).thenAnswer(
                invocation -> {
                    List<BsRequest<BsRequestPhrase>> requests = invocation.getArgument(0);
                    return generateDefaultBsAuctionResponse(requests);
                }
        );
        when(bsTrafaretClient.getAuctionResultsWithPositionCtrCorrection(anyList())).thenAnswer(
                invocation -> {
                    List<BsRequest<BsRequestPhrase>> requests = invocation.getArgument(0);
                    return generateDefaultBsTrafaretResponse(requests);
                }
        );
        when(advqClient.search(anyCollection(), any(), any())).thenAnswer(
                invocation -> {
                    Collection<SearchRequest> requests = invocation.getArgument(0);
                    return StreamEx.of(requests)
                            .mapToEntry(r -> StreamEx.of(r.getKeywords())
                                    .mapToEntry(keyword -> SearchKeywordResult.success(
                                            new SearchItem()
                                                    .withReq(keyword.getPhrase())
                                                    .withStat(new Statistics().withTotalCount(100L))))
                                    .toMap()
                            )
                            .toCustomMap(IdentityHashMap::new);
                }
        );
    }

    private Bid keywordToBid(Keyword keyword) {
        return new Bid()
                .withId(keyword.getId())
                .withType(ShowConditionType.KEYWORD)
                .withAdGroupId(keyword.getAdGroupId())
                .withCampaignId(keyword.getCampaignId())
                .withPrice(BigDecimal.ZERO)
                .withPriceContext(BigDecimal.ZERO);
    }

    @Test
    public void getBsResults_success() {
        Collection<CompleteBidData<KeywordBidBsAuctionData>> bidData =
                keywordBidDynamicDataService.getCompleteBidData(keywordInfo.getAdGroupInfo().getClientId(),
                        singletonList(keywordToBid(keywordInfo.getKeyword())), true, true, true);

        softly.assertThat(bidData)
                .singleElement()
                .extracting(CompleteBidData::getDynamicData)
                .extracting(KeywordBidDynamicData::getBsAuctionData, KeywordBidDynamicData::getPokazometerData)
                .doesNotContainNull();
    }

    @Test
    public void getBsResults_notCallBsAuction_whenKeywordHasNoBanner() {
        Collection<CompleteBidData<KeywordBidBsAuctionData>> bidData =
                keywordBidDynamicDataService.getCompleteBidData(keywordWithoutBannerInfo.getAdGroupInfo().getClientId(),
                        singletonList(keywordToBid(keywordWithoutBannerInfo.getKeyword())), true, true,
                        true);

        softly.assertThat(bidData)
                .singleElement()
                .extracting(CompleteBidData::getDynamicData)
                .extracting(KeywordBidDynamicData::getBsAuctionData, KeywordBidDynamicData::getPokazometerData)
                .containsOnlyNulls();
    }

    @Test
    public void getBsTrafaretResults_success() {
        Collection<CompleteBidData<KeywordTrafaretData>> bidData =
                keywordBidDynamicDataService
                        .getCompleteBidDataTrafaretFormat(keywordInfo.getAdGroupInfo().getClientId(),
                                singletonList(keywordToBid(keywordInfo.getKeyword())), true, true, true);
        softly.assertThat(bidData)
                .singleElement()
                .extracting(CompleteBidData::getDynamicData)
                .extracting(KeywordBidDynamicData::getBsAuctionData, KeywordBidDynamicData::getPokazometerData)
                .doesNotContainNull();
    }

    @Test
    // возможно тут проверяется устаревшее поведение (до трафаретных торгов)
    public void getCompleteBidData_AdGroupWithLibraryMinusKeywords_MinusKeywordsMerged() {
        keywordBidDynamicDataService.getCompleteBidData(adGroupWithLibraryMinusKeywords.getClientId(),
                singletonList(keywordToBid(adGroupKeyword.getKeyword())), true, true, true);
        ArgumentCaptor<List<BsRequest<BsRequestPhraseWrapper>>> bsRequestCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(bsTrafaretClient, atLeastOnce()).getAuctionResults(bsRequestCaptor.capture());

        List<BsRequest<BsRequestPhraseWrapper>> bsClientArgument = bsRequestCaptor.getValue();

        AdGroup expected = new AdGroup().withMinusKeywords(allMinusKeywords);

        assertThat(bsClientArgument)
                .singleElement()
                .extracting(BsRequest::getPhrases)
                .asInstanceOf(InstanceOfAssertFactories.list(BsRequestPhraseWrapper.class))
                .singleElement()
                .extracting(r -> r.getAdGroupForAuction().getAdGroup())
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(expected);
    }

    @Test
    public void getCompleteBidDataTrafaretFormat_AdGroupWithLibraryMinusKeywords_MinusKeywordsMerged() {
        keywordBidDynamicDataService.getCompleteBidDataTrafaretFormat(adGroupWithLibraryMinusKeywords.getClientId(),
                singletonList(keywordToBid(adGroupKeyword.getKeyword())), true, true, true);
        ArgumentCaptor<List<BsRequest<BsRequestPhraseWrapper>>> bsRequestCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(bsTrafaretClient, atLeastOnce()).getAuctionResultsWithPositionCtrCorrection(bsRequestCaptor.capture());

        List<BsRequest<BsRequestPhraseWrapper>> bsClientArgument = bsRequestCaptor.getValue();

        AdGroup expected = new AdGroup().withMinusKeywords(allMinusKeywords);

        assertThat(bsClientArgument)
                .singleElement()
                .extracting(BsRequest::getPhrases)
                .asInstanceOf(InstanceOfAssertFactories.list(BsRequestPhraseWrapper.class))
                .singleElement()
                .extracting(r -> r.getAdGroupForAuction().getAdGroup())
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(expected);
    }

}
