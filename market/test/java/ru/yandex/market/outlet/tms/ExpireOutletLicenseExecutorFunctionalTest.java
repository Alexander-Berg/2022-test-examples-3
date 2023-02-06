package ru.yandex.market.outlet.tms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest;
import ru.yandex.market.shop.FunctionalTest;

/**
 * Тесты для {@link ExpireOutletLicenseExecutor}.
 *
 * @author Vladislav Bauer
 */
class ExpireOutletLicenseExecutorFunctionalTest extends FunctionalTest {
    @Autowired
    private ExpireOutletLicenseExecutor executor;

    @Test
    @DisplayName("Проверить что нет протухших лицензий")
    void testExecutorNoData() {
        executor.doJob(null);

        Mockito.verifyNoInteractions(partnerNotificationClient);
    }

    @Test
    @DisplayName("Проверить корректное выключение протухших лицензий")
    @DbUnitDataSet(
            before = "ExpireOutletLicenseExecutorFunctionalTest.before.csv",
            after = "ExpireOutletLicenseExecutorFunctionalTest.after.csv"
    )
    void testExecutor() {
        executor.doJob(null);

        PartnerNotificationApiServiceTest.verifySentNotificationType(partnerNotificationClient, 3, 1556277437L);
    }

    @Test
    @DisplayName("Проверить что для выключенных лицензий не будет отправлено письмо")
    @DbUnitDataSet(
            before = "ExpireOutletLicenseExecutorFunctionalTest.before.csv",
            after = "ExpireOutletLicenseExecutorFunctionalTest.after.csv"
    )
    void testExecutorTwice() {
        executor.doJob(null);
        executor.doJob(null);

        PartnerNotificationApiServiceTest.verifySentNotificationType(partnerNotificationClient, 3, 1556277437L);
    }
}
