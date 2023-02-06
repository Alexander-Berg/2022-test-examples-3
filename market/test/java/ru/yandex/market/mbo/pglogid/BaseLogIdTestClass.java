package ru.yandex.market.mbo.pglogid;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.io.ByteStreams;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.mbo.pglogid.config.DbConfig;
import ru.yandex.market.mbo.pglogid.config.LogIdTestConfig;
import ru.yandex.market.mbo.pglogid.config.PgInitializer;

/**
 * @author amaslak
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
    initializers = PgInitializer.class,
    classes = {DbConfig.class, LogIdTestConfig.class}
)
@Transactional
public abstract class BaseLogIdTestClass {

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

        jdbcTemplate.execute("select logid_test.log_id_init_tables('test', 'test_table')");
        jdbcTemplate.execute("select logid_test.log_id_init_functions('test', 'test_table')");
        jdbcTemplate.execute("select logid_test.log_id_init_triggers('test', 'test_table')");
    }

    @After
    public void tearDown() {
        jdbcTemplate.execute("drop schema if exists logid_test cascade");
        jdbcTemplate.execute("drop schema if exists test cascade");
    }

    protected void fillTestData(int id, String name) {
        jdbcTemplate.update("insert into test.test_table (id, name, parent_id, data) " +
                " values (?, ?, 1, '{\"a\": 1, \"complex\": {\"b\": 2}, \"list\": [1, 2, 3]}'::jsonb)",
                id, name
        );
    }

    @SuppressWarnings({"UnstableApiUsage", "ConstantConditions"})
    String readResource(String name) {
        try {
            return new String(ByteStreams.toByteArray(
                getClass().getClassLoader().getResourceAsStream(name)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
