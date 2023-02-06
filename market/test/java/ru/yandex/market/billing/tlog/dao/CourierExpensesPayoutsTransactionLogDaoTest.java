package ru.yandex.market.billing.tlog.dao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.OperatingUnit;
import ru.yandex.market.billing.model.tlog.CourierTransactionLogItem;
import ru.yandex.market.billing.tlog.model.PayoutsTransactionLogItem;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.payment.PaymentOrderCurrency;
import ru.yandex.market.core.payment.PaymentOrderFactoring;
import ru.yandex.market.core.payment.PaysysTypeCc;
import ru.yandex.market.core.payment.RecordType;
import ru.yandex.market.core.payment.TransactionLogProductType;
import ru.yandex.market.core.payment.TransactionType;
import ru.yandex.market.core.util.DateTimes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link CourierExpensesPayoutsTransactionLogDao}
 */
public class CourierExpensesPayoutsTransactionLogDaoTest extends FunctionalTest {
    @Autowired
    private CourierExpensesPayoutsTransactionLogDao courierExpensesPayoutsTransactionLogDao;

    @Test
    @DbUnitDataSet(before = "courierExpensesPayoutsTransactionLogDaoTest/testGetAllItems.before.csv")
    void testGetAllItems() {
        List<PayoutsTransactionLogItem> transactionLogItems =
                courierExpensesPayoutsTransactionLogDao.getTransactionLogItems(0, 2);

        assertThat(transactionLogItems).hasSize(2);
        assertThat(transactionLogItems.stream()
                .map(PayoutsTransactionLogItem::getServiceTransactionId)
                .collect(Collectors.toList())
        ).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    @DbUnitDataSet(after = "courierExpensesPayoutsTransactionLogDaoTest/testStoreItems.after.csv")
    void testStoreItems() {
        LocalDate localDate = LocalDate.of(2022, 4, 13);
        Instant instant = DateTimes.toInstantAtDefaultTz(LocalDateTime.of(localDate, LocalTime.of(10, 10, 10)));
        courierExpensesPayoutsTransactionLogDao.insertCourierTransactionLogItems(List.of(
                CourierTransactionLogItem
                        .builder()
                        .setAmount(100L)
                        .setContractId(102L)
                        .setClientId(41L)
                        .setCurrency(PaymentOrderCurrency.RUB)
                        .setUserShiftId(103L)
                        .setEventTime(instant)
                        .setExportTime(instant)
                        .setFactoring(PaymentOrderFactoring.MARKET)
                        .setIgnoreInBalance(false)
                        .setIgnoreInOebs(false)
                        .setKey("key")
                        .setOperatingUnit(OperatingUnit.YANDEX_MARKET)
                        .setPartnerId(106L)
                        .setPayload("payload")
                        .setPaysysPartnerId(107L)
                        .setPaysysTypeCc(PaysysTypeCc.ACC_APPLE_PAY)
                        .setPreviousTransactionId(109L)
                        .setRecordType(RecordType.ACCRUAL)
                        .setSecuredPayment(true)
                        .setServiceId(1100)
                        .setServiceTransactionId("service-transaction-id")
                        .setProduct(TransactionLogProductType.PARTNER_PAYMENT)
                        .setTransactionTime(instant)
                        .setTransactionType(TransactionType.PAYMENT)
                        .build()
        ));
    }
}
