package ru.yandex.cs.billing.tms;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.CsBillingCoreConstants;
import ru.yandex.cs.billing.tms.multi.MultiServiceExecutor;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class DatasourceHoldBillingExternalFunctionalTest extends AbstractCsBillingTmsExternalFunctionalTest {

    private final FinanceCutoffExecutor financeCutoffExecutor;
    private final MultiServiceExecutor updateActualBalanceExecutor;
    private final BillingExecutor billingExecutor;
    private final Clock clock;

    @Autowired
    public DatasourceHoldBillingExternalFunctionalTest(FinanceCutoffExecutor financeCutoffExecutor,
                                                       MultiServiceExecutor updateActualBalanceExecutor,
                                                       BillingExecutor billingExecutor,
                                                       Clock clock) {
        this.financeCutoffExecutor = financeCutoffExecutor;
        this.updateActualBalanceExecutor = updateActualBalanceExecutor;
        this.billingExecutor = billingExecutor;
        this.clock = clock;
    }

    @DisplayName("Проверяем обилливание разных вариантов холдов за разные даты, автопрощение")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/DatasourceHoldBillingExternalFunctionalTest/testCampaignBillingWithMultipleHolds/before.csv",
            after = "/ru/yandex/cs/billing/tms/DatasourceHoldBillingExternalFunctionalTest/testCampaignBillingWithMultipleHolds/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCampaignBillingWithMultipleHolds() {
        LocalDateTime billingHappensAt = LocalDateTime.of(2021, Month.APRIL, 1, 0, 5);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        JobExecutionContext context = mockContext();

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, context);
        updateActualBalanceExecutor.doJob(context);
    }

    private JobExecutionContext mockContext() {
        JobExecutionContext mockContext = Mockito.mock(JobExecutionContext.class);
        JobDetail mockJobDetail = Mockito.mock(JobDetail.class);
        Mockito.when(mockContext.getJobDetail()).thenReturn(mockJobDetail);
        Mockito.when(mockJobDetail.getKey()).thenReturn(new JobKey("test"));
        return mockContext;
    }
}
