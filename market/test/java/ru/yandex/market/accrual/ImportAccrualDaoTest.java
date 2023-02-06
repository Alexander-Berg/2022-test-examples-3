package ru.yandex.market.accrual;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.oebs.OperatingUnit;
import ru.yandex.market.core.order.payment.AccrualProductType;
import ru.yandex.market.core.order.payment.EntityType;
import ru.yandex.market.core.order.payment.PaymentOrderCurrency;
import ru.yandex.market.core.order.payment.PayoutStatus;
import ru.yandex.market.core.order.payment.PaysysTypeCc;
import ru.yandex.market.core.order.payment.TransactionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Тест для {@link ImportAccrualDao}
 */
class ImportAccrualDaoTest extends FunctionalTest {

    @Autowired
    private ImportAccrualDao importAccrualDao;

    @Test
    @DbUnitDataSet(after = "ImportAccrualDaoTest.testInsertAccrual.after.csv")
    void testInsertAccrual() {
        importAccrualDao.insertAccrual(List.of(
                Accrual.builder()
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
                        .setCreatedAt(
                                LocalDateTime.of(2021, 12, 10, 16, 0)
                                        .atZone(ZoneId.systemDefault()).toInstant()
                        )
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPayoutStatus(PayoutStatus.NEW)
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "ImportAccrualDaoTest.testDeleteDataByDates.before.csv",
            after = "ImportAccrualDaoTest.testDeleteDataByDates.after.csv"
    )
    void testDeleteDataByDates() {
        importAccrualDao.deleteDataByDates(
                LocalDate.of(2021, 12, 20),
                LocalDate.of(2021, 12, 21)
        );
    }
}
