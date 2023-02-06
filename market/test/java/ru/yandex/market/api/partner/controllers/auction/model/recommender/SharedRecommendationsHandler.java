package ru.yandex.market.api.partner.controllers.auction.model.recommender;

import java.math.BigDecimal;

import ru.yandex.market.api.partner.controllers.auction.model.AuctionOffer;
import ru.yandex.market.core.auction.model.AuctionOfferId;

import static ru.yandex.market.mbi.util.MoneyValuesHelper.centsToUE;

/**
 * @author vbudnev
 */
public class SharedRecommendationsHandler {
    static final String SOME_OFFER_NAME = "some_offer_name";
    static final AuctionOfferId SOME_AUCTION_OFFER_ID = new AuctionOfferId(12L, "123");
    static final AuctionOffer SOME_AUCTION_OFFER = new AuctionOffer() {{
        setOfferId(SOME_AUCTION_OFFER_ID);
    }};

    static final Integer BID_CENTS_111 = 111;
    static final Integer BID_CENTS_222 = 222;
    static final Integer BID_CENTS_333 = 333;
    static final Integer BID_CENTS_1 = 1;
    static final Integer BID_CENTS_2 = 2;
    static final Integer BID_CENTS_3 = 3;
    static final BigDecimal BID_UE_1_11 = centsToUE(111);
    static final BigDecimal BID_UE_2_22 = centsToUE(222);
    static final BigDecimal BID_UE_3_33 = centsToUE(333);
    static final BigDecimal BID_UE_0_01 = centsToUE(1);
    static final BigDecimal BID_UE_0_02 = centsToUE(2);
    static final BigDecimal BID_UE_0_03 = centsToUE(3);
}
