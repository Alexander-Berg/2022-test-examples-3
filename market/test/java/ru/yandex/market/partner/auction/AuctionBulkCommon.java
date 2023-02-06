package ru.yandex.market.partner.auction;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.market.common.parser.InputStreamParser;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.parser.xml.GeneralMarketReportXmlParserFactory;
import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.dto.report.ReportRecommendationsAnswerDto;
import ru.yandex.market.core.auction.dto.report.ReportRecommendationsAnswerOrError;
import ru.yandex.market.core.auction.marketreport.AuctionMarketSearchMarketReportXmlParser;
import ru.yandex.market.core.auction.marketreport.AuctionMarketSearchMarketReportXmlParserSettings;
import ru.yandex.market.core.auction.marketreport.AuctionMarketSearchMarketReportXmlParserTest;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionBidGroup;
import ru.yandex.market.core.auction.model.AuctionBidStatus;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionBidValuesLimits;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.core.auction.recommend.BidRecommendationRequest;
import ru.yandex.market.core.auction.recommend.BidRecommendations;
import ru.yandex.market.core.auction.recommend.BidRecommendatorImpl;
import ru.yandex.market.core.auction.recommend.ParallelSearchBidRecommendator;
import ru.yandex.market.core.auction.recommend.ParallelSearchRecommendationParser;
import ru.yandex.market.core.auction.recommend.ReportMarketSearchBidRecommendator;
import ru.yandex.market.core.geobase.model.Region;
import ru.yandex.market.core.report.parser.ReportResponseXmlParser;
import ru.yandex.market.mbi.report.DefaultMarketSearchService;
import ru.yandex.market.mbi.report.MarketSearchService;
import ru.yandex.market.partner.auction.BulkUpdateRequest.Builder;
import ru.yandex.market.partner.auction.request.AuctionBulkRequest;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_FEE_PRIORITY;
import static ru.yandex.market.core.auction.model.AuctionGoalPlace.FIRST_PAGE;
import static ru.yandex.market.core.auction.model.AuctionGoalPlace.FIRST_PLACE;
import static ru.yandex.market.core.auction.model.AuctionGoalPlace.PREMIUM;
import static ru.yandex.market.core.auction.model.AuctionGoalPlace.PREMIUM_FIRST_PLACE;
import static ru.yandex.market.partner.auction.HybridGoal.GoalType.HYBRID_CARD;
import static ru.yandex.market.partner.auction.HybridGoal.GoalType.MARKET_SEARCH;
import static ru.yandex.market.partner.auction.HybridGoal.GoalType.PARALLEL_SEARCH;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasBidUpdateCount;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasFinalBidUpdateCount;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasFoundBidCount;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasValidBidCount;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasWarnings;

/**
 * @author vbudnev
 */
public final class AuctionBulkCommon {

    public static final HybridGoal HYBRID_CARD_PREM_FIRST_PLACE = new HybridGoal(HYBRID_CARD, PREMIUM_FIRST_PLACE);
    public static final HybridGoal HYBRID_CARD_PREM_FIRST_PLACE_TIED = new HybridGoal(HYBRID_CARD, PREMIUM_FIRST_PLACE, true);
    public static final HybridGoal HYBRID_CARD_PREM_PAGE = new HybridGoal(HYBRID_CARD, PREMIUM);
    public static final HybridGoal HYBRID_CARD_FIRST_PAGE = new HybridGoal(HYBRID_CARD, FIRST_PAGE);
    //первое место в блоке цен пока не используется, так как нет информации из репорта, но в ближ время обещали снабдить
    public static final HybridGoal HYBRID_CARD_FIRST_PLACE = new HybridGoal(HYBRID_CARD, FIRST_PLACE);
    public static final HybridGoal PARALLEL_SEARCH_FIRST_PAGE = new HybridGoal(PARALLEL_SEARCH, FIRST_PAGE);
    public static final HybridGoal PARALLEL_SEARCH_FIRST_PAGE_TIED = new HybridGoal(PARALLEL_SEARCH, FIRST_PAGE, true);
    public static final HybridGoal PARALLEL_SEARCH_FIRST_PLACE = new HybridGoal(PARALLEL_SEARCH, FIRST_PLACE);
    public static final HybridGoal MARKET_SEARCH_FIRST_PAGE = new HybridGoal(MARKET_SEARCH, FIRST_PAGE);
    public static final HybridGoal MARKET_SEARCH_FIRST_PLACE = new HybridGoal(MARKET_SEARCH, FIRST_PLACE);
    public static final HybridGoal MARKET_SEARCH_FIRST_PAGE_TIED = new HybridGoal(MARKET_SEARCH, FIRST_PAGE, true);


