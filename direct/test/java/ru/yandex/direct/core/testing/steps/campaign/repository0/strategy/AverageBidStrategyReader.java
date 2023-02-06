package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageBidStrategy;

public class AverageBidStrategyReader implements StrategyReader {

    @Override
    public AverageBidStrategy read(DbStrategy dbStrategy) {
        return new AverageBidStrategy()
                .withAverageBid(dbStrategy.getStrategyData().getAvgBid())
                .withMaxWeekSum(dbStrategy.getStrategyData().getSum())
                .withUnknownFields(dbStrategy.getStrategyData().getUnknownFields());
    }
}
