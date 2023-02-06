package ru.yandex.market.accrual;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.oebs.OperatingUnit;
import ru.yandex.market.core.order.payment.AccrualProductType;
import ru.yandex.market.core.order.payment.EntityType;
import ru.yandex.market.core.order.payment.PaymentOrderCurrency;
import ru.yandex.market.core.order.payment.PaysysTypeCc;
import ru.yandex.market.core.order.payment.TransactionType;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Тест для {@link ImportAccrualCorrectionDao}
 */
class ImportAccrualCorrectionDaoTest extends FunctionalTest {

    @Autowired
    private ImportAccrualCorrectionDao importAccrualCorrectionDao;

    @Test
    @DbUnitDataSet(after = "ImportAccrualCorrectionDaoTest.testInsertAccrualCorrection.after.csv")
    void testInsertAccrualCorrection() {
        importAccrualCorrectionDao.insertAccrualCorrection(List.of(
                AccrualCorrection.builder()
                        .setId(1L)
                        .setEntityId(2L)
                        .setAccrualProductType(AccrualProductType.PARTNER_PAYMENT)
                        .setPaysysType(PaysysTypeCc.ACC_APPLE_PAY)
                        .setCheckouterId(3L)
                        .setTransactionType(TransactionType.PAYMENT)
                        .setOrderId(4L)
                        .setPartnerId(5L)
                        .setTrantime(
                                LocalDateTime.of(2021, 12, 10, 15, 0)
                                        .atZone(ZoneId.systemDefault()).toInstant()
                        )
                        .setAmount(6L)
                        .setExportedToTlog(true)
                        .setEntityType(EntityType.ITEM)
                        .setPaysysPartnerId(7L)
                        .setCurrency(PaymentOrderCurrency.RUB)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setUuid("123")
                        .setStTicket("234")
                        .setLogin("345")
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "ImportAccrualCorrectionDaoTest.testDeleteAllData.before.csv",
            after = "ImportAccrualCorrectionDaoTest.testDeleteAllData.after.csv"
    )
    void testDeleteAllData() {
        importAccrualCorrectionDao.deleteAllData();
    }

}
