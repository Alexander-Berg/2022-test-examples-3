package ru.yandex.market.payout.Dao;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.payout.model.PayoutGroupPaymentOrder;

/**
 * Тест для {@link PayoutGroupPaymentOrderDao}
 */
class PayoutGroupPaymentOrderDaoTest extends FunctionalTest {

    @Autowired
    private PayoutGroupPaymentOrderDao payoutGroupPaymentOrderDao;

    @DbUnitDataSet(
            before = "PayoutGroupPaymentOrderDaoTest/PayoutGroupPaymentOrderDaoTest.duplicateInsert.before.csv"
    )
    @DisplayName("Проверяем выбрасывание ошибки при добавлении связи с существующим payment_order_id")
    @Test
    void testDuplicateInsert() {
        Assertions.assertThrows(DuplicateKeyException.class,
                () -> payoutGroupPaymentOrderDao.insertPayoutGroupPaymentOrder(List.of(
                        new PayoutGroupPaymentOrder(1L, 1L))));
    }

    @DbUnitDataSet(
            before = "PayoutGroupPaymentOrderDaoTest/PayoutGroupPaymentOrderDaoTest.insert.before.csv",
            after = "PayoutGroupPaymentOrderDaoTest/PayoutGroupPaymentOrderDaoTest.insert.after.csv"
    )
    @DisplayName("Проверяем добавление связи в PayoutGroupPaymentOrder.")
    @Test
    void testInsert() {
        payoutGroupPaymentOrderDao.insertPayoutGroupPaymentOrder(List.of(
                new PayoutGroupPaymentOrder(1L, 1L)));
    }

    @DbUnitDataSet(
            before = "PayoutGroupPaymentOrderDaoTest/PayoutGroupPaymentOrderDaoTest.deleteAllData.before.csv",
            after = "PayoutGroupPaymentOrderDaoTest/PayoutGroupPaymentOrderDaoTest.deleteAllData.after.csv"
    )
    @DisplayName("Проверяем удаление всех данных из таблицы")
    @Test
    void testDeleteAllData() {
        payoutGroupPaymentOrderDao.deleteAllData();
    }
}
