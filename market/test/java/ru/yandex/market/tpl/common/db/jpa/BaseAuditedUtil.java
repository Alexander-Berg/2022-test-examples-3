package ru.yandex.market.tpl.common.db.jpa;

import java.time.Instant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BaseAuditedUtil {
    public static void setCreatedAt(BaseJpaEntity.BaseAudited<?> pushCarrierNotification, Instant createdAt) {
        pushCarrierNotification.setCreatedAt(createdAt);
    }
}
