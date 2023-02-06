package ru.yandex.market.tpl.core.domain.push.notification.model;

import java.time.Instant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PushNotificationUtil {
    public static void setCreatedAt(PushNotification notification, Instant createdAt) {
        notification.setCreatedAt(createdAt);
    }
}
