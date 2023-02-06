package ru.yandex.market.cpa;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class BankOrderDaoTest extends FunctionalTest {

    @Autowired
    private BankOrderDao bankOrderDao;

    @Test
    @DbUnitDataSet(
            before = "BankOrderItem.before.csv",
            after = "BankOrderItem.after.csv"
    )
    void test_deleteBankOrderItemWhenPaymentBatchIdGiven() {
        bankOrderDao.deleteBankOrderItems(Set.of("102", "103", "104", "105", "106"));
    }
}
