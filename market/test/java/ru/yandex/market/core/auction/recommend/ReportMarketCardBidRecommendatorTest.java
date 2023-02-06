package ru.yandex.market.core.auction.recommend;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.MarketSearchRequest;
import ru.yandex.market.common.report.model.RecommendationType;
import ru.yandex.market.common.report.parser.xml.GeneralMarketReportXmlParserFactory;
import ru.yandex.market.core.AbstractParserTest;
import ru.yandex.market.core.auction.ShouldGoToApiReportAnalyzer;
import ru.yandex.market.core.auction.marketreport.AuctionShortCardMarketReportXmlParser;
import ru.yandex.market.core.auction.marketreport.AuctionShortCardMarketReportXmlParserSettings;
import ru.yandex.market.core.auction.marketreport.AuctionShortCardMarketReportXmlParserTest;
import ru.yandex.market.core.auction.matchers.FoundOfferMatchers;
import ru.yandex.market.core.auction.model.ComplexBid;
import ru.yandex.market.core.auction.model.StatusCode;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static ru.yandex.market.core.auction.matchers.ComplexBidMatchers.hasBid;
import static ru.yandex.market.core.auction.matchers.ComplexBidMatchers.hasFee;
import static ru.yandex.market.core.auction.matchers.ComplexBidMatchers.hasStatus;
import static ru.yandex.market.core.auction.matchers.OfferAuctionStatsMatchers.hasMinBid;
import static ru.yandex.market.core.auction.matchers.OfferAuctionStatsMatchers.hasMinFee;
import static ru.yandex.market.core.auction.matchers.TargetAuctionStatsMatchers.hasCurPosAll;
import static ru.yandex.market.core.auction.matchers.TargetAuctionStatsMatchers.hasCurPosTop;
import static ru.yandex.market.core.auction.matchers.TargetAuctionStatsMatchers.hasTopOffersCount;

/**
 * @author vbudnev
 */
class ReportMarketCardBidRecommendatorTest extends AbstractParserTest {
    private static final String FILE_GENERAL_OK = "ok.xml";
    private static AuctionShortCardMarketReportXmlParser parser;
    private static ReportMarketCardBidRecommendator partiallyMockedRecommendator;
    private static BidRecommendationRequest recommendationRequest;
    private static AuctionShortCardMarketReportXmlParserSettings PARSER_SETTINGS;
    private static Set<Integer> TARGET_POSITIONS_1_3_7_10;

    @BeforeAll
    static void initOnce() {
        PARSER_SETTINGS = AuctionShortCardMarketReportXmlParserTest.createMinimalTestSettings();
        TARGET_POSITIONS_1_3_7_10 = new HashSet<>(Arrays.asList(1, 3, 7, 10));
    }

    @BeforeEach
    void initMethodEnv() {
        parser = new AuctionShortCardMarketReportXmlParser(PARSER_SETTINGS);
        GeneralMarketReportXmlParserFactory parserFactory = Mockito.mock(GeneralMarketReportXmlParserFactory.class);
        doReturn(parser).when(parserFactory).newParser();

        MarketSearchRequest marketSearchRequest = Mockito.mock(MarketSearchRequest.class);

        AsyncMarketReportService marketReportService = Mockito.mock(AsyncMarketReportService.class);
        ShouldGoToApiReportAnalyzer shouldGoToApiReportAnalyzer = Mockito.mock(ShouldGoToApiReportAnalyzer.class);
        doReturn(CompletableFuture.completedFuture(parser)).when(marketReportService).async(eq(marketSearchRequest), any());

        partiallyMockedRecommendator = Mockito.spy(
                new ReportMarketCardBidRecommendator(parserFactory, marketReportService, shouldGoToApiReportAnalyzer)
        );

        doReturn(marketSearchRequest).when(partiallyMockedRecommendator).prepareShopOffersRequest(any());

        recommendationRequest = new BidRecommendationRequest();
    }

    @Test
    void test_calculate_flow_works_in_general() throws IOException, SAXException, ExecutionException, InterruptedException {

        try (InputStream in = getContentStream(FILE_GENERAL_OK)) {
            parser.parseXmlStream(in);

            recommendationRequest.setTargetPositions(TARGET_POSITIONS_1_3_7_10);
            BidRecommendations bidRecommendations = partiallyMockedRecommendator.calculate(recommendationRequest).get();

            OfferAuctionStats stats = bidRecommendations.getShopOffersAuctionStats().get(0);
            OfferAuctionStats.TargetAuctionStats cpcStats = stats.getTargetStats(RecommendationType.CARD);
            OfferAuctionStats.TargetAuctionStats cpaStats = stats.getTargetStats(RecommendationType.CARD_CPA);

            assertNotNull("There should be recommendation block for CARD", cpcStats);
            assertNotNull("There should be recommendation block for CARD_CPA", cpaStats);

            assertThat("CARD",
                    cpcStats,
                    allOf(hasCurPosAll(1), hasCurPosTop(2), hasTopOffersCount(6))
            );

            Map<Integer, ComplexBid> complexBidsCpc = cpcStats.getPositionComplexBids();

            assertThat("Expected bid collection size differs", complexBidsCpc.size(), is(4));

            assertThat(complexBidsCpc.get(1),
                    allOf(hasBid(701), hasFee(null), hasStatus(StatusCode.OK))
            );

            assertNull("There should be no bid if position is not specified in target positions for CARD", complexBidsCpc.get(2));

            assertThat("CARD_CPA",
                    cpaStats,
                    allOf(hasCurPosAll(3), hasCurPosTop(4), hasTopOffersCount(4))
            );

            Map<Integer, ComplexBid> complexBidsCpa = cpaStats.getPositionComplexBids();

            assertThat("Expected bid collection size differs", complexBidsCpa.size(), is(4));

            assertThat(complexBidsCpa.get(1),
                    //0 так как старое оригинальное поведение, мб рудимент
                    allOf(hasBid(0), hasFee(210), hasStatus(StatusCode.OK))
            );

            assertNull("There should be no bid if position is not specified in target positions for CARD_CPA", complexBidsCpa.get(2));

            assertThat(stats,
                    allOf(hasMinBid(57), hasMinFee(200))
            );

            assertThat(stats.getOffer(),
                    allOf(FoundOfferMatchers.hasFee(210), FoundOfferMatchers.hasBid(4176))
            );
        }
    }
}