    public static final long GROUP_ID_0_DEFAULT = 0L;
    public static final long GROUP_ID_1 = 1L;
    public static final long GROUP_ID_2 = 2L;
    public static final long GROUP_ID_3 = 3L;
    public static final long GROUP_ID_4 = 4L;
    public static final long SHOP_ID_774 = 774L;
    public static final long SHOP_ID_100 = 100L;
    public static final String OFFER_NAME_1 = "offerName1";
    public static final String OFFER_NAME_2 = "offerName2";
    public static final String OFFER_NAME_3 = "offerName3";
    public static final String OFFER_NAME_4 = "offerName4";
    public static final String OFFER_NAME_5 = "offerName5";
    public static final String OFFER_NAME_6 = "offerName6";
    public static final String OFFER_NAME_7 = "offerName7";

    public static final BigInteger AUCTION_OFFER_BID_VALUE_1 = BigInteger.ONE;
    public static final BigInteger AUCTION_OFFER_BID_VALUE_2 = new BigInteger("2");
    public static final BigInteger AUCTION_OFFER_BID_VALUE_3 = new BigInteger("3");
    public static final BigInteger AUCTION_OFFER_BID_VALUE_29 = new BigInteger("29");
    public static final BigInteger AUCTION_OFFER_BID_VALUE_80 = new BigInteger("80");
    public static final BigInteger AUCTION_OFFER_BID_VALUE_200 = new BigInteger("200");
    public static final BigInteger AUCTION_OFFER_BID_VALUE_300 = new BigInteger("300");
    public static final BigInteger AUCTION_OFFER_BID_VALUE_574 = new BigInteger("574");
    public static final BigInteger AUCTION_OFFER_BID_VALUE_2000 = new BigInteger("2000");
    public static final Integer BID_CENTS_1 = 1;
    public static final Integer BID_CENTS_2 = 2;
    public static final Integer BID_CENTS_3 = 3;
    public static final Integer BID_CENTS_7 = 7;
    public static final Integer BID_CENTS_10 = 10;
    public static final Integer BID_CENTS_200 = 200;
    public static final Integer BID_CENTS_111 = 111;
    public static final Integer BID_CENTS_222 = 222;
    public static final Integer BID_CENTS_333 = 333;
    public static final Integer BID_CENTS_555 = 555;
    public static final Integer BID_CENTS_9999 = 9999;
    public static final Integer BID_CENTS_10000 = 10000;
    public static final Integer BID_CENTS_10100 = 10100;
    public static final AuctionOfferId SOME_TITLE_OFFER_ID = new AuctionOfferId("some irrelevant offer name");
    /**
     * Для тестов аналог {@link AuctionBidValues#KEEP_OLD_BID_VALUE} в виде int
     */
    public static final Integer BID_KEEP_OLD = -1;

    public static final BidReq BIDREQ_111 = BidReq.Builder.builder().withValue(BID_CENTS_111).build();
    public static final BidReq BIDREQ_222 = BidReq.Builder.builder().withValue(BID_CENTS_222).build();
    public static final BidReq BIDREQ_333 = BidReq.Builder.builder().withValue(BID_CENTS_333).build();


    public static final BigInteger LIMIT_VAL_1 = BigInteger.ONE;
    public static final BigInteger LIMIT_VAL_7 = BigInteger.valueOf(7);
    public static final BigInteger LIMIT_VAL_10 = BigInteger.TEN;
    public static final BigInteger LIMIT_VAL_200 = BigInteger.valueOf(200L);
    public static final BigInteger LIMIT_VAL_10000 = BigInteger.valueOf(10000L);
    public static final BigInteger LIMIT_VAL_9999 = BigInteger.valueOf(9999L);

