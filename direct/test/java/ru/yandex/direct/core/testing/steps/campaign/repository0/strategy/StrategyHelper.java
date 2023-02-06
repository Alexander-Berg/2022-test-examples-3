package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.Strategy;

@Component
public class StrategyHelper {

    @Autowired
    private StrategyWriterFactory strategyWriterFactory;

    @Autowired
    private StrategyReaderFactory strategyReaderFactory;

    public Strategy read(DbStrategy dbStrategy) {
        return strategyReaderFactory.getStrategyReader(dbStrategy).read(dbStrategy);
    }

    public DbStrategy write(Strategy strategy) {
        return strategyWriterFactory.getStrategyWriter(strategy.getClass()).write(strategy);
    }
}
