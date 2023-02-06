package ru.yandex.market.partner.auction.servantlet.serialization;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.xml.sax.SAXException;

import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionBidStatus;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionNAReason;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.core.auction.model.WithCount;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.partner.auction.AuctionBulkCommon;
import ru.yandex.market.partner.auction.BulkReadQueryType;
import ru.yandex.market.partner.auction.LightOfferExistenceChecker;
import ru.yandex.market.partner.auction.ReportRecommendationService;
import ru.yandex.market.partner.auction.servantlet.bulk.PartiallyRecommendatorsFactory;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_111;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_333;

@ExtendWith(MockitoExtension.class)
@DisplayName("Структура/формат ответа auctionBulkOfferBids")
class AnswerStructureSerialisationFunctionalTest extends FunctionalTest {

    //todo убрать из игнора дату
    private static final Set<String> IGNORE_RESPONSE_ATTRIBUTES = ImmutableSet.of("host", "executing-time",
            "last-modified-date");

    private static final Logger LOG = LoggerFactory.getLogger(AnswerStructureSerialisationFunctionalTest.class);
    private static final long SHOP_ID_774 = 774;
    private static final long CAMPAIGN_ID_10774 = 10774;
    private static final long GROUP_ID_0 = 0;
    private static final String SOME_OFFER_NAME = "someOfferName";
    private static final String SOME_OFFER_ID = "123456789";
    private static final String SOME_SEARCH_QUERY = "someSearchQuery";
    private static final String RESOURCE_PATH = "./resources/";
    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final AuctionOfferId SOME_OFFER_TITLE_ID = new AuctionOfferId(SOME_OFFER_NAME);
    private static final AuctionOfferId SOME_OFFER_FEED_ID = new AuctionOfferId(200304546L, SOME_OFFER_ID);
    @Autowired
    private AuctionService auctionService;
    @Autowired
    private ReportRecommendationService reportRecommendationService;
    @Autowired
    @Qualifier(value = "marketReportService")
    private AsyncMarketReportService marketReportService;
    @Mock
    private LightOfferExistenceChecker offerExistenceChecker;

    /**
     * В рамках тестов данные компонент для {@link BidPlace#SEARCH} и {@link BidPlace#CARD} имеют разные значения,
     * но в реалиях эти значения синхронизованы и всегда используется {@link BidPlace#SEARCH}.
     * Две компоненты остаются до того как в репорте и индексаторе перейдут к одному значению.
     */
    private static Stream<Arguments> byGroupIdTestCases() {
        return Stream.of(
                of(
                        "HYBRID_REC_GROUP",
                        "&type=" + BulkReadQueryType.HYBRID_REC_GROUP,
                        "by-group-card.xml",
                        AuctionBulkCommon.OFFER_EXISTS
                ),
                of(
                        "HYBRID_REC_GROUP (no model card)",
                        "&type=" + BulkReadQueryType.HYBRID_REC_GROUP,
                        "by-group-card-no-model-card.xml",
                        AuctionBulkCommon.OFFER_EXISTS_NO_CARD
                ),
                of(
                        "HYBRID_REC_GROUP (offer not found)",
                        "&type=" + BulkReadQueryType.HYBRID_REC_GROUP,
                        "by-group-card-no-offer-found.xml",
                        AuctionBulkCommon.OFFER_NOT_FOUND
                ),
                of(
                        "PARALLEL_SEARCH_GROUP",
                        "&type=" + BulkReadQueryType.PARALLEL_SEARCH_GROUP,
                        "by-group-parallel-search.xml",
                        AuctionBulkCommon.OFFER_EXISTS
                ),
                of(
                        "MARKET_SEARCH_GROUP",
                        "&type=" + BulkReadQueryType.MARKET_SEARCH_GROUP,
                        "by-group-market-search.xml",
                        AuctionBulkCommon.OFFER_EXISTS
                )
        );
    }