    public static final Long SOME_MODEL_ID = 12345L;
    public static final LightOfferExistenceChecker.LightOfferInfo OFFER_EXISTS_NO_CARD
            = new LightOfferExistenceChecker.LightOfferInfo(OFFER_NAME_1, null);
    public static final LightOfferExistenceChecker.LightOfferInfo OFFER_EXISTS
            = new LightOfferExistenceChecker.LightOfferInfo(OFFER_NAME_1, SOME_MODEL_ID);
    public static final LightOfferExistenceChecker.LightOfferInfo OFFER_NOT_FOUND
            = new LightOfferExistenceChecker.LightOfferInfo();


    /**
     * Используется если в тесте не имеет значение, какая позиция для цели CARD
     */
    public static final HybridGoal CARD_IRRELEVANT_PLACE = new HybridGoal(HYBRID_CARD, FIRST_PAGE);
    /**
     * Используется если в тесте не имеет значение, какая позиция для цели PARALLEL_SEARCH
     */
    public static final HybridGoal PARALLEL_SEARCH_IRRELEVANT_PLACE = new HybridGoal(PARALLEL_SEARCH, FIRST_PLACE);
    /**
     * Используется если в тесте не имеет значение, какая позиция для цели MARKET_SEARCH
     */
    public static final HybridGoal MARKET_SEARCH_IRRELEVANT_PLACE = new HybridGoal(MARKET_SEARCH, FIRST_PLACE);

    public static final AuctionBulkRequest UPD_REQ_SHOP_774_GID_1 = createBulkServantletURequest(
            SHOP_ID_774,
            GROUP_ID_1
    );

    public static final AuctionBulkRequest UPD_REQ_SHOP_100_GID2 = createBulkServantletURequest(
            SHOP_ID_100,
            GROUP_ID_2
    );

    public static final String SOME_QUERY = "some_query";

    public static final AuctionBulkRequest UPD_REQ_SHOP_100_GID2_SOME_Q = createBulkServantletURequest(
            SHOP_ID_100,
            GROUP_ID_2,
            SOME_QUERY
    );

    public static final AuctionBulkRequest UPD_REQ_SHOP_774_GID_1_SOME_Q = createBulkServantletURequest(
            SHOP_ID_774,
            GROUP_ID_1,
            SOME_QUERY
    );

    public static final AuctionBidValues MIN_VALUES = new AuctionBidValues(
            ImmutableMap.of(
                    BidPlace.CARD, LIMIT_VAL_10,
                    BidPlace.SEARCH, LIMIT_VAL_7,
                    BidPlace.MARKET_SEARCH, LIMIT_VAL_7,
                    BidPlace.MARKET_PLACE, LIMIT_VAL_200
            )
    );

    public static final AuctionBidValues DEF_VALUES = new AuctionBidValues(
            ImmutableMap.of(
                    BidPlace.CARD, LIMIT_VAL_10,
                    BidPlace.SEARCH, LIMIT_VAL_10,
                    BidPlace.MARKET_SEARCH, LIMIT_VAL_10,
                    BidPlace.MARKET_PLACE, LIMIT_VAL_10
            )
    );

    public static final AuctionBidValues MAX_VALUES = new AuctionBidValues(
            ImmutableMap.of(
                    BidPlace.CARD, LIMIT_VAL_10000,
                    BidPlace.SEARCH, LIMIT_VAL_10000,
                    BidPlace.MARKET_SEARCH, LIMIT_VAL_10000,
                    BidPlace.MARKET_PLACE, LIMIT_VAL_9999
            )
    );

    public static final AuctionBidValuesLimits LIMITS = new AuctionBidValuesLimits(MIN_VALUES, MAX_VALUES, DEF_VALUES);


    public static final List<BulkUpdateRequest> TITLE_VALUE_UPDATE_REQUESTS = Arrays.asList(
            createCbidUReqByName(OFFER_NAME_1, BID_CENTS_111, CARD_NO_LINK_FEE_PRIORITY),
            createCbidUReqByName(OFFER_NAME_2, BID_CENTS_222, CARD_NO_LINK_FEE_PRIORITY),
            createCbidUReqByName(OFFER_NAME_3, BID_CENTS_333, CARD_NO_LINK_FEE_PRIORITY)
    );

