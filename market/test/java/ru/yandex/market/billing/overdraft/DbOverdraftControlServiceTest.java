package ru.yandex.market.billing.overdraft;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.overdraft.imprt.BalanceInvoiceStatus;
import ru.yandex.market.billing.overdraft.imprt.BalanceOverdraftInvoice;
import ru.yandex.market.billing.overdraft.imprt.UnpaidInvoiceInfo;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для {@link DbOverdraftControlService}.
 *
 * @author vbudnev
 */
class DbOverdraftControlServiceTest extends FunctionalTest {

    private static final Instant I_2019_06_10_232323 = DateTimes.toInstantAtDefaultTz(2019, 6, 10, 23, 23, 23);
    private static final Instant I_2019_06_01_222222 = DateTimes.toInstantAtDefaultTz(2019, 6, 1, 22, 22, 22);

    @Autowired
    private DbOverdraftControlService service;

    @DbUnitDataSet(
            before = "db/DbOverdraftControlServiceTest.markWhiteListed.before.csv",
            after = "db/DbOverdraftControlServiceTest.markWhiteListed.after.csv"
    )
    @Test
    void test_markWhiteListed() {
        service.markWhiteListed(ImmutableList.of(12L, 13L));
    }

    @DbUnitDataSet(before = "db/DbOverdraftControlServiceTest.filterExisting.before.csv")
    @Test
    void test_filterExisting() {
        Set<String> expected = ImmutableSet.of("eid-1", "eid-3");
        Set<String> actual = service.filterExisting(ImmutableSet.of("eid-1", "eid-2", "eid-3", "eid-4"));

        assertEquals(expected, actual);
    }

    @DbUnitDataSet(
            before = "db/DbOverdraftControlServiceTest.markExpired.before.csv",
            after = "db/DbOverdraftControlServiceTest.markExpired.after.csv"
    )
    @DisplayName("Отключение ранее предупрежденных")
    @Test
    void test_updateStatusForExpiredInvoices() {
        Instant expiredTime = I_2019_06_01_222222;
        service.updateStatusForExpiredInvoices(expiredTime);
    }

    @DbUnitDataSet(
            before = "db/DbOverdraftControlServiceTest.markPaid.before.csv",
            after = "db/DbOverdraftControlServiceTest.markPaid.after.csv"
    )
    @DisplayName("Маркировка оплаченных")
    @Test
    void test_updateStatusForPaidInvoices() {
        Instant paidTime = I_2019_06_10_232323;
        service.updateStatusForPaidInvoices(
                paidTime,
                ImmutableSet.of("eid-11", "eid-22", "eid-33", "eid-44", "eid-55", "eid-66", "eid-77", "unknown")
        );
    }

    @DbUnitDataSet(
            before = "db/DbOverdraftControlServiceTest.persistUnpaid.before.csv",
            after = "db/DbOverdraftControlServiceTest.persistUnpaid.after.csv"
    )
    @DisplayName("Сохранение данных о новых неоплаченных счетах")
    @Test
    void test_persistUnpaid() {
        service.persistUnpaid(
                ImmutableList.of(
                        UnpaidInvoiceInfo.builder()
                                .setEid("eid-22")
                                .setClientId(22L)
                                .setInvoiceCreationTime(I_2019_06_01_222222)
                                .setPaymentDeadlineDate(LocalDate.of(2019, 6, 3))
                                .setWarningTime(DateTimes.toInstantAtDefaultTz(2019, 6, 2, 22, 22, 22))
                                .setInitialExpireTime(DateTimes.toInstantAtDefaultTz(2019, 6, 4, 22, 22, 22))
                                .build(),
                        UnpaidInvoiceInfo.builder()
                                .setEid("eid-33")
                                .setClientId(22L)
                                .setInvoiceCreationTime(I_2019_06_01_222222)
                                .setPaymentDeadlineDate(LocalDate.of(2019, 6, 5))
                                .setWarningTime(DateTimes.toInstantAtDefaultTz(2019, 6, 4, 22, 22, 22))
                                .setInitialExpireTime(DateTimes.toInstantAtDefaultTz(2019, 6, 7, 22, 22, 22))
                                .build()
                ),
                ImmutableMap.of(
                        22L, ImmutableList.of(2201L, 2202L),
                        33L, ImmutableList.of(3301L, 3302L)
                )
        );
    }

