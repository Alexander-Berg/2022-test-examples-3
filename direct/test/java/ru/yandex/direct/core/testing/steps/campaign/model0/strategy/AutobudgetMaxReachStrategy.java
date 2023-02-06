package ru.yandex.direct.core.testing.steps.campaign.model0.strategy;

import java.math.BigDecimal;

/**
 * Настройки автобюджетной стратегии: "Снижение цены повторных показов", еженедельно.
 */
public class AutobudgetMaxReachStrategy extends AbstractStrategy implements Strategy {
    /**
     * Средняя цена за тысячу показов.
     */
    private BigDecimal avgCpm;

    /**
     * Ограничение недельного бюджета.
     */
    private BigDecimal sum;

    @Override
    public boolean isAutobudget() {
        return true;
    }

    public void setAvgCpm(BigDecimal avgCpm) {
        this.avgCpm = avgCpm;
    }

    public BigDecimal getAvgCpm() {
        return avgCpm;
    }

    public void setSum(BigDecimal sum) {
        this.sum = sum;
    }

    public BigDecimal getSum() {
        return sum;
    }

    public AutobudgetMaxReachStrategy withAvgCpm(BigDecimal avgCpm) {
        this.avgCpm = avgCpm;
        return this;
    }

    public AutobudgetMaxReachStrategy withSum(BigDecimal sum) {
        this.sum = sum;
        return this;
    }
}
