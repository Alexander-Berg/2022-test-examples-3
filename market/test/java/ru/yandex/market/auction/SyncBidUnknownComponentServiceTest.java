package ru.yandex.market.auction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.market.auction.SyncBidComponentServiceCommon.BID_IDS;
import static ru.yandex.market.auction.SyncBidComponentServiceCommon.DATE_2019_02_10_0000;

/**
 * Тесты для {@link SyncBidComponentService}.
 * Для неизвестного типа оффера.
 *
 * @author vbudnev
 */
@DbUnitDataSet(before = "db/common.csv")
class SyncBidUnknownComponentServiceTest extends FunctionalTest {

    @Autowired
    private SyncBidComponentService service;

    /**
     * Тесты для неизвестного типа оффера.
     */
    @DisplayName("Неизвестны тип оффера. cbid > bid")
    @DbUnitDataSet(
            before = "db/unknown/cbidGreaterThenBid.before.csv",
            after = "db/unknown/cbidGreaterThenBid.after.csv"
    )
    @Test
    void test_syncBasedOnUnknownBids_cbidGtBid() {
        service.syncBasedOnMinBids(BID_IDS, DATE_2019_02_10_0000);
    }

    @DisplayName("Неизвестны тип оффера. cbid < bid")
    @DbUnitDataSet(
            before = "db/unknown/bidGreaterThenCbid.before.csv",
            after = "db/unknown/bidGreaterThenCbid.after.csv"
    )
    @Test
    void test_syncBasedOnUnknownBids_cbidLtBid() {
        service.syncBasedOnMinBids(BID_IDS, DATE_2019_02_10_0000);
    }

    @DisplayName("Неизвестны тип оффера. cbid is null & bid is not null")
    @DbUnitDataSet(
            before = "db/unknown/bidNotNullCbidNull.before.csv",
            after = "db/unknown/bidNotNullCbidNull.after.csv"
    )
    @Test
    void test_syncBasedOnUnknownBids_cbidIsNull() {
        service.syncBasedOnMinBids(BID_IDS, DATE_2019_02_10_0000);
    }

    @DisplayName("Неизвестны тип оффера. cbid is not null & bid is null")
    @DbUnitDataSet(
            before = "db/unknown/bidNullCbidNotNull.before.csv",
            after = "db/unknown/bidNullCbidNotNull.after.csv"
    )
    @Test
    void test_syncBasedOnUnknownBids_bidIsNull() {
        service.syncBasedOnMinBids(BID_IDS, DATE_2019_02_10_0000);
    }

}