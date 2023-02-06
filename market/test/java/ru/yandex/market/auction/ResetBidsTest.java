package ru.yandex.market.auction;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.market.auction.SyncBidComponentServiceCommon.DATE_2019_02_10_0000;

/**
 * Тесты для {@link SyncBidComponentService}.
 *
 * @author vbudnev
 */
@DbUnitDataSet(before = "db/common.csv")
class ResetBidsTest extends FunctionalTest {

    @Autowired
    private SyncBidComponentService service;

    @DisplayName("Сброс fee для офферов и категорий")
    @DbUnitDataSet(
            before = "db/reset/ResetBidsTest.before.csv",
            after = "db/reset/ResetBidsTest.after.csv"
    )
    @Test
    void test_resetShopFeeBids() {
        service.resetShopFeeBids(ImmutableList.of(1L, 2L), DATE_2019_02_10_0000);
    }

}