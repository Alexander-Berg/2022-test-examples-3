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
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import ru.yandex.market.common.report.model.RecommendationType;
import ru.yandex.market.core.AbstractParserTest;
import ru.yandex.market.core.auction.matchers.FoundOfferMatchers;
import ru.yandex.market.core.auction.model.ComplexBid;
import ru.yandex.market.core.auction.model.StatusCode;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.geobase.model.Region;
import ru.yandex.market.core.geobase.model.RegionType;
import ru.yandex.market.mbi.report.MarketSearchService;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.core.auction.matchers.ComplexBidMatchers.hasBid;
import static ru.yandex.market.core.auction.matchers.ComplexBidMatchers.hasFee;
import static ru.yandex.market.core.auction.matchers.ComplexBidMatchers.hasStatus;
import static ru.yandex.market.core.auction.matchers.OfferAuctionStatsMatchers.containsStats;
import static ru.yandex.market.core.auction.matchers.OfferAuctionStatsMatchers.hasMinBid;
import static ru.yandex.market.core.auction.matchers.OfferAuctionStatsMatchers.hasMinFee;
import static ru.yandex.market.core.auction.matchers.TargetAuctionStatsMatchers.hasCurPosAll;

/**
 * @author vbudnev
 */
public class ReportParallelSearchBidRecommendatorTest extends AbstractParserTest {

    private static final Set<Integer> TARGET_POSITIONS_1_3_9 = new HashSet<>(Arrays.asList(1, 3, 9));
    private static final Integer AUCTION_BLOCK_SIZE = 9;
    private static final String FILE_GENERAL_OK = "ok.xml";
    private static ParallelSearchRecommendationParser parser;
    private static ParallelSearchBidRecommendator partiallyMockedRecommendator;
    private static BidRecommendationRequest recommendationRequest;

    @Before
    public void initMethodEnv() {
        parser = new ParallelSearchRecommendationParser();

        RegionService regionService = Mockito.mock(RegionService.class);
        Region region = new Region(2L, "Moscow", 7L, RegionType.CITY);
        doReturn(region).when(regionService).getRegion(any(Long.class));
        MarketSearchService marketSearchService = Mockito.mock(MarketSearchService.class);
        doReturn(CompletableFuture.completedFuture(parser)).when(marketSearchService).executeAsync(any(), any(ParallelSearchRecommendationParser.class));

        partiallyMockedRecommendator = Mockito.spy(
                new ParallelSearchBidRecommendator(marketSearchService, 10)
        );

        recommendationRequest = new BidRecommendationRequest();
        recommendationRequest.setRegion(Mockito.mock(Region.class));
    }

    @Test
    public void test_calculate_flow_works_in_general() throws IOException, SAXException, ExecutionException, InterruptedException {
        try (InputStream in = getContentStream(FILE_GENERAL_OK)) {
            parser.parseXmlStream(in);

            recommendationRequest.setTargetPositions(TARGET_POSITIONS_1_3_9);
            BidRecommendations bidRecommendations = partiallyMockedRecommendator.calculate(recommendationRequest).get();

            OfferAuctionStats stats = bidRecommendations.getShopOffersAuctionStats().get(0);

            assertThat(stats, containsStats(RecommendationType.SEARCH));
            OfferAuctionStats.TargetAuctionStats ts = stats.getTargetStats(RecommendationType.SEARCH);

            assertThat(ts, hasCurPosAll(5));

            Map<Integer, ComplexBid> complexBids = ts.getPositionComplexBids();

            assertThat("Expected bid collection size differs", complexBids.size(), is(3));

            assertThat(
                    complexBids.get(1),
                    allOf(hasBid(200), hasFee(null), hasStatus(StatusCode.OK))
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