package ru.yandex.market.partner.auction.inference;

import java.util.Arrays;
import java.util.Collection;

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
import ru.yandex.market.core.auction.model.AuctionCategoryBid;
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
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_111;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.MAX_VALUES;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SHOP_ID_774;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.BID_CENTS_101;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.BID_CENTS_102;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.BID_CENTS_103;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.DEF_VALUES;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.MIN_VALUES;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.createOfferForPartialTest;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.valuesHasB;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.valuesHasBC;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.valuesHasBCF;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.valuesHasBF;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.valuesHasC;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.valuesHasF;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.valuesHasFC;
import static ru.yandex.market.partner.auction.inference.InferenceCommon.verifyResultsForHybridPayload;

/**
 * Проверка выведения референсных значений.
 * <br>Проверяем, что при выполнении следующих условий:
 * <br>-наличии актуальных значений ставок
 * <br>-наличии категорийных
 * <br>-наличии {@link AuctionBidValuesLimits#minValues}
 * <br>Значения {@link AuctionOffer#referenceValues} берутся из актуальных
 *
 * @author vbudnev
 */
@RunWith(Parameterized.class)
public class InferenceFromActualTest {

    private final static AuctionOfferId ID = new AuctionOfferId("some_irrelevant_name");

    private final static AuctionBidValuesLimits LIMITS = new AuctionBidValuesLimits(MIN_VALUES, MAX_VALUES, DEF_VALUES);

    private final static AuctionCategoryBid CATEGORY_BIDS_FOR_ALL = new AuctionCategoryBid(
            SHOP_ID_774,
            AuctionService.CATEGORY_ALL,
            AuctionBidValues.fromSameBids(BID_CENTS_111)
    );

    private AuctionBidComponentsLink existedLinkType;
    private AuctionBidValues values;
    private Integer expectedBid;
    private Integer expectedCbid;
    private Integer expectedFee;

