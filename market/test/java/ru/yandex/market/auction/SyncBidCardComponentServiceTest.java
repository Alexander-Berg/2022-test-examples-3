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
 * Для карточного оффера.
 *
 * @author vbudnev
 */
@DbUnitDataSet(before = "db/common.csv")
class SyncBidCardComponentServiceTest extends FunctionalTest {

    @Autowired
    private SyncBidComponentService service;

    /**
     * Тесты для карточного оффера.
     */
    @DisplayName("Карточка. cbid != bid")
    @DbUnitDataSet(
            before = "db/card/cbidGreaterThenBid.before.csv",
            after = "db/card/cbidGreaterThenBid.after.csv"
    )
    @Test
    void test_syncBasedOnCardBids_bothNonNullNotEq() {
        service.syncBasedOnCardBids(BID_IDS, DATE_2019_02_10_0000);
    }

    @DisplayName("Карточка. cbid is null & bid is not null")
    @DbUnitDataSet(
            before = "db/card/cbidIsNull.before.csv",
            after = "db/card/cbidIsNull.after.csv"
    )
    @Test
    void test_syncBasedOnCardBids_cbidIsNull() {
        service.syncBasedOnCardBids(BID_IDS, DATE_2019_02_10_0000);
    }

    @DisplayName("Карточка. cbid is not null & bid is null")
    @DbUnitDataSet(
            before = "db/card/cbidGreaterThenBid.before.csv",
            after = "db/card/cbidGreaterThenBid.after.csv"
    )
    @Test
    void test_syncBasedOnCardBids_cbidIsNotNull() {
        service.syncBasedOnCardBids(BID_IDS, DATE_2019_02_10_0000);
    }
}