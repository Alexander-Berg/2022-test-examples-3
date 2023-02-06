package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxReachCustomPeriodStrategy;

/**
 * Класс для чтения автобюджетных периодных стратегий из базы.
 */
public class AutobudgetCustomPeriodStrategyReader implements StrategyReader {

    @Override
    public AutobudgetMaxReachCustomPeriodStrategy read(DbStrategy dbStrategy) {
        return new AutobudgetMaxReachCustomPeriodStrategy()
                .withAutoProlongation(dbStrategy.getStrategyData().getAutoProlongation())
                .withAvgCpm(dbStrategy.getStrategyData().getAvgCpm())
                .withStartDate(dbStrategy.getStrategyData().getStart())
                .withFinishDate(dbStrategy.getStrategyData().getFinish())
                .withBudget(dbStrategy.getStrategyData().getBudget());
    }
}
