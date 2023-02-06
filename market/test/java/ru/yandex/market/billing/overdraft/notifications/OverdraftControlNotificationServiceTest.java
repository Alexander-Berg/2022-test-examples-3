package ru.yandex.market.billing.overdraft.notifications;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.overdraft.imprt.UnpaidInvoiceInfo;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.ds.model.DatasourceInfo;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Тесты для {@link OverdraftControlNotificationService}.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class OverdraftControlNotificationServiceTest extends FunctionalTest {


    @Autowired
    private OverdraftControlNotificationService controlNotificationService;

    @Mock
    private NotificationService notificationServiceMocked;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private CampaignService campaignService;

    /**
     * Тест не проверяет рендер непосретсдвенно, но проверяет, что шаблон для заданного теймплейтка корректно находится
     * и сконфигурен.
     * Рендер можно смотреть в {@link ru.yandex.market.core.transform.EmailTemplateGenerationTest}.
     */
    @DbUnitDataSet(before = "db/OverdraftControlNotificationService.before.csv")
    @Test
    void test_sendNotifications() {
        environmentService.setValue("mbi.overdraft_control.notifications_status", "enabled");

        List<OverdraftNotificationInfo> notificationInfos = controlNotificationService.prepareNotifications(
                List.of(
                        UnpaidInvoiceInfo.builder()
                                .setClientId(11L)
                                .setEid("eid-11")
                                .setInvoiceCreationTime(DateTimes.toInstant(2019, 1, 1))
                                .setWarningTime(DateTimes.toInstant(2019, 5, 1))
                                .setInitialExpireTime(DateTimes.toInstant(2019, 6, 1))
                                .setPaymentDeadlineDate(LocalDate.of(2019, 3, 1))
                                .build()
                ),
                Map.of(11L, List.of(111L, 222L))
        );

        controlNotificationService.sendNotifications(notificationInfos);
    }

    @DisplayName("Не отправлять нотификации если guard в env")
    @Test
    void test_sendNotifications_dontSendWhenEnvGuarded() {
        environmentService.setValue("mbi.overdraft_control.notifications_status", "some_not_enabled_value");
        new OverdraftControlNotificationService(notificationServiceMocked, environmentService, null, campaignService)
                .sendNotifications(
                        ImmutableList.of(new OverdraftNotificationInfo(new DatasourceInfo()))
                );

        verifyNoInteractions(notificationServiceMocked);
    }
}
