package ru.yandex.market.partner.auction.inference;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.BidLimits;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionBidValuesLimits;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.partner.auction.AuctionOffer;
import ru.yandex.market.partner.auction.BidComponentsInferenceManager;

import static org.mockito.Mockito.when;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_LINK_CBID_VARIABLE;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_LINK_FEE_VARIABLE;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CBID_PRIORITY;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CPA_ONLY;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_FEE_PRIORITY;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.MAX_VALUES;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SHOP_ID_774;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.BID_CENTS_33;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.BID_CENTS_55;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.BID_CENTS_77;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.MIN_VALUES;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.createOfferForPartialTest;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.valuesEmpty;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.verifyResultsForHybridPayload;

/**
 * Проверка выведения референсных значений.
 * <br>Проверяем, что при выполнении следующих условий:
 * <br>-отсутствии актуальных значений ставок
 * <br>-отсутствии категорийных
 * <br>-наличии {@link AuctionBidValuesLimits#minValues}
 * <br>Значения {@link AuctionOffer#referenceValues} берутся из {@link AuctionBidValuesLimits#minValues}
 *
 * @author vbudnev
 */
@RunWith(Parameterized.class)
public class InferenceFromMinTest {

    private final static AuctionOfferId ID = new AuctionOfferId("some_irrelevant_name");

    private static final AuctionBidValuesLimits LIMITS_NO_DEF = new AuctionBidValuesLimits(MIN_VALUES, MAX_VALUES);
    private static final Integer IGNORE = null;

    private AuctionBidComponentsLink existedLinkType;
    private AuctionBidValues values;
    private Integer expectedBid;
    private Integer expectedCbid;
    private Integer expectedFee;

    public InferenceFromMinTest(AuctionBidComponentsLink existedLinkType, AuctionBidValues values, Integer expectedBid, Integer expectedCbid, Integer expectedFe) {
        this.existedLinkType = existedLinkType;
        this.values = values;
        this.expectedBid = expectedBid;
        this.expectedCbid = expectedCbid;
        this.expectedFee = expectedFe;
    }

    @Parameterized.Parameters(name = "{index}: originalLink={0} existedActualValues={1} expected=(b:{2},c:{3},f:{4})")
    public static Collection<Object[]> testCases() {
        return Arrays.asList(
                new Object[][]{
                        {CARD_NO_LINK_CBID_PRIORITY, null, BID_CENTS_55, BID_CENTS_33, BID_CENTS_77},
                        {CARD_LINK_FEE_VARIABLE, null, BID_CENTS_55, IGNORE, BID_CENTS_77},
                        {CARD_LINK_CBID_VARIABLE, null, BID_CENTS_55, BID_CENTS_33, IGNORE},
                        {CARD_NO_LINK_FEE_PRIORITY, null, BID_CENTS_55, BID_CENTS_33, BID_CENTS_77},
                        {CARD_NO_LINK_CPC_ONLY, null, BID_CENTS_55, BID_CENTS_33, BID_CENTS_77},
                        {CARD_NO_LINK_CPA_ONLY, null, BID_CENTS_55, BID_CENTS_33, BID_CENTS_77},

                        {CARD_NO_LINK_CBID_PRIORITY, valuesEmpty, BID_CENTS_55, BID_CENTS_33, BID_CENTS_77},
                        {CARD_LINK_FEE_VARIABLE, valuesEmpty, BID_CENTS_55, IGNORE, BID_CENTS_77},
                        {CARD_LINK_CBID_VARIABLE, valuesEmpty, BID_CENTS_55, BID_CENTS_33, IGNORE},
                        {CARD_NO_LINK_FEE_PRIORITY, valuesEmpty, BID_CENTS_55, BID_CENTS_33, BID_CENTS_77},
                        {CARD_NO_LINK_CPC_ONLY, valuesEmpty, BID_CENTS_55, BID_CENTS_33, BID_CENTS_77},
                        {CARD_NO_LINK_CPA_ONLY, valuesEmpty, BID_CENTS_55, BID_CENTS_33, BID_CENTS_77},
                }
        );
    }

    @Test
    public void test_prepareHybridPayload_should_inferFromMin_whenHasMinButNoDefaultNoActualNoCategory() {
        BidLimits bidLimits = Mockito.mock(BidLimits.class);
        when(bidLimits.limits()).thenReturn(LIMITS_NO_DEF);

        AuctionService auctionService = Mockito.mock(AuctionService.class);
        when(auctionService.getCategoryBids(SHOP_ID_774)).thenReturn(Collections.EMPTY_LIST);

        BidComponentsInferenceManager bidComponentsInferenceManager = new BidComponentsInferenceManager(auctionService, bidLimits);

        AuctionOffer offer = createOfferForPartialTest(ID, existedLinkType, values);
        bidComponentsInferenceManager.buildAndMergeReferenceValues(ImmutableList.of(offer), SHOP_ID_774);

        verifyResultsForHybridPayload(offer, expectedBid, expectedCbid, expectedFee);
    }

}