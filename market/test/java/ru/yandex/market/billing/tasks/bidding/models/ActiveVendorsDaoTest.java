package ru.yandex.market.billing.tasks.bidding.models;

import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class ActiveVendorsDaoTest extends FunctionalTest {

    @Autowired
    private ActiveVendorsDao activeVendorsDao;

    /**
     * Корректное обновление стирает старые записаи, публикует новые.
     */
    @DbUnitDataSet(
            before = "db/active_vendors_before.csv",
            after = "db/active_vendors_after.csv"
    )
    @Test
    void test_reset_when_nonEmptyDataSet_should_clearOldAndInsertNew() {
        Collection<ActiveVendorInfo> testData = ImmutableList.of(
                new ActiveVendorInfo(4L),
                new ActiveVendorInfo(5L),
                new ActiveVendorInfo(6L)
        );
        activeVendorsDao.replaceActiveVendorsWith(testData);
    }

    /**
     * На уровне DAO, ставим запрос на обновление, если на входе пустые данные.
     */
    @Test
    void test_reset_when_emptyDataSet_should_throw() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> activeVendorsDao.replaceActiveVendorsWith(Collections.emptyList()));
    }
}
