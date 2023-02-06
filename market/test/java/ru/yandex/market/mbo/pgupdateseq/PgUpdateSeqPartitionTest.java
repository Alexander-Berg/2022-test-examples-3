package ru.yandex.market.mbo.pgupdateseq;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PgUpdateSeqPartitionTest extends BaseUpdateSeqTestClass {
    @Test
    public void testItCreatesPartitions() {
        int count = 1000;
        IntStream.range(0, count).forEach(i -> fillTestData(i, "test"));

        while (true) {
            updateSeqService.createPartitionsIfNeeded(100);
            long copied = updateSeqService.copyFromStaging(50);
            if (copied == 0) {
                break;
            }
        }

        List<PgUpdateSeqService.PartitionInfo> partitions = updateSeqService.findPartitions();
        // 1001 is max seq, and it'll ensure there is 100 more free space, and last cycle is empty, just ensuring
        assertThat(partitions).hasSize(12);
    }

    @Test
    public void testItCleansUpPartitions() {
        int count = 1000;
        IntStream.range(0, count).forEach(i -> fillTestData(i, "test"));

        while (true) {
            updateSeqService.createPartitionsIfNeeded(100);
            long copied = updateSeqService.copyFromStaging(50);
            if (copied == 0) {
                break;
            }
        }

        List<PgUpdateSeqService.PartitionInfo> partitions = updateSeqService.findPartitions();
        assertThat(partitions).hasSize(12);
        updatePartitionBackInTime(partitions.get(0), 6);
        updatePartitionBackInTime(partitions.get(1), 4);
        updatePartitionBackInTime(partitions.get(2), 2);

        updateSeqService.cleanupPartitions(Instant.now().minus(5, ChronoUnit.HOURS));
        List<PgUpdateSeqService.PartitionInfo> firstCleanup = updateSeqService.findPartitions();
        assertThat(firstCleanup).hasSize(11);

        updateSeqService.cleanupPartitions(Instant.now().minus(3, ChronoUnit.HOURS));
        List<PgUpdateSeqService.PartitionInfo> secondCleanup = updateSeqService.findPartitions();
        assertThat(secondCleanup).hasSize(10);

        updateSeqService.cleanupPartitions(Instant.now().minus(1, ChronoUnit.HOURS));
        List<PgUpdateSeqService.PartitionInfo> thirdCleanup = updateSeqService.findPartitions();
        assertThat(thirdCleanup).hasSize(9);

        updateSeqService.cleanupPartitions(Instant.now().plus(1, ChronoUnit.HOURS));
        List<PgUpdateSeqService.PartitionInfo> lastCleanup = updateSeqService.findPartitions();
        assertThat(lastCleanup).hasSize(1); // Empty partition isn't removed
    }

    private void updatePartitionBackInTime(PgUpdateSeqService.PartitionInfo partitionInfo, int hours) {
        jdbcTemplate.update("update updateseq_test." + partitionInfo.name + " set update_moved_ts = ?",
                new Timestamp(Instant.now().minus(hours, ChronoUnit.HOURS).toEpochMilli()));
    }
}
