package ru.yandex.market.billing.payment.dao;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.OperatingUnit;
import ru.yandex.market.billing.payment.model.OrderPayoutTrantime;
import ru.yandex.market.billing.payment.model.TrantimeStatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тесты для {@link PaymentDao}
 */
class PaymentDaoTest extends FunctionalTest {
    private static final LocalDateTime TEST_DATE_TIME = LocalDateTime.of(2021, 11, 11, 10, 0, 0);
    @Autowired
    private PaymentDao paymentDao;

    @DisplayName("Вставка записей в OrderPayoutTrantimes")
    @Test
    @DbUnitDataSet(
            after = "PaymentDaoTest.testInsertOrderPayoutTrantimes.after.csv"
    )
    void testInsertOrderPayoutTrantimes() {
        List<OrderPayoutTrantime> values = List.of(
                OrderPayoutTrantime.builder()
                        .setOrderId(1L)
                        .setPartnerId(10L)
                        .setStatus(TrantimeStatus.NEW)
                        .setTrantime(TEST_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant())
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .build(),
                OrderPayoutTrantime.builder()
                        .setOrderId(2L)
                        .setPartnerId(20L)
                        .setStatus(TrantimeStatus.PROCESSED)
                        .setTrantime(TEST_DATE_TIME.plusHours(1).atZone(ZoneId.systemDefault()).toInstant())
                        .setOrgId(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .build()
        );
        paymentDao.insertOrderPayoutTrantimesIfNotExists(values);
    }
}
