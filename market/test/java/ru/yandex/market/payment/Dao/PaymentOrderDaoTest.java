package ru.yandex.market.payment.Dao;


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
import ru.yandex.market.core.order.payment.PaymentOrderCurrency;
import ru.yandex.market.core.order.payment.PaymentOrderFactoring;
import ru.yandex.market.core.order.payment.PaysysTypeCc;
import ru.yandex.market.core.order.payment.ProductType;
import ru.yandex.market.core.order.payment.TransactionType;
import ru.yandex.market.payment.model.PaymentOrder;

/**
 * Тест для {@link PaymentOrderDao}
 */
class PaymentOrderDaoTest extends FunctionalTest {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private static final LocalDate DATE_2021_06_28 =
            LocalDate.of(2021, 6, 28);

    private static final LocalDate DATE_2021_06_29 =
            LocalDate.of(2021, 6, 29);

    @Autowired
    private PaymentOrderDao paymentOrderDao;


    @DbUnitDataSet(
            before = "PaymentOrderDaoTest.duplicateInsert.before.csv"
    )
    @DisplayName("Проверяем добавление Payment Order с id, который уже есть в таблице.")
    @Test
    void testDuplicateInsert() {
        Assertions.assertThrows(DuplicateKeyException.class, () -> paymentOrderDao.insertPaymentOrder(List.of(
                PaymentOrder.builder()
                        .setId(1L)
                        .setClientId(2L)
                        .setContractId(3L).setServiceId(4L)
                        .setTransactionType(TransactionType.REFUND)
                        .setSecuredPayment(true)
                        .setFactoring(PaymentOrderFactoring.MARKET)
                        .setProductType(ProductType.SUBSIDY)
                        .setPaysysTypeCc(PaysysTypeCc.ACC_APPLE_PAY)
                        .setTrantime(DATE_2021_06_28.atStartOfDay().atZone(ZONE_ID).toInstant())
                        .setAmount(100L)
                        .setCurrency(PaymentOrderCurrency.RUB)
                        .setExportedToTlog(true).build())
        ));
    }

    @DbUnitDataSet(
            before = "PaymentOrderDaoTest.insert.before.csv",
            after = "PaymentOrderDaoTest.insert.after.csv"
    )
    @DisplayName("Проверяем добавление команд на выплату в Payment Order.")
    @Test
    void testInsert() {
        paymentOrderDao.insertPaymentOrder(List.of(
                PaymentOrder.builder()
                        .setId(1L)
                        .setClientId(1L)
                        .setContractId(1L).setServiceId(1L)
                        .setTransactionType(TransactionType.REFUND)
                        .setSecuredPayment(true)
                        .setFactoring(PaymentOrderFactoring.MARKET)
                        .setProductType(ProductType.PARTNER_PAYMENT)
                        .setPaysysTypeCc(PaysysTypeCc.ACC_APPLE_PAY)
                        .setTrantime(DATE_2021_06_28.atStartOfDay().atZone(ZONE_ID).toInstant())
                        .setAmount(100L)
                        .setCurrency(PaymentOrderCurrency.RUB)
                        .setExportedToTlog(true).build(),
                PaymentOrder.builder()
                        .setId(2L)
                        .setClientId(1L)
                        .setContractId(1L).setServiceId(1L)
                        .setTransactionType(TransactionType.REFUND)
                        .setSecuredPayment(true)
                        .setFactoring(PaymentOrderFactoring.MARKET)
                        .setProductType(ProductType.PARTNER_PAYMENT)
                        .setPaysysTypeCc(PaysysTypeCc.ACC_APPLE_PAY)
                        .setTrantime(DATE_2021_06_28.atStartOfDay().atZone(ZONE_ID).toInstant())
                        .setAmount(100L)
                        .setCurrency(PaymentOrderCurrency.RUB)
                        .setExportedToTlog(true).build()));
    }

    @DbUnitDataSet(
            before = "PaymentOrderDaoTest.deleteBetweenDate.before.csv",
            after = "PaymentOrderDaoTest.deleteBetweenDate.after.csv"
    )
    @DisplayName("Проверяем удаление данных в промежутке.")
    @Test
    void testDeleteBetweenDate() {
        paymentOrderDao.deleteDataForDate(DATE_2021_06_28, DATE_2021_06_29);
    }

    @DbUnitDataSet(
            before = "PaymentOrderDaoTest.deleteAllData.before.csv",
            after = "PaymentOrderDaoTest.deleteAllData.after.csv"
    )
    @DisplayName("Проверяем удаление всех данных из таблицы")
    @Test
    void testDeleteAllData() {
        paymentOrderDao.deleteAllData();
    }

}
