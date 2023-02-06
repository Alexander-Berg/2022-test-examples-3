package ru.yandex.market.vendor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.cs.placement.tms.notification.NotifyModelbidsPrepaidFinanceLowExecutor;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/NotifyModelbidsPrepaidFinanceLowExecutorFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/NotifyModelbidsPrepaidFinanceLowExecutorFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class NotifyModelbidsPrepaidFinanceLowExecutorFunctionalTest extends AbstractCsPlacementTmsFunctionalTest {
    @Autowired
    private NotifyModelbidsPrepaidFinanceLowExecutor executor;
    @Autowired
    private NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate;
    @Autowired
    private Clock clock;

    @BeforeEach
    void before() {
        csBillingNamedParameterJdbcTemplate.update("" +
                "INSERT INTO CS_BILLING.CAMPAIGN_CHARGES " +
                "(ID, CS_ID,CAMPAIGN_ID,SRV_TYPE_ID,DT,PRICE,AMOUNT,SUM) " +
                "VALUES (42, 132, 500, 11, SYSTIMESTAMP - 3, 100, 1, 100) ", Collections.emptyMap());
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.now()));
    }

    /**
     * Проверяем, что в результате выполнения задачи, было сохранено событие уведомления
     */
    @Test
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/NotifyModelbidsPrepaidFinanceLowExecutorFunctionalTest/testNotificationPersistence/after.csv",
            dataSource = "vendorDataSource"
    )
    void testNotificationPersistence() {
        executor.doJob(null);
    }
}
