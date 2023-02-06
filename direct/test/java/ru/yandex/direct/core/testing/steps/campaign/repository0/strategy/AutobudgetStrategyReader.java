package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxReachStrategy;

/**
 * Класс для чтения автобюджетных недельных стратегий из базы.
 */
public class AutobudgetStrategyReader implements StrategyReader {

    @Override
    public AutobudgetMaxReachStrategy read(DbStrategy dbStrategy) {
        return new AutobudgetMaxReachStrategy()
                .withSum(dbStrategy.getStrategyData().getSum())
                .withAvgCpm(dbStrategy.getStrategyData().getAvgCpm())
                .withUnknownFields(dbStrategy.getStrategyData().getUnknownFields());
    }
}
