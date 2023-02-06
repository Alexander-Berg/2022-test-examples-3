package ru.yandex.market.mbo.pgaudit;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yuramalinov
 * @created 02.08.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class AuditListenerTest extends BaseAuditTestClass {
    private static final Logger log = LogManager.getLogger();
    @Autowired
    @Qualifier("interceptingJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PgAuditRepository repository;

    @SuppressWarnings("unchecked")
    @Test
    public void listenerShouldInitUserNameAndContext() {
        initAudit();

        log.info("Hey there!");
        jdbcTemplate.execute(
            "insert into test.test_table (name, parent_id, data) " +
                " values ('Some test', 1, '{\"a\": 1, \"complex\": {\"b\": 2}, \"list\": [1, 2, 3]}'::jsonb)");

        List<PgAuditRecord> records = repository.findAll();
        assertThat(records).hasSize(1);

        PgAuditRecord record = records.get(0);
        assertThat(record.getUserLogin()).isEqualTo("test");
        assertThat(record.getContext()).isEqualTo(Thread.currentThread().getName());
    }
}
