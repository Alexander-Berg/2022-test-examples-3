package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.Strategy;

public interface StrategyReader {
    Strategy read(DbStrategy dbStrategy);
}
