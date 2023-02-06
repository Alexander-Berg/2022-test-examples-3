package ru.yandex.direct.core.testing.db;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.sharding.ShardSupport;

public class TestShardHelper extends ShardHelper {
    private static final Logger logger = LoggerFactory.getLogger(TestShardHelper.class);

    public TestShardHelper(ShardSupport shardSupport) {
        super(shardSupport);
    }

    // нужно для правильного выставления диапазона id цели и соответственно типа целей
    @Override
    public List<Long> generateMobileAppGoalIds(int count) {
        List<Long> ids = super.generateMobileAppGoalIds(count).stream()
                .map(id -> id + Goal.LAL_SEGMENT_UPPER_BOUND)
                .collect(Collectors.toList());
        logger.info("generateMobileAppGoalIds({}) => {}", count, ids);
        return ids;
    }

    @Override
    public List<Long> generateLalSegmentIds(int count) {
        List<Long> ids = super.generateLalSegmentIds(count).stream()
                .map(id -> id + Goal.METRIKA_SEGMENT_UPPER_BOUND)
                .collect(Collectors.toList());
        logger.info("generateLalSegmentIds({}) => {}", count, ids);
        return ids;
    }
}