    public static final List<BulkUpdateRequest> TITLE_VALUE_UPDATE_REQUESTS_WITH_2_BROKEN = Arrays.asList(
            createCbidUReqByName(null, BID_CENTS_222, CARD_NO_LINK_FEE_PRIORITY),
            createCbidUReqByName(OFFER_NAME_2, null, CARD_NO_LINK_FEE_PRIORITY)
    );

    public static final AuctionBidGroup GROUP_0_FOR_774 = new AuctionBidGroup() {{
        this.setId(GROUP_ID_0_DEFAULT);
        this.setBidsCount(100500);
        this.setShopId(SHOP_ID_774);
        this.setEmptyCount(0);
        this.setName("group_0_for_shop_774");
    }};


    public static final AuctionBidGroup GROUP_1_FOR_774 = new AuctionBidGroup() {{
        this.setId(GROUP_ID_1);
        this.setBidsCount(100500);
        this.setShopId(SHOP_ID_774);
        this.setEmptyCount(0);
        this.setName("group_1_for_shop_774");
    }};

    public static final AuctionBidGroup GROUP_2_FOR_774 = new AuctionBidGroup() {{
        this.setId(GROUP_ID_2);
        this.setBidsCount(100500);
        this.setShopId(SHOP_ID_774);
        this.setEmptyCount(0);
        this.setName("group_2_for_shop_774");
    }};

    public static final AuctionBidGroup GROUP_2_FOR_100 = new AuctionBidGroup() {{
        this.setId(GROUP_ID_2);
        this.setBidsCount(100500);
        this.setShopId(SHOP_ID_100);
        this.setEmptyCount(0);
        this.setName("group_2_for_shop_100");
    }};

    public static final AuctionBidGroup GROUP_3_FOR_100 = new AuctionBidGroup() {{
        this.setId(GROUP_ID_3);
        this.setBidsCount(100500);
        this.setShopId(SHOP_ID_100);
        this.setEmptyCount(0);
        this.setName("group_3_for_shop_100");
    }};

    public static final AuctionBidGroup GROUP_4_EMPTY_FOR_100 = new AuctionBidGroup() {{
        this.setId(GROUP_ID_4);
        this.setBidsCount(0);
        this.setShopId(SHOP_ID_100);
        this.setEmptyCount(0);
        this.setName("group_4_empty_for_shop_100");
    }};

    private AuctionBulkCommon() {
        throw new UnsupportedOperationException();
    }

    public static AuctionOfferBid createAuctionOfferBidWithoutValues(Long shopId, Long groupId, String offerTitle, AuctionBidStatus status) {
        return createAuctionOfferBidWithoutValues(shopId, groupId, new AuctionOfferId(offerTitle), status);
    }

    public static AuctionOfferBid createAuctionOfferBidWithoutValues(Long shopId, Long groupId, AuctionOfferId offerId, AuctionBidStatus status) {
        AuctionOfferBid aob = new AuctionOfferBid();
        aob.setShopId(shopId);
        aob.setGroupId(groupId);
        aob.setOfferId(offerId);
        aob.setStatus(status);
        return aob;
    }

    public static AuctionOfferBid createAuctionOfferBidWithoutValues(Long shopId, Long groupId, String offerId, AuctionBidStatus status, AuctionBidComponentsLink linkType) {
        AuctionOfferBid aob = new AuctionOfferBid();
        aob.setShopId(shopId);
        aob.setGroupId(groupId);
        aob.setOfferId(new AuctionOfferId(offerId));
        aob.setStatus(status);
        aob.setLinkType(linkType);
        return aob;
    }

    public static AuctionOfferBid createFakeExistingTitleOfferForAuctionService(Long shopId, Long groupId, String offerName) {
        AuctionOfferBid aob = new AuctionOfferBid(
                shopId,
                new AuctionOfferId(offerName),
                offerName,
                groupId,
                //не имеет значения какие именно компоненты заданы
                new AuctionBidValues(ImmutableMap.of(BidPlace.MARKET_PLACE, BigInteger.TEN))
        );
        aob.setLinkType(AuctionBidComponentsLink.DEFAULT_LINK_TYPE);
        return aob;
    }