    private static Stream<Arguments> byOfferTestCases() {
        return Stream.of(
                of(
                        "PARALLEL_SEARCH_REC_OFFER",
                        "&type=" + BulkReadQueryType.PARALLEL_SEARCH_REC_OFFER +
                                "&req.size=1" +
                                "&req1.offerName=" + SOME_OFFER_NAME +
                                "&req1.searchQuery=" + SOME_SEARCH_QUERY,
                        "by-offer-parallel-search.xml",
                        AuctionBulkCommon.OFFER_EXISTS
                ),
                of(
                        "MARKET_SEARCH_REC_OFFER",
                        "&type=" + BulkReadQueryType.MARKET_SEARCH_REC_OFFER +
                                "&req.size=1" +
                                "&req1.offerName=" + SOME_OFFER_NAME +
                                "&req1.searchQuery=" + SOME_SEARCH_QUERY,
                        "by-offer-market-search.xml",
                        AuctionBulkCommon.OFFER_EXISTS
                ),
                of(
                        "HYBRID_REC_OFFER",
                        "&type=" + BulkReadQueryType.HYBRID_REC_OFFER +
                                "&req.size=1" +
                                "&req1.offerName=" + SOME_OFFER_NAME +
                                "&req1.fee.value=123" +
                                "&req1.linkType=" + AuctionBidComponentsLink.CARD_NO_LINK_CBID_PRIORITY,
                        "by-offer-card.xml",
                        AuctionBulkCommon.OFFER_EXISTS
                ),
                of(
                        "HYBRID_REC_OFFER (no model card)",
                        "&type=" + BulkReadQueryType.HYBRID_REC_OFFER +
                                "&req.size=1" +
                                "&req1.offerName=" + SOME_OFFER_NAME +
                                "&req1.fee.value=123" +
                                "&req1.linkType=" + AuctionBidComponentsLink.CARD_NO_LINK_CBID_PRIORITY,
                        "by-offer-card-no-model-card.xml",
                        AuctionBulkCommon.OFFER_EXISTS_NO_CARD
                ),
                of(
                        "HYBRID_REC_OFFER (no offer found)",
                        "&type=" + BulkReadQueryType.HYBRID_REC_OFFER +
                                "&req.size=1" +
                                "&req1.offerName=" + SOME_OFFER_NAME +
                                "&req1.fee.value=123" +
                                "&req1.linkType=" + AuctionBidComponentsLink.CARD_NO_LINK_CBID_PRIORITY,
                        "by-offer-card-no-offer-found.xml",
                        AuctionBulkCommon.OFFER_NOT_FOUND
                ),
                of(
                        "HYBRID_REC_OFFER no passed link type",
                        "&type=" + BulkReadQueryType.HYBRID_REC_OFFER +
                                "&req.size=1" +
                                "&req1.offerName=" + SOME_OFFER_NAME +
                                "&req1.fee.value=123" +
                                "&unified=true",
                        "by-offer-card_cpc_only.xml",
                        AuctionBulkCommon.OFFER_EXISTS
                )
        );
    }

    @BeforeEach
    void beforeEach() throws IOException, SAXException, ParseException {
        mockAuction(AuctionOfferIdType.TITLE, SOME_OFFER_TITLE_ID);
        mockCheckHomeRegionInIndex();

        PartiallyRecommendatorsFactory.mockAsyncServiceForCard(
                this.getClass().getResourceAsStream(RESOURCE_PATH + "card_ok.xml"),
                marketReportService

        );
        reportRecommendationService.setParallelSearchBidRecommendator(
                PartiallyRecommendatorsFactory.buildParallelSearchRecommendator(
                        this.getClass().getResourceAsStream(RESOURCE_PATH + "parallel_search_ok.xml")
                )
        );
        reportRecommendationService.setMarketSearchBidRecommendator(
                PartiallyRecommendatorsFactory.buildMarketSearchRecommendator(
                        this.getClass().getResourceAsStream(RESOURCE_PATH + "market_search_ok.xml")
                )
        );

        reportRecommendationService.setOfferExistenceService(offerExistenceChecker);

        PartiallyRecommendatorsFactory.mockAsyncServiceForPrime(
                this.getClass().getResourceAsStream(RESOURCE_PATH + "prime-casio.json"),
                marketReportService,
                "casio"
        );
    }

