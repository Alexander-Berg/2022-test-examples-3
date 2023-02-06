package ru.yandex.market.auction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * Общие данные для тестов {@link SyncBidComponentService}.
 *
 * @author vbudnev
 */
class SyncBidComponentServiceCommon {

    static final long SHOP_774 = 774L;
    static final long FEED_ID = 1001L;
    static final String OFFER_IDENTITY = "some_identity_paddinggg";

    static final LocalDateTime DATE_2019_02_10_0000 = LocalDate.of(2019, 2, 10).atStartOfDay();
    static final List<BidId> BID_IDS = ImmutableList.of(
            new BidId(SHOP_774, FEED_ID, OFFER_IDENTITY),
            new BidId(SHOP_774, FEED_ID, "some missing offer identity")
    );
}