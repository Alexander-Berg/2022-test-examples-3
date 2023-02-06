package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.Strategy;

public interface StrategyWriter {
    Long VERSION = 1L;

    DbStrategy write(Strategy strategy);
}
