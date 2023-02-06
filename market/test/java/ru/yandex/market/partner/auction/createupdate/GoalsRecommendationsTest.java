package ru.yandex.market.partner.auction.createupdate;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

import ru.yandex.market.core.auction.dto.report.ReportRecommendationsAnswerOrError;
import ru.yandex.market.core.auction.err.BidValueLimitsViolationException;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.core.auction.recommend.BidRecommendations;
import ru.yandex.market.partner.auction.AbstractParserTestExtended;
import ru.yandex.market.partner.auction.AuctionBulkCommon;
import ru.yandex.market.partner.auction.HybridGoal;

import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_LINK_CBID_VARIABLE;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_LINK_FEE_VARIABLE;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CBID_PRIORITY;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CPA_ONLY;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_FEE_PRIORITY;
import static ru.yandex.market.core.auction.model.AuctionBidStatus.PUBLISHED;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_222;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_333;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_KEEP_OLD;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_ID_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.HYBRID_CARD_FIRST_PAGE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.HYBRID_CARD_PREM_FIRST_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.HYBRID_CARD_PREM_FIRST_PLACE_TIED;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.HYBRID_CARD_PREM_PAGE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.LIMITS;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.MARKET_SEARCH_FIRST_PAGE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.MARKET_SEARCH_FIRST_PAGE_TIED;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.MARKET_SEARCH_FIRST_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.PARALLEL_SEARCH_FIRST_PAGE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.PARALLEL_SEARCH_FIRST_PAGE_TIED;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.PARALLEL_SEARCH_FIRST_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SHOP_ID_774;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.buildCardRecommendationsFromFile;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.buildMarketSearchRecommendationsFromFile;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.buildParallelSearchRecommendationsFromFile;
import static ru.yandex.market.partner.auction.BulkUpdateRequest.Builder.builder;
import static ru.yandex.market.partner.auction.createupdate.CreateUpdateCommon.testCreateBidUpdateMethod;

/**
 * @author vbudnev
 */
@RunWith(Parameterized.class)
public class GoalsRecommendationsTest extends AbstractParserTestExtended {

    private static final String CARD_FILE_REC_OK = "Card_Recommendations_ok.xml";
    private static final String PARALLEL_SEARCH_FILE_REC_OK = "ParallelSearch_Recommendations_ok.xml";
    private static final String MARKET_SEARCH_FILE_REC_OK = "MarketSearch_Recommendations_ok.xml";

    private AuctionBidComponentsLink linkType;
    private HybridGoal goal;
    private BigInteger expectedBid;
    private BigInteger expectedCbid;
    private BigInteger expectedFee;

    public GoalsRecommendationsTest(
            AuctionBidComponentsLink linkType,
            HybridGoal goal,
            Integer expectedBid,
            Integer expectedCbid,
            Integer expectedFee
    ) {
        this.linkType = linkType;
        this.expectedBid = BigInteger.valueOf(expectedBid);
        this.expectedCbid = BigInteger.valueOf(expectedCbid);
        this.expectedFee = BigInteger.valueOf(expectedFee);
        this.goal = goal;
    }