    public InferenceFromActualTest(AuctionBidComponentsLink existedLinkType, AuctionBidValues values, Integer expectedBid, Integer expectedCbid, Integer expectedFe) {
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
                        {CARD_NO_LINK_CBID_PRIORITY, valuesHasBCF, BID_CENTS_101, BID_CENTS_102, BID_CENTS_103},
                        {CARD_LINK_FEE_VARIABLE, valuesHasBCF, BID_CENTS_101, BID_CENTS_102, BID_CENTS_103},
                        {CARD_LINK_CBID_VARIABLE, valuesHasBCF, BID_CENTS_101, BID_CENTS_102, BID_CENTS_103},
                        {CARD_NO_LINK_FEE_PRIORITY, valuesHasBCF, BID_CENTS_101, BID_CENTS_102, BID_CENTS_103},
                        {CARD_NO_LINK_CPA_ONLY, valuesHasBCF, BID_CENTS_101, BID_CENTS_102, BID_CENTS_103},
                        {CARD_NO_LINK_CPC_ONLY, valuesHasBCF, BID_CENTS_101, BID_CENTS_102, BID_CENTS_103},

                        {CARD_LINK_CBID_VARIABLE, valuesHasF, BID_CENTS_111, BID_CENTS_111, BID_CENTS_103},
                        {CARD_NO_LINK_FEE_PRIORITY, valuesHasF, BID_CENTS_111, BID_CENTS_111, BID_CENTS_103},
                        {CARD_NO_LINK_CBID_PRIORITY, valuesHasF, BID_CENTS_111, BID_CENTS_111, BID_CENTS_103},
                        {CARD_LINK_FEE_VARIABLE, valuesHasF, BID_CENTS_111, BID_CENTS_111, BID_CENTS_103},
                        {CARD_NO_LINK_CPA_ONLY, valuesHasF, BID_CENTS_111, BID_CENTS_111, BID_CENTS_103},
                        {CARD_NO_LINK_CPC_ONLY, valuesHasF, BID_CENTS_111, BID_CENTS_111, BID_CENTS_103},

                        {CARD_NO_LINK_CBID_PRIORITY, valuesHasB, BID_CENTS_101, BID_CENTS_111, BID_CENTS_111},
                        {CARD_LINK_FEE_VARIABLE, valuesHasB, BID_CENTS_101, BID_CENTS_111, BID_CENTS_111},
                        {CARD_LINK_CBID_VARIABLE, valuesHasB, BID_CENTS_101, BID_CENTS_111, BID_CENTS_111},
                        {CARD_NO_LINK_FEE_PRIORITY, valuesHasB, BID_CENTS_101, BID_CENTS_111, BID_CENTS_111},
                        {CARD_NO_LINK_CPA_ONLY, valuesHasB, BID_CENTS_101, BID_CENTS_111, BID_CENTS_111},
                        {CARD_NO_LINK_CPC_ONLY, valuesHasB, BID_CENTS_101, BID_CENTS_111, BID_CENTS_111},

                        {CARD_LINK_CBID_VARIABLE, valuesHasC, BID_CENTS_111, BID_CENTS_102, BID_CENTS_111},
                        {CARD_NO_LINK_FEE_PRIORITY, valuesHasC, BID_CENTS_111, BID_CENTS_102, BID_CENTS_111},
                        {CARD_NO_LINK_CBID_PRIORITY, valuesHasC, BID_CENTS_111, BID_CENTS_102, BID_CENTS_111},
                        {CARD_LINK_FEE_VARIABLE, valuesHasC, BID_CENTS_111, BID_CENTS_102, BID_CENTS_111},
                        {CARD_NO_LINK_CPA_ONLY, valuesHasC, BID_CENTS_111, BID_CENTS_102, BID_CENTS_111},
                        {CARD_NO_LINK_CPC_ONLY, valuesHasC, BID_CENTS_111, BID_CENTS_102, BID_CENTS_111},

                        {CARD_NO_LINK_CBID_PRIORITY, valuesHasFC, BID_CENTS_111, BID_CENTS_102, BID_CENTS_103},
                        {CARD_LINK_FEE_VARIABLE, valuesHasFC, BID_CENTS_111, BID_CENTS_102, BID_CENTS_103},
                        {CARD_LINK_CBID_VARIABLE, valuesHasFC, BID_CENTS_111, BID_CENTS_102, BID_CENTS_103},
                        {CARD_NO_LINK_FEE_PRIORITY, valuesHasFC, BID_CENTS_111, BID_CENTS_102, BID_CENTS_103},
                        {CARD_NO_LINK_CPA_ONLY, valuesHasFC, BID_CENTS_111, BID_CENTS_102, BID_CENTS_103},
                        {CARD_NO_LINK_CPC_ONLY, valuesHasFC, BID_CENTS_111, BID_CENTS_102, BID_CENTS_103},

                        {CARD_NO_LINK_CBID_PRIORITY, valuesHasBC, BID_CENTS_101, BID_CENTS_102, BID_CENTS_111},
                        {CARD_LINK_FEE_VARIABLE, valuesHasBC, BID_CENTS_101, BID_CENTS_102, BID_CENTS_111},
                        {CARD_LINK_CBID_VARIABLE, valuesHasBC, BID_CENTS_101, BID_CENTS_102, BID_CENTS_111},
                        {CARD_NO_LINK_FEE_PRIORITY, valuesHasBC, BID_CENTS_101, BID_CENTS_102, BID_CENTS_111},
                        {CARD_NO_LINK_CPA_ONLY, valuesHasBC, BID_CENTS_101, BID_CENTS_102, BID_CENTS_111},
                        {CARD_NO_LINK_CPC_ONLY, valuesHasBC, BID_CENTS_101, BID_CENTS_102, BID_CENTS_111},

                        {CARD_NO_LINK_CBID_PRIORITY, valuesHasBF, BID_CENTS_101, BID_CENTS_111, BID_CENTS_103},
                        {CARD_LINK_FEE_VARIABLE, valuesHasBF, BID_CENTS_101, BID_CENTS_111, BID_CENTS_103},
                        {CARD_LINK_CBID_VARIABLE, valuesHasBF, BID_CENTS_101, BID_CENTS_111, BID_CENTS_103},
                        {CARD_NO_LINK_FEE_PRIORITY, valuesHasBF, BID_CENTS_101, BID_CENTS_111, BID_CENTS_103},
                        {CARD_NO_LINK_CPA_ONLY, valuesHasBF, BID_CENTS_101, BID_CENTS_111, BID_CENTS_103},
                        {CARD_NO_LINK_CPC_ONLY, valuesHasBF, BID_CENTS_101, BID_CENTS_111, BID_CENTS_103}

                }
        );
    }

    @Test
    public void test_prepareHybridPayload_should_inferFromActual_whenHasActualHasCategoryHasDefaultHasMin() {
        BidLimits bidLimits = Mockito.mock(BidLimits.class);
        when(bidLimits.limits()).thenReturn(LIMITS);

        AuctionService auctionService = Mockito.mock(AuctionService.class);
        when(auctionService.getCategoryBids(SHOP_ID_774)).thenReturn(ImmutableList.of(CATEGORY_BIDS_FOR_ALL));

        BidComponentsInferenceManager bidComponentsInferenceManager = new BidComponentsInferenceManager(auctionService, bidLimits);

        AuctionOffer offer = createOfferForPartialTest(ID, existedLinkType, values);
        bidComponentsInferenceManager.buildAndMergeReferenceValues(ImmutableList.of(offer), SHOP_ID_774);

        verifyResultsForHybridPayload(offer, expectedBid, expectedCbid, expectedFee);
    }

}