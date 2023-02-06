package ru.yandex.market.jmf.db.api.test.partitionStrategy.strategies;

import java.time.Period;

import ru.yandex.market.jmf.db.api.AbstractByDatePartitionStrategy;
import ru.yandex.market.jmf.metadata.Fqn;

public class ByDateDeletionIntervalOneMonthPartitionStrategyTest extends AbstractByDatePartitionStrategy {
    public ByDateDeletionIntervalOneMonthPartitionStrategyTest() {
        super(Fqn.of("test"), new PartitionDeletionInfo(true, Period.ofMonths(1)));
    }
}

