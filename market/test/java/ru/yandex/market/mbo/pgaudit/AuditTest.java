package ru.yandex.market.mbo.pgaudit;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yuramalinov
 * @created 02.08.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class AuditTest extends BaseAuditTestClass {
    private static final long EVENT_ID = 42L;

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PgAuditRepository repository;

    @Autowired
    private PgAuditService auditService;

    @Value("${sql.port}")
    private int sqlPort;

    @Value("${sql.url}")
    private String sqlUrl;

    @Value("${sql.password}")
    private String sqlPassword;

    @Value("${sql.username}")
    private String sqlUsername;

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRecordInsertUpdateDelete() {
        initAudit();
        jdbcTemplate.execute(
            "insert into test.test_table (name, parent_id, data) " +
                " values ('Some test', 1, '{\"a\": 1, \"complex\": {\"b\": 2}, \"list\": [1, 2, 3]}'::jsonb)");

        List<PgAuditRecord> records = repository.findAll();
        assertThat(records).hasSize(1);

        PgAuditRecord record = records.get(0);
        assertThat(record.getChangeType()).isEqualTo(PgAuditChangeType.INSERT);
        assertThat(record.getEntityType()).isEqualTo("test_table");
        assertThat(record.getEntityKey()).isEqualTo("key1");
        assertThat(record.getEventId()).isNotNull();
        assertThat(record.getEventTimestamp()).isNotNull();
        assertThat(record.getUserLogin()).isNull(); // We didn't set it
        assertThat(record.getContext()).isNull(); // We didn't set it
        assertThat(record.getKeys().get("parent_id")).isEqualTo(1);

        Map<String, Object> changes = record.getChanges();
        assertThat(changes.get("id")).isEqualTo(1);
        assertThat(changes.get("name")).isEqualTo("Some test");
        assertThat(changes.get("parent_id")).isEqualTo(1);
        assertThat(changes.get("data")).isEqualTo(
            ImmutableMap.of(
                "a", 1,
                "complex", ImmutableMap.of("b", 2),
                "list", asList(1, 2, 3)));


        // Issue update
        jdbcTemplate.execute(
            " update test.test_table set " +
                " name = 'Changed!', " +
                " data = '{\"a\": 2, \"complex\": {\"b\": 3}, \"list\": [2, 3, 1], \"new\": {\"a\": 42}}'::jsonb");

        records = repository.findAll();
        assertThat(records).hasSize(2);

        record = records.get(0);
        assertThat(record.getChangeType()).isEqualTo(PgAuditChangeType.UPDATE);
        assertThat(record.getEntityType()).isEqualTo("test_table");
        assertThat(record.getEntityKey()).isEqualTo("key1");
        assertThat(record.getEventId()).isNotNull();
        assertThat(record.getEventTimestamp()).isNotNull();
        assertThat(record.getUserLogin()).isNull(); // We didn't set it
        assertThat(record.getContext()).isNull(); // We didn't set it
        assertThat(record.getKeys().get("parent_id")).isEqualTo(1);

        changes = record.getChanges();
        assertThat(changes).doesNotContainKeys("id", "parent_id"); // We didn't update them
        assertThat((List) changes.get("name")).containsExactly("Some test", "Changed!");
        assertThat(changes.get("data"))
            .isEqualTo(
                ImmutableMap.of(
                    "a", asList(1, 2),
                    "complex", ImmutableMap.of("b", asList(2, 3)),
                    "list", asList(asList(1, 2, 3), asList(2, 3, 1)),
                    "new", asList(null, ImmutableMap.of("a", 42))
                )
            );

        // Finally - delete something
        jdbcTemplate.execute("delete from test.test_table");

        records = repository.findAll();
        assertThat(records).hasSize(3);

        record = records.get(0);
        assertThat(record.getChangeType()).isEqualTo(PgAuditChangeType.DELETE);
        assertThat(record.getEntityType()).isEqualTo("test_table");
        assertThat(record.getEntityKey()).isEqualTo("key1");
        assertThat(record.getEventId()).isNotNull();
        assertThat(record.getEventTimestamp()).isNotNull();
        assertThat(record.getUserLogin()).isNull(); // We didn't set it
        assertThat(record.getContext()).isNull(); // We didn't set it
        assertThat(record.getKeys().get("parent_id")).isEqualTo(1);

        changes = record.getChanges();
        // should contain all fields, this is last state of the record
        assertThat(changes.get("id")).isEqualTo(1);
        assertThat(changes.get("name")).isEqualTo("Changed!");
        assertThat(changes.get("parent_id")).isEqualTo(1);
        assertThat(changes.get("data")).isEqualTo(
                // Pre-mortem state of jsonb field
                ImmutableMap.of(
                        "a", 2,
                        "complex", ImmutableMap.of("b", 3),
                        "list", asList(2, 3, 1),
                        "new", ImmutableMap.of("a", 42)
                )
        );
    }

    @Test
    public void whenUpdateJsonInternalObjectToNullShouldNotFail() {
        initAudit();
        jdbcTemplate.execute(
                "insert into test.test_table (name, parent_id, data) " +
                        " values ('test', 1, '{\"shelfLife\": {\"time\": 360, \"unit\": \"DAY\"}}'::jsonb)");

        // Issue update
        jdbcTemplate.execute(
                " update test.test_table set data = '{\"shelfLife\": null}'::jsonb");

        List<PgAuditRecord> records = repository.findAll();
        assertThat(records).hasSize(2);

        PgAuditRecord record = records.get(0);
        assertThat(record.getChangeType()).isEqualTo(PgAuditChangeType.UPDATE);
        assertThat(record.getEntityType()).isEqualTo("test_table");
        assertThat(record.getEntityKey()).isEqualTo("key1");
        assertThat(record.getEventId()).isNotNull();
        assertThat(record.getEventTimestamp()).isNotNull();
        assertThat(record.getUserLogin()).isNull(); // We didn't set it
        assertThat(record.getContext()).isNull(); // We didn't set it
        assertThat(record.getKeys().get("parent_id")).isEqualTo(1);

        Map<String, Object> changes = record.getChanges();
        assertThat(changes).doesNotContainKeys("id", "parent_id"); // We didn't update them
        assertThat(changes.get("data")).isEqualTo(
                ImmutableMap.of("shelfLife", asList(
                        ImmutableMap.of("time", 360, "unit", "DAY"),
                        null
                        )
                )
        );
    }

    @Test
    public void testContextIsRecorded() {
        initAudit();
        transactionTemplate.execute(status -> {
            auditService.setContext("test_user", "some context", EVENT_ID);

            jdbcTemplate.execute(
                    "insert into test.test_table (name, parent_id, data) " +
                            " values ('Some test', 1, '{\"a\": 1, \"complex\": {\"b\": 2}, \"list\": [1, 2, 3]}'::jsonb)");

            List<PgAuditRecord> records = repository.findAll();
            assertThat(records).hasSize(1);

            PgAuditRecord record = records.get(0);
            assertThat(record.getChangeType()).isEqualTo(PgAuditChangeType.INSERT);
            assertThat(record.getEntityType()).isEqualTo("test_table");
            assertThat(record.getEntityKey()).isEqualTo("key1");
            assertThat(record.getEventTimestamp()).isNotNull();

            // Context is set! so values are recorded
            assertThat(record.getUserLogin()).isEqualTo("test_user");
            assertThat(record.getEventId()).isEqualTo(EVENT_ID);
            assertThat(record.getContext()).isEqualTo("some context");

            auditService.clearContext();
            jdbcTemplate.execute("update test.test_table set name = 'Changed!'");

            records = repository.findAll();
            assertThat(records).hasSize(2);

            record = records.get(0);
            assertThat(record.getChangeType()).isEqualTo(PgAuditChangeType.UPDATE);
            assertThat(record.getEventId()).isNotNull(); // Auto generated
            assertThat(record.getUserLogin()).isNull(); // No context
            assertThat(record.getContext()).isNull(); // No context

            return null;
        });
    }

    @Test
    public void shouldntRecordWhenUpdateWithNoChanges() {
        initAudit();
        jdbcTemplate.execute(
                "insert into test.test_table (name, parent_id, data) " +
                        " values ('not changeable', 1, '{\"a\": 1, \"complex\": {\"b\": 2}, \"list\": [1, 2, 3]}'::jsonb)");

        List<PgAuditRecord> records = repository.findAll();
        assertThat(records).hasSize(1);

        PgAuditRecord record = records.get(0);
        assertThat(record.getChangeType()).isEqualTo(PgAuditChangeType.INSERT);
        assertThat(record.getEntityType()).isEqualTo("test_table");
        assertThat(record.getEntityKey()).isEqualTo("key1");
        assertThat(record.getEventId()).isNotNull();
        assertThat(record.getEventTimestamp()).isNotNull();
        assertThat(record.getUserLogin()).isNull(); // We didn't set it
        assertThat(record.getContext()).isNull(); // We didn't set it
        assertThat(record.getKeys().get("parent_id")).isEqualTo(1);

        Map<String, Object> changes = record.getChanges();
        assertThat(changes.get("id")).isEqualTo(1);
        assertThat(changes.get("name")).isEqualTo("not changeable");
        assertThat(changes.get("parent_id")).isEqualTo(1);
        assertThat(changes.get("data")).isEqualTo(
                ImmutableMap.of(
                        "a", 1,
                        "complex", ImmutableMap.of("b", 2),
                        "list", asList(1, 2, 3)));


        // try update
        jdbcTemplate.execute(
                " update test.test_table set " +
                        " name = 'not changeable', " +
                        " data = '{\"a\": 1, \"complex\": {\"b\": 2}, \"list\": [1, 2, 3]}'::jsonb");

        records = repository.findAll();
        assertThat(records).hasSize(1);

        record = records.get(0);
        assertThat(record.getChangeType()).isEqualTo(PgAuditChangeType.INSERT);
        assertThat(record.getEntityType()).isEqualTo("test_table");
        assertThat(record.getEntityKey()).isEqualTo("key1");
        assertThat(record.getEventId()).isNotNull();
        assertThat(record.getEventTimestamp()).isNotNull();
        assertThat(record.getUserLogin()).isNull(); // We didn't set it
        assertThat(record.getContext()).isNull(); // We didn't set it
        assertThat(record.getKeys().get("parent_id")).isEqualTo(1);

        changes = record.getChanges();
        assertThat(changes.get("id")).isEqualTo(1);
        assertThat(changes.get("name")).isEqualTo("not changeable");
        assertThat(changes.get("parent_id")).isEqualTo(1);
        assertThat(changes.get("data")).isEqualTo(
                ImmutableMap.of(
                        "a", 1,
                        "complex", ImmutableMap.of("b", 2),
                        "list", asList(1, 2, 3)));
    }
}