    @DbUnitDataSet(before = "db/DbOverdraftControlServiceTest.loadClientMapping.before.csv")
    @Test
    void test_loadClientMapping() {
        Map<Long, List<Long>> expected = ImmutableMap.of(
                1L, ImmutableList.of(101L, 103L),
                100L, ImmutableList.of(1001L, 1002L)
        );
        Map<Long, List<Long>> actual = service.loadClientMapping(ImmutableSet.of(1L, 100L));

        ReflectionAssert.assertLenientEquals(expected, actual);
    }

    @DbUnitDataSet(
            before = "db/DbOverdraftControlServiceTest.mergeInvoicePaymentTime.before.csv",
            after = "db/DbOverdraftControlServiceTest.mergeInvoicePaymentTime.after.csv"
    )
    @Test
    void test_mergeInvoicePaymentTime() {
        Set<String> actualUpdated = service.mergeInvoicePaymentTime(
                ImmutableList.of(
                        new BalanceOverdraftInvoice(
                                "eid-111",
                                LocalDate.of(2019, 6, 10),
                                111,
                                BalanceInvoiceStatus.OVERDUE_PAID,
                                DateTimes.toInstantAtDefaultTz(2019, 6, 11, 10, 10, 10),
                                DateTimes.toInstantAtDefaultTz(2019, 5, 13, 10, 10, 10)
                        ),
                        new BalanceOverdraftInvoice(
                                "eid-222",
                                LocalDate.of(2019, 6, 11),
                                222,
                                BalanceInvoiceStatus.OVERDUE_PAID,
                                DateTimes.toInstantAtDefaultTz(2019, 6, 12, 10, 10, 10),
                                DateTimes.toInstantAtDefaultTz(2019, 5, 13, 10, 10, 10)
                        ),
                        // дедлайн нулл потому что счет превратился в предоплатный. должны просто пометить оплаченным
                        new BalanceOverdraftInvoice(
                                "eid-333",
                                null,
                                333,
                                BalanceInvoiceStatus.OVERDUE_PAID,
                                DateTimes.toInstantAtDefaultTz(2019, 6, 12, 10, 10, 10),
                                DateTimes.toInstantAtDefaultTz(2019, 5, 13, 10, 10, 10)
                        ),
                        new BalanceOverdraftInvoice(
                                "eid-444",
                                LocalDate.of(2019, 6, 12),
                                444,
                                BalanceInvoiceStatus.OVERDUE_PAID,
                                DateTimes.toInstantAtDefaultTz(2019, 6, 13, 10, 10, 10),
                                DateTimes.toInstantAtDefaultTz(2019, 5, 13, 10, 10, 10)
                        ),
                        new BalanceOverdraftInvoice(
                                "eid-1000",
                                LocalDate.of(2019, 6, 12),
                                1000,
                                BalanceInvoiceStatus.OVERDUE_PAID,
                                DateTimes.toInstantAtDefaultTz(2019, 6, 13, 10, 10, 10),
                                DateTimes.toInstantAtDefaultTz(2019, 5, 13, 10, 10, 10)
                        ),
                        // prepayment счет но на него у нас нет овердрафтных платежей. игнорируем
                        new BalanceOverdraftInvoice(
                                "eid-1100",
                                LocalDate.of(2019, 6, 12),
                                1100,
                                BalanceInvoiceStatus.OVERDUE_PAID,
                                DateTimes.toInstantAtDefaultTz(2019, 6, 13, 10, 10, 10),
                                DateTimes.toInstantAtDefaultTz(2019, 5, 13, 10, 10, 10)
                        )

                )
        );

        assertEquals(ImmutableSet.of("eid-111", "eid-222", "eid-333"), actualUpdated);
    }

    @Test
    void test_errorWhenMissingReceiptDt() {
        Exception ex = Assertions.assertThrows(
                NullPointerException.class,
                () -> service.mergeInvoicePaymentTime(
                        ImmutableList.of(
                                new BalanceOverdraftInvoice(
                                        "eid-1000",
                                        LocalDate.of(2019, 6, 12),
                                        1000,
                                        BalanceInvoiceStatus.OVERDUE_PAID,
                                        null,
                                        DateTimes.toInstantAtDefaultTz(2019, 5, 13, 10, 10, 10)
                                )
                        )
                )
        );
        assertEquals(ex.getMessage(), "missing receipt_dt for eid = eid-1000");
    }
}