    @DisplayName("Запросы по ид группы")
    @ParameterizedTest(name = "{0}")
    @MethodSource("byGroupIdTestCases")
    @DbUnitDataSet(before = "db/GetBidsStructure.before.csv")
    void test_serialisationStructureByGroupId(String desc,
                                              String urlParams,
                                              String expectedResponseFileName,
                                              LightOfferExistenceChecker.LightOfferInfo lightOfferInfo
    ) {
        when(offerExistenceChecker.getOfferInfo(any())).
                thenReturn(CompletableFuture.completedFuture(lightOfferInfo));

        final String url = baseUrl + "/auctionBulkOfferBids?id=" + CAMPAIGN_ID_10774 + urlParams;
        final ResponseEntity<String> actualResponse = FunctionalTestHelper.get(url);

        assertResponseVsExpectedFile(expectedResponseFileName, actualResponse);
    }

    @DisplayName("Запросы по ид оффера")
    @ParameterizedTest(name = "{0}")
    @MethodSource("byOfferTestCases")
    @DbUnitDataSet(before = "db/GetBidsStructure.before.csv")
    void test_serialisationStructureByOffer(String desc,
                                            String urlParams,
                                            String expectedResponseFileName,
                                            LightOfferExistenceChecker.LightOfferInfo lightOfferInfo
    ) {
        when(offerExistenceChecker.getOfferInfo(any())).
                thenReturn(CompletableFuture.completedFuture(lightOfferInfo));

        final String url = baseUrl + "/auctionBulkOfferBids?id=" + CAMPAIGN_ID_10774 + urlParams;
        final ResponseEntity<String> actualResponse = FunctionalTestHelper.get(url);

        assertResponseVsExpectedFile(expectedResponseFileName, actualResponse);
    }