    public static AuctionOfferBid createFakeExistingOfferIdForAuctionService(Long shopId, Long groupId,
                                                                             String offerId, Long feedId) {
        AuctionOfferBid aob = new AuctionOfferBid(
                shopId,
                new AuctionOfferId(feedId, offerId),
                AuctionOfferBid.SEARCH_QUERY_STUB_UNKNOWN,
                groupId,
                //не имеет значения какие именно компоненты заданы
                new AuctionBidValues(ImmutableMap.of(BidPlace.MARKET_PLACE, BigInteger.TEN))
        );
        aob.setLinkType(AuctionBidComponentsLink.DEFAULT_LINK_TYPE);
        return aob;
    }

    public static BulkUpdateRequest createCbidUReqByName(
            String offerName,
            Integer cbidValue,
            AuctionBidComponentsLink linkType
    ) {
        BidReq cbid = BidReq.Builder.builder().withValue(cbidValue).build();
        return Builder.builder()
                .withCbid(cbid)
                .withOfferName(offerName)
                .build();
    }

    public static BulkUpdateRequest createUReqByName(
            String offerName,
            HybridGoal goal,
            AuctionBidComponentsLink linkType
    ) {
        return Builder.builder()
                .withOfferName(offerName)
                .withGoal(goal)
                .build();
    }

    public static BulkUpdateRequest createEmptyUReqByNameWithGroup(
            String offerName,
            Long groupId
    ) {
        return Builder.builder()
                .withOfferName(offerName)
                .withNewGroup(groupId)
                .build();
    }


    public static AuctionBulkRequest createBulkServantletURequest(long shopId, long groupId) {
        AuctionBulkRequest req = new AuctionBulkRequest();
        req.setShopId(shopId);
        req.setGroupId(groupId);
        return req;
    }

    public static AuctionBulkRequest createBulkServantletURequest(long shopId, long groupId, String query) {
        AuctionBulkRequest req = new AuctionBulkRequest();
        req.setShopId(shopId);
        req.setGroupId(groupId);
        req.setQuery(query);
        return req;
    }

    public static AuctionOffer createOfferFromBidWithLimits(AuctionOfferBid bid, AuctionBidValuesLimits limits) {
        AuctionOffer auctionOffer = new AuctionOffer(SOME_TITLE_OFFER_ID);
        auctionOffer.setBidValuesLimits(limits);
        auctionOffer.setOfferBid(bid);
        return auctionOffer;
    }

    public static ReportRecommendationsAnswerOrError buildCardRecommendationsFromFile(InputStream in)
            throws IOException, ExecutionException, InterruptedException {

        final ReportResponseXmlParser<ReportRecommendationsAnswerDto> parser
                = new ReportResponseXmlParser<>(ReportRecommendationsAnswerDto.class);

        parser.parse(in);

        final AsyncMarketReportService marketReportService = Mockito.mock(AsyncMarketReportService.class);
        doReturn(CompletableFuture.completedFuture(parser.getResult()))
                .when(marketReportService).async(any(), any());

        final BidRecommendatorImpl partiallyMockedRecommendator = new BidRecommendatorImpl(marketReportService);

        final BidRecommendationRequest recommendationRequest = new BidRecommendationRequest();
        recommendationRequest.setOfferId(new AuctionOfferId("irrelevant name"));
        recommendationRequest.setRegion(new Region(4L, "region", null));
        recommendationRequest.setTargetPositions(ImmutableSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        return partiallyMockedRecommendator.loadRecommendations(recommendationRequest)
                .get();
    }

    public static BidRecommendations buildParallelSearchRecommendationsFromFile(InputStream in)
            throws IOException, SAXException, ExecutionException, InterruptedException {

        ParallelSearchRecommendationParser parser = new ParallelSearchRecommendationParser();
        parser.parseXmlStream(in);

        MarketSearchService marketReportService = Mockito.mock(DefaultMarketSearchService.class);
        doReturn(CompletableFuture.completedFuture(parser)).when(marketReportService).executeAsync(any(), any(InputStreamParser.class));

        ParallelSearchBidRecommendator partiallyMockedRecommendator = Mockito.spy(
                new ParallelSearchBidRecommendator(
                        marketReportService,
                        0
                )
        );

        BidRecommendationRequest recommendationRequest = new BidRecommendationRequest();
        recommendationRequest.setOfferId(new AuctionOfferId("irrelevant name"));
        recommendationRequest.setRegion(new Region(4L, "region", null));
        recommendationRequest.setTargetPositions(ImmutableSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9));

        return partiallyMockedRecommendator.calculate(recommendationRequest).get();
    }

