package ru.yandex.market.mbo.pglogid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author amaslak
 */
public class PgLogIdServiceTest extends BaseLogIdTestClass {

    private PgLogIdService<Long> pgLogIdService;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.pgLogIdService = new PgLogIdService<>(
                jdbcTemplate,
                "logid_test",
                "test",
                "test_table",
                (rs, rowNum) -> rs.getLong("id")
        );
    }

    @Test
    public void testUpdateModifiedSequence() {
        List<Map<String, Object>> rs1 = jdbcTemplate.queryForList("select * from logid_test.log_id_test_table");
        Assertions.assertThat(rs1).isEmpty();

        int testId = 959487;
        fillTestData(testId, "Some test");

        List<Map<String, Object>> rs2 = jdbcTemplate.queryForList("select * from logid_test.log_id_test_table");
        Assertions.assertThat(rs2).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(rs2.get(0).get("id")).isEqualTo(testId);
            s.assertThat(rs2.get(0).get("modified_seq_id")).isNull();
        });

        pgLogIdService.updateModifiedSequence(10);

        List<Map<String, Object>> rs3 = jdbcTemplate.queryForList("select * from logid_test.log_id_test_table");
        Assertions.assertThat(rs3).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(rs3.get(0).get("id")).isEqualTo(testId);
            s.assertThat(rs3.get(0).get("modified_seq_id")).isEqualTo(1L);
        });
    }

    @Test
    public void testGetLastModifiedSequenceId() {
        int initialSeqId = PgLogIdService.INITIAL_MODIFIED_SEQUENCE_ID;
        Assertions.assertThat(pgLogIdService.getLastModifiedSequenceId()).isEqualTo(initialSeqId);

        int modifiedRows = 421;
        IntStream.range(0, modifiedRows).forEach(i -> fillTestData(i, "Some test"));
        Assertions.assertThat(pgLogIdService.getLastModifiedSequenceId()).isEqualTo(initialSeqId);

        // fill some modified ids
        int batchSize = 132;
        pgLogIdService.updateModifiedSequence(batchSize);
        Assertions.assertThat(pgLogIdService.getLastModifiedSequenceId()).isEqualTo(batchSize);

        // fill all modified ids
        pgLogIdService.updateModifiedSequence(modifiedRows);
        Assertions.assertThat(pgLogIdService.getLastModifiedSequenceId()).isEqualTo(modifiedRows);
    }

    @Test
    public void testGetModifiedSequenceIdCount() {
        int initialSeqId = PgLogIdService.INITIAL_MODIFIED_SEQUENCE_ID;
        Assertions.assertThat(pgLogIdService.getModifiedSequenceIdCount(initialSeqId)).isEqualTo(0);

        int modifiedRows = 421;
        IntStream.range(0, modifiedRows).forEach(i -> fillTestData(i, "Some test"));
        Assertions.assertThat(pgLogIdService.getLastModifiedSequenceId()).isEqualTo(initialSeqId);

        // fill some modified ids
        int batchSize = 132;
        pgLogIdService.updateModifiedSequence(batchSize);
        Assertions.assertThat(pgLogIdService.getModifiedSequenceIdCount(initialSeqId)).isEqualTo(batchSize);
        int delta = 5;
        Assertions.assertThat(pgLogIdService.getModifiedSequenceIdCount(initialSeqId + delta))
                .isEqualTo(batchSize - delta);

        // fill all modified ids
        pgLogIdService.updateModifiedSequence(modifiedRows);
        Assertions.assertThat(pgLogIdService.getModifiedSequenceIdCount(initialSeqId)).isEqualTo(modifiedRows);
    }

    @Test
    public void testGetModifiedRecordsIdBatch() {
        long initialSeqId = PgLogIdService.INITIAL_MODIFIED_SEQUENCE_ID;
        Assertions.assertThat(pgLogIdService.getModifiedRecordsIdBatch(initialSeqId, 1)).isEmpty();

        int modifiedRows = 421;
        IntStream.range(0, modifiedRows).forEach(i -> fillTestData(i, "Some test"));
        Assertions.assertThat(pgLogIdService.getModifiedRecordsIdBatch(initialSeqId, 1)).isEmpty();

        // fill some modified ids
        int batchSize = 5;
        pgLogIdService.updateModifiedSequence(batchSize);
        List<PgLogIdRow<Long>> batch1 = pgLogIdService.getModifiedRecordsIdBatch(initialSeqId, batchSize);
        long lastSeqId1 = initialSeqId + batchSize;

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(batch1).hasSize(batchSize);
            s.assertThat(batch1).extracting("key")
                    .isEqualTo(LongStream.range(0, batchSize).boxed().collect(Collectors.toList()));
            s.assertThat(batch1).extracting("modifiedSeqId")
                    .isEqualTo(LongStream.rangeClosed(1, lastSeqId1).boxed().collect(Collectors.toList()));
        });

        // fill all modified ids
        pgLogIdService.updateModifiedSequence(modifiedRows);
        List<PgLogIdRow<Long>> batch2 = pgLogIdService.getModifiedRecordsIdBatch(lastSeqId1, modifiedRows);
        long lastSeqId2 = initialSeqId + modifiedRows;

        // check new batch contains all ids in order
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(batch2).hasSize(modifiedRows - batchSize);
            s.assertThat(batch2).extracting("key")
                    .isEqualTo(LongStream.range(batchSize, modifiedRows).boxed().collect(Collectors.toList()));
            s.assertThat(batch2).extracting("modifiedSeqId")
                    .isEqualTo(LongStream.rangeClosed(lastSeqId1 + 1, lastSeqId2).boxed().collect(Collectors.toList()));
        });

        // update with changed value
        List<Long> changedKeys = List.of(1L, 16L, 23L, 127L);
        changedKeys.forEach(i -> {
            jdbcTemplate.update("update test.test_table set name = ? where id = ?", "Changed name", i);
            pgLogIdService.updateModifiedSequence(1);
        });
        List<PgLogIdRow<Long>> batch3 = pgLogIdService.getModifiedRecordsIdBatch(lastSeqId2, 100);
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
        long initialSeqId = PgLogIdService.INITIAL_MODIFIED_SEQUENCE_ID;

        int modifiedRows = 421;
        IntStream.range(0, modifiedRows).forEach(i -> fillTestData(i, "Some test"));

        // fill some modified ids
        int batchSize = 5;
        pgLogIdService.updateModifiedSequence(batchSize);
        List<Long> batch1 = new ArrayList<>();
        // use batch size to check total count limit
        long lastSeqId1 = pgLogIdService.processUpdatedKeys(initialSeqId, 2, batchSize, batch1::addAll);
        long expectedLastSeqId1 = initialSeqId + batchSize;

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lastSeqId1).isEqualTo(expectedLastSeqId1);
            s.assertThat(batch1).hasSize(batchSize);
            s.assertThat(batch1).isEqualTo(LongStream.range(0, batchSize).boxed().collect(Collectors.toList()));
        });

        // fill all modified ids
        pgLogIdService.updateModifiedSequence(modifiedRows);
        List<Long> batch2 = new ArrayList<>();
        long lastSeqId2 = pgLogIdService.processUpdatedKeys(lastSeqId1, modifiedRows, null, batch2::addAll);
        long expectedLastSeqId2 = initialSeqId + modifiedRows;

        // check new batch contains all ids in order
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lastSeqId2).isEqualTo(expectedLastSeqId2);
            s.assertThat(batch2).hasSize(modifiedRows - batchSize);
            s.assertThat(batch2)
                    .isEqualTo(LongStream.range(batchSize, modifiedRows).boxed().collect(Collectors.toList()));
        });

        // update with changed value
        List<Long> changedKeys = List.of(1L, 16L, 76L, 23L, 127L);
        changedKeys.forEach(i -> {
            jdbcTemplate.update("update test.test_table set name = ? where id = ?", "Changed name", i);
            pgLogIdService.updateModifiedSequence(1);
        });

        List<Long> batch3 = new ArrayList<>();
        long lastSeqId3 = pgLogIdService.processUpdatedKeys(lastSeqId2, 100, null, batch3::addAll);
        long expectedLastSeqId3 = lastSeqId2 + changedKeys.size();

        // check new batch contains changed ids in order
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lastSeqId3).isEqualTo(expectedLastSeqId3);
            s.assertThat(batch3).hasSize(changedKeys.size());
            s.assertThat(batch3).isEqualTo(changedKeys);
        });
    }

}