    @DisplayName("Перечень title-идентификаторов предложений в группе (type=NAMES)")
    @Test
    @DbUnitDataSet(before = {"db/GetBidsStructure.before.csv"})
    void test_groupOffersByTitle() {
        when(auctionService.getAuctionOfferIdType(SHOP_ID_774))
                .thenReturn(AuctionOfferIdType.TITLE);
        when(auctionService.getGroupOffers(anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(new WithCount<>(2, ImmutableList.of("abc", "def")));


        final String url = baseUrl + "/auctionBulkOfferBids?id=" + CAMPAIGN_ID_10774;
        final ResponseEntity<String> actualResponse = FunctionalTestHelper.get(url);

        assertResponseVsExpectedFile("names-by-title.xml", actualResponse);
    }

    @DisplayName("Перечень feed-offer-id-идентификаторов предложений в группе (type=NAMES)")
    @Test
    @DbUnitDataSet(before = {"db/GetBidsStructure.before.csv"})
    void test_groupOffersByFeedOfferId() {
        when(auctionService.getAuctionOfferIdType(SHOP_ID_774))
                .thenReturn(AuctionOfferIdType.SHOP_OFFER_ID);

        when(auctionService.getGroupOfferIds(anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(
                        new WithCount<>(
                                2,
                                ImmutableList.of(
                                        new FeedOfferId("someStringId", 1001L),
                                        new FeedOfferId("4567", 1002L)
                                )
                        )
                );

        final String url = baseUrl + "/auctionBulkOfferBids?id=" + CAMPAIGN_ID_10774;
        final ResponseEntity<String> actualResponse = FunctionalTestHelper.get(url);

        assertResponseVsExpectedFile("names-by-feed-offer-id.xml", actualResponse);
    }


    @DisplayName("Поисковая выдача для магазина с идентификацией по TITLE")
    @Test
    @DbUnitDataSet(before = "db/GetBidsStructure.before.csv")
    void test_searchOffersForTitleShop() {
        when(offerExistenceChecker.getOfferInfo(any())).
                thenReturn(CompletableFuture.completedFuture(AuctionBulkCommon.OFFER_EXISTS));

        final String url = baseUrl + "/searchAuctionOffers?q=casio&id=" + CAMPAIGN_ID_10774;
        final ResponseEntity<String> actualResponse = FunctionalTestHelper.get(url);

        assertResponseVsExpectedFile("search-all-title.xml", actualResponse);
    }

    /**
     * Важный момент: вы выдаче для поисковго плейса репорта подкладывается содержимое с двумя офферами,
     * а в хранилище биддинга подкладывается существование только одного из них, чтобы в рамках одного теста была два
     * кейса.
     */
    @DisplayName("Поисковая выдача для магазина с идентификацией по ID")
    @Test
    @DbUnitDataSet(before = "db/GetBidsStructure.before.csv")
    void test_searchOffersForIdShop() throws ParseException {
        mockAuction(AuctionOfferIdType.SHOP_OFFER_ID, SOME_OFFER_FEED_ID);
        when(offerExistenceChecker.getOfferInfo(any())).
                thenReturn(CompletableFuture.completedFuture(AuctionBulkCommon.OFFER_EXISTS));
        mockExistingBidsForSearch(SHOP_ID_774, SOME_OFFER_FEED_ID);

        final String url = baseUrl + "/searchAuctionOffers?q=casio&id=" + CAMPAIGN_ID_10774;
        final ResponseEntity<String> actualResponse = FunctionalTestHelper.get(url);

        assertResponseVsExpectedFile("search-all-id.xml", actualResponse);
    }

    private void mockAuction(AuctionOfferIdType auctionOfferIdType, AuctionOfferId auctionOfferId)
            throws ParseException {
        Mockito.reset(auctionService);
        when(auctionService.canManageAuction(SHOP_ID_774))
                .thenReturn(AuctionNAReason.NONE);

        when(auctionService.getAuctionOfferIdType(SHOP_ID_774))
                .thenReturn(auctionOfferIdType);

        final AuctionOfferBid someExistingBid = new AuctionOfferBid(
                SHOP_ID_774,
                auctionOfferId,
                SOME_SEARCH_QUERY,
                GROUP_ID_0,
                Collections.emptyMap(),
                AuctionBidValues.fromSameBids(BID_CENTS_111).toBuilder().bid(BidPlace.CARD, BID_CENTS_333).build()
        );

        someExistingBid.setStatus(AuctionBidStatus.PUBLISHED);
        someExistingBid.setLinkType(AuctionBidComponentsLink.CARD_NO_LINK_FEE_PRIORITY);
        final Date someModificationDate = FORMAT.parse("2018-01-02 03:04:05");
        someExistingBid.setModifedDate(someModificationDate);

        final List<AuctionOfferBid> bids = new ArrayList<>();
        bids.add(someExistingBid);

        when(auctionService.getGroupBids(anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(new WithCount<>(bids.size(), bids));
    }

    private void assertResponseVsExpectedFile(String expectedResponseFile, ResponseEntity<String> response) {
        String expectedOutput = StringTestUtil.getString(this.getClass(), expectedResponseFile);
        String actualOutput = response.getBody();

        LOG.debug("Actual string: " + actualOutput);
        LOG.debug("Expected string: " + expectedOutput);

        MbiAsserts.assertXmlEquals(
                expectedOutput,
                actualOutput,
                IGNORE_RESPONSE_ATTRIBUTES
        );
    }

    protected void mockCheckHomeRegionInIndex() throws IOException {
        PartiallyRecommendatorsFactory.mockAsyncServiceForPrime(
                this.getClass().getResourceAsStream(RESOURCE_PATH + "prime-empty.json"),
                marketReportService,
                ""
        );
    }

    private void mockExistingBidsForSearch(long shopId, AuctionOfferId auctionOfferId) throws ParseException {
        final AuctionOfferBid someExistingBid = new AuctionOfferBid(
                SHOP_ID_774,
                auctionOfferId,
                SOME_SEARCH_QUERY,
                GROUP_ID_0,
                Collections.emptyMap(),
                AuctionBidValues.fromSameBids(BID_CENTS_111).toBuilder().bid(BidPlace.CARD, BID_CENTS_333).build()
        );

        someExistingBid.setStatus(AuctionBidStatus.PUBLISHED);
        someExistingBid.setLinkType(AuctionBidComponentsLink.CARD_NO_LINK_FEE_PRIORITY);
        final Date someModificationDate = FORMAT.parse("2018-01-02 03:04:05");
        someExistingBid.setModifedDate(someModificationDate);

        when(auctionService.getOfferBids(
                eq(shopId),
                Mockito.argThat(arg -> arg.contains(auctionOfferId)))
        ).thenReturn(ImmutableList.of(someExistingBid));
    }
}
