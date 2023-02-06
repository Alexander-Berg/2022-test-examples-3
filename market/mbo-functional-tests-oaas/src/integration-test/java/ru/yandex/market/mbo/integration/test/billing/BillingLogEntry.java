package ru.yandex.market.mbo.integration.test.billing;

import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.statistic.model.TaskType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

public class BillingLogEntry {
    private static final int SCALE = 5;

    private long userId;
    private long guruCategoryId;
    private LocalDateTime time;
    private PaidAction operation;
    private long sourceId;
    private long auditActionId;
    private double count;
    private BigDecimal price;
    private TaskType externalSource;
    private String externalSourceId;
    private String parameterName;

    public long getUserId() {
        return userId;
    }

    public BillingLogEntry setUserId(long userId) {
        this.userId = userId;
        return this;
    }

    public long getGuruCategoryId() {
        return guruCategoryId;
    }

    public BillingLogEntry setGuruCategoryId(long guruCategoryId) {
        this.guruCategoryId = guruCategoryId;
        return this;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public BillingLogEntry setTime(LocalDateTime time) {
        this.time = time;
        return this;
    }

    public PaidAction getOperation() {
        return operation;
    }

    public BillingLogEntry setOperation(PaidAction operation) {
        this.operation = operation;
        return this;
    }

    public long getSourceId() {
        return sourceId;
    }

    public BillingLogEntry setSourceId(long sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    public long getAuditActionId() {
        return auditActionId;
    }

    public BillingLogEntry setAuditActionId(long auditActionId) {
        this.auditActionId = auditActionId;
        return this;
    }

    public double getCount() {
        return count;
    }

    public BillingLogEntry setCount(double count) {
        this.count = count;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getRoundPrice() {
        return price == null ? null : price.setScale(SCALE, RoundingMode.CEILING);
    }

    public BillingLogEntry setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public TaskType getExternalSource() {
        return externalSource;
    }

    public BillingLogEntry setExternalSource(TaskType externalSource) {
        this.externalSource = externalSource;
        return this;
    }

    public String getExternalSourceId() {
        return externalSourceId;
    }

    public BillingLogEntry setExternalSourceId(String externalSourceId) {
        this.externalSourceId = externalSourceId;
        return this;
    }

    public String getParameterName() {
        return parameterName;
    }

    public BillingLogEntry setParameterName(String parameterName) {
        this.parameterName = parameterName;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BillingLogEntry that = (BillingLogEntry) o;
        return userId == that.userId &&
            guruCategoryId == that.guruCategoryId &&
            sourceId == that.sourceId &&
            auditActionId == that.auditActionId &&
            Double.compare(that.count, count) == 0 &&
            Objects.equals(time, that.time) &&
            operation == that.operation &&
            Objects.equals(getRoundPrice(), that.getRoundPrice()) &&
            externalSource == that.externalSource &&
            Objects.equals(externalSourceId, that.externalSourceId) &&
            Objects.equals(parameterName, that.parameterName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, guruCategoryId, time, operation, sourceId, auditActionId, count,
            getRoundPrice(), externalSource, externalSourceId, parameterName);
    }

    @Override
    public String toString() {
        return "BillingLogEntry{" +
            "userId=" + userId +
            ", guruCategoryId=" + guruCategoryId +
            ", time=" + time +
            ", operation=" + operation +
            ", sourceId=" + sourceId +
            ", auditActionId=" + auditActionId +
            ", count=" + count +
            ", price=" + getRoundPrice() +
            ", externalSource=" + externalSource +
            ", externalSourceId='" + externalSourceId + '\'' +
            ", parameterName='" + parameterName + '\'' +
            '}';
    }
}
