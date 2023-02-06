package ru.yandex.market.vendor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.placement.tms.notification.NotificationsSenderExecutor;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.vendor.notification.VendorNotificationParameterFormatter;

import static org.mockito.Mockito.doReturn;


@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/NotificationsSenderExecutorFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/NotificationsSenderExecutorFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class NotificationsSenderExecutorFunctionalTest extends AbstractCsPlacementTmsFunctionalTest {
    @Autowired
    private NotificationsSenderExecutor executor;
    @Autowired
    private VendorNotificationParameterFormatter vendorNotificationParameterFormatter;

    @BeforeEach
    void beforeEachTest() {
        doReturn("2019").when(vendorNotificationParameterFormatter).year();
    }

    /**
     * Проверяем, что флаг отправки email-а сохраняется
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotificationsSenderExecutorFunctionalTest/testSendingStatusPersistence/before.csv",
            after = "/ru/yandex/market/vendor/NotificationsSenderExecutorFunctionalTest/testSendingStatusPersistence/after.csv",
            dataSource = "vendorDataSource"
    )
    void testSendingStatusPersistence() {
        executor.doJob(null);
    }

}
