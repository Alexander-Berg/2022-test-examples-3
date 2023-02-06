package ru.yandex.market.partner.auction.createupdate.limits;

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
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_10;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_7;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_KEEP_OLD;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.CARD_IRRELEVANT_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_ID_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.LIMITS;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.MARKET_SEARCH_IRRELEVANT_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.PARALLEL_SEARCH_IRRELEVANT_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SHOP_ID_774;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.buildCardRecommendationsFromFile;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.buildMarketSearchRecommendationsFromFile;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.buildParallelSearchRecommendationsFromFile;
import static ru.yandex.market.partner.auction.BulkUpdateRequest.Builder.builder;
import static ru.yandex.market.partner.auction.createupdate.CreateUpdateCommon.testCreateBidUpdateMethod;

/**
 * Ограничение значений полученных в рекомендациях.
 * <br>Значения ниже min, подтягиваются до минимального.
 *
 * @author vbudnev
 */
@RunWith(Parameterized.class)
public class RecommendedAreTooSmallTest extends AbstractParserTestExtended {

    private static final String HYBRID_CARD_FILE_REC_OK_SMALL_VALUES = "./resources/card/ValueLimits_ok_with_small_values.xml";
    private static final String PARALLEL_SEARCH_FILE_REC_OK_SMALL_VALUES = "./resources/parallel_search/ValueLimits_ok_with_small_values.xml";
    private static final String MARKET_SEARCH_FILE_REC_OK_SMALL_VALUES = "./resources/market_search/ValueLimits_ok_with_small_values.xml";

    private AuctionBidComponentsLink linkType;
    private HybridGoal goal;
    private BigInteger expectedBid;
    private BigInteger expectedCbid;
    private BigInteger expectedFee;

    public RecommendedAreTooSmallTest(
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

    @Parameterized.Parameters(name = "{index}: linkType={0} goal={1} expectedBid(b:{2},c:{3},f:{4})")
    public static Collection<Object[]> testCases() {
        return Arrays.asList(
                new Object[][]{
                        /**
                         * Тип связи более ничего не значит для карточных рекомендаций. Кейсы будут поерзаны с удалением
                         * {@link AuctionBidComponentsLink}.
                        */
                        {CARD_LINK_FEE_VARIABLE, CARD_IRRELEVANT_PLACE, BID_KEEP_OLD, BID_CENTS_10, BID_KEEP_OLD},
                        {CARD_LINK_FEE_VARIABLE, PARALLEL_SEARCH_IRRELEVANT_PLACE, BID_CENTS_7, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_LINK_FEE_VARIABLE, MARKET_SEARCH_IRRELEVANT_PLACE, BID_CENTS_7, BID_KEEP_OLD, BID_KEEP_OLD},

                        {CARD_LINK_CBID_VARIABLE, CARD_IRRELEVANT_PLACE, BID_KEEP_OLD, BID_CENTS_10, BID_KEEP_OLD},
                        {CARD_LINK_CBID_VARIABLE, PARALLEL_SEARCH_IRRELEVANT_PLACE, BID_CENTS_7, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_LINK_CBID_VARIABLE, MARKET_SEARCH_IRRELEVANT_PLACE, BID_CENTS_7, BID_KEEP_OLD, BID_KEEP_OLD},

                        {CARD_NO_LINK_FEE_PRIORITY, CARD_IRRELEVANT_PLACE, BID_KEEP_OLD, BID_CENTS_10, BID_KEEP_OLD},
                        {CARD_NO_LINK_FEE_PRIORITY, PARALLEL_SEARCH_IRRELEVANT_PLACE, BID_CENTS_7, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_NO_LINK_FEE_PRIORITY, MARKET_SEARCH_IRRELEVANT_PLACE, BID_CENTS_7, BID_KEEP_OLD, BID_KEEP_OLD},

                        {CARD_NO_LINK_CBID_PRIORITY, CARD_IRRELEVANT_PLACE, BID_KEEP_OLD, BID_CENTS_10, BID_KEEP_OLD},
                        {CARD_NO_LINK_CBID_PRIORITY, PARALLEL_SEARCH_IRRELEVANT_PLACE, BID_CENTS_7, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_NO_LINK_CBID_PRIORITY, MARKET_SEARCH_IRRELEVANT_PLACE, BID_CENTS_7, BID_KEEP_OLD, BID_KEEP_OLD},

                        {CARD_NO_LINK_CPC_ONLY, CARD_IRRELEVANT_PLACE, BID_KEEP_OLD, BID_CENTS_10, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPC_ONLY, PARALLEL_SEARCH_IRRELEVANT_PLACE, BID_CENTS_7, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPC_ONLY, MARKET_SEARCH_IRRELEVANT_PLACE, BID_CENTS_7, BID_KEEP_OLD, BID_KEEP_OLD},

                        {CARD_NO_LINK_CPA_ONLY, CARD_IRRELEVANT_PLACE, BID_KEEP_OLD, BID_CENTS_10, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPA_ONLY, PARALLEL_SEARCH_IRRELEVANT_PLACE, BID_CENTS_7, BID_KEEP_OLD, BID_KEEP_OLD},
                        {CARD_NO_LINK_CPA_ONLY, MARKET_SEARCH_IRRELEVANT_PLACE, BID_CENTS_7, BID_KEEP_OLD, BID_KEEP_OLD}
                }
        );
    }

    /**
     * <b>Установленное</b> рекомендованное значение для компоненты ставки не должно быть меньше минимального из глобальных limits.
     */
    @Test
    public void test_updateBid_should_correctlyApplyMinLimitsForRecommendationValues_when_recommendedValueIsTooSmall()
            throws InterruptedException, SAXException, ExecutionException, IOException, BidValueLimitsViolationException {

        ReportRecommendationsAnswerOrError cardRecommendations
                = buildCardRecommendationsFromFile(getContentStreamFromExplicitFile(HYBRID_CARD_FILE_REC_OK_SMALL_VALUES));
        BidRecommendations parallelSearchRecommendations
                = buildParallelSearchRecommendationsFromFile(getContentStreamFromExplicitFile(PARALLEL_SEARCH_FILE_REC_OK_SMALL_VALUES));
        BidRecommendations marketSearchRecommendations
                = buildMarketSearchRecommendationsFromFile(getContentStreamFromExplicitFile(MARKET_SEARCH_FILE_REC_OK_SMALL_VALUES));

        Map<BidPlace, Integer> hybridReferenceValuesMap = ImmutableMap.of(
                BidPlace.CARD, BID_CENTS_1,
                BidPlace.MARKET_PLACE, BID_CENTS_1
        );

        testCreateBidUpdateMethod(
                (goal, link) -> builder()
                        .withGoal(goal)
                        .withOfferName("irrelevant name")
                        .build(),
                () -> AuctionBulkCommon.createAuctionOfferBidWithoutValues(SHOP_ID_774, GROUP_ID_1, "irrelevant name", PUBLISHED),
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