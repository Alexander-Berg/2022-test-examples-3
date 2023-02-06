package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageCpaStrategy;

public class AverageCpaStrategyReader implements StrategyReader {

    @Override
    public AverageCpaStrategy read(DbStrategy dbStrategy) {
        return new AverageCpaStrategy()
                .withAverageCpa(dbStrategy.getStrategyData().getAvgCpa())
                .withGoalId(dbStrategy.getStrategyData().getGoalId())
                .withMaxWeekSum(dbStrategy.getStrategyData().getSum())
                .withMaxBid(dbStrategy.getStrategyData().getBid())
                .withUnknownFields(dbStrategy.getStrategyData().getUnknownFields());
    }
}
