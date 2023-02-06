package ru.yandex.market.partner.auction.createupdate.limits;

import java.io.IOException;
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
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_10100;
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
 * <br>Значения выше max-глобальных приводят к {@link BidValueLimitsViolationException}
 *
 * @author vbudnev
 */
@RunWith(Parameterized.class)
public class RecommendedExceedsGlobalLimitTest extends AbstractParserTestExtended {

    private static final String HYBRID_CARD_FILE_REC_OK_BIG_VALUES = "./resources/card/ValueLimits_ok_with_big_values.xml";
    private static final String PARALLEL_SEARCH_FILE_REC_OK_BIG_VALUES = "./resources/parallel_search/ValueLimits_ok_with_big_values.xml";
    private static final String MARKET_SEARCH_FILE_REC_OK_BIG_VALUES = "./resources/market_search/ValueLimits_ok_with_big_values.xml";

    private AuctionBidComponentsLink linkType;
    private HybridGoal goal;

    public RecommendedExceedsGlobalLimitTest(AuctionBidComponentsLink linkType, HybridGoal goal) {
        this.linkType = linkType;
        this.goal = goal;
    }

    @Parameterized.Parameters(name = "{index}: linkType={0} goal={1})")
    public static Collection<Object[]> testCases() {
        return Arrays.asList(
                new Object[][]{
                        {CARD_LINK_FEE_VARIABLE, CARD_IRRELEVANT_PLACE},
                        {CARD_LINK_FEE_VARIABLE, PARALLEL_SEARCH_IRRELEVANT_PLACE},
                        {CARD_LINK_FEE_VARIABLE, MARKET_SEARCH_IRRELEVANT_PLACE},
                        {CARD_LINK_CBID_VARIABLE, CARD_IRRELEVANT_PLACE},
                        {CARD_LINK_CBID_VARIABLE, PARALLEL_SEARCH_IRRELEVANT_PLACE},
                        {CARD_LINK_CBID_VARIABLE, MARKET_SEARCH_IRRELEVANT_PLACE},
                        {CARD_NO_LINK_FEE_PRIORITY, CARD_IRRELEVANT_PLACE},
                        {CARD_NO_LINK_FEE_PRIORITY, PARALLEL_SEARCH_IRRELEVANT_PLACE},
                        {CARD_NO_LINK_FEE_PRIORITY, MARKET_SEARCH_IRRELEVANT_PLACE},
                        {CARD_NO_LINK_CBID_PRIORITY, CARD_IRRELEVANT_PLACE},
                        {CARD_NO_LINK_CBID_PRIORITY, PARALLEL_SEARCH_IRRELEVANT_PLACE},
                        {CARD_NO_LINK_CBID_PRIORITY, MARKET_SEARCH_IRRELEVANT_PLACE},
                        {CARD_NO_LINK_CPC_ONLY, CARD_IRRELEVANT_PLACE},
                        {CARD_NO_LINK_CPC_ONLY, PARALLEL_SEARCH_IRRELEVANT_PLACE},
                        {CARD_NO_LINK_CPC_ONLY, MARKET_SEARCH_IRRELEVANT_PLACE},
                        {CARD_NO_LINK_CPA_ONLY, CARD_IRRELEVANT_PLACE},
                        {CARD_NO_LINK_CPA_ONLY, PARALLEL_SEARCH_IRRELEVANT_PLACE},
                        {CARD_NO_LINK_CPA_ONLY, MARKET_SEARCH_IRRELEVANT_PLACE},
                }
        );
    }

    /**
     * Установленное рекомендованное значение для компоненты ставки не может превышать глобальное max значение для компоненты.
     */
    @Test(expected = BidValueLimitsViolationException.class)
    public void test_updateBid_should_throw_when_recommendedValueExceedsGlobalLimits()
            throws InterruptedException, SAXException, ExecutionException, IOException, BidValueLimitsViolationException {


        Map<BidPlace, Integer> hybridReferenceValuesMap = ImmutableMap.of(
                BidPlace.CARD, BID_CENTS_10100,
                BidPlace.MARKET_PLACE, BID_CENTS_10100
        );

        ReportRecommendationsAnswerOrError cardRecommendations
                = buildCardRecommendationsFromFile(getContentStreamFromExplicitFile(HYBRID_CARD_FILE_REC_OK_BIG_VALUES));

        BidRecommendations parallelSearchRecommendations
                = buildParallelSearchRecommendationsFromFile(getContentStreamFromExplicitFile(PARALLEL_SEARCH_FILE_REC_OK_BIG_VALUES));

        BidRecommendations marketSearchRecommendations
                = buildMarketSearchRecommendationsFromFile(getContentStreamFromExplicitFile(MARKET_SEARCH_FILE_REC_OK_BIG_VALUES));

        testCreateBidUpdateMethod(
                (goal, link) -> builder()
                        .withGoal(goal)
                        .withOfferName("irrelevant name")
                        .build(),
                () -> AuctionBulkCommon.createAuctionOfferBidWithoutValues(SHOP_ID_774, GROUP_ID_1, "irrelevant name", PUBLISHED),
                linkType,
                goal,
                null,
                null,
                null,
                LIMITS,
                cardRecommendations,
                parallelSearchRecommendations,
                marketSearchRecommendations,
                hybridReferenceValuesMap
        );
    }

}