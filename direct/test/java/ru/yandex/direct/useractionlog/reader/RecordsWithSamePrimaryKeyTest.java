package ru.yandex.direct.useractionlog.reader;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.PeekingIterator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.JUnitSoftAssertions;
import org.jooq.SQLDialect;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.direct.binlogclickhouse.schema.FieldValueList;
import ru.yandex.direct.clickhouse.ClickHouseCluster;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.test.clickhouse.JunitRuleClickHouseCluster;
import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.tracing.data.DirectTraceInfo;
import ru.yandex.direct.useractionlog.ClientId;
import ru.yandex.direct.useractionlog.TableNames;
import ru.yandex.direct.useractionlog.db.ActionLogWriteRepository;
import ru.yandex.direct.useractionlog.db.ReadActionLogTable;
import ru.yandex.direct.useractionlog.db.WriteActionLogTable;
import ru.yandex.direct.useractionlog.schema.ActionLogRecord;
import ru.yandex.direct.useractionlog.schema.ActionLogSchema;
import ru.yandex.direct.useractionlog.schema.ObjectPath;
import ru.yandex.direct.useractionlog.schema.Operation;
import ru.yandex.direct.useractionlog.schema.RecordSource;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
@YaIgnore
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
public class RecordsWithSamePrimaryKeyTest {
    @ClassRule
    public static JunitRuleClickHouseCluster junitRuleClickHouseCluster = new JunitRuleClickHouseCluster();
    private static ReadActionLogTable readActionLogTable;
    private static DatabaseWrapper databaseWrapper;
    private static List<ActionLogRecord> allRecords;
    private static List<ActionLogRecord> filteredRecords;

    private static final RecordSource daemonSource = RecordSource.makeDaemonRecordSource();
    private static final RecordSource manualSource = new RecordSource(
            RecordSource.RECORD_SOURCE_MANUAL, daemonSource.getTimestamp());
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private static ActionLogRecord getRecord(int idx, RecordSource source) {
        return ActionLogRecord.builder()
                .withDateTime(LocalDateTime.of(2000, 1, 1, 0, 0, 0)
                        .plusDays(idx))
                .withPath(new ObjectPath.ClientPath(new ClientId(idx)))
                .withGtid("serveruuid:" + idx)
                .withQuerySerial(0)
                .withRowSerial(0)
                .withDirectTraceInfo(DirectTraceInfo.empty())
                .withDb("ppc")
                .withType("clients")
                .withOperation(Operation.INSERT)
                .withOldFields(FieldValueList.empty())
                .withNewFields(FieldValueList.empty())
                .withRecordSource(source)
                .build();
    }

    private static Pair<LocalDateTime, Integer> getRecordKey(ActionLogRecord record) {
        return Pair.of(record.getDateTime(), record.getRecordSource().getType());
    }

    @BeforeClass
    public static void setUpClass() throws IOException, InterruptedException {
        ClickHouseCluster cluster = junitRuleClickHouseCluster.singleServerCluster();
        databaseWrapper = new DatabaseWrapper(
                TestUtils.randomName("testdb", 10),
                new ClickHouseDataSource(cluster.getClickHouseJdbcUrls().values().iterator().next()),
                SQLDialect.DEFAULT,
                EnvironmentType.DB_TESTING);

        readActionLogTable = new ReadActionLogTable(
                ignored -> databaseWrapper,
                TableNames.READ_USER_ACTION_LOG_TABLE);
        ActionLogWriteRepository actionLogWriteRepository = new WriteActionLogTable(
                ignored -> databaseWrapper,
                // ничего не перепутано, в этом тесте одна таблица и для чтения, и для записи
                TableNames.READ_USER_ACTION_LOG_TABLE);
        databaseWrapper.getDslContext().connection(
                new ActionLogSchema("default", TableNames.READ_USER_ACTION_LOG_TABLE)::createTable);

        allRecords = new ArrayList<>();
        filteredRecords = new ArrayList<>();
        for (int idx = 0; idx < 5; idx++) {
            ActionLogRecord daemonRecord = getRecord(idx, daemonSource);
            allRecords.add(daemonRecord);
            if (idx % 2 == 0) {
                ActionLogRecord manualRecord = getRecord(idx, manualSource);
                allRecords.add(manualRecord);
                filteredRecords.add(manualRecord);
            } else {
                filteredRecords.add(daemonRecord);
            }
        }
        allRecords.sort(Comparator.comparing(RecordsWithSamePrimaryKeyTest::getRecordKey).reversed());
        filteredRecords.sort(Comparator.comparing(RecordsWithSamePrimaryKeyTest::getRecordKey).reversed());

        TestUtils.assumeThat(softAssertions -> {
            List<Pair<LocalDateTime, Integer>> uniqueKeys =
                    allRecords.stream()
                            .map(RecordsWithSamePrimaryKeyTest::getRecordKey)
                            .collect(Collectors.toSet())
                            .stream()
                            .sorted(Comparator.<Pair<LocalDateTime, Integer>>naturalOrder().reversed())
                            .collect(Collectors.toList());
            softAssertions.assertThat(allRecords)
                    .describedAs("Pairs <LocalDateTime, RecordSource.Type> should be unique to simplify comparison")
                    .extracting(RecordsWithSamePrimaryKeyTest::getRecordKey)
                    .isEqualTo(uniqueKeys);
        });
        actionLogWriteRepository.insert(allRecords);
    }

    @Parameters({"2", "3", "4", "5", "6", "7", "8"})
    @Test
    public void filteredFetch(int limit) {
        ReadRequestStats readRequestStats = new ReadRequestStats();
        PeekingIterator<ActionLogRecord> userActionLogIterator = new FilterByVersion(
                new OrderedChunkActionLogReader(
                        readActionLogTable,
                        sqlBuilder -> {
                            // empty consumer
                        },
                        limit,
                        null,
                        ReadActionLogTable.Order.DESC,
                        readRequestStats));
        List<ActionLogRecord> result = new ArrayList<>();
        userActionLogIterator.forEachRemaining(result::add);
        softly.assertThat(result)
                .describedAs("Check that only records with highest version were fetched")
                .extracting(RecordsWithSamePrimaryKeyTest::getRecordKey)
                .isEqualTo(filteredRecords.stream()
                        .map(RecordsWithSamePrimaryKeyTest::getRecordKey)
                        .collect(Collectors.toList()));
    }

    @Test(expected = RuntimeException.class)
    public void lowLimitFetch() {
        ReadRequestStats readRequestStats = new ReadRequestStats();
        PeekingIterator<ActionLogRecord> userActionLogIterator = new FilterByVersion(
                new OrderedChunkActionLogReader(
                        readActionLogTable,
                        sqlBuilder -> {
                            // empty consumer
                        },
                        1,
                        null,
                        ReadActionLogTable.Order.DESC,
                        readRequestStats));
        List<ActionLogRecord> result = new ArrayList<>();
        userActionLogIterator.forEachRemaining(result::add);
    }
}
