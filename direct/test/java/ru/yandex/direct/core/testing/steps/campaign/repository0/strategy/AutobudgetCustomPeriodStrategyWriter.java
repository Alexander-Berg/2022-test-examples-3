package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import java.math.BigDecimal;

import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsDayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxReachCustomPeriodStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.Strategy;

import static ru.yandex.direct.core.testing.steps.campaign.repository0.strategy.WriterUtils.checkArgument;

/**
 * Класс для записи автобюджетных периодных стратегий в базу.
 */
public class AutobudgetCustomPeriodStrategyWriter implements StrategyWriter {

    private final StrategyName strategyName;

    public AutobudgetCustomPeriodStrategyWriter(StrategyName strategyName) {
        this.strategyName = strategyName;
    }

    @Override
    public DbStrategy write(Strategy strategy) {
        checkArgument(strategy, AutobudgetMaxReachCustomPeriodStrategy.class);
        AutobudgetMaxReachCustomPeriodStrategy autobudgetMaxReachCustomPeriodStrategy =
                (AutobudgetMaxReachCustomPeriodStrategy) strategy;

        return (DbStrategy) new DbStrategy()
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyName(strategyName)
                .withDayBudgetShowMode(CampaignsDayBudgetShowMode.DEFAULT_)
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetDailyChangeCount(0L)
                .withStrategyData(new StrategyData()
                        .withAvgCpm(autobudgetMaxReachCustomPeriodStrategy.getAvgCpm())
                        .withBudget(autobudgetMaxReachCustomPeriodStrategy.getBudget())
                        .withStart(autobudgetMaxReachCustomPeriodStrategy.getStartDate())
                        .withFinish(autobudgetMaxReachCustomPeriodStrategy.getFinishDate())
                        .withAutoProlongation(autobudgetMaxReachCustomPeriodStrategy.getAutoProlongation())
                        .withUnknownFields(autobudgetMaxReachCustomPeriodStrategy.getUnknownFields())
                        .withVersion(VERSION));
    }
}
