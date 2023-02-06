package ru.yandex.market.jmf.db.api.test.partitionStrategy.strategies;

import java.time.Period;

import ru.yandex.market.jmf.db.api.AbstractByDayPartitionStrategy;
import ru.yandex.market.jmf.metadata.Fqn;

public class ByDayDeletionIntervalOneDayPartitionStrategyTest extends AbstractByDayPartitionStrategy {
    public ByDayDeletionIntervalOneDayPartitionStrategyTest() {
        super(Fqn.of("test"), new PartitionDeletionInfo(true, Period.ofDays(1)));
    }
}

