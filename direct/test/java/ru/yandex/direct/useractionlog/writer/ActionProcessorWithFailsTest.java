package ru.yandex.direct.useractionlog.writer;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.JUnitSoftAssertions;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.db.config.DbConfig;
import ru.yandex.direct.mysql.MySQLBinlogState;
import ru.yandex.direct.mysql.MySQLServerBuilder;
import ru.yandex.direct.mysql.MySQLUtils;
import ru.yandex.direct.mysql.TmpMySQLServerWithDataDir;
import ru.yandex.direct.test.mysql.DirectMysqlDb;
import ru.yandex.direct.test.mysql.TestMysqlConfig;
import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.useractionlog.db.DbConfigUtil;
import ru.yandex.direct.useractionlog.dict.DictDataCategory;
import ru.yandex.direct.useractionlog.dict.MemoryDictRepository;
import ru.yandex.direct.useractionlog.schema.Operation;
import ru.yandex.direct.useractionlog.schema.RecordSource;
import ru.yandex.direct.useractionlog.writer.generator.StateProcessingStrategy;
import ru.yandex.direct.utils.Checked;

@ParametersAreNonnullByDefault
public class ActionProcessorWithFailsTest {
    private static final String[] QUERIES = new String[]{
            "insert into root (gid) values (100)",
            "insert into related1 (gid, value) values (100, 'r1first')",
            "insert into related2 (gid, value) values (100, 'r2first')",
            "update related1 set value = 'r1second'",
            "insert into related3 (gid, value) values (100, 'r3first')",
            "update related2 set value = 'r2second'",
            "delete from related3",
            "delete from related2",
            "delete from related1",
            "delete from root",
    };
    private static final List<Pair<Operation, String>> EXPECTED_LOGS = ImmutableList.of(
            Pair.of(Operation.INSERT, "related1=r1first"),
            Pair.of(Operation.UPDATE, "related1=r1first related2=r2first"),
            Pair.of(Operation.UPDATE, "related1=r1second related2=r2first"),
            Pair.of(Operation.UPDATE, "related1=r1second related2=r2first related3=r3first"),
            Pair.of(Operation.UPDATE, "related1=r1second related2=r2second related3=r3first"),
            Pair.of(Operation.UPDATE, "related1=r1second related2=r2second"),
            Pair.of(Operation.UPDATE, "related1=r1second"),
            Pair.of(Operation.DELETE, null));

    private static RecordSource recordSource;
    private static MySQLServerBuilder mySQLServerBuilder;
    private static TmpMySQLServerWithDataDir mysql;
    private static MySQLBinlogState startState;
    private static MySQLBinlogState endState;

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @BeforeClass
    public static void setUpClass() throws InterruptedException, SQLException {
        mySQLServerBuilder = new DirectMysqlDb(TestMysqlConfig.directConfig())
                .useSandboxMysqlServerIfPossible(new MySQLServerBuilder()
                        .setGracefulStopTimeout(Duration.ZERO)
                        .setServerId(12345)
                        .withNoSync(true));

        recordSource = RecordSource.makeDaemonRecordSource();
        mysql = TmpMySQLServerWithDataDir.createWithBinlog("ignored", mySQLServerBuilder.copy());

        TestUtils.assumeThat("You should review the whole test if you add or remove queries",
                QUERIES.length,
                Matchers.equalTo(10));

        try (Connection connection = mysql.connect()) {
            MySQLUtils.executeUpdate(connection, "create database if not exists ppc");
            MySQLUtils.executeUpdate(connection, "use ppc");
            MySQLUtils.executeUpdate(connection,
                    "create table if not exists root (gid int primary key)");
            MySQLUtils.executeUpdate(connection,
                    "create table if not exists related1 (gid int primary key, value text)");
            MySQLUtils.executeUpdate(connection,
                    "create table if not exists related2 (gid int primary key, value text)");
            MySQLUtils.executeUpdate(connection,
                    "create table if not exists related3 (gid int primary key, value text)");
            MySQLUtils.executeUpdate(connection, "truncate table root");
            MySQLUtils.executeUpdate(connection, "truncate table related1");
            MySQLUtils.executeUpdate(connection, "truncate table related2");
            MySQLUtils.executeUpdate(connection, "truncate table related3");

            startState = MySQLBinlogState.snapshot(connection);

            for (String query : QUERIES) {
                MySQLUtils.executeUpdate(connection, "begin");
                MySQLUtils.executeUpdate(connection, query);
                MySQLUtils.executeUpdate(connection, "commit");
            }

            endState = MySQLBinlogState.snapshot(connection);
        }
    }

    @AfterClass
    public static void tearDownClass() {
        if (mysql != null) {
            mysql.close();
        }
    }

