package ru.yandex.market.mbo.pglogid;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author apluhin
 */
public abstract class BaseLogIdTestWithDeleteTriggerClass extends BaseLogIdTestClass {

    @Autowired
    @Qualifier("jdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        jdbcTemplate.execute(readResource("migrations/log-id-sql/schema.sql").replace("${logid.schema}", "logid_test"));
        jdbcTemplate.execute(readResource("migrations/log-id-sql/log_id.sql").replace("${logid.schema}", "logid_test"));

        this.jdbcTemplate.execute("create schema test;");
        this.jdbcTemplate.execute(
            "create table test.test_table (id serial primary key, name text, parent_id int, data jsonb);");

        jdbcTemplate.execute("select logid_test.log_id_init_tables_with_option_fk('test', 'test_table', false)");
        jdbcTemplate.execute("select logid_test.log_id_init_functions('test', 'test_table')");
        jdbcTemplate.execute("select logid_test.log_id_init_triggers('test', 'test_table')");
        jdbcTemplate.execute("select logid_test.log_id_init_delete_trigger('test', 'test_table')");
    }

    @After
    public void tearDown() {
        jdbcTemplate.execute("drop schema if exists logid_test cascade");
        jdbcTemplate.execute("drop schema if exists test cascade");
    }

    protected long removeTestDataById(int id) {
        return jdbcTemplate.update("delete from test.test_table where id = ?", id);
    }

}
