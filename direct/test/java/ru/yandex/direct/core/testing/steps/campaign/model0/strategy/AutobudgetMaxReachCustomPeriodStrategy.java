package ru.yandex.direct.core.testing.steps.campaign.model0.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Настройки автобюджетной стратегии: "Снижение цены повторных показов", за период.
 */
public class AutobudgetMaxReachCustomPeriodStrategy extends AbstractStrategy implements Strategy {

    /**
     * Дата начала периода.
     */
    private LocalDate startDate;

    /**
     * Дата окончания периода.
     */
    private LocalDate finishDate;

    /**
     * Бюджет на период.
     */
    private BigDecimal budget;

    /**
     * Средняя цена за тысячу показов.
     */
    private BigDecimal avgCpm;

    /**
     * Продлевать ли период автоматически.
     */
    private Long autoProlongation;

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(LocalDate finishDate) {
        this.finishDate = finishDate;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public BigDecimal getAvgCpm() {
        return avgCpm;
    }

    public void setAvgCpm(BigDecimal avgCpm) {
        this.avgCpm = avgCpm;
    }

    public Long getAutoProlongation() {
        return autoProlongation;
    }

    public void setAutoProlongation(Long autoProlongation) {
        this.autoProlongation = autoProlongation;
    }

    public AutobudgetMaxReachCustomPeriodStrategy withStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public AutobudgetMaxReachCustomPeriodStrategy withFinishDate(LocalDate finishDate) {
        this.finishDate = finishDate;
        return this;
    }

    public AutobudgetMaxReachCustomPeriodStrategy withBudget(BigDecimal budget) {
        this.budget = budget;
        return this;
    }

    public AutobudgetMaxReachCustomPeriodStrategy withAvgCpm(BigDecimal avgCpm) {
        this.avgCpm = avgCpm;
        return this;
    }

    public AutobudgetMaxReachCustomPeriodStrategy withAutoProlongation(Long autoProlongation) {
        this.autoProlongation = autoProlongation;
        return this;
    }

    @Override
    public boolean isAutobudget() {
        return true;
    }
}