    @Test
    public void withoutFails() throws InterruptedException {
        runTest("No fails while saving states, batchSize = 1", new MemoryStateReaderWriter(), 1);
        runTest("No fails while saving states, batchSize = 5", new MemoryStateReaderWriter(), 5);
    }

    // Успешно записывается лог, стейт логов, словарь. При перезапуске в словаре
    // будут значения из будущего.
    @Test
    public void breaksBeforeDictState() throws InterruptedException {
        runTest("Fails on 1st dict state, batchSize = " + QUERIES.length,
                new MemoryStateReaderWriter().breakBeforeDictSavingAttempts(1),
                QUERIES.length);
        runTest("Fails on 3rd and 7th dict state, batchSize = 1",
                new MemoryStateReaderWriter().breakBeforeDictSavingAttempts(3, 7),
                1);
        runTest("Fails on 1st and 3rd dict state, batchSize = 3",
                new MemoryStateReaderWriter().breakBeforeDictSavingAttempts(1, 3),
                3);
    }

    // Падает перед записью логов. На момент падения в БД валидные стейты, логи и словарь.
    // Возможны дубликаты логов.
    @Test
    public void breaksBeforeLogsState() throws InterruptedException {
        runTest("Fails on 2nd dict state and 3rd log state, batchSize = 2",
                new MemoryStateReaderWriter()
                        .breakBeforeDictSavingAttempts(2)
                        .breakBeforeLogSavingAttempts(3),
                2);
    }

    // Падает сразу после записи логов. После перезапуска словарь догоняет лог.
    @Test
    public void breaksAfterLogState() throws InterruptedException {
        runTest("Fails between 1st log and dict state, batchSize = 3",
                new MemoryStateReaderWriter().breakAfterLogSavingAttempts(1),
                3);
    }

    private void runTest(String description, MemoryStateReaderWriter memoryStateReaderWriter, int batchSize)
            throws InterruptedException {
        MemoryActionLogWriteRepository memoryActionLogWriteRepository = new MemoryActionLogWriteRepository();
        MemoryDictRepository memoryDictRepository = new MemoryDictRepository();

        DbConfig dbConfig = DbConfigUtil.dbConfigForMysql(mysql);
        dbConfig.setDb("ppc");
        dbConfig.setDbName("ppc:1");
        dbConfig.setConnectTimeout(4.0);
        memoryStateReaderWriter.saveBothStates("ppc:1", startState);

        ActionProcessor actionProcessor = new ActionProcessor.Builder()
                .withBatchDuration(Duration.ofSeconds(1))
                .withBinlogKeepAliveTimeout(Duration.ofMinutes(10))
                .withBinlogStateFetchingSemaphore(null)
                .withDirectConfig(DirectConfigFactory.getCachedConfig())
                .withDbConfig(dbConfig)
                .withDictRepository(memoryDictRepository)
                .withEventBatchSize(batchSize)
                .withInitialServerId(null)
                .withMaxBufferedEvents(1)
                .withReadWriteStateTable(memoryStateReaderWriter.asStateReaderWriter())
                .withRecordBatchSize(batchSize)
                .withRowProcessingStrategy(new StateProcessingStrategy.Builder()
                        .setDictDataCategory(DictDataCategory.CAMPAIGN_NAME)
                        .setIdField("gid")
                        .setRecordSource(recordSource)
                        .setRootTableName("root")
                        .setValueField("value")
                        .build())
                .withSchemaReplicaMysqlBuilder(mySQLServerBuilder.copy())
                .withSchemaReplicaMysqlSemaphore(null)
                .withSkipErroneousEvents(false)
                .withUntilGtidSet(endState.getGtidSet())
                .withWriteActionLogTable(memoryActionLogWriteRepository)
                .build();

        int errorsCached = 0;
        while (true) {
            try {
                actionProcessor.run();
                break;
            } catch (Checked.CheckedException exc) {
                if (!(exc.getCause() instanceof MemoryStateReaderWriter.ScaryTerribleException)) {
                    throw exc;
                }
                ++errorsCached;
                memoryStateReaderWriter.fix();
            }
        }
        TestUtils.assumeThat(description + ": Stub threw all wanted exceptions",
                errorsCached,
                Matchers.equalTo(memoryStateReaderWriter.errorsShouldBeThrown()));

        List<Pair<Operation, String>> records = memoryActionLogWriteRepository.getDistinctWrittenRecords().stream()
                .map(r -> Pair.of(r.getOperation(), r.getNewFields().toMap().get("value")))
                .collect(Collectors.toList());
        softly.assertThat(records)
                .describedAs("Fails on 2nd dict state and 3rd log state, batchSize = 2")
                .isEqualTo(EXPECTED_LOGS);
    }
}
