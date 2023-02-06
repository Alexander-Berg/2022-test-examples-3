package ru.yandex.market.mbo.pglogid;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

/**
 * @author amaslak
 */
public class PgLogIdTriggersTest extends BaseLogIdTestClass {

    @Test
    public void whenInsertNewRecodrShouldWriteNull() {
        List<Map<String, Object>> rs1 = jdbcTemplate.queryForList("select * from logid_test.log_id_test_table");
        Assertions.assertThat(rs1).isEmpty();

        int testId = 2997;
        fillTestData(testId, "Some test");

        List<Map<String, Object>> rs2 = jdbcTemplate.queryForList("select * from logid_test.log_id_test_table");
        Assertions.assertThat(rs2).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(rs2.get(0).get("id")).isEqualTo(testId);
            s.assertThat(rs2.get(0).get("modified_seq_id")).isNull();
        });
    }

    @Test
    public void whenUpdateSeqShouldReplaceNullWithNumber() {
        List<Map<String, Object>> rs1 = jdbcTemplate.queryForList("select * from logid_test.log_id_test_table");
        Assertions.assertThat(rs1).isEmpty();

        int testId = 2;
        fillTestData(testId, "Some test");

        jdbcTemplate.execute("select logid_test.log_id_test_table_update_seq(100)");

        List<Map<String, Object>> rs2 = jdbcTemplate.queryForList("select * from logid_test.log_id_test_table");
        Assertions.assertThat(rs2).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(rs2.get(0).get("id")).isEqualTo(testId);
            s.assertThat(rs2.get(0).get("modified_seq_id")).isEqualTo(1L);
        });
    }

    @Test
    public void whenNoChangesShouldNotWriteNull() {
        List<Map<String, Object>> rs1 = jdbcTemplate.queryForList("select * from logid_test.log_id_test_table");
        Assertions.assertThat(rs1).isEmpty();

        int testId = 22997;
        String testName = "Some test";
        fillTestData(testId, testName);

        jdbcTemplate.execute("select logid_test.log_id_test_table_update_seq(100)");

        // update with same value
        jdbcTemplate.update("update test.test_table set name = ? where id = ?", testName, testId);

        List<Map<String, Object>> rs2 = jdbcTemplate.queryForList("select * from logid_test.log_id_test_table");
        Assertions.assertThat(rs2).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(rs2.get(0).get("id")).isEqualTo(testId);
            s.assertThat(rs2.get(0).get("modified_seq_id")).isNotNull(); // old sequence id preserved
        });
    }

    @Test
    public void whenHasChangesShouldWriteNull() {
        List<Map<String, Object>> rs1 = jdbcTemplate.queryForList("select * from logid_test.log_id_test_table");
        Assertions.assertThat(rs1).isEmpty();

        int testId = 22997;
        String testName = "Some test";
        fillTestData(testId, testName);

        jdbcTemplate.execute("select logid_test.log_id_test_table_update_seq(100)");

        // update with changed value
        jdbcTemplate.update("update test.test_table set name = ? where id = ?", "Changed name", testId);

        List<Map<String, Object>> rs2 = jdbcTemplate.queryForList("select * from logid_test.log_id_test_table");
        Assertions.assertThat(rs2).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(rs2.get(0).get("id")).isEqualTo(testId);
            s.assertThat(rs2.get(0).get("modified_seq_id")).isNull(); // old sequence id replaced with null
        });
    }

    @Test
    public void whenRecreateTriggersShouldWriteChangedIdLog() {
        jdbcTemplate.execute("select logid_test.log_id_drop_triggers('test', 'test_table')");
        jdbcTemplate.execute("select logid_test.log_id_init_triggers('test', 'test_table')");

        List<Map<String, Object>> rs1 = jdbcTemplate.queryForList("select * from logid_test.log_id_test_table");
        Assertions.assertThat(rs1).isEmpty();

        int testId = 22997;
        fillTestData(testId, "Some test");

        List<Map<String, Object>> rs2 = jdbcTemplate.queryForList("select * from logid_test.log_id_test_table");
        Assertions.assertThat(rs2).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(rs2.get(0).get("id")).isEqualTo(testId);
            s.assertThat(rs2.get(0).get("modified_seq_id")).isNull();
        });
    }

}
