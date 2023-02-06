package ru.yandex.cs.placement.tms.notification;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;

public class NotifyAnalyticsPlatformPaymentFunctionalTest extends AbstractCsPlacementTmsFunctionalTest {
    private final NotifyAnalyticsPlatformPaymentExecutor executor;

    @Autowired
    public NotifyAnalyticsPlatformPaymentFunctionalTest(
            NotifyAnalyticsPlatformPaymentExecutor executor,
            Clock clock
    ) {
        this.executor = executor;
        this.clock = clock;
    }

    private final Clock clock;

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(clock);
        Mockito.when(clock.instant()).thenReturn(
                TimeUtil.toInstant(LocalDateTime.of(2021, Month.JANUARY, 2, 0, 0))
        );
    }

    @DisplayName("Срабатывание уведомления NotifyAnalyticsPlatformPayment")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/notification/NotifyAnalyticsPlatformPaymentFunctionalTest/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/notification/NotifyAnalyticsPlatformPaymentFunctionalTest/before.vendors.csv",
            after = "/ru/yandex/cs/placement/tms/notification/NotifyAnalyticsPlatformPaymentFunctionalTest/after.csv",
            dataSource = "vendorDataSource"
    )
    void testTriggerAPPaymentNotification() {
        executor.doJob(null);
    }
}
