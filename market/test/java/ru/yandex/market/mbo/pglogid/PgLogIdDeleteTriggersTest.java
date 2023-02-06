package ru.yandex.market.mbo.pglogid;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

/**
 * @author apluhin
 */
public class PgLogIdDeleteTriggersTest extends BaseLogIdTestWithDeleteTriggerClass {

    @Test
    public void whenDeleteSeqShouldReplaceNullWithNumber() {
        List<Map<String, Object>> rs1 = jdbcTemplate.queryForList("select * from logid_test.log_id_test_table");
        Assertions.assertThat(rs1).isEmpty();

        int testId = 22997;
        String testName = "Some test";
        fillTestData(testId, testName);

        jdbcTemplate.execute("select logid_test.log_id_test_table_update_seq(100)");

        // update with changed value
        long deleted = removeTestDataById(testId);
        Assertions.assertThat(deleted).isEqualTo(1);

        List<Map<String, Object>> rs2 = jdbcTemplate.queryForList("select * from logid_test.log_id_test_table");
        Assertions.assertThat(rs2).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(rs2.get(0).get("id")).isEqualTo(testId);
            s.assertThat(rs2.get(0).get("modified_seq_id")).isNull(); // old sequence id replaced with null
        });
    }

}
