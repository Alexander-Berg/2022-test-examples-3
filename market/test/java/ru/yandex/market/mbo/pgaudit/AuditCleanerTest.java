package ru.yandex.market.mbo.pgaudit;

import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author amaslak
 * @created 02.08.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class AuditCleanerTest extends BaseAuditTestClass {
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

    @Autowired
    private PgAuditCleanerService pgAuditCleanerService;

    @Test
    public void testCleanerRemovesRecords() {
        initAudit();
        transactionTemplate.execute(status -> {
            auditService.setContext("test_user", "some context", EVENT_ID);

            jdbcTemplate.execute(
                "insert into test.test_table (name, parent_id, data) " +
                    " values ('Some test', 1, '{\"a\": 1, \"complex\": {\"b\": 2}, \"list\": [1, 2, 3]}'::jsonb)");

            jdbcTemplate.execute("update test.test_table set name = 'Changed 1!'");
            jdbcTemplate.execute("update test.test_table set name = 'Changed 2!'");
            jdbcTemplate.execute("update test.test_table set name = 'Changed 3!'");

            List<PgAuditRecord> records = repository.findAll();
            Assertions.assertThat(records).hasSize(4);

            // will clean this amount of first records
            int cleanCount = 2;
            List<Long> idsToClean = records.subList(0, cleanCount)
                .stream().map(PgAuditRecord::getId)
                .collect(Collectors.toList());
            List<PgAuditRecord> recordsToRemain = records.subList(cleanCount, records.size());

            pgAuditCleanerService.storeAuditRecordsToClean(idsToClean);
            pgAuditCleanerService.clearAuditRecords();
            Assertions.assertThat(pgAuditCleanerService.getLatestStoredAuditRecordIdToClean()).isNull();

            List<PgAuditRecord> recordsAfter = repository.findAll();
            Assertions.assertThat(recordsAfter).hasSize(recordsToRemain.size());

            Assertions.assertThat(recordsAfter)
                .usingFieldByFieldElementComparator()
                .isEqualTo(recordsToRemain);

            return null;
        });
    }
}
