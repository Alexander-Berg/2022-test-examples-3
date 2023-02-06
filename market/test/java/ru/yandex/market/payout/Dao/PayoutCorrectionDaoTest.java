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
import ru.yandex.market.payout.model.PayoutCorrection;

/**
 * Тест для {@link PayoutCorrectionDao}
 */
class PayoutCorrectionDaoTest extends FunctionalTest {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private static final LocalDate DATE_2021_06_28 =
            LocalDate.of(2021, 6, 28);

    private static final LocalDate DATE_2021_06_29 =
            LocalDate.of(2021, 6, 29);

    @Autowired
    private PayoutCorrectionDao payoutCorrectionDao;

    @DbUnitDataSet(
            before = "PayoutCorrectionDaoTest/PayoutCorrectionDaoTest.duplicateInsert.before.csv"
    )
    @DisplayName("Проверяем Payout Correction на повторное добавление данных, которые должны быть уникальны.")
    @Test
    void testDuplicateInsert() {
        Assertions.assertThrows(DuplicateKeyException.class, () -> payoutCorrectionDao.insertPayoutCorrection(List.of(
                PayoutCorrection.builder()
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
                        .setUuid("11122")
                        .setLogin("22111")
                        .setStTicket("1122")
                        .setPayoutGroupId(22312L)
                        .setEntityType(EntityType.DELIVERY)
                        .setPaysysPartnerId(null).build())
        ));
    }

    @DbUnitDataSet(
            before = "PayoutCorrectionDaoTest/PayoutCorrectionDaoTest.insert.before.csv",
            after = "PayoutCorrectionDaoTest/PayoutCorrectionDaoTest.insert.after.csv"
    )
    @DisplayName("Проверяем добавление Payout Correction.")
    @Test
    void testInsert() {
        payoutCorrectionDao.insertPayoutCorrection(List.of(
                PayoutCorrection.builder()
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
                        .setUuid("11122")
                        .setLogin("22111")
                        .setStTicket("1122")
                        .setPayoutGroupId(22312L)
                        .setEntityType(EntityType.DELIVERY)
                        .setAccrualCorrectionId(666L)
                        .setPaysysPartnerId(1L).build(),
                PayoutCorrection.builder()
                        .setPayoutId(2L)
                        .setEntityId(1L)
                        .setCheckouterId(1L)
                        .setTransactionType(TransactionType.REFUND)
                        .setProductType(ProductType.PARTNER_PAYMENT)
                        .setPaysysTypeCc(PaysysTypeCc.ACC_APPLE_PAY)
                        .setOrderId(1L)
                        .setPartnerId(1L)
                        .setTrantime(DATE_2021_06_28.atStartOfDay().atZone(ZONE_ID).toInstant())
                        .setAmount(1000L)
                        .setUuid("11122")
                        .setLogin("22111")
                        .setStTicket("1122")
                        .setPayoutGroupId(22312L)
                        .setEntityType(EntityType.DELIVERY)
                        .setAccrualCorrectionId(null)
                        .setPaysysPartnerId(null).build()));
    }

    @DbUnitDataSet(
            before = "PayoutCorrectionDaoTest/PayoutCorrectionDaoTest.deleteBetweenDate.before.csv",
            after = "PayoutCorrectionDaoTest/PayoutCorrectionDaoTest.deleteBetweenDate.after.csv"
    )
    @DisplayName("Проверяем удаление данных в промежутке.")
    @Test
    void testDeleteBetweenDate() {
        payoutCorrectionDao.deleteDataForDate(DATE_2021_06_28, DATE_2021_06_29);
    }

    @DbUnitDataSet(
            before = "PayoutCorrectionDaoTest/PayoutCorrectionDaoTest.deleteAllData.before.csv",
            after = "PayoutCorrectionDaoTest/PayoutCorrectionDaoTest.deleteAllData.after.csv"
    )
    @DisplayName("Проверяем удаление всех данных из таблицы")
    @Test
    void testDeleteAllData() {
        payoutCorrectionDao.deleteAllData();
    }

}
