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
 * Для не карточного оффера.
 *
 * @author vbudnev
 */
@DbUnitDataSet(before = "db/common.csv")
class SyncBidSearchComponentServiceTest extends FunctionalTest {

    @Autowired
    private SyncBidComponentService service;

    /**
     * Тесты для НЕ карточного оффера.
     */
    @DisplayName("Поисковый оффер. cbid != bid")
    @DbUnitDataSet(
            before = "db/search/bidGreaterThenCbid.before.csv",
            after = "db/search/bidGreaterThenCbid.after.csv"
    )
    @Test
    void test_syncBasedOnSearchBids_bothNonNullNotEq() {
        service.syncBasedOnSearchBids(BID_IDS, DATE_2019_02_10_0000);
    }

    @DisplayName("Поисковый оффер. bid is null & cbid is not null")
    @DbUnitDataSet(
            before = "db/search/bidIsNull.before.csv",
            after = "db/search/bidIsNull.after.csv"
    )
    @Test
    void test_syncBasedOnSearchBids_bidIsNull() {
        service.syncBasedOnSearchBids(BID_IDS, DATE_2019_02_10_0000);
    }

    @DisplayName("Поисковый оффер. bid is not null & cbid is null")
    @DbUnitDataSet(
            before = "db/search/bidIsNotNull.before.csv",
            after = "db/search/bidIsNotNull.after.csv"
    )
    @Test
    void test_syncBasedOnSearchBids_bidIsNotNull() {
        service.syncBasedOnSearchBids(BID_IDS, DATE_2019_02_10_0000);
    }

}