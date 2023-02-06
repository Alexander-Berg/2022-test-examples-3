package ru.yandex.direct.jobs.segment;

import java.time.LocalDate;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.jobs.base.logdatatransfer.LogFetchingStrategy;
import ru.yandex.direct.jobs.segment.log.IntermediateSegmentYtRepositoryMock;
import ru.yandex.direct.jobs.segment.log.SegmentSourceData;
import ru.yandex.direct.jobs.segment.log.cpmdefault.CpmDefaultLogFetchingStrategyFactory;
import ru.yandex.direct.jobs.segment.log.greedy.GreedySegmentLogFetchingStrategy;

public class MockedSegmentLogFetchingStrategyFactory extends CpmDefaultLogFetchingStrategyFactory {

    private final LocalDate finishLogDate;

    public MockedSegmentLogFetchingStrategyFactory(
            LocalDate finishLogDate,
            PpcPropertiesSupport ppcPropertiesSupport) {
        super(null, ppcPropertiesSupport);
        this.finishLogDate = finishLogDate;
    }

    @Override
    public LogFetchingStrategy<UsersSegment, SegmentSourceData> createGreedyLogFetchingStrategy() {
        IntermediateSegmentYtRepositoryMock intermediateSegmentYtRepositoryMock =
                new IntermediateSegmentYtRepositoryMock();
        intermediateSegmentYtRepositoryMock.putEmptyData(finishLogDate.minusDays(1));

        return new GreedySegmentLogFetchingStrategy(
                intermediateSegmentYtRepositoryMock,
                () -> 10_000L);
    }
}
