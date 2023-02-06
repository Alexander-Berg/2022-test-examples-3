package ru.yandex.direct.useractionlog.reader;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
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
public class OrderedChunkActionLogReaderTest {
    private static final int GTID_COUNT = 17;
    private static final int QUERY_SERIAL_COUNT = 3;
    private static final int ROW_SERIAL_COUNT = 2;
    @ClassRule
    public static JunitRuleClickHouseCluster junitRuleClickHouseCluster = new JunitRuleClickHouseCluster();
    private static ReadActionLogTable readActionLogTable;
    private static DatabaseWrapper databaseWrapper;
    private static List<ActionLogRecord> allRecords;
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

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

        RecordSource recordSource = RecordSource.makeDaemonRecordSource();
        allRecords = ImmutableList.copyOf(IntStream.range(0, GTID_COUNT).boxed().flatMap(idx ->
                IntStream.range(0, QUERY_SERIAL_COUNT).boxed().flatMap(querySerial ->
                        IntStream.range(0, ROW_SERIAL_COUNT).mapToObj(rowSerial ->
                                ActionLogRecord.builder()
                                        .withDateTime(LocalDateTime.of(2000, 1, 1, 0, 0, 0)
                                                .plusDays(idx)
                                                .plusHours(querySerial)
                                                .plusMinutes(rowSerial))
                                        .withPath(new ObjectPath.ClientPath(new ClientId(
                                                idx * 10_000 + querySerial * 100 + rowSerial)))
                                        .withGtid("serveruuid:" + idx)
                                        .withQuerySerial(querySerial)
                                        .withRowSerial(rowSerial)
                                        .withDirectTraceInfo(DirectTraceInfo.empty())
                                        .withDb("ppc")
                                        .withType("clients")
                                        .withOperation(Operation.INSERT)
                                        .withOldFields(FieldValueList.empty())
                                        .withNewFields(FieldValueList.empty())
                                        .withRecordSource(recordSource)
                                        .build())))
                .sorted(Comparator.comparing(ActionLogRecord::getDateTime).reversed())
                .collect(Collectors.toList()));
        TestUtils.assumeThat(softAssertions -> {
            List<LocalDateTime> uniqueDates =
                    allRecords.stream()
                            .map(ActionLogRecord::getDateTime)
                            .collect(Collectors.toSet())
                            .stream()
                            .sorted(Comparator.<LocalDateTime>naturalOrder().reversed())
                            .collect(Collectors.toList());
            softAssertions.assertThat(allRecords)
                    .describedAs("All dates are should unique to simplify comparison")
                    .extracting(ActionLogRecord::getDateTime)
                    .isEqualTo(uniqueDates);
        });
        actionLogWriteRepository.insert(allRecords);
    }

    @Parameters({
            "0,ASC",
            "0,DESC",
            "1,ASC",
            "1,DESC",
            "100,ASC",
            "100,DESC",
    })
    @Test
    public void oneQueryFetch(int addToLimit, ReadActionLogTable.Order order) {
        ReadRequestStats readRequestStats = new ReadRequestStats();
        OrderedChunkActionLogReader target = new OrderedChunkActionLogReader(
                readActionLogTable,
                sqlBuilder -> {
                    // empty consumer
                },
                allRecords.size() + addToLimit,
                null,
                order,
                readRequestStats);
        List<ActionLogRecord> result = new ArrayList<>();
        target.forEachRemaining(result::add);
        softly.assertThat(result)
                .describedAs("Check all records was fetched")
                .extracting(ActionLogRecord::getDateTime)
                .isEqualTo(allRecords.stream()
                        .map(ActionLogRecord::getDateTime)
                        .sorted(order == ReadActionLogTable.Order.ASC
                                ? Comparator.naturalOrder()
                                : Comparator.reverseOrder())
                        .collect(Collectors.toList()));
        softly.assertThat(readRequestStats.recordQueriesDone)
                .describedAs("Check was fetched by one query")
                .isEqualTo(1);
        softly.assertThat(readRequestStats.recordsFetched)
                .describedAs("Stats about all fetched record count should not lie")
                .isEqualTo(allRecords.size());
    }

    @Parameters({
            "7,ASC",
            "7,DESC",
            "19,ASC",
            "19,DESC",
            "23,ASC",
            "23,DESC",
    })
    @Test
    public void severalQueriesFetch(int limit, ReadActionLogTable.Order order) {
        TestUtils.assumeThat(softAssertions -> {
            softAssertions.assertThat(limit % GTID_COUNT)
                    .describedAs("limit should not be divisible by gtid count")
                    .isNotEqualTo(0);
            softAssertions.assertThat(limit % QUERY_SERIAL_COUNT)
                    .describedAs("limit should not be divisible by query serial count")
                    .isNotEqualTo(0);
            softAssertions.assertThat(limit % ROW_SERIAL_COUNT)
                    .describedAs("limit should not be divisible by row serial count")
                    .isNotEqualTo(0);
        });
        ReadRequestStats readRequestStats = new ReadRequestStats();
        OrderedChunkActionLogReader target = new OrderedChunkActionLogReader(
                readActionLogTable,
                sqlBuilder -> {
                    // empty consumer
                },
                limit,
                null,
                order,
                readRequestStats);
        List<ActionLogRecord> result = new ArrayList<>();
        target.forEachRemaining(result::add);
        softly.assertThat(result)
                .describedAs("Check all records was fetched")
                .extracting(ActionLogRecord::getDateTime)
                .isEqualTo(allRecords.stream()
                        .map(ActionLogRecord::getDateTime)
                        .sorted(order == ReadActionLogTable.Order.ASC
                                ? Comparator.naturalOrder()
                                : Comparator.reverseOrder())
                        .collect(Collectors.toList()));
        softly.assertThat(readRequestStats.recordQueriesDone)
                .describedAs("Check was fetched by several queries")
                .isEqualTo((allRecords.size() - 1) / limit + 1);
        softly.assertThat(readRequestStats.recordsFetched)
                .describedAs("Stats about all fetched record count should not lie")
                .isEqualTo(allRecords.size());
    }

    @Test
    public void emptyResult() {
        ReadRequestStats readRequestStats = new ReadRequestStats();
        OrderedChunkActionLogReader target = new OrderedChunkActionLogReader(
                readActionLogTable,
                sqlBuilder -> {
                    sqlBuilder.where("2 + 2 = 5");
                },
                1,
                null,
                ReadActionLogTable.Order.DESC,
                readRequestStats);
        List<ActionLogRecord> result = new ArrayList<>();
        target.forEachRemaining(result::add);
        softly.assertThat(result)
                .describedAs("Check no record was fetched")
                .isEmpty();
        softly.assertThat(readRequestStats.recordQueriesDone)
                .describedAs("Check was fetched by one query")
                .isEqualTo(1);
        softly.assertThat(readRequestStats.recordsFetched)
                .describedAs("Stats about all fetched record count should not lie")
                .isEqualTo(0);
    }
}
