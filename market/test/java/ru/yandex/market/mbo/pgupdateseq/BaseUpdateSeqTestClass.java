package ru.yandex.market.mbo.pgupdateseq;

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

import ru.yandex.market.mbo.pgupdateseq.config.DbConfig;
import ru.yandex.market.mbo.pgupdateseq.config.PgInitializer;
import ru.yandex.market.mbo.pgupdateseq.config.UpdateSeqTestConfig;

/**
 * @author amaslak
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
        initializers = PgInitializer.class,
        classes = {DbConfig.class, UpdateSeqTestConfig.class}
)
@Transactional
public abstract class BaseUpdateSeqTestClass {

    public static final int DEFAULT_PARTITION_SIZE = 10000;
    public static final int DEFAULT_BATCH_SIZE = 100;
    @Autowired
    @Qualifier("jdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    protected PgUpdateSeqService<Integer> updateSeqService;

    @Before
    public void setUp() throws Exception {
        jdbcTemplate.execute(readResource("migrations/update-seq-sql/schema.sql").replace("${updateseq.schema}",
                "updateseq_test"));
        jdbcTemplate.execute(readResource("migrations/update-seq-sql/updateseq.sql").replace("${updateseq.schema}",
                "updateseq_test"));

        this.jdbcTemplate.execute("create schema test;");
        this.jdbcTemplate.execute(
                "create table test.test_table (id serial primary key, name text, parent_id int, data jsonb);");

        jdbcTemplate.execute("select updateseq_test.update_seq_init_tables('test', 'test_table')");
        jdbcTemplate.execute("select updateseq_test.update_seq_init_functions('test', 'test_table')");
        jdbcTemplate.execute("select updateseq_test.update_seq_init_triggers('test', 'test_table')");

        updateSeqService = new PgUpdateSeqService<>(jdbcTemplate, "updateseq_test", "test_table",
                (rs, n) -> rs.getInt("id"));
    }

    @After
    public void tearDown() {
        jdbcTemplate.execute("drop schema if exists updateseq_test cascade");
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

    protected void copyFromStaging() {
        copyFromStaging(DEFAULT_BATCH_SIZE);
    }

    protected void copyFromStaging(int batchSize) {
        copyFromStaging(batchSize, DEFAULT_PARTITION_SIZE);
    }

    protected void copyFromStaging(int batchSize, int partitionSize) {
        updateSeqService.createPartitionsIfNeeded(partitionSize);
        updateSeqService.copyFromStaging(batchSize);
    }
}