    public static BidRecommendations buildMarketSearchRecommendationsFromFile(InputStream in)
            throws IOException, SAXException, ExecutionException, InterruptedException {
        AuctionMarketSearchMarketReportXmlParserSettings PARSER_SETTINGS
                = AuctionMarketSearchMarketReportXmlParserTest.createMinimalTestSettings();
        AuctionMarketSearchMarketReportXmlParser parser = new AuctionMarketSearchMarketReportXmlParser(PARSER_SETTINGS);

        parser.parseXmlStream(in);

        GeneralMarketReportXmlParserFactory parserFactory = Mockito.mock(GeneralMarketReportXmlParserFactory.class);
        doReturn(parser).when(parserFactory).newParser();

        AsyncMarketReportService marketReportService = Mockito.mock(AsyncMarketReportService.class);
        doReturn(CompletableFuture.completedFuture(parser)).when(marketReportService).async(any(), any());

        ReportMarketSearchBidRecommendator partiallyMockedRecommendator = Mockito.spy(
                new ReportMarketSearchBidRecommendator(parserFactory, marketReportService));

        BidRecommendationRequest recommendationRequest = new BidRecommendationRequest();
        recommendationRequest.setOfferId(new AuctionOfferId("irrelevant name"));
        recommendationRequest.setRegion(new Region(4L, "region", null));
        recommendationRequest.setTargetPositions(ImmutableSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));

        return partiallyMockedRecommendator.calculate(recommendationRequest).get();
    }


    /**
     * Мок сущесвтующийх ставок для магазина в сервисе
     */
    public static void mockOfferBidsForShopAsExisting(AuctionService mockedAuctionService, long shopId, AuctionOfferBid... bids) {
        when(mockedAuctionService.getOfferBids(eq(shopId), ArgumentMatchers.any()))
                .thenReturn(
                        ImmutableList.copyOf(bids)
                );
    }

    public static void mockAuctionService(AuctionService mockedAuctionService) {
        //774
        when(mockedAuctionService.getGroups(SHOP_ID_774))
                .thenReturn(
                        ImmutableList.of(
                                GROUP_0_FOR_774,
                                GROUP_1_FOR_774,
                                GROUP_2_FOR_774
                        )
                );

        when(mockedAuctionService.getAuctionOfferIdType(SHOP_ID_774))
                .thenReturn(
                        AuctionOfferIdType.TITLE
                );

        //100
        when(mockedAuctionService.getGroups(SHOP_ID_100))
                .thenReturn(
                        ImmutableList.of(
                                GROUP_2_FOR_100,
                                GROUP_3_FOR_100,
                                GROUP_4_EMPTY_FOR_100
                        )
                );


        when(mockedAuctionService.getAuctionOfferIdType(SHOP_ID_100))
                .thenReturn(
                        AuctionOfferIdType.SHOP_OFFER_ID
                );

    }

    public static void assertSuccessValidBidUpdate(MockServResponse response, int count) {
        assertThat(response, not(hasWarnings()));
        assertThat(response, hasValidBidCount(count));
        assertThat(response, hasFoundBidCount(count));
        assertThat(response, hasFinalBidUpdateCount(count));
    }

    public static void assertSuccessValidBidCreation(MockServResponse response, int count) {
        assertThat(response, not(hasWarnings()));
        assertThat(response, hasValidBidCount(count));
        assertThat(response, hasBidUpdateCount(count));
        assertThat(response, hasFoundBidCount(0));
        assertThat(response, hasFinalBidUpdateCount(count));
    }
}