package ru.yandex.direct.core.testing.steps.campaign.model0.strategy;

import java.math.BigDecimal;

/**
 * Настройки автоматической стратегии: "средняя цена клика".
 */
public class AverageBidStrategy extends AbstractStrategy implements Strategy {

    /**
     * Средняя цена
     */
    private BigDecimal averageBid;

    /**
     * Ограничение недельного бюджета
     */
    private BigDecimal maxWeekSum;

    /**
     * Дневной бюджет
     */
    private DayBudget dayBudget;

    @Override
    public boolean isAutobudget() {
        return true;
    }


    public BigDecimal getAverageBid() {
        return averageBid;
    }

    public void setAverageBid(BigDecimal averageBid) {
        this.averageBid = averageBid;
    }

    public BigDecimal getMaxWeekSum() {
        return maxWeekSum;
    }

    public void setMaxWeekSum(BigDecimal maxWeekSum) {
        this.maxWeekSum = maxWeekSum;
    }

    public AverageBidStrategy withAverageBid(BigDecimal averageBid) {
        this.averageBid = averageBid;
        return this;
    }

    public AverageBidStrategy withMaxWeekSum(BigDecimal maxWeekSum) {
        this.maxWeekSum = maxWeekSum;
        return this;
    }

    public DayBudget getDayBudget() {
        return dayBudget;
    }

    public void setDayBudget(DayBudget dayBudget) {
        this.dayBudget = dayBudget;
    }

    public AverageBidStrategy withDayBudget(DayBudget dayBudget) {
        this.dayBudget = dayBudget;
        return this;
    }
}
