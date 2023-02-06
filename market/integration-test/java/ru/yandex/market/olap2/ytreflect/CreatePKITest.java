package ru.yandex.market.olap2.ytreflect;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.olap2.config.IntegrationTestConfig;
import ru.yandex.market.olap2.load.LoadTask;
import ru.yandex.market.olap2.load.TestLoadTask;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class CreatePKITest {
    @Autowired
    private CreatePK createPK;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    // see recreate_test_table.sh
    public void createPK() throws Exception {
        LoadTask task = new TestLoadTask("CreatePKITestevid1", YtTestTable.TBL);
        jdbcTemplate.getJdbcOperations().execute(
            "drop table if exists " + task.getTable() + " cascade");
        jdbcTemplate.getJdbcOperations().execute(
            "create table " + task.getTable() + " (pk_int_col int not null) ");
        createPK.createPK(task);
        assertThat(jdbcTemplate.queryForObject("select column_name from primary_keys " +
            "where table_name = :table_name",
            ImmutableMap.of("table_name", task.getTable()),
            String.class), is("pk_int_col"));
    }
}
