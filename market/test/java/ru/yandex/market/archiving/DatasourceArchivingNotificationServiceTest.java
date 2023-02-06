package ru.yandex.market.archiving;


import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.archiving.step.DatasourceArchivingContext;
import ru.yandex.market.archiving.step.DatasourceArchivingStepType;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.SingleFileCsvProducer;
import ru.yandex.market.partner.notification.client.model.SendNotificationRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Тесты для {@link DatasourceArchivingNotificationService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class DatasourceArchivingNotificationServiceTest extends FunctionalTest {
    @Autowired
    private DatasourceArchivingNotificationService datasourceArchivingNotificationService;
    @Autowired
    Clock clock;

    @Test
    @DisplayName("Неизвестный тип нотификации")
    void testInvalidNotification() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            var context = new DatasourceArchivingContext(DatasourceArchivingStepType.INITIAL, List.of(1L, 2L));
            datasourceArchivingNotificationService.send(context);
        });
    }

    @Test
    @DisplayName("Нотификация 14 дней")
    @DbUnitDataSet(
            before = "csv/datasourceArchivingNotificationService.14.before.csv",
            after = "csv/datasourceArchivingNotificationService.14.after.csv"
    )
    void testFirstWarningStep() {
        testSendNotification(DatasourceArchivingStepType.FIRST_WARNING, 1534397128L);
    }

    @Test
    @DisplayName("Нотификация 2 дня")
    @DbUnitDataSet(
            before = "csv/datasourceArchivingNotificationService.2.before.csv",
            after = "csv/datasourceArchivingNotificationService.2.after.csv"
    )
    void testSecondWarningStep() {
        testSendNotification(DatasourceArchivingStepType.SECOND_WARNING, 1534402499L);
    }

    private void testSendNotification(DatasourceArchivingStepType stepType, long notificationType) {
        // given
        when(clock.instant()).thenReturn(SingleFileCsvProducer.Functions.sysdate().toInstant());

        // when
        var context = new DatasourceArchivingContext(stepType, List.of(1001L, 1002L, 1003L, 1004L));
        datasourceArchivingNotificationService.send(context);

        // then
        var reqCaptor = verifySentNotificationType(partnerNotificationClient, 4, notificationType);
        assertThat(reqCaptor.getAllValues().stream().map(SendNotificationRequest::getData)).satisfiesExactlyInAnyOrder(
                req -> assertThat(req).contains("10001"),
                req -> assertThat(req).contains("10002"),
                req -> assertThat(req).contains("10003"),
                req -> assertThat(req).contains("10004").contains("test.ru")
        );
    }
}
