package ru.yandex.cs.billing.tms;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.AbstractCsBillingTmsFunctionalTest;
import ru.yandex.cs.billing.billing.Money;
import ru.yandex.cs.billing.invoice.overdraft.dao.OverdraftInvoiceYtDao;
import ru.yandex.cs.billing.invoice.overdraft.model.OverdraftInvoice;
import ru.yandex.cs.billing.tms.multi.MultiServiceExecutor;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.cs.billing.CsBillingCoreConstants.ANALYTICS_SERVICE_ID;
import static ru.yandex.cs.billing.CsBillingCoreConstants.VENDOR_SERVICE_ID;

public class OverdraftInvoiceImportExecutorFunctionalTest extends AbstractCsBillingTmsFunctionalTest {
    @Autowired
    private MultiServiceExecutor overdraftInvoiceImportExecutor;
    @Autowired
    private OverdraftInvoiceYtDao overdraftInvoiceYtDao;
    @Autowired
    private Clock clock;

    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/OverdraftInvoiceImportExecutorFunctionalTest/testDontImportAgainToday/before.csv",
            after = "/ru/yandex/cs/billing/tms/OverdraftInvoiceImportExecutorFunctionalTest/testDontImportAgainToday/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testDontImportAgainToday() {
        OverdraftInvoice overdraftInvoice1 = new OverdraftInvoice(
                VENDOR_SERVICE_ID,
                99L,
                "Test1",
                12L,
                LocalDate.of(2021, Month.SEPTEMBER, 15).atStartOfDay(),
                Money.valueOf(1000L),
                Money.valueOf(10000L),
                LocalDate.of(2021, Month.SEPTEMBER, 17)
        );
        OverdraftInvoice overdraftInvoice2 = new OverdraftInvoice(
                VENDOR_SERVICE_ID,
                100L,
                "Test2",
                13L,
                LocalDate.of(2021, Month.SEPTEMBER, 16).atStartOfDay(),
                Money.valueOf(5000L),
                Money.valueOf(50000L),
                LocalDate.of(2021, Month.SEPTEMBER, 17)
        );
        when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDate.of(2021, Month.SEPTEMBER, 18)));

        when(overdraftInvoiceYtDao.findTableLastModificationDate(eq(VENDOR_SERVICE_ID)))
                .thenReturn(Optional.of(LocalDate.of(2021, 9, 18).atStartOfDay()));

        when(overdraftInvoiceYtDao.findAll(eq(VENDOR_SERVICE_ID)))
                .thenReturn(List.of(overdraftInvoice1, overdraftInvoice2));

        when(overdraftInvoiceYtDao.findAll(eq(ANALYTICS_SERVICE_ID)))
                .thenReturn(List.of());

        overdraftInvoiceImportExecutor.doJob(mockContext());
    }

    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/OverdraftInvoiceImportExecutorFunctionalTest/testImportAgainToday/before.csv",
            after = "/ru/yandex/cs/billing/tms/OverdraftInvoiceImportExecutorFunctionalTest/testImportAgainToday/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testImportAgainToday() {
        OverdraftInvoice overdraftInvoice1 = new OverdraftInvoice(
                VENDOR_SERVICE_ID,
                99L,
                "Test3",
                13L,
                LocalDate.of(2021, Month.SEPTEMBER, 16).atStartOfDay(),
                Money.valueOf(1001L),
                Money.valueOf(10001L),
                LocalDate.of(2021, Month.SEPTEMBER, 18)
        );
        OverdraftInvoice overdraftInvoice2 = new OverdraftInvoice(
                VENDOR_SERVICE_ID,
                100L,
                "Test4",
                14L,
                LocalDate.of(2021, Month.SEPTEMBER, 17).atStartOfDay(),
                Money.valueOf(5001L),
                Money.valueOf(50001L),
                LocalDate.of(2021, Month.SEPTEMBER, 19)
        );
        when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDate.of(2021, Month.SEPTEMBER, 19)));

        when(overdraftInvoiceYtDao.findTableLastModificationDate(eq(VENDOR_SERVICE_ID)))
                .thenReturn(Optional.of(LocalDate.of(2021, 9, 19).atStartOfDay()));

        when(overdraftInvoiceYtDao.findAll(eq(VENDOR_SERVICE_ID)))
                .thenReturn(List.of(overdraftInvoice1, overdraftInvoice2));

        when(overdraftInvoiceYtDao.findAll(eq(ANALYTICS_SERVICE_ID)))
                .thenReturn(List.of());

        overdraftInvoiceImportExecutor.doJob(mockContext());
    }

    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/OverdraftInvoiceImportExecutorFunctionalTest/testImportFirstTime/before.csv",
            after = "/ru/yandex/cs/billing/tms/OverdraftInvoiceImportExecutorFunctionalTest/testImportFirstTime/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testImportFirstTime() {
        OverdraftInvoice overdraftInvoice1 = new OverdraftInvoice(
                VENDOR_SERVICE_ID,
                1L,
                "Test1",
                12L,
                LocalDate.of(2021, Month.SEPTEMBER, 15).atStartOfDay(),
                Money.valueOfKeepOriginalScale(BigDecimal.valueOf(10000000L, 6)),
                Money.valueOf(10000L),
                LocalDate.of(2021, Month.SEPTEMBER, 17)
        );
        OverdraftInvoice overdraftInvoice2 = new OverdraftInvoice(
                VENDOR_SERVICE_ID,
                2L,
                "Test2",
                13L,
                LocalDate.of(2021, Month.SEPTEMBER, 16).atStartOfDay(),
                Money.valueOf(5000L),
                Money.valueOf(50000L),
                LocalDate.of(2021, Month.SEPTEMBER, 17)
        );
        when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDate.of(2021, Month.SEPTEMBER, 19)));

        when(overdraftInvoiceYtDao.findTableLastModificationDate(eq(VENDOR_SERVICE_ID)))
                .thenReturn(Optional.of(LocalDate.of(2021, 9, 19).atStartOfDay()));

        when(overdraftInvoiceYtDao.findAll(eq(VENDOR_SERVICE_ID)))
                .thenReturn(List.of(overdraftInvoice1, overdraftInvoice2));

        when(overdraftInvoiceYtDao.findAll(eq(ANALYTICS_SERVICE_ID)))
                .thenReturn(List.of());

        overdraftInvoiceImportExecutor.doJob(mockContext());
    }

    private static JobExecutionContext mockContext() {
        JobExecutionContext mockContext = mock(JobExecutionContext.class);
        JobDetail mockJobDetail = mock(JobDetail.class);
        when(mockContext.getJobDetail()).thenReturn(mockJobDetail);
        when(mockJobDetail.getKey()).thenReturn(new JobKey("test"));
        return mockContext;
    }
}
