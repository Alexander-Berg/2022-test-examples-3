package ru.yandex.market.tms.quartz2.util;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

public class QrtzLogTableCleanerTest {
    private static final int TEST_DAYS_TO_KEEP_LOGS = 7;
    private static final String TEST_QRTZ_LOG_TABLE_NAME = "TEST_QRTZ_LOG";
    private static final int TEST_BATCH_SIZE = 2;

    private static final ZonedDateTime NOW = ZonedDateTime.now();
    private static final ZonedDateTime FIVE_MIN_AGO = NOW.minus(5, ChronoUnit.MINUTES);
    private static final ZonedDateTime EIGHT_DAYS_AGO = NOW.minusDays(8);
    private static final ZonedDateTime EIGHT_DAYS_AND_FIVE_MIN_AGO = NOW.minusDays(8).minus(5, ChronoUnit.MINUTES);

    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate txTemplate;
    private QrtzLogTableCleaner qrtzLogTableCleaner;

    @BeforeEach
    void setUp() {
        DataSource dataSource =
                new EmbeddedDatabaseBuilder()
                        .setName(new Date() + " Logs")
                        .setType(EmbeddedDatabaseType.H2)
                        .addScript("classpath:/sql/tms-core-quartz2_log_table.sql")
                        .setName(String.valueOf(System.nanoTime()))
                        .build();
        jdbcTemplate = new JdbcTemplate(dataSource);
        PlatformTransactionManager txManager = new DataSourceTransactionManager(dataSource);
        txTemplate = new TransactionTemplate(txManager);

        qrtzLogTableCleaner = new QrtzLogTableCleaner(
                TEST_DAYS_TO_KEEP_LOGS,
                TEST_QRTZ_LOG_TABLE_NAME,
                jdbcTemplate,
                txTemplate
        );
        qrtzLogTableCleaner.setBatchSize(TEST_BATCH_SIZE);
    }

    /**
     * Тест проверяет, что старые данные успешно удаляются из базы данных при вызове
     * {@code ru.yandex.market.tms.quartz2.util.QrtzLogTableCleaner#clean()}.
     */
    @Test
    @DbUnitDataSet
    public void testSuccessfulCleanupOfOldData() {
        long newEntryId = createQrtzLogEntry(FIVE_MIN_AGO, NOW);
        long firstOldEntryId = createQrtzLogEntry(EIGHT_DAYS_AGO, EIGHT_DAYS_AND_FIVE_MIN_AGO);
        long secondOldEntryId = createQrtzLogEntry(EIGHT_DAYS_AGO, EIGHT_DAYS_AND_FIVE_MIN_AGO);
        long thirdOldEntryId = createQrtzLogEntry(EIGHT_DAYS_AGO, EIGHT_DAYS_AND_FIVE_MIN_AGO);

        assertThat(newEntryId, not(anyOf(
                equalTo(firstOldEntryId),
                equalTo(secondOldEntryId),
                equalTo(thirdOldEntryId)
        )));
        assertThat(firstOldEntryId, not(anyOf(equalTo(secondOldEntryId), equalTo(thirdOldEntryId))));
        assertThat(secondOldEntryId, not(equalTo(thirdOldEntryId)));

        qrtzLogTableCleaner.clean();

        List<Long> logEntryIds = selectCurrentLogEntryIds();

        assertThat(logEntryIds, equalTo(Collections.singletonList(newEntryId)));
    }

    /**
     * Тест проверяет, что задача успешно завершается, если нет логов для очистки.
     */
    @Test
    @DbUnitDataSet
    public void testNothingToCleanup() {
        long newEntryId = createQrtzLogEntry(FIVE_MIN_AGO, NOW);

        qrtzLogTableCleaner.clean();

        List<Long> logEntryIds = selectCurrentLogEntryIds();

        assertThat(logEntryIds, equalTo(Collections.singletonList(newEntryId)));
    }

    /**
     * Тест проверят, что в случае ошибки в середине очистки, откатится только последний батч.
     */
    @Test
    @DbUnitDataSet
    public void testFailureInTheMiddleOfCleanup() {
        createQrtzLogEntry(EIGHT_DAYS_AGO, EIGHT_DAYS_AND_FIVE_MIN_AGO);
        createQrtzLogEntry(EIGHT_DAYS_AGO, EIGHT_DAYS_AND_FIVE_MIN_AGO);
        long third = createQrtzLogEntry(EIGHT_DAYS_AGO, EIGHT_DAYS_AND_FIVE_MIN_AGO);

        JdbcTemplate jdbcTemplateMock = spy(jdbcTemplate);
        doThrow(new TestException())
                .when(jdbcTemplateMock).batchUpdate(anyString(), eq(Collections.singletonList(third)), anyInt(), any());

        qrtzLogTableCleaner = new QrtzLogTableCleaner(
                TEST_DAYS_TO_KEEP_LOGS,
                TEST_QRTZ_LOG_TABLE_NAME,
                jdbcTemplateMock,
                txTemplate
        );
        qrtzLogTableCleaner.setBatchSize(TEST_BATCH_SIZE);

        try {
            qrtzLogTableCleaner.clean();
            fail("Expected exception didn't occur");
        } catch (TestException ignore) {
        }

        List<Long> logEntryIds = selectCurrentLogEntryIds();

        assertThat(logEntryIds, equalTo(Collections.singletonList(third)));
    }

    /**
     * Тест проверяет, что задачи, для которых не было проставлено время завершения (например из-за их прерывания)
     * также будут очищаться из логов.
     */
    @Test
    @DbUnitDataSet
    void testStuckJobsCleanup() {
        createQrtzLogEntry(EIGHT_DAYS_AGO, EIGHT_DAYS_AND_FIVE_MIN_AGO);
        createQrtzLogEntry(EIGHT_DAYS_AGO, null);

        qrtzLogTableCleaner.clean();

        List<Long> logEntryIds = selectCurrentLogEntryIds();

        assertThat(logEntryIds.size(), equalTo(0));
    }

    private List<Long> selectCurrentLogEntryIds() {
        return jdbcTemplate.queryForList("select ID from " + TEST_QRTZ_LOG_TABLE_NAME, Long.class);
    }

    private long createQrtzLogEntry(
            @Nonnull ZonedDateTime triggerFireTime,
            @Nullable ZonedDateTime jobFinishTime
    ) {
        KeyHolder holder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement(
                            "insert into " + TEST_QRTZ_LOG_TABLE_NAME + " "
                                    + "(JOB_NAME, JOB_GROUP, TRIGGER_FIRE_TIME, JOB_FINISHED_TIME, JOB_STATUS, " +
                                    "HOST_NAME) "
                                    + "values ('testJob', 'DEFAULT', ?, ?, 'OK', 'some.host')",
                            new String[]{"ID"}
                    );

                    ps.setTimestamp(1, Timestamp.from(triggerFireTime.toInstant()));
                    if (jobFinishTime == null) {
                        ps.setNull(2, Types.TIMESTAMP);
                    } else {
                        ps.setTimestamp(2, Timestamp.from(jobFinishTime.toInstant()));
                    }

                    return ps;
                },
                holder
        );

        return holder.getKey().longValue();
    }

    private static class TestException extends RuntimeException {
    }

}
