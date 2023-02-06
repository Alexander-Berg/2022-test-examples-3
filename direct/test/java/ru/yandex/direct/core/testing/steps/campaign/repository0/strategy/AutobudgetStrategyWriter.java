package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import java.math.BigDecimal;

import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsDayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxReachStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.Strategy;

import static ru.yandex.direct.core.testing.steps.campaign.repository0.strategy.WriterUtils.checkArgument;

/**
 * Класс для записи автобюджетных недельных стратегий в базу.
 */
public class AutobudgetStrategyWriter implements StrategyWriter {

    private final StrategyName strategyName;

    public AutobudgetStrategyWriter(StrategyName strategyName) {
        this.strategyName = strategyName;
    }

    @Override
    public DbStrategy write(Strategy strategy) {
        checkArgument(strategy, AutobudgetMaxReachStrategy.class);
        AutobudgetMaxReachStrategy autobudgetMaxReachStrategy = (AutobudgetMaxReachStrategy) strategy;

        return (DbStrategy) new DbStrategy()
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyName(strategyName)
                .withDayBudgetShowMode(CampaignsDayBudgetShowMode.DEFAULT_)
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetDailyChangeCount(0L)
                .withStrategyData(new StrategyData()
                        .withAvgCpm(autobudgetMaxReachStrategy.getAvgCpm())
                        .withSum(autobudgetMaxReachStrategy.getSum())
                        .withUnknownFields(autobudgetMaxReachStrategy.getUnknownFields())
                        .withVersion(VERSION));
    }
}
