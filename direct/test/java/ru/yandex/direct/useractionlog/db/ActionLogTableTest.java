package ru.yandex.direct.useractionlog.db;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.JUnitSoftAssertions;
import org.jooq.SQLDialect;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.SingleColumnRowMapper;

import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.direct.binlogclickhouse.schema.FieldValueList;
import ru.yandex.direct.clickhouse.ClickHouseCluster;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.test.clickhouse.JunitRuleClickHouseCluster;
import ru.yandex.direct.tracing.data.DirectTraceInfo;
import ru.yandex.direct.useractionlog.ClientId;
import ru.yandex.direct.useractionlog.schema.ActionLogRecord;
import ru.yandex.direct.useractionlog.schema.ActionLogSchema;
import ru.yandex.direct.useractionlog.schema.ObjectPath;
import ru.yandex.direct.useractionlog.schema.Operation;
import ru.yandex.direct.useractionlog.schema.RecordSource;

import static ru.yandex.direct.utils.DateTimeUtils.MOSCOW_TIMEZONE;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
@YaIgnore
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
public class ActionLogTableTest {
    private static final String TEST_TABLE = "test_log";
    @ClassRule
    public static JunitRuleClickHouseCluster junitRuleClickHouseCluster = new JunitRuleClickHouseCluster();
    private static TimeZone initialTimeZone;
    private static DatabaseWrapper databaseWrapper;

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @BeforeClass
    public static void setUpClass() throws IOException, InterruptedException {
        initialTimeZone = TimeZone.getDefault();
        ClickHouseCluster clickHouseCluster = junitRuleClickHouseCluster.singleServerCluster();
        databaseWrapper = new DatabaseWrapper("test",
                new ClickHouseDataSource(clickHouseCluster.getClickHouseJdbcUrls().values().iterator().next()),
                SQLDialect.DEFAULT,
                EnvironmentType.DB_TESTING);
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(initialTimeZone);
    }

    /**
     * Независимо от того, в какой таймзоне находится каждый компонент системы, в базу должны записываться даты
     * и время в зоне UTC, и все LocalDate(Time)? должны быть в зоне UTC, а не в локальной зоне.
     * <p>
     * Какие даты и время были записаны, такие и должны быть прочитаны.
     */
    @Parameters({
            // Записывающий процесс в одной таймзоне, читающий в другой, а clickhouse в третьей
            "Antarctica/Troll, Asia/Yakutsk",

            // Записывающий и читающий процесс в одной таймзоне, а clickhouse в другой
            "Asia/Yakutsk, Asia/Yakutsk",

            // Записывающий процесс в одной таймзоне, а читающий и clickhouse в другой
            "Asia/Yakutsk, " + MOSCOW_TIMEZONE,

            // Записывающий процесс и clickhouse в одной таймзоне, а clickhouse в другой
            MOSCOW_TIMEZONE + ", Asia/Novosibirsk",

            // И записывающий процесс, и читающий, и clickhouse в одной таймзоне
            MOSCOW_TIMEZONE + ", " + MOSCOW_TIMEZONE,
    })
    @Test
    public void writesAndReadsInUtcTimeZone(String insertTimezone, String selectTimezone) {
        databaseWrapper.query(jdbc -> jdbc.update("DROP TABLE IF EXISTS " + TEST_TABLE));
        databaseWrapper.getDslContext().connection(new ActionLogSchema("default", TEST_TABLE)::createTable);

        TimeZone.setDefault(TimeZone.getTimeZone(insertTimezone));

        List<LocalDateTime> writtenDateTimes = IntStream.range(0, 24)
                .mapToObj(hour -> LocalDateTime.of(2018, 1, 1, hour, 0))
                .collect(Collectors.toList());
        List<ActionLogRecord> records = writtenDateTimes.stream()
                .map(dateTime -> ActionLogRecord.builder()
                        .withDateTime(dateTime)
                        .withPath(new ObjectPath.ClientPath(new ClientId(1)))
                        .withGtid("serveruuid:" + dateTime.getHour())
                        .withQuerySerial(0)
                        .withRowSerial(0)
                        .withDirectTraceInfo(DirectTraceInfo.empty())
                        .withDb("test")
                        .withType("test")
                        .withOperation(Operation.INSERT)
                        .withOldFields(FieldValueList.empty())
                        .withNewFields(FieldValueList.empty())
                        .withRecordSource(RecordSource.makeDaemonRecordSource())
                        .build())
                .collect(Collectors.toList());

        new WriteActionLogTable(ignored -> databaseWrapper, TEST_TABLE).insert(records);

        TimeZone.setDefault(TimeZone.getTimeZone(selectTimezone));

        List<String> readDateTimesRaw = databaseWrapper.query(
                String.format("SELECT DISTINCT toString(%1$s, 'UTC') FROM %2$s ORDER BY %1$s ASC",
                        ActionLogSchema.DATETIME.getExpr(), TEST_TABLE),
                new SingleColumnRowMapper<String>());
        softly.assertThat(readDateTimesRaw)
                .describedAs("Insert timezone %s, Select timezone %s, Reading dates by raw SQL as strings",
                        insertTimezone, selectTimezone)
                .isEqualTo(writtenDateTimes.stream()
                        .map(s -> s.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .collect(Collectors.toList()));

        ReadActionLogTable readActionLogTable = new ReadActionLogTable(ignoder -> databaseWrapper, TEST_TABLE);
        List<LocalDateTime> readDateTimesRepo =
                readActionLogTable.select(readActionLogTable.sqlBuilderWithSort(null, ReadActionLogTable.Order.DESC))
                        .stream()
                        .map(ActionLogRecord::getDateTime)
                        .sorted(Comparator.naturalOrder())
                        .collect(Collectors.toList());

        softly.assertThat(readDateTimesRepo)
                .describedAs("Insert timezone %s, Select timezone %s, Reading dates by ReadActionLogTable",
                        insertTimezone, selectTimezone)
                .isEqualTo(writtenDateTimes);
    }
}
