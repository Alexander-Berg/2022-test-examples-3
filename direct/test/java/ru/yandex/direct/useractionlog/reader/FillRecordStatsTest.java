package ru.yandex.direct.useractionlog.reader;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

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
import ru.yandex.direct.useractionlog.ChangeSource;
import ru.yandex.direct.useractionlog.ClientId;
import ru.yandex.direct.useractionlog.Methods;
import ru.yandex.direct.useractionlog.Services;
import ru.yandex.direct.useractionlog.TableNames;
import ru.yandex.direct.useractionlog.db.ReadPpclogApiTable;
import ru.yandex.direct.useractionlog.schema.ActionLogRecord;
import ru.yandex.direct.useractionlog.schema.ActionLogRecordWithStats;
import ru.yandex.direct.useractionlog.schema.ObjectPath;
import ru.yandex.direct.useractionlog.schema.Operation;
import ru.yandex.direct.useractionlog.schema.PpclogApiSchema;
import ru.yandex.direct.useractionlog.schema.RecordSource;
import ru.yandex.direct.useractionlog.schema.RecordStats;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
@YaIgnore
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
public class FillRecordStatsTest {
    @ClassRule
    public static JunitRuleClickHouseCluster junitRuleClickHouseCluster = new JunitRuleClickHouseCluster();
    private static ReadPpclogApiTable readPpclogApiTable;
    private static DatabaseWrapper databaseWrapper;
    private static List<ActionLogRecord> allRecords = new ArrayList<>();
    private static List<Pair<String, RecordStats>> expected = new ArrayList<>();

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
        readPpclogApiTable = new ReadPpclogApiTable(ignored -> databaseWrapper, TableNames.READ_PPCLOG_API_TABLE);
        databaseWrapper.getDslContext().connection(
                new PpclogApiSchema("default", TableNames.READ_PPCLOG_API_TABLE)::createTable);

        List<String> services = Arrays.asList(Services.PERL_WEB, Services.PERL_WEB, Services.WEB, Services.INTAPI, Services.API5,
                Services.JSON_API, Services.SOAP_API, Services.JOBS, null);
        List<String> methods = Arrays.asList(Methods.CONFIRM_SAVE_CAMP_XLS, "someWebMethod",
                Methods.GRID_EXECUTE_RECOMMENDATION, Methods.SHOWCONDITIONS_UPDATE, "someApiMethod", "someApiMethod",
                "someApiMethod", "someJobsMethod", "someMethod");
        List<RecordStats> stats = Arrays.asList(new RecordStats(ChangeSource.XLS, false),
                new RecordStats(ChangeSource.WEB, false),
                new RecordStats(ChangeSource.WEB, false),
                new RecordStats(ChangeSource.WEB, true),
                new RecordStats(ChangeSource.API_APP, false),
                new RecordStats(ChangeSource.API_APP, false),
                new RecordStats(ChangeSource.API_APP, false),
                new RecordStats(ChangeSource.OTHER, false),
                new RecordStats(ChangeSource.OTHER, false));

        RecordSource recordSource = RecordSource.makeDaemonRecordSource();
        for (int index = 0; index < services.size(); index++) {
            allRecords.add(ActionLogRecord.builder()
                    .withDateTime(LocalDateTime.of(2000, 1, 1, 0, 0, 0))
                    .withPath(new ObjectPath.ClientPath(new ClientId(100500)))
                    .withGtid("serveruuid:" + index)
                    .withQuerySerial(0)
                    .withRowSerial(0)
                    .withDirectTraceInfo(new DirectTraceInfo(
                            OptionalLong.of(index),
                            services.get(index) == null ? Optional.empty() : Optional.of(services.get(index)),
                            methods.get(index) == null ? Optional.empty() : Optional.of(methods.get(index)),
                            OptionalLong.of(index)))
                    .withDb("ppc")
                    .withType("clients")
                    .withOperation(Operation.INSERT)
                    .withOldFields(FieldValueList.empty())
                    .withNewFields(FieldValueList.empty())
                    .withRecordSource(recordSource)
                    .build());
            expected.add(Pair.of(allRecords.get(index).getGtid(), stats.get(index)));
        }
        TestUtils.assumeThat(softAssertions -> {
            List<String> uniqueGtids =
                    allRecords.stream()
                            .map(ActionLogRecord::getGtid)
                            .collect(Collectors.toSet())
                            .stream()
                            .sorted(Comparator.naturalOrder())
                            .collect(Collectors.toList());
            softAssertions.assertThat(allRecords)
                    .describedAs("Gtids should be unique to simplify comparison")
                    .extracting(ActionLogRecord::getGtid)
                    .isEqualTo(uniqueGtids);
        });
    }

    @Parameters({"1", "5", "100"})
    @Test
    public void testFetch(int chunkSize) {
        ReadRequestStats readRequestStats = new ReadRequestStats();
        Iterator<ActionLogRecordWithStats> iterator = new FillRecordStats(
                chunkSize,
                allRecords.iterator(),
                readPpclogApiTable,
                readRequestStats);
        List<ActionLogRecordWithStats> result = new ArrayList<>();
        iterator.forEachRemaining(result::add);
        softly.assertThat(result)
                .describedAs("Check that all records were fetched")
                .extracting(r -> Pair.of(r.getRecord().getGtid(), r.getStats()))
                .isEqualTo(expected);
    }
}
