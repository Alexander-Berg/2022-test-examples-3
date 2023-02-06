package ru.yandex.market.mbo.pgupdateseq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author amaslak
 */
public class PgUpdateSeqServiceTest extends BaseUpdateSeqTestClass {

    @Test
    public void testUpdateModifiedSequence() {
        List<Map<String, Object>> staging1 =
                jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq_staging");
        assertThat(staging1).isEmpty();
        List<Map<String, Object>> rs1 = jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq");
        assertThat(rs1).isEmpty();

        int testId = 959487;
        fillTestData(testId, "Some test");

        List<Map<String, Object>> staging2 =
                jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq_staging");
        assertThat(staging2).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(staging2.get(0).get("id")).isEqualTo(testId);
        });

        copyFromStaging();

        List<Map<String, Object>> rs3 = jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq");
        assertThat(rs3).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(rs3.get(0).get("id")).isEqualTo(testId);
            s.assertThat(rs3.get(0).get("modified_seq_id")).isEqualTo(1L);
        });
    }

    @Test
    public void testGetLastModifiedSequenceId() {
        var initialSeqId = PgUpdateSeqService.INITIAL_MODIFIED_SEQUENCE_ID;
        assertThat(updateSeqService.getLastModifiedSequenceId()).isEqualTo(initialSeqId);

        int modifiedRows = 421;
        IntStream.range(0, modifiedRows).forEach(i -> fillTestData(i, "Some test"));
        assertThat(updateSeqService.getLastModifiedSequenceId()).isEqualTo(initialSeqId);

        // fill some modified ids
        int batchSize = 132;
        copyFromStaging(batchSize);
        assertThat(updateSeqService.getLastModifiedSequenceId()).isEqualTo(batchSize);

        // fill all modified ids
        updateSeqService.copyFromStaging(modifiedRows);
        assertThat(updateSeqService.getLastModifiedSequenceId()).isEqualTo(modifiedRows);
    }

    @Test
    public void testGetModifiedSequenceIdCount() {
        var initialSeqId = PgUpdateSeqService.INITIAL_MODIFIED_SEQUENCE_ID;
        assertThat(updateSeqService.getModifiedSequenceIdCount(initialSeqId)).isEqualTo(0);

        int modifiedRows = 421;
        IntStream.range(0, modifiedRows).forEach(i -> fillTestData(i, "Some test"));
        assertThat(updateSeqService.getLastModifiedSequenceId()).isEqualTo(initialSeqId);

        // fill some modified ids
        int batchSize = 132;
        copyFromStaging(batchSize);
        assertThat(updateSeqService.getModifiedSequenceIdCount(initialSeqId)).isEqualTo(batchSize);
        int delta = 5;
        assertThat(updateSeqService.getModifiedSequenceIdCount(initialSeqId + delta))
                .isEqualTo(batchSize - delta);

        // fill all modified ids
        updateSeqService.copyFromStaging(modifiedRows);
        assertThat(updateSeqService.getModifiedSequenceIdCount(initialSeqId)).isEqualTo(modifiedRows);
    }

    @Test
    public void testGetModifiedRecordsIdBatch() {
        long initialSeqId = PgUpdateSeqService.INITIAL_MODIFIED_SEQUENCE_ID;
        assertThat(updateSeqService.getModifiedRecordsIdBatch(initialSeqId, 1)).isEmpty();

        int modifiedRows = 421;
        IntStream.range(0, modifiedRows).forEach(i -> fillTestData(i, "Some test"));
        assertThat(updateSeqService.getModifiedRecordsIdBatch(initialSeqId, 1)).isEmpty();

        // fill some modified ids
        int batchSize = 5;
        copyFromStaging(batchSize);
        List<PgUpdateSeqRow<Integer>> batch1 = updateSeqService.getModifiedRecordsIdBatch(initialSeqId, batchSize);
        long lastSeqId1 = initialSeqId + batchSize;

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(batch1).hasSize(batchSize);
            s.assertThat(batch1).extracting("key")
                    .isEqualTo(IntStream.range(0, batchSize).boxed().collect(Collectors.toList()));
            s.assertThat(batch1).extracting("modifiedSeqId")
                    .isEqualTo(LongStream.rangeClosed(1, lastSeqId1).boxed().collect(Collectors.toList()));
        });

        // fill all modified ids
        copyFromStaging(modifiedRows);
        List<PgUpdateSeqRow<Integer>> batch2 = updateSeqService.getModifiedRecordsIdBatch(lastSeqId1, modifiedRows);
        long lastSeqId2 = initialSeqId + modifiedRows;

        // check new batch contains all ids in order
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(batch2).hasSize(modifiedRows - batchSize);
            s.assertThat(batch2).extracting("key")
                    .isEqualTo(IntStream.range(batchSize, modifiedRows).boxed().collect(Collectors.toList()));
            s.assertThat(batch2).extracting("modifiedSeqId")
                    .isEqualTo(LongStream.rangeClosed(lastSeqId1 + 1, lastSeqId2).boxed().collect(Collectors.toList()));
        });

        // update with changed value
        List<Integer> changedKeys = List.of(1, 16, 23, 127);
        changedKeys.forEach(i -> {
            jdbcTemplate.update("update test.test_table set name = ? where id = ?", "Changed name", i);
        });
        copyFromStaging();
        List<PgUpdateSeqRow<Integer>> batch3 = updateSeqService.getModifiedRecordsIdBatch(lastSeqId2, 100);
        long lastSeqId3 = lastSeqId2 + changedKeys.size();

        // check new batch contains changed ids in order
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(batch3).hasSize(changedKeys.size());
            s.assertThat(batch3).extracting("key").isEqualTo(changedKeys);
            s.assertThat(batch3).extracting("modifiedSeqId")
                    .isEqualTo(LongStream.rangeClosed(lastSeqId2 + 1, lastSeqId3).boxed().collect(Collectors.toList()));
        });
    }

    @Test
    public void testProcessUpdatedKeys() {
        long initialSeqId = PgUpdateSeqService.INITIAL_MODIFIED_SEQUENCE_ID;

        int modifiedRows = 421;
        IntStream.range(0, modifiedRows).forEach(i -> fillTestData(i, "Some test"));

        // fill some modified ids
        int batchSize = 5;
        copyFromStaging(batchSize);
        List<Integer> batch1 = new ArrayList<>();
        // use batch size to check total count limit
        long lastSeqId1 = updateSeqService.processUpdatedKeys(initialSeqId, 2, batchSize, batch1::addAll);
        long expectedLastSeqId1 = initialSeqId + batchSize;

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lastSeqId1).isEqualTo(expectedLastSeqId1);
            s.assertThat(batch1).hasSize(batchSize);
            s.assertThat(batch1).isEqualTo(IntStream.range(0, batchSize).boxed().collect(Collectors.toList()));
        });

        // fill all modified ids
        updateSeqService.copyFromStaging(modifiedRows);
        List<Integer> batch2 = new ArrayList<>();
        long lastSeqId2 = updateSeqService.processUpdatedKeys(lastSeqId1, modifiedRows, null, batch2::addAll);
        long expectedLastSeqId2 = initialSeqId + modifiedRows;

        // check new batch contains all ids in order
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lastSeqId2).isEqualTo(expectedLastSeqId2);
            s.assertThat(batch2).hasSize(modifiedRows - batchSize);
            s.assertThat(batch2)
                    .isEqualTo(IntStream.range(batchSize, modifiedRows).boxed().collect(Collectors.toList()));
        });

        // update with changed value
        List<Integer> changedKeys = List.of(1, 16, 76, 23, 127);
        changedKeys.forEach(i -> {
            jdbcTemplate.update("update test.test_table set name = ? where id = ?", "Changed name", i);
            updateSeqService.copyFromStaging(1);
        });

        List<Integer> batch3 = new ArrayList<>();
        long lastSeqId3 = updateSeqService.processUpdatedKeys(lastSeqId2, 100, null, batch3::addAll);
        long expectedLastSeqId3 = lastSeqId2 + changedKeys.size();

        // check new batch contains changed ids in order
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lastSeqId3).isEqualTo(expectedLastSeqId3);
            s.assertThat(batch3).hasSize(changedKeys.size());
            s.assertThat(batch3).isEqualTo(changedKeys);
        });
    }

    @Test
    public void testSeveralChangesToKeyAreFetchedAsSeveralChanges() {
        int modifiedRows = 20;
        IntStream.range(0, modifiedRows).forEach(i -> fillTestData(i, "Some test"));
        copyFromStaging();
        long lastSeqId = updateSeqService.getLastModifiedSequenceId();

        List<Integer> changedKeys = List.of(1, 3, 5, 7, 1, 3);
        AtomicInteger n = new AtomicInteger();
        changedKeys.forEach(i -> {
            jdbcTemplate.update(
                    "update test.test_table set name = ? where id = ?",
                    "Changed name " + n.getAndIncrement(), i);
        });
        copyFromStaging();

        List<Integer> modifiedKeys = new ArrayList<>();
        updateSeqService.processUpdatedKeys(lastSeqId, 100, null, modifiedKeys::addAll);
        assertThat(modifiedKeys).containsExactlyElementsOf(changedKeys);
    }
}
