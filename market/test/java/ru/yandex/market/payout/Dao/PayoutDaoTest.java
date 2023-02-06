package ru.yandex.market.payout.Dao;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.order.payment.EntityType;
import ru.yandex.market.core.order.payment.PaysysTypeCc;
import ru.yandex.market.core.order.payment.ProductType;
import ru.yandex.market.core.order.payment.TransactionType;
import ru.yandex.market.payout.model.Payout;

/**
 * Тест для {@link PayoutDao}
 */
class PayoutDaoTest extends FunctionalTest {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private static final LocalDate DATE_2021_06_28 =
            LocalDate.of(2021, 6, 28);

    private static final LocalDate DATE_2021_06_29 =
            LocalDate.of(2021, 6, 29);

    @Autowired
    private PayoutDao payoutDao;


    @DbUnitDataSet(
            before = "PayoutDaoTest/PayoutDaoTest.duplicateInsert.before.csv"
    )
    @DisplayName("Проверяем Payout на повторное добавление данных, которые должны быть уникальны.")
    @Test
    void testDuplicateInsert() {
        Assertions.assertThrows(DuplicateKeyException.class, () -> payoutDao.insertPayout(List.of(
                Payout.builder()
                        .setPayoutId(1L)
                        .setEntityId(1L)
                        .setCheckouterId(1L)
                        .setTransactionType(TransactionType.REFUND)
                        .setProductType(ProductType.PARTNER_PAYMENT)
                        .setPaysysTypeCc(PaysysTypeCc.ACC_APPLE_PAY)
                        .setOrderId(1L)
                        .setPartnerId(1L)
                        .setTrantime(DATE_2021_06_28.atStartOfDay().atZone(ZONE_ID).toInstant())
                        .setAmount(1000L)
                        .setPayoutGroupId(22312L)
                        .setEntityType(EntityType.DELIVERY)
                        .setPaysysPartnerId(1L).build())
        ));
        Assertions.assertThrows(DuplicateKeyException.class, () -> payoutDao.insertPayout(List.of(
                Payout.builder()
                        .setPayoutId(1999L)
                        .setEntityId(1L)
                        .setCheckouterId(1L)
                        .setTransactionType(TransactionType.REFUND)
                        .setProductType(ProductType.PARTNER_PAYMENT)
                        .setPaysysTypeCc(PaysysTypeCc.ACC_APPLE_PAY)
                        .setOrderId(1L)
                        .setPartnerId(1L)
                        .setTrantime(DATE_2021_06_28.atStartOfDay().atZone(ZONE_ID).toInstant())
                        .setAmount(1000L)
                        .setPayoutGroupId(22312L)
                        .setEntityType(EntityType.DELIVERY)
                        .setPaysysPartnerId(1L).build())
        ));
    }

    @DbUnitDataSet(
            before = "PayoutDaoTest/PayoutDaoTest.insert.before.csv",
            after = "PayoutDaoTest/PayoutDaoTest.insert.after.csv"
    )
    @DisplayName("Проверяем добавление Payout.")
    @Test
    void testInsert() {
        payoutDao.insertPayout(List.of(
                Payout.builder()
                        .setPayoutId(1L)
                        .setEntityId(1L)
                        .setCheckouterId(1L)
                        .setTransactionType(TransactionType.REFUND)
                        .setProductType(ProductType.PARTNER_PAYMENT)
                        .setPaysysTypeCc(PaysysTypeCc.ACC_APPLE_PAY)
                        .setOrderId(1L)
                        .setPartnerId(1L)
                        .setTrantime(DATE_2021_06_28.atStartOfDay().atZone(ZONE_ID).toInstant())
                        .setAmount(1000L)
                        .setPayoutGroupId(22312L)
                        .setEntityType(EntityType.DELIVERY)
                        .setPaysysPartnerId(1L).build(),
                Payout.builder()
                        .setPayoutId(2L)
                        .setEntityId(2L)
                        .setCheckouterId(1L)
                        .setTransactionType(TransactionType.REFUND)
                        .setProductType(ProductType.PARTNER_PAYMENT)
                        .setPaysysTypeCc(PaysysTypeCc.ACC_APPLE_PAY)
                        .setOrderId(1L)
                        .setPartnerId(1L)
                        .setTrantime(DATE_2021_06_28.atStartOfDay().atZone(ZONE_ID).toInstant())
                        .setAmount(1000L)
                        .setPayoutGroupId(22312L)
                        .setEntityType(EntityType.DELIVERY)
                        .setPaysysPartnerId(null).build()));
    }

    @DbUnitDataSet(
            before = "PayoutDaoTest/PayoutDaoTest.deleteBetweenDate.before.csv",
            after = "PayoutDaoTest/PayoutDaoTest.deleteBetweenDate.after.csv"
    )
    @DisplayName("Проверяем удаление данных в промежутке.")
    @Test
    void testDeleteBetweenDate() {
        payoutDao.deleteDataForDate(DATE_2021_06_28, DATE_2021_06_29);
    }

    @DbUnitDataSet(
            before = "PayoutDaoTest/PayoutDaoTest.deleteAllData.before.csv",
            after = "PayoutDaoTest/PayoutDaoTest.deleteAllData.after.csv"
    )
    @DisplayName("Проверяем удаление всех данных из таблицы")
    @Test
    void testDeleteAllData() {
        payoutDao.deleteAllData();
    }

}
