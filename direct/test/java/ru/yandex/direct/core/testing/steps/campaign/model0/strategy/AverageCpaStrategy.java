package ru.yandex.direct.core.testing.steps.campaign.model0.strategy;

import java.math.BigDecimal;

/**
 * Настройки автоматической стратегии: "средняя цена конверсии".
 */
public class AverageCpaStrategy extends AbstractStrategy implements Strategy {

    /**
     * Средняя цена конверсии
     */
    private BigDecimal averageCpa;

    /**
     * Ограничение недельного бюджета
     */
    private BigDecimal maxWeekSum;

    /**
     * Ограничение максимальной цены за клик
     */
    private BigDecimal maxBid;

    /**
     * Id цели в Метрике
     */
    private Long goalId;


    @Override
    public boolean isAutobudget() {
        return true;
    }

    public BigDecimal getAverageCpa() {
        return averageCpa;
    }

    public void setAverageCpa(BigDecimal averageCpa) {
        this.averageCpa = averageCpa;
    }

    public BigDecimal getMaxWeekSum() {
        return maxWeekSum;
    }

    public void setMaxWeekSum(BigDecimal maxWeekSum) {
        this.maxWeekSum = maxWeekSum;
    }

    public BigDecimal getMaxBid() {
        return maxBid;
    }

    public void setMaxBid(BigDecimal maxBid) {
        this.maxBid = maxBid;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public AverageCpaStrategy withAverageCpa(BigDecimal averageCpa) {
        this.averageCpa = averageCpa;
        return this;
    }

    public AverageCpaStrategy withMaxWeekSum(BigDecimal maxWeekSum) {
        this.maxWeekSum = maxWeekSum;
        return this;
    }

    public AverageCpaStrategy withMaxBid(BigDecimal maxBid) {
        this.maxBid = maxBid;
        return this;
    }

    public AverageCpaStrategy withGoalId(Long goalId) {
        this.goalId = goalId;
        return this;
    }
}