    /**
     * <br>HYBRID_CARD_FIRST_PLACE - исключено так как от репорта пока не приезжают данные о такой позции для карточки
     */
    @Parameterized.Parameters(name = "{index}: linkType={0} goal={1} expectedBid(b:{2},c:{3},f:{4})")
    public static Collection<Object[]> testCases() {
        return Arrays.asList(
                new Object[][]{
                        /**
                         * Тип связи более ничего не значит для карточных рекомендаций. Всегда изменятеся только cbid
                         * компонента (если не задан {@link HybridGoal#tied}).
                        */
                        //card
                        {CARD_NO_LINK_FEE_PRIORITY, HYBRID_CARD_PREM_FIRST_PLACE, BID_KEEP_OLD, 701, BID_KEEP_OLD},
                        {CARD_NO_LINK_FEE_PRIORITY, HYBRID_CARD_PREM_FIRST_PLACE_TIED, 701, 701, BID_KEEP_OLD},
                        {CARD_NO_LINK_FEE_PRIORITY, HYBRID_CARD_PREM_PAGE, BID_KEEP_OLD, 169, BID_KEEP_OLD},
                        {CARD_NO_LINK_FEE_PRIORITY, HYBRID_CARD_FIRST_PAGE, BID_KEEP_OLD, 131, BID_KEEP_OLD},
//                        {CARD_NO_LINK_FEE_PRIORITY , HYBRID_CARD_FIRST_PLACE, BID_KEEP_OLD, BID_CENTS_222, 216},

                        {CARD_NO_LINK_CBID_PRIORITY, HYBRID_CARD_PREM_FIRST_PLACE, BID_KEEP_OLD, 701, BID_KEEP_OLD},
                        {CARD_NO_LINK_CBID_PRIORITY, HYBRID_CARD_PREM_FIRST_PLACE_TIED, 701, 701, BID_KEEP_OLD},
                        {CARD_NO_LINK_CBID_PRIORITY, HYBRID_CARD_PREM_PAGE, BID_KEEP_OLD, 169, BID_KEEP_OLD},
                        {CARD_NO_LINK_CBID_PRIORITY, HYBRID_CARD_FIRST_PAGE, BID_KEEP_OLD, 131, BID_KEEP_OLD},
//                        {CARD_NO_LINK_CBID_PRIORITY, HYBRID_CARD_FIRST_PLACE, BID_KEEP_OLD, 631, BID_CENTS_333},

                        {CARD_LINK_FEE_VARIABLE, HYBRID_CARD_PREM_FIRST_PLACE, BID_KEEP_OLD, 701, BID_KEEP_OLD},
                        {CARD_LINK_FEE_VARIABLE, HYBRID_CARD_PREM_FIRST_PLACE_TIED, 701, 701, BID_KEEP_OLD},
                        {CARD_LINK_FEE_VARIABLE, HYBRID_CARD_PREM_PAGE, BID_KEEP_OLD, 169, BID_KEEP_OLD},
                        {CARD_LINK_FEE_VARIABLE, HYBRID_CARD_FIRST_PAGE, BID_KEEP_OLD, 131, BID_KEEP_OLD},
//                        {CARD_LINK_FEE_VARIABLE, HYBRID_CARD_FIRST_PLACE, BID_KEEP_OLD, 151, 216},

                        {CARD_LINK_CBID_VARIABLE, HYBRID_CARD_PREM_FIRST_PLACE, BID_KEEP_OLD, 701, BID_KEEP_OLD},
                        {CARD_LINK_CBID_VARIABLE, HYBRID_CARD_PREM_FIRST_PLACE_TIED, 701, 701, BID_KEEP_OLD},
                        {CARD_LINK_CBID_VARIABLE, HYBRID_CARD_PREM_PAGE, BID_KEEP_OLD, 169, BID_KEEP_OLD},
                        {CARD_LINK_CBID_VARIABLE, HYBRID_CARD_FIRST_PAGE, BID_KEEP_OLD, 131, BID_KEEP_OLD},
//                        {CARD_LINK_CBID_VARIABLE , HYBRID_CARD_FIRST_PLACE, BID_KEEP_OLD, 631, 221},

                        {CARD_NO_LINK_CPA_ONLY, HYBRID_CARD_PREM_FIRST_PLACE, BID_KEEP_OLD, 701, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPA_ONLY, HYBRID_CARD_PREM_FIRST_PLACE_TIED, 701, 701, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPA_ONLY, HYBRID_CARD_PREM_PAGE, BID_KEEP_OLD, 169, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPA_ONLY, HYBRID_CARD_FIRST_PAGE, BID_KEEP_OLD, 131, BID_KEEP_OLD},
//                        {CARD_NO_LINK_CPA_ONLY, HYBRID_CARD_FIRST_PLACE, BID_KEEP_OLD, BID_KEEP_OLD, 221},

                        {CARD_NO_LINK_CPC_ONLY, HYBRID_CARD_PREM_FIRST_PLACE, BID_KEEP_OLD, 701, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPC_ONLY, HYBRID_CARD_PREM_FIRST_PLACE_TIED, 701, 701, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPC_ONLY, HYBRID_CARD_PREM_PAGE, BID_KEEP_OLD, 169, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPC_ONLY, HYBRID_CARD_FIRST_PAGE, BID_KEEP_OLD, 131, BID_KEEP_OLD},
//                        {CARD_NO_LINK_CPC_ONLY, HYBRID_CARD_FIRST_PLACE, BID_KEEP_OLD, 631, BID_KEEP_OLD},

                        //parallel_search
                        {CARD_LINK_FEE_VARIABLE, PARALLEL_SEARCH_FIRST_PAGE, 1147, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_LINK_FEE_VARIABLE, PARALLEL_SEARCH_FIRST_PAGE_TIED, 1147, 1147, BID_KEEP_OLD},
                        {CARD_LINK_FEE_VARIABLE, PARALLEL_SEARCH_FIRST_PLACE, 1701, BID_KEEP_OLD, BID_KEEP_OLD},

                        {CARD_LINK_CBID_VARIABLE, PARALLEL_SEARCH_FIRST_PAGE, 1147, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_LINK_CBID_VARIABLE, PARALLEL_SEARCH_FIRST_PAGE_TIED, 1147, 1147, BID_KEEP_OLD},
                        {CARD_LINK_CBID_VARIABLE, PARALLEL_SEARCH_FIRST_PLACE, 1701, BID_KEEP_OLD, BID_KEEP_OLD},

                        {CARD_NO_LINK_FEE_PRIORITY, PARALLEL_SEARCH_FIRST_PAGE, 1147, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_NO_LINK_FEE_PRIORITY, PARALLEL_SEARCH_FIRST_PAGE_TIED, 1147, 1147, BID_KEEP_OLD},
                        {CARD_NO_LINK_FEE_PRIORITY, PARALLEL_SEARCH_FIRST_PLACE, 1701, BID_KEEP_OLD, BID_KEEP_OLD},

                        {CARD_NO_LINK_CBID_PRIORITY, PARALLEL_SEARCH_FIRST_PAGE, 1147, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_NO_LINK_CBID_PRIORITY, PARALLEL_SEARCH_FIRST_PAGE_TIED, 1147, 1147, BID_KEEP_OLD},
                        {CARD_NO_LINK_CBID_PRIORITY, PARALLEL_SEARCH_FIRST_PLACE, 1701, BID_KEEP_OLD, BID_KEEP_OLD},

                        {CARD_NO_LINK_CPA_ONLY, PARALLEL_SEARCH_FIRST_PAGE, 1147, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPC_ONLY, PARALLEL_SEARCH_FIRST_PAGE_TIED, 1147, 1147, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPA_ONLY, PARALLEL_SEARCH_FIRST_PLACE, 1701, BID_KEEP_OLD, BID_KEEP_OLD},

                        {CARD_NO_LINK_CPC_ONLY, PARALLEL_SEARCH_FIRST_PAGE, 1147, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPC_ONLY, PARALLEL_SEARCH_FIRST_PAGE_TIED, 1147, 1147, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPC_ONLY, PARALLEL_SEARCH_FIRST_PLACE, 1701, BID_KEEP_OLD, BID_KEEP_OLD},

                        //market_search
                        {CARD_LINK_FEE_VARIABLE, MARKET_SEARCH_FIRST_PAGE, 1010, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_LINK_FEE_VARIABLE, MARKET_SEARCH_FIRST_PAGE_TIED, 1010, 1010, BID_KEEP_OLD},
                        {CARD_LINK_FEE_VARIABLE, MARKET_SEARCH_FIRST_PLACE, 1701, BID_KEEP_OLD, BID_KEEP_OLD},

                        {CARD_LINK_CBID_VARIABLE, MARKET_SEARCH_FIRST_PAGE, 1010, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_LINK_CBID_VARIABLE, MARKET_SEARCH_FIRST_PAGE_TIED, 1010, 1010, BID_KEEP_OLD},
                        {CARD_LINK_CBID_VARIABLE, MARKET_SEARCH_FIRST_PLACE, 1701, BID_KEEP_OLD, BID_KEEP_OLD},

                        {CARD_NO_LINK_FEE_PRIORITY, MARKET_SEARCH_FIRST_PAGE, 1010, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_NO_LINK_FEE_PRIORITY, MARKET_SEARCH_FIRST_PAGE_TIED, 1010, 1010, BID_KEEP_OLD},
                        {CARD_NO_LINK_FEE_PRIORITY, MARKET_SEARCH_FIRST_PLACE, 1701, BID_KEEP_OLD, BID_KEEP_OLD},

                        {CARD_NO_LINK_CBID_PRIORITY, MARKET_SEARCH_FIRST_PAGE, 1010, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_NO_LINK_CBID_PRIORITY, MARKET_SEARCH_FIRST_PAGE_TIED, 1010, 1010, BID_KEEP_OLD},
                        {CARD_NO_LINK_CBID_PRIORITY, MARKET_SEARCH_FIRST_PLACE, 1701, BID_KEEP_OLD, BID_KEEP_OLD},

                        {CARD_NO_LINK_CPA_ONLY, MARKET_SEARCH_FIRST_PAGE, 1010, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPA_ONLY, MARKET_SEARCH_FIRST_PAGE_TIED, 1010, 1010, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPA_ONLY, MARKET_SEARCH_FIRST_PLACE, 1701, BID_KEEP_OLD, BID_KEEP_OLD},

                        {CARD_NO_LINK_CPC_ONLY, MARKET_SEARCH_FIRST_PAGE, 1010, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPA_ONLY, MARKET_SEARCH_FIRST_PAGE_TIED, 1010, 1010, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPC_ONLY, MARKET_SEARCH_FIRST_PLACE, 1701, BID_KEEP_OLD, BID_KEEP_OLD},

                }
        );
    }

