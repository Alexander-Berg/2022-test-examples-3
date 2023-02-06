package ru.yandex.direct.core.entity.notification.container;

import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static ru.yandex.direct.core.entity.notification.container.OptimizationResultNotification.CAMPAIGN_ID;
import static ru.yandex.direct.core.entity.notification.container.OptimizationResultNotification.DAYS_TO_GO;
import static ru.yandex.direct.core.entity.notification.container.OptimizationResultNotification.EMAIL;
import static ru.yandex.direct.core.entity.notification.container.OptimizationResultNotification.FIO;
import static ru.yandex.direct.core.entity.notification.container.OptimizationResultNotification.IS_SECOND_AID;
import static ru.yandex.direct.core.entity.notification.container.OptimizationResultNotification.UID;

/**
 * Тест проверяет, что OptimizationResultNotification возвращает ожидаемую map-у при вызове getNotificationsData()
 *
 * @see OptimizationResultNotification
 */
public class OptimizationResultNotificationDataMapTest {

    @Test
    public void checkOptimizationResultNotificationDataMap() {
        OptimizationResultNotification notification = new OptimizationResultNotification()
                .withCampaignId(123)
                .withUid(321)
                .withSecondAid(true)
                .withDaysToGo(3)
                .withFio("Vasya Pupkin")
                .withEmail("pupkin@yandex.ru");

        Map<String, Object> notificationData = notification.getNotificationData();

        assertThat(notificationData).containsOnly(
                entry(CAMPAIGN_ID, notification.getCampaignId()),
                entry(UID, notification.getUid()),
                entry(IS_SECOND_AID, notification.isSecondAid()),
                entry(DAYS_TO_GO, notification.getDaysToGo()),
                entry(FIO, notification.getFio()),
                entry(EMAIL, notification.getEmail())
        );
    }
}
