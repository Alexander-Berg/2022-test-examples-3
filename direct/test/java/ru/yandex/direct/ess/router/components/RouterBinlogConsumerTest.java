package ru.yandex.direct.ess.router.components;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlogbroker.logbroker_utils.models.BinlogEventWithOffset;
import ru.yandex.direct.ess.common.converter.LogicObjectWithSystemInfoConverter;
import ru.yandex.direct.ess.common.models.LogicObjectListWithInfo;
import ru.yandex.direct.ess.router.models.rule.ProcessedObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.ess.router.utils.RouterUtilsKt.pingProcessedObjectsCreator;

class RouterBinlogConsumerTest {

    private RouterBinlogConsumer routerBinlogConsumer;

    @BeforeEach
    public void before() {
        routerBinlogConsumer = new RouterBinlogConsumer(mock(RulesProcessingService.class),
                mock(LogbrokerWriterFactory.class), mock(RouterBinlogMonitoring.class), 0, 0);
    }

    @Test
    void getBinlogEventsPartitionMetricsTest() {

        BinlogEventWithOffset binlogEventWithOffsetPartition1MinSeqNo =
                new BinlogEventWithOffset(new BinlogEvent()
                        .withRows(ImmutableList.of(new BinlogEvent.Row(), new BinlogEvent.Row()))
                        .withUtcTimestamp(LocalDateTime.of(2019, 5, 19, 1, 1, 2)),
                        0, 1, 1);

        BinlogEventWithOffset binlogEventWithOffsetPartition1 =
                new BinlogEventWithOffset(new BinlogEvent()
                        .withRows(ImmutableList.of(new BinlogEvent.Row()))
                        .withUtcTimestamp(LocalDateTime.of(2019, 5, 19, 1, 1, 1)),
                        0, 1, 2);

        BinlogEventWithOffset binlogEventWithOffsetPartition1MaxTimestamp =
                new BinlogEventWithOffset(new BinlogEvent()
                        .withRows(ImmutableList.of(new BinlogEvent.Row(), new BinlogEvent.Row(), new BinlogEvent.Row()))
                        .withUtcTimestamp(LocalDateTime.of(2019, 5, 19, 1, 1, 4)),
                        0, 1, 3);

        // min seqNo
        BinlogEventWithOffset binlogEventWithOffsetPartition2MinSeqNo =
                new BinlogEventWithOffset(new BinlogEvent()
                        .withRows(ImmutableList.of(new BinlogEvent.Row(), new BinlogEvent.Row(), new BinlogEvent.Row()))
                        .withUtcTimestamp(LocalDateTime.of(2019, 5, 19, 2, 3, 3)),
                        0, 2, 5);

        // max timestamp
        BinlogEventWithOffset binlogEventWithOffsetPartition2MaxTimestamp =
                new BinlogEventWithOffset(new BinlogEvent()
                        .withRows(ImmutableList.of(new BinlogEvent.Row(), new BinlogEvent.Row(), new BinlogEvent.Row()))
                        .withUtcTimestamp(LocalDateTime.of(2019, 5, 19, 2, 3, 5)),
                        0, 2, 6);

        BinlogEventWithOffset binlogEventWithOffsetPartition2 =
                new BinlogEventWithOffset(new BinlogEvent()
                        .withRows(ImmutableList.of(new BinlogEvent.Row(), new BinlogEvent.Row(), new BinlogEvent.Row()))
                        .withUtcTimestamp(LocalDateTime.of(2019, 5, 19, 2, 3, 4)),
                        0, 2, 7);


        Map<Integer, PartitionMetrics> got =
                routerBinlogConsumer.getBinlogEventsPartitionMetrics(ImmutableList.of(
                        binlogEventWithOffsetPartition1MinSeqNo,
                        binlogEventWithOffsetPartition1,
                        binlogEventWithOffsetPartition1MaxTimestamp,
                        binlogEventWithOffsetPartition2MinSeqNo,
                        binlogEventWithOffsetPartition2,
                        binlogEventWithOffsetPartition2MaxTimestamp
                ));

        assertThat(got).hasSize(2);
        assertThat(got).containsKeys(1, 2);
        PartitionMetrics partitionMetrics1 = got.get(1);
        assertThat(partitionMetrics1.maxTimestamp).isEqualTo(1558227664L);
        assertThat(partitionMetrics1.minSeqNo).isEqualTo(1L);
        assertThat(partitionMetrics1.rowsCount).isEqualTo(6L);

        PartitionMetrics partitionMetrics2 = got.get(2);
        assertThat(partitionMetrics2.maxTimestamp).isEqualTo(1558231385L);
        assertThat(partitionMetrics2.minSeqNo).isEqualTo(5L);
        assertThat(partitionMetrics2.rowsCount).isEqualTo(9L);
    }


    /**
     * Тест проверяет, что при создании ping объекта не помеялись местами seqNo и timestamp
     */
    @Test
    void pingProcessedObjectsCreatorTest() {
        PartitionMetrics partitionMetrics = new PartitionMetrics(1241411L,
                13L, 1);
        List<ProcessedObject> got = pingProcessedObjectsCreator(partitionMetrics).get();
        assertThat(got).hasSize(1);
        ProcessedObject processedObject = got.get(0);
        assertThat(processedObject.getSeqNo()).isEqualTo(13L);
        LogicObjectListWithInfo logicObjectListWithInfo =
                LogicObjectWithSystemInfoConverter.getDefaultConverter().fromJson(processedObject.getLogicObjectWithSystemInfo());
        long utcTimestamp = logicObjectListWithInfo.getUtcTimestamp();
        boolean isPingObject = logicObjectListWithInfo.getIsPingObject();
        assertThat(utcTimestamp).isEqualTo(1241411L);
        assertThat(isPingObject).isTrue();
    }
}