    /**
     * Проверяем, что для разных значений целей {@link HybridGoal} и типа связи {@link AuctionBidComponentsLink}
     * проставляются ожидаемые значения на основе блоков рекомендаций и опорных значений.
     */
    @Test
    public void test_updateBid_should_correctlyExtractPlaceRecommendations_when_differentGoalsAndDifferentLinkTypesUsed()
            throws BidValueLimitsViolationException, ExecutionException, InterruptedException, IOException, SAXException {


        Map<BidPlace, Integer> hybridReferenceValuesMap = ImmutableMap.of(
                BidPlace.CARD, BID_CENTS_222,
                BidPlace.MARKET_PLACE, BID_CENTS_333
        );

        ReportRecommendationsAnswerOrError cardRecommendations
                = buildCardRecommendationsFromFile(getContentStreamFromExplicitFile(CARD_FILE_REC_OK));
        BidRecommendations parallelSearchRecommendations
                = buildParallelSearchRecommendationsFromFile(getContentStreamFromExplicitFile(PARALLEL_SEARCH_FILE_REC_OK));
        BidRecommendations marketSearchRecommendations
                = buildMarketSearchRecommendationsFromFile(getContentStreamFromExplicitFile(MARKET_SEARCH_FILE_REC_OK));


        testCreateBidUpdateMethod(
                (goal, link) -> builder().withGoal(goal).withOfferName("irrelevant name").build(),
                () -> AuctionBulkCommon.createAuctionOfferBidWithoutValues(
                        SHOP_ID_774,
                        GROUP_ID_1,
                        "irrelevant name",
                        PUBLISHED
                ),
                linkType,
                goal,
                expectedBid,
                expectedCbid,
                expectedFee,
                LIMITS,
                cardRecommendations,
                parallelSearchRecommendations,
                marketSearchRecommendations,
                hybridReferenceValuesMap
        );
    }

}