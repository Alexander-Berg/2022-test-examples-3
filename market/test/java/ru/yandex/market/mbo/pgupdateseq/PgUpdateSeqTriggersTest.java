package ru.yandex.market.mbo.pgupdateseq;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author amaslak
 */
public class PgUpdateSeqTriggersTest extends BaseUpdateSeqTestClass {

    @Test
    public void whenInsertShouldInsertThisInStagingTable() {
        List<Map<String, Object>> rs1 =
                jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq_staging");
        assertThat(rs1).isEmpty();

        int testId = 2997;
        fillTestData(testId, "Some test");

        List<Map<String, Object>> rs2 =
                jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq_staging");
        assertThat(rs2).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(rs2.get(0).get("id")).isEqualTo(testId);
            s.assertThat(rs2.get(0).get("update_ts")).isNotNull();
            s.assertThat(rs2.get(0).get("update_id")).isNotNull();
        });
    }

    @Test
    public void whenUpdateSeqShouldCopyIntoSeqTable() {
        List<Map<String, Object>> rs1 =
                jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq_staging");
        assertThat(rs1).isEmpty();

        int testId = 2;
        fillTestData(testId, "Some test");

        assertThat(jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq")).isEmpty();
        copyFromStaging();

        List<Map<String, Object>> rs2 =
                jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq");
        assertThat(rs2).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(rs2.get(0).get("id")).isEqualTo(testId);
            s.assertThat(rs2.get(0).get("modified_seq_id")).isEqualTo(1L);
        });
    }

    @Test
    public void whenNoChangesShouldNotWriteToStaging() {
        List<Map<String, Object>> rs1 = jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq");
        assertThat(rs1).isEmpty();

        int testId = 22997;
        String testName = "Some test";
        fillTestData(testId, testName);

        copyFromStaging();

        // update with same value
        jdbcTemplate.update("update test.test_table set name = ? where id = ?", testName, testId);

        List<Map<String, Object>> rs2 =
                jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq_staging");
        assertThat(rs2).isEmpty();
    }

    @Test
    public void whenHasChangesShouldWriteToStaging() {
        List<Map<String, Object>> rs1 = jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq");
        assertThat(rs1).isEmpty();

        int testId = 22997;
        String testName = "Some test";
        fillTestData(testId, testName);

        copyFromStaging();

        // update with changed value
        jdbcTemplate.update("update test.test_table set name = ? where id = ?", "Changed name", testId);

        List<Map<String, Object>> staging =
                jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq_staging");

        copyFromStaging();
        List<Map<String, Object>> seq =
                jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq");
        assertThat(staging).hasSize(1);
        assertThat(seq).hasSize(2);

        assertThat(seq).allSatisfy(row -> {
            assertThat(row.get("id")).isEqualTo(testId);
            assertThat(row.get("modified_seq_id")).isNotNull();
        });
    }

    @Test
    public void whenDeleteShouldWriteChange() {
        List<Map<String, Object>> rs1 = jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq");
        assertThat(rs1).isEmpty();

        int testId = 22997;
        String testName = "Some test";
        fillTestData(testId, testName);

        copyFromStaging();

        // update with changed value
        jdbcTemplate.update("delete from test.test_table");

        List<Map<String, Object>> staging =
                jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq_staging");
        assertThat(staging).hasSize(1);

        copyFromStaging();
        List<Map<String, Object>> seq =
                jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq");
        assertThat(seq).hasSize(2);

        assertThat(seq).allSatisfy(row -> {
            assertThat(row.get("id")).isEqualTo(testId);
            assertThat(row.get("modified_seq_id")).isNotNull();
        });
    }

    @Test
    public void whenRecreateTriggersShouldWriteChangedIdLog() {
        jdbcTemplate.execute("select updateseq_test.update_seq_drop_triggers('test', 'test_table')");
        jdbcTemplate.execute("select updateseq_test.update_seq_init_triggers('test', 'test_table')");

        List<Map<String, Object>> rs1 = jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq");
        assertThat(rs1).isEmpty();

        int testId = 22997;
        fillTestData(testId, "Some test");
        copyFromStaging();

        List<Map<String, Object>> rs2 = jdbcTemplate.queryForList("select * from updateseq_test.test_table_update_seq");
        assertThat(rs2).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(rs2.get(0).get("id")).isEqualTo(testId);
            s.assertThat(rs2.get(0).get("modified_seq_id")).isNotNull();
        });
    }

}
