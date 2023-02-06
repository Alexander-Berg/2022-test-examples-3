package ru.yandex.direct.core.testing.steps.campaign.model0.strategy;

/**
 * Настройки автоматической стратегии: "средняя цена конверсии".
 */
public class AverageCpaPerCampStrategy extends AbstractStrategy implements Strategy {


    /**
     * Средняя цена конверсии
     */
    private Double averageCpa;

    /**
     * Ограничение недельного бюджета
     */
    private Double maxWeekSum;

    /**
     * Ограничение максимальной цены за клик
     */
    private Double maxBid;

    /**
     * Id цели в Метрике
     */
    private Long goalId;


    @Override
    public boolean isAutobudget() {
        return true;
    }

    public Double getAverageCpa() {
        return averageCpa;
    }

    public void setAverageCpa(Double averageCpa) {
        this.averageCpa = averageCpa;
    }

    public Double getMaxWeekSum() {
        return maxWeekSum;
    }

    public void setMaxWeekSum(Double maxWeekSum) {
        this.maxWeekSum = maxWeekSum;
    }

    public Double getMaxBid() {
        return maxBid;
    }

    public void setMaxBid(Double maxBid) {
        this.maxBid = maxBid;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public AverageCpaPerCampStrategy withAverageCpa(Double averageCpa) {
        this.averageCpa = averageCpa;
        return this;
    }

    public AverageCpaPerCampStrategy withMaxWeekSum(Double maxWeekSum) {
        this.maxWeekSum = maxWeekSum;
        return this;
    }

    public AverageCpaPerCampStrategy withMaxBid(Double maxBid) {
        this.maxBid = maxBid;
        return this;
    }

    public AverageCpaPerCampStrategy withGoalId(Long goalId) {
        this.goalId = goalId;
        return this;
    }

}
