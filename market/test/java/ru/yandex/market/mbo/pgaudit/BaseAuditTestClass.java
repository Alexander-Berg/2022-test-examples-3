package ru.yandex.market.mbo.pgaudit;

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

import ru.yandex.market.mbo.pgaudit.config.AuditTestConfig;
import ru.yandex.market.mbo.pgaudit.config.DbConfig;
import ru.yandex.market.mbo.pgaudit.config.PgInitializer;

/**
 * @author yuramalinov
 * @created 02.08.2019
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
    initializers = PgInitializer.class,
    classes = {DbConfig.class, AuditTestConfig.class}
)
public abstract class BaseAuditTestClass {
    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setup() {
        tearDown();
        jdbcTemplate.execute(readResource("migrations/audit-sql/schema.sql").replace("${audit.schema}", "audit_test"));
        jdbcTemplate.execute(readResource("migrations/audit-sql/audit.sql").replace("${audit.schema}", "audit_test"));
    }

    @After
    public void tearDown() {
        jdbcTemplate.execute("drop schema if exists audit_test cascade");
        jdbcTemplate.execute("drop schema if exists test cascade");
        jdbcTemplate.execute("drop table if exists public.audit_user");
    }

    protected void initAudit() {
        jdbcTemplate.execute("create schema test;");
        jdbcTemplate.execute(
            "create table test.test_table (id serial primary key, name text, parent_id int, data jsonb);");
        jdbcTemplate.execute("alter type audit_test.entity_type add value 'test_table'");
        jdbcTemplate.execute("select audit_test.init_audit(" +
            "'test.test_table', 'test_table', '''key'' || $row.id::text', " +
            "'jsonb_build_object(''parent_id'', $row.parent_id)')");
    }

    @SuppressWarnings({"UnstableApiUsage", "ConstantConditions"})
    private String readResource(String name) {
        try {
            return new String(ByteStreams.toByteArray(
                getClass().getClassLoader().getResourceAsStream(name)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
