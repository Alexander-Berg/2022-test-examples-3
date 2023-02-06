package ru.yandex.market.billing.imports.bankorder.dao;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class BankOrderDaoTest extends FunctionalTest {

    @Autowired
    private BankOrderDao bankOrderDao;

    @DbUnitDataSet(before = "BankOrderItem.before.csv",
            after = "BankOrderItem.after.csv")
    @Test
    void test_deleteBankOrderItemWhenPaymentBatchIdGiven() {
        bankOrderDao.deleteBankOrderItems(Set.of(102L, 103L, 104L, 105L, 106L));
    }
}
