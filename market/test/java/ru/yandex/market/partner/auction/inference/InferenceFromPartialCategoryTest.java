package ru.yandex.market.partner.auction.inference;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.BidLimits;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionBidValuesLimits;
import ru.yandex.market.core.auction.model.AuctionCategoryBid;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.partner.auction.AuctionOffer;
import ru.yandex.market.partner.auction.BidComponentsInferenceManager;

import static org.mockito.Mockito.when;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CBID_PRIORITY;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.MAX_VALUES;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SHOP_ID_774;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.BID_CENTS_22;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.BID_CENTS_33;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.BID_CENTS_55;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.BID_CENTS_77;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.DEF_VALUES;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.LIMIT_VAL_22;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.LIMIT_VAL_33;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.LIMIT_VAL_55;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.MIN_VALUES;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.createOfferForPartialTest;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.valuesEmpty;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.verifyResultsForHybridPayload;

/**
 * Проверка наследования компонет для категорийных значений ставок.
 * <br>Проверяем не конкретный метод, а более общий случай - выведение референсных значений на основе категорийных.
 * <br>Проверяем, при выполнении следующих условий:
 * <br>-отсутствии актуальных значений ставок
 * <br>-наличии категорийных(неполных)
 * <br>-наличии {@link AuctionBidValuesLimits#minValues}
 * <br>Значения {@link AuctionOffer#referenceValues} берутся из категорийных(с учетом наследования)
 *
 * @author vbudnev
 */
@RunWith(Parameterized.class)
public class InferenceFromPartialCategoryTest {

    private final static AuctionOfferId ID = new AuctionOfferId("some_irrelevant_name");

    private final static AuctionBidValuesLimits LIMITS = new AuctionBidValuesLimits(MIN_VALUES, MAX_VALUES, DEF_VALUES);

    /**
     * mock'и категорийных ставок для ситуации, когда не все компоненты заданы явно.
     */
    private final static AuctionCategoryBid PARTIAL_CATEGORY_BIDS_S = new AuctionCategoryBid(
            SHOP_ID_774,
            AuctionService.CATEGORY_ALL,
            new AuctionBidValues(ImmutableMap.of(
                    BidPlace.SEARCH, LIMIT_VAL_22
            ))
    );

    private final static AuctionCategoryBid PARTIAL_CATEGORY_BIDS_FOR_ALL_SF = new AuctionCategoryBid(
            SHOP_ID_774,
            AuctionService.CATEGORY_ALL,
            new AuctionBidValues(ImmutableMap.of(
                    BidPlace.SEARCH, LIMIT_VAL_22,
                    BidPlace.MARKET_PLACE, LIMIT_VAL_55
            ))
    );

    private final static AuctionCategoryBid PARTIAL_CATEGORY_BIDS_FOR_ALL_SCF = new AuctionCategoryBid(
            SHOP_ID_774,
            AuctionService.CATEGORY_ALL,
            new AuctionBidValues(ImmutableMap.of(
                    BidPlace.SEARCH, LIMIT_VAL_22,
                    BidPlace.CARD, LIMIT_VAL_33,
                    BidPlace.MARKET_PLACE, LIMIT_VAL_55
            ))
    );

    private static final AuctionBidComponentsLink IRRELEVANT_LINK = CARD_NO_LINK_CBID_PRIORITY;

    private AuctionBidComponentsLink existedLinkType;
    private AuctionBidValues values;
    private Integer expectedBid;
    private Integer expectedCbid;
    private Integer expectedFee;
    private AuctionCategoryBid categoryBidsForAll;

    public InferenceFromPartialCategoryTest(
            AuctionBidComponentsLink existedLinkType,
            AuctionBidValues values,
            AuctionCategoryBid categoryBidsForAll,
            Integer expectedBid,
            Integer expectedCbid,
            Integer expectedFee
    ) {
        this.existedLinkType = existedLinkType;
        this.values = values;
        this.expectedBid = expectedBid;
        this.expectedCbid = expectedCbid;
        this.expectedFee = expectedFee;
        this.categoryBidsForAll = categoryBidsForAll;
    }

    /**
     * Для теста наследования в категорийных ставках перебирать все типы нет необходиомсти.
     */
    @Parameterized.Parameters(name = "{index}: originalLink={0} existedActualValues={1} categoryBidsForALL={2} expected=(b:{3},c:{4},f:{5})")
    public static Collection<Object[]> testCases() {
        return Arrays.asList(
                new Object[][]{
                        {IRRELEVANT_LINK, null, PARTIAL_CATEGORY_BIDS_S, BID_CENTS_22, BID_CENTS_22, BID_CENTS_77},
                        {IRRELEVANT_LINK, valuesEmpty, PARTIAL_CATEGORY_BIDS_S, BID_CENTS_22, BID_CENTS_22, BID_CENTS_77},

                        {IRRELEVANT_LINK, null, PARTIAL_CATEGORY_BIDS_FOR_ALL_SF, BID_CENTS_22, BID_CENTS_22, BID_CENTS_55},
                        {IRRELEVANT_LINK, valuesEmpty, PARTIAL_CATEGORY_BIDS_FOR_ALL_SF, BID_CENTS_22, BID_CENTS_22, BID_CENTS_55},

                        {IRRELEVANT_LINK, null, PARTIAL_CATEGORY_BIDS_FOR_ALL_SCF, BID_CENTS_22, BID_CENTS_33, BID_CENTS_55},
                        {IRRELEVANT_LINK, valuesEmpty, PARTIAL_CATEGORY_BIDS_FOR_ALL_SCF, BID_CENTS_22, BID_CENTS_33, BID_CENTS_55},
                }
        );
    }

    @Test
    public void test_prepareHybridPayload_should_inferFromCategory_whenHasCategoryButNoActual() {
        BidLimits bidLimits = Mockito.mock(BidLimits.class);
        when(bidLimits.limits()).thenReturn(LIMITS);

        AuctionService auctionService = Mockito.mock(AuctionService.class);
        when(auctionService.getCategoryBids(SHOP_ID_774)).thenReturn(ImmutableList.of(categoryBidsForAll));

        BidComponentsInferenceManager bidComponentsInferenceManager = new BidComponentsInferenceManager(auctionService, bidLimits);

        AuctionOffer offer = createOfferForPartialTest(ID, existedLinkType, values);
        bidComponentsInferenceManager.buildAndMergeReferenceValues(ImmutableList.of(offer), SHOP_ID_774);

        verifyResultsForHybridPayload(offer, expectedBid, expectedCbid, expectedFee);
    }

}