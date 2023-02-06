package ru.yandex.market.core.auction.recommend;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.MarketSearchRequest;
import ru.yandex.market.common.report.model.RecommendationType;
import ru.yandex.market.core.AbstractParserTest;
import ru.yandex.market.core.DefaultMarketReportParserFactory;
import ru.yandex.market.core.auction.marketreport.AuctionMarketSearchMarketReportXmlParser;
import ru.yandex.market.core.auction.marketreport.AuctionMarketSearchMarketReportXmlParserSettings;
import ru.yandex.market.core.auction.marketreport.AuctionMarketSearchMarketReportXmlParserTest;
import ru.yandex.market.core.auction.matchers.FoundOfferMatchers;
import ru.yandex.market.core.auction.model.ComplexBid;
import ru.yandex.market.core.auction.model.StatusCode;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.core.auction.matchers.ComplexBidMatchers.hasBid;
import static ru.yandex.market.core.auction.matchers.ComplexBidMatchers.hasFee;
import static ru.yandex.market.core.auction.matchers.ComplexBidMatchers.hasStatus;
import static ru.yandex.market.core.auction.matchers.OfferAuctionStatsMatchers.containsStats;
import static ru.yandex.market.core.auction.matchers.OfferAuctionStatsMatchers.hasMinBid;
import static ru.yandex.market.core.auction.matchers.OfferAuctionStatsMatchers.hasMinFee;
import static ru.yandex.market.core.auction.matchers.TargetAuctionStatsMatchers.hasCurPosAll;
import static ru.yandex.market.core.auction.matchers.TargetAuctionStatsMatchers.hasModelCount;

/**
 * @author vbudnev
 */
public class ReportMarketSearchBidRecommendatorTest extends AbstractParserTest {

    private static final String FILE_GENERAL_OK = "ok.xml";
    private static AuctionMarketSearchMarketReportXmlParser parser;
    private static ReportMarketSearchBidRecommendator partiallyMockedRecommendator;
    private static BidRecommendationRequest recommendationRequest;
    private static AuctionMarketSearchMarketReportXmlParserSettings PARSER_SETTINGS;
    private static Set<Integer> TARGET_POSITIONS_1_3_7_12;
    private static Integer AUCTION_BLOCK_SIZE = 12;

    @BeforeClass
    public static void initOnce() {
        PARSER_SETTINGS = AuctionMarketSearchMarketReportXmlParserTest.createMinimalTestSettings();
        TARGET_POSITIONS_1_3_7_12 = new HashSet<>(Arrays.asList(1, 3, 7, 12));
    }

    @Before
    public void initMethodEnv() {
        parser = new AuctionMarketSearchMarketReportXmlParser(PARSER_SETTINGS);
        DefaultMarketReportParserFactory parserFactory = Mockito.mock(DefaultMarketReportParserFactory.class);
        doReturn(parser).when(parserFactory).newParser();

        MarketSearchRequest marketSearchRequest = Mockito.mock(MarketSearchRequest.class);

        AsyncMarketReportService marketReportService = Mockito.mock(AsyncMarketReportService.class);
        doReturn(CompletableFuture.completedFuture(parser)).when(marketReportService).async(eq(marketSearchRequest), any());

        partiallyMockedRecommendator = Mockito.spy(
                new ReportMarketSearchBidRecommendator(parserFactory, marketReportService)
        );

        doReturn(marketSearchRequest).when(partiallyMockedRecommendator).prepareShopOffersRequest(any());

        recommendationRequest = new BidRecommendationRequest();
    }

    @Test
    public void test_calculate_flow_works_in_general() throws IOException, SAXException, ExecutionException, InterruptedException {
        try (InputStream in = getContentStream(FILE_GENERAL_OK)) {
            parser.parseXmlStream(in);

            recommendationRequest.setTargetPositions(TARGET_POSITIONS_1_3_7_12);
            BidRecommendations bidRecommendations = partiallyMockedRecommendator.calculate(recommendationRequest).get();

            OfferAuctionStats stats = bidRecommendations.getShopOffersAuctionStats().get(0);
            assertThat(stats, containsStats(RecommendationType.MARKET_SEARCH));

            OfferAuctionStats.TargetAuctionStats ts = stats.getTargetStats(RecommendationType.MARKET_SEARCH);

            assertThat(ts,
                    allOf(hasCurPosAll(5), hasModelCount(12))
            );

            Map<Integer, ComplexBid> complexBids = ts.getPositionComplexBids();

            assertThat("Expected bid collection size differs", complexBids.size(), is(4));

            assertThat(
                    complexBids.get(1),
                    allOf(hasBid(200), hasFee(2000), hasStatus(StatusCode.OK))
            );

            assertNotNull("There should be all AUCTION_BLOCK_SIZE available positions if xml is ok", complexBids.get(AUCTION_BLOCK_SIZE));
            assertNull("There should be no more recommendations then block size", complexBids.get(AUCTION_BLOCK_SIZE + 1));

            assertNull("There should be no bid if position is not specified in target positions", complexBids.get(2));

            assertThat(
                    stats,
                    allOf(hasMinBid(29), hasMinFee(200))
            );

            assertThat(
                    stats.getOffer(),
                    allOf(FoundOfferMatchers.hasFee(3400), FoundOfferMatchers.hasBid(1200))
            );
        }
    }

}