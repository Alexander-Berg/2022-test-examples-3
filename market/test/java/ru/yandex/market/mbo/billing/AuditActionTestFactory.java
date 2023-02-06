package ru.yandex.market.mbo.billing;

import ru.yandex.market.mbo.gwt.models.audit.AuditAction;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * @author yuramalinov
 * @created 09.11.18
 */
public class AuditActionTestFactory {
    private Instant currentTime;
    private AuditAction.EntityType entityType;

    public AuditActionTestFactory(AuditAction.EntityType entityType) {
        this(Instant.now(), entityType);
    }

    public AuditActionTestFactory(Instant currentTime, AuditAction.EntityType entityType) {
        this.currentTime = currentTime;
        this.entityType = entityType;
    }

    public AuditAction createAction(long entityId, long userId, AuditAction.ActionType type,
                                String oldValue, String newValue) {
        return createAction(entityId, userId, type, oldValue, newValue, AuditAction.BillingMode.BILLING_MODE_FILL);
    }

    public AuditAction createAction(long entityId, long userId, AuditAction.ActionType type,
                                    String oldValue, String newValue,
                                    AuditAction.BillingMode billingMode) {
        AuditAction result = new AuditAction();
        result.setEntityType(this.entityType);
        result.setCategoryId(1L);
        result.setEntityId(entityId);
        result.setDate(nextDate());
        result.setEntityName("SKU1");
        result.setUserId(userId);
        result.setActionType(type);
        result.setOldValue(oldValue);
        result.setNewValue(newValue);
        result.setBillingMode(billingMode);
        result.setActionId(-1L);
        return result;
    }

    private Date nextDate() {
        currentTime = currentTime.plus(1, ChronoUnit.SECONDS);
        return new Date(currentTime.toEpochMilli());
    }
}
