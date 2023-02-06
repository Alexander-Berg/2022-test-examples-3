package ru.yandex.market.partner.auction.inference;

import java.math.BigInteger;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ru.yandex.market.core.AbstractParserTest;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.partner.auction.AuctionOffer;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.core.matchers.MapHasSize.mapIsEmpty;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.LIMIT_VAL_10000;

/**
 * @author vbudnev
 */
public class InferenceCommon extends AbstractParserTest {


    static final Integer BID_CENTS_21 = 21;
    static final Integer BID_CENTS_22 = 22;
    static final Integer BID_CENTS_23 = 23;
    static final Integer BID_CENTS_24 = 24;
    static final Integer BID_CENTS_33 = 33;
    static final Integer BID_CENTS_55 = 55;
    static final Integer BID_CENTS_77 = 77;
    static final Integer BID_CENTS_101 = 101;
    static final Integer BID_CENTS_102 = 102;
    static final Integer BID_CENTS_103 = 103;
    static final Integer BID_CENTS_111 = 111;
    static final BigInteger LIMIT_VAL_21 = BigInteger.valueOf(BID_CENTS_21);
    static final BigInteger LIMIT_VAL_22 = BigInteger.valueOf(BID_CENTS_22);
    static final BigInteger LIMIT_VAL_23 = BigInteger.valueOf(BID_CENTS_23);
    static final BigInteger LIMIT_VAL_24 = BigInteger.valueOf(BID_CENTS_24);
    static final BigInteger LIMIT_VAL_33 = BigInteger.valueOf(BID_CENTS_33);
    static final BigInteger LIMIT_VAL_55 = BigInteger.valueOf(BID_CENTS_55);
    static final BigInteger LIMIT_VAL_77 = BigInteger.valueOf(BID_CENTS_77);
    static final AuctionBidValues MIN_VALUES = new AuctionBidValues(ImmutableMap.of(
            BidPlace.CARD, LIMIT_VAL_33,
            BidPlace.SEARCH, LIMIT_VAL_55,
            BidPlace.MARKET_SEARCH, LIMIT_VAL_33,
            BidPlace.MARKET_PLACE, LIMIT_VAL_77
    ));
    static final AuctionBidValues DEF_VALUES = new AuctionBidValues(ImmutableMap.of(
            BidPlace.CARD, LIMIT_VAL_21,
            BidPlace.SEARCH, LIMIT_VAL_22,
            BidPlace.MARKET_SEARCH, LIMIT_VAL_23,
            BidPlace.MARKET_PLACE, LIMIT_VAL_24
    ));
    static final AuctionBidValues MAX_VALUES = new AuctionBidValues(ImmutableMap.of(
            BidPlace.CARD, LIMIT_VAL_10000,
            BidPlace.SEARCH, LIMIT_VAL_10000,
            BidPlace.MARKET_SEARCH, LIMIT_VAL_10000,
            BidPlace.MARKET_PLACE, LIMIT_VAL_10000
    ));
    static final AuctionBidValues valuesEmpty = new AuctionBidValues();
    static final AuctionBidValues valuesHasC = new AuctionBidValues(ImmutableMap.of(
            BidPlace.CARD, new BigInteger("102")
    ));
    static final AuctionBidValues valuesHasF = new AuctionBidValues(ImmutableMap.of(
            BidPlace.MARKET_PLACE, new BigInteger("103")
    ));
    static final AuctionBidValues valuesHasFC = new AuctionBidValues(ImmutableMap.of(
            BidPlace.MARKET_PLACE, new BigInteger("103"),
            BidPlace.CARD, new BigInteger("102")
    ));
    static final AuctionBidValues valuesHasB = new AuctionBidValues(ImmutableMap.of(
            BidPlace.SEARCH, new BigInteger("101")
    ));
    static final AuctionBidValues valuesHasBF = new AuctionBidValues(ImmutableMap.of(
            BidPlace.SEARCH, new BigInteger("101"),
            BidPlace.MARKET_PLACE, new BigInteger("103")
    ));
    static final AuctionBidValues valuesHasBC = new AuctionBidValues(ImmutableMap.of(
            BidPlace.SEARCH, new BigInteger("101"),
            BidPlace.CARD, new BigInteger("102")
    ));
    static final AuctionBidValues valuesHasBCF = new AuctionBidValues(ImmutableMap.of(
            BidPlace.SEARCH, new BigInteger("101"),
            BidPlace.CARD, new BigInteger("102"),
            BidPlace.MARKET_PLACE, new BigInteger("103")
    ));

    static AuctionOffer createOfferForPartialTest(AuctionOfferId offerId, AuctionBidComponentsLink linkType, AuctionBidValues actualValues) {
        AuctionOfferBid bid = new AuctionOfferBid();
        bid.setLinkType(linkType);
        bid.setOfferId(offerId);
        bid.setValues(actualValues);

        AuctionOffer offer = new AuctionOffer(offerId);
        offer.setOfferBid(bid);
        return offer;
    }

    public static void verifyResultsForHybridPayload(
            AuctionOffer offer,
            Integer expectedBidValue,
            Integer expectedCbidValue,
            Integer expectedFeeValue
    ) {
        Map<BidPlace, Integer> referenceValues = offer.getReferenceValues();
        assertNotNull(
                String.format("inference values must not be null. values=%s link=%s offerId=%s",
                        offer.getOfferBid().getValues(),
                        offer.getOfferBid().getLinkType(),
                        offer.getOfferId()
                ),
                referenceValues
        );
        assertThat(
                String.format("inference values must not be null. values=%s link=%s offerId=%s",
                        offer.getOfferBid().getValues(),
                        offer.getOfferBid().getLinkType(),
                        offer.getOfferId()
                ),
                referenceValues,
                not(mapIsEmpty())
        );

        if (expectedCbidValue != null) {
            assertThat(
                    String.format("invalid bid value for CBID component for values=%s link=%s offerId=%s",
                            offer.getOfferBid().getValues(),
                            offer.getOfferBid().getLinkType(),
                            offer.getOfferId()
                    ),
                    referenceValues, hasEntry(BidPlace.CARD, expectedCbidValue)
            );
        }
        if (expectedFeeValue != null) {
            assertThat(
                    String.format("invalid bid value for FEE component for values=%s link=%s offerId=%s",
                            offer.getOfferBid().getValues(),
                            offer.getOfferBid().getLinkType(),
                            offer.getOfferId()
                    ),
                    referenceValues, hasEntry(BidPlace.MARKET_PLACE, expectedFeeValue)
            );
        }

        if (expectedBidValue != null) {
            assertThat(
                    String.format("invalid bid value for BID component for values=%s link=%s offerId=%s",
                            offer.getOfferBid().getValues(),
                            offer.getOfferBid().getLinkType(),
                            offer.getOfferId()
                    ),
                    referenceValues, hasEntry(BidPlace.SEARCH, expectedBidValue)
            );
        }


        //не используется и не должно неявно заполнятся
        assertThat(
                String.format("invalid bid value for MBID component for values=%s link=%s offerId=%s",
                        offer.getOfferBid().getValues(),
                        offer.getOfferBid().getLinkType(),
                        offer.getOfferId()
                ),
                referenceValues, not(hasKey(BidPlace.MARKET_SEARCH))
        );

    }
}