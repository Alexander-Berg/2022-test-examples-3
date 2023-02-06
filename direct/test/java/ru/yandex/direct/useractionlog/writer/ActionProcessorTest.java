package ru.yandex.direct.useractionlog.writer;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.direct.binlogclickhouse.schema.FieldValue;
import ru.yandex.direct.binlogclickhouse.schema.FieldValueList;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.db.config.DbConfig;
import ru.yandex.direct.mysql.MySQLBinlogState;
import ru.yandex.direct.mysql.MySQLServerBuilder;
import ru.yandex.direct.mysql.MySQLUtils;
import ru.yandex.direct.mysql.TmpMySQLServerWithDataDir;
import ru.yandex.direct.test.mysql.DirectMysqlDb;
import ru.yandex.direct.test.mysql.TestMysqlConfig;
import ru.yandex.direct.useractionlog.Gtid;
import ru.yandex.direct.useractionlog.db.DbConfigUtil;
import ru.yandex.direct.useractionlog.dict.DictDataCategory;
import ru.yandex.direct.useractionlog.dict.DictRequest;
import ru.yandex.direct.useractionlog.dict.MemoryDictRepository;
import ru.yandex.direct.useractionlog.schema.ActionLogRecord;
import ru.yandex.direct.useractionlog.schema.Operation;
import ru.yandex.direct.useractionlog.schema.RecordSource;
import ru.yandex.direct.useractionlog.writer.generator.StateProcessingStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.MockitoAnnotations.initMocks;

@ParametersAreNonnullByDefault
public class ActionProcessorTest {
    private static final String TEST_UUID = "f92ce28f-f2de-4980-9786-64fc1ebe1cf8";  // chosen once random uuid

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private RecordSource recordSource;
    private MySQLServerBuilder mySQLServerBuilder;
    private DirectConfig directConfig;

    private static long extractEventId(String gtidSet) {
        List<Gtid> gtids = Gtid.fromGtidSet(gtidSet);
        Preconditions.checkState(gtids.size() == 1);
        return gtids.get(0).getEventId();
    }

    @Before
    public void setUp() {
        recordSource = RecordSource.makeDaemonRecordSource();
        mySQLServerBuilder = new DirectMysqlDb(TestMysqlConfig.directConfig())
                .useSandboxMysqlServerIfPossible(new MySQLServerBuilder()
                        .setGracefulStopTimeout(Duration.ZERO)
                        .setServerId(12345)
                        .withNoSync(true));
        directConfig = DirectConfigFactory.getCachedConfig();

        initMocks(this);
    }

    @Test
    public void finalStateNotReached() {
        softly.assertThat(ActionProcessor.finalStateNotReached(TEST_UUID + ":1-100", null))
                .describedAs("100-null")
                .isTrue();
        softly.assertThat(ActionProcessor.finalStateNotReached(TEST_UUID + ":1-100", TEST_UUID + ":1-200"))
                .describedAs("100-200")
                .isTrue();
        softly.assertThat(ActionProcessor.finalStateNotReached(TEST_UUID + ":1-200", TEST_UUID + ":1-200"))
                .describedAs("200-200")
                .isFalse();
        softly.assertThat(ActionProcessor.finalStateNotReached(TEST_UUID + ":1-201", TEST_UUID + ":1-200"))
                .describedAs("201-200")
                .isFalse();
    }

    @Test
    public void finalStateOvertaken() {
        softly.assertThat(ActionProcessor.finalStateOvertaken(TEST_UUID + ":1-100", null))
                .describedAs("100-null")
                .isFalse();
        softly.assertThat(ActionProcessor.finalStateOvertaken(TEST_UUID + ":1-100", TEST_UUID + ":1-200"))
                .describedAs("100-200")
                .isFalse();
        softly.assertThat(ActionProcessor.finalStateOvertaken(TEST_UUID + ":1-200", TEST_UUID + ":1-200"))
                .isFalse();
        softly.assertThat(ActionProcessor.finalStateOvertaken(TEST_UUID + ":1-201", TEST_UUID + ":1-200"))
                .isTrue();
    }

    /**
     * Проверяет, что синхронизатор может переработать сообщения в пределе (start_state, end_state].
     */
    @Test
    public void justLogs() throws InterruptedException, SQLException {
        MemoryDictRepository memoryDictRepository = new MemoryDictRepository();
        MemoryStateReaderWriter memoryStateReaderWriter = new MemoryStateReaderWriter();
        MemoryActionLogWriteRepository memoryActionLogWriteRepository = new MemoryActionLogWriteRepository();

        try (TmpMySQLServerWithDataDir mysql = TmpMySQLServerWithDataDir
                .createWithBinlog("ignored", mySQLServerBuilder.copy())) {
            MySQLBinlogState startState;
            MySQLBinlogState endState;
            try (Connection connection = mysql.connect()) {
                MySQLUtils.executeUpdate(connection, "create database ppc");
                MySQLUtils.executeUpdate(connection, "use ppc");
                MySQLUtils.executeUpdate(connection, "create table foobar (a int)");

                startState = MySQLBinlogState.snapshot(connection);

                MySQLUtils.executeUpdate(connection, "begin");
                MySQLUtils.executeUpdate(connection, "insert into foobar (a) values (123)");
                MySQLUtils.executeUpdate(connection, "insert into foobar (a) values (456)");
                MySQLUtils.executeUpdate(connection, "commit");

                MySQLUtils.executeUpdate(connection, "begin");
                MySQLUtils.executeUpdate(connection, "update foobar set a = 777 where a = 123");
                MySQLUtils.executeUpdate(connection, "commit");

                MySQLUtils.executeUpdate(connection, "begin");
                MySQLUtils.executeUpdate(connection, "delete from foobar");
                MySQLUtils.executeUpdate(connection, "commit");

                endState = MySQLBinlogState.snapshot(connection);
            }

            DbConfig dbConfig = DbConfigUtil.dbConfigForMysql(mysql);
            dbConfig.setDb("ppc");
            dbConfig.setDbName("ppc:1");
            dbConfig.setConnectTimeout(4.0);
            memoryStateReaderWriter.saveBothStates("ppc:1", startState);

            ActionProcessor actionProcessor = new ActionProcessor.Builder()
                    .withBatchDuration(Duration.ofSeconds(1))
                    .withBinlogKeepAliveTimeout(Duration.ofSeconds(60))
                    .withBinlogStateFetchingSemaphore(null)
                    .withDirectConfig(directConfig)
                    .withDbConfig(dbConfig)
                    .withDictRepository(memoryDictRepository)
                    .withEventBatchSize(1)
                    .withInitialServerId(null)
                    .withMaxBufferedEvents(10)
                    .withReadWriteStateTable(memoryStateReaderWriter.asStateReaderWriter())
                    .withRecordBatchSize(1)
                    .withRowProcessingStrategy(new AsIsRowProcessingStrategy(recordSource))
                    .withSchemaReplicaMysqlBuilder(mySQLServerBuilder.copy())
                    .withSchemaReplicaMysqlSemaphore(null)
                    .withSkipErroneousEvents(false)
                    .withUntilGtidSet(endState.getGtidSet())
                    .withWriteActionLogTable(memoryActionLogWriteRepository)
                    .build();

            actionProcessor.run();

            List<ActionLogRecord> records = memoryActionLogWriteRepository.getAllWrittenRecords();
            softly.assertThat(records.stream().map(r -> extractEventId(r.getGtid())))
                    .describedAs("Checking gtids")
                    .containsExactly(
                            // two inserts
                            extractEventId(startState.getGtidSet()) + 1,
                            extractEventId(startState.getGtidSet()) + 1,

                            // one update
                            extractEventId(startState.getGtidSet()) + 2,

                            // two deletes
                            extractEventId(startState.getGtidSet()) + 3,
                            extractEventId(startState.getGtidSet()) + 3);
            softly.assertThat(records.stream().map(ActionLogRecord::getOperation))
                    .describedAs("Checking operations")
                    .containsExactly(
                            Operation.INSERT, Operation.INSERT,
                            Operation.UPDATE,
                            Operation.DELETE, Operation.DELETE);
        }
    }

    @Test
    public void shouldMakeChunkedHandlingForOneTransaction() throws InterruptedException,
            SQLException {
        var memoryDictRepository = new MemoryDictRepository();
        var memoryStateReaderWriter = new MemoryStateReaderWriter();
        var memoryActionLogWriteRepository = Mockito.spy(new MemoryActionLogWriteRepository());
        try (TmpMySQLServerWithDataDir mysql = TmpMySQLServerWithDataDir
                .createWithBinlog("ignored", mySQLServerBuilder.copy())) {
            MySQLBinlogState startState;
            MySQLBinlogState endState;
            try (Connection connection = mysql.connect()) {
                MySQLUtils.executeUpdate(connection, "create database ppc");
                MySQLUtils.executeUpdate(connection, "use ppc");
                MySQLUtils.executeUpdate(connection, "create table foobar (a int)");
                startState = MySQLBinlogState.snapshot(connection);
                MySQLUtils.executeUpdate(connection, "begin");
                MySQLUtils.executeUpdate(connection, "insert into foobar (a) values (123), (124), (125)");
                MySQLUtils.executeUpdate(connection, "insert into foobar (a) values (126), (127), (128)");
                MySQLUtils.executeUpdate(connection, "commit");
                endState = MySQLBinlogState.snapshot(connection);
            }
            var dbConfig = DbConfigUtil.dbConfigForMysql(mysql);
            dbConfig.setDb("ppc");
            dbConfig.setDbName("ppc:1");
            dbConfig.setConnectTimeout(4.0);
            memoryStateReaderWriter.saveBothStates("ppc:1", startState);
            AtomicInteger invocations = new AtomicInteger();
            // Проверяем обработку транзакции, каждый INSERT должен обрабатываться отдельно (у нас пакеты по 3)
            doAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                var records = (List<ActionLogRecord>) invocation.getArgument(0);
                switch (invocations.get()) {
                    case 0:
                        assertThat(records).hasSize(3);
                        assertThat(
                                records
                                        .stream()
                                        .map(ActionLogRecord::getNewFields)
                                        .flatMap((FieldValueList fieldValueList) -> fieldValueList.getValues().stream())
                                        .collect(Collectors.toList())
                        ).containsExactly("123", "124", "125");
                        break;
                    case 1:
                        assertThat(records).hasSize(3);
                        assertThat(
                                records
                                        .stream()
                                        .map(ActionLogRecord::getNewFields)
                                        .flatMap((FieldValueList fieldValueList) -> fieldValueList.getValues().stream())
                                        .collect(Collectors.toList())
                        )
                                .containsExactly("126", "127", "128");
                        break;
                    default:
                        return null;
                }
                invocations.getAndIncrement();
                return null;
            }).when(memoryActionLogWriteRepository).insert(any());
            var actionProcessor = new ActionProcessor.Builder()
                    .withBatchDuration(Duration.ofSeconds(1))
                    .withBinlogKeepAliveTimeout(Duration.ofSeconds(60))
                    .withBinlogStateFetchingSemaphore(null)
                    .withDirectConfig(directConfig)
                    .withDbConfig(dbConfig)
                    .withDictRepository(memoryDictRepository)
                    .withEventBatchSize(3)
                    .withInitialServerId(null)
                    .withMaxBufferedEvents(3)
                    .withReadWriteStateTable(memoryStateReaderWriter.asStateReaderWriter())
                    .withRecordBatchSize(3)
                    .withRowProcessingStrategy(new AsIsRowProcessingStrategy(recordSource))
                    .withSchemaReplicaMysqlBuilder(mySQLServerBuilder.copy())
                    .withSchemaReplicaMysqlSemaphore(null)
                    .withSkipErroneousEvents(false)
                    .withUntilGtidSet(endState.getGtidSet())
                    .withWriteActionLogTable(memoryActionLogWriteRepository)
                    .build();
            actionProcessor.run();
        }
    }

    @Test
    public void dictLag() throws InterruptedException, SQLException {
        MemoryDictRepository memoryDictRepository = new MemoryDictRepository();
        MemoryStateReaderWriter memoryStateReaderWriter = new MemoryStateReaderWriter();
        MemoryActionLogWriteRepository memoryActionLogWriteRepository = new MemoryActionLogWriteRepository();

        try (TmpMySQLServerWithDataDir mysql = TmpMySQLServerWithDataDir
                .createWithBinlog("ignored", mySQLServerBuilder.copy())) {
            MySQLBinlogState logStartState;
            MySQLBinlogState dictStartState;
            MySQLBinlogState endState;
            try (Connection connection = mysql.connect()) {
                MySQLUtils.executeUpdate(connection, "create database ppc");
                MySQLUtils.executeUpdate(connection, "use ppc");
                MySQLUtils.executeUpdate(connection, "create table root (id int)");
                MySQLUtils.executeUpdate(connection, "create table r1 (id int, value text)");
                MySQLUtils.executeUpdate(connection, "create table r2 (id int, value text)");
                MySQLUtils.executeUpdate(connection, "create table r3 (id int, value text)");
                MySQLUtils.executeUpdate(connection, "create table r4 (id int, value text)");
                MySQLUtils.executeUpdate(connection, "create table r5 (id int, value text)");
                MySQLUtils.executeUpdate(connection, "begin");
                MySQLUtils.executeUpdate(connection, "insert into root values (1)");
                MySQLUtils.executeUpdate(connection, "commit");

                dictStartState = MySQLBinlogState.snapshot(connection);

                int i = 1;

                while (i <= 3) {
                    MySQLUtils.executeUpdate(connection, "begin");
                    MySQLUtils.executeUpdate(connection, "insert into r" + i + " (id, value) values (1, '" + i + "')");
                    MySQLUtils.executeUpdate(connection, "commit");
                    ++i;
                }

                logStartState = MySQLBinlogState.snapshot(connection);

                while (i <= 5) {
                    MySQLUtils.executeUpdate(connection, "begin");
                    MySQLUtils.executeUpdate(connection, "insert into r" + i + " (id, value) values (1, '" + i + "')");
                    MySQLUtils.executeUpdate(connection, "commit");
                    ++i;
                }

                endState = MySQLBinlogState.snapshot(connection);
            }

            DbConfig dbConfig = DbConfigUtil.dbConfigForMysql(mysql);
            dbConfig.setDb("ppc");
            dbConfig.setDbName("ppc:1");
            dbConfig.setConnectTimeout(4.0);

            memoryStateReaderWriter.saveLogState("ppc:1", logStartState);
            memoryStateReaderWriter.saveDictState("ppc:1", dictStartState);
            memoryDictRepository.addData(DictDataCategory.CAMPAIGN_NAME, 1, "");

            ActionProcessor actionProcessor = new ActionProcessor.Builder()
                    .withBatchDuration(Duration.ofSeconds(1))
                    .withBinlogKeepAliveTimeout(Duration.ofMinutes(10))
                    .withBinlogStateFetchingSemaphore(null)
                    .withDirectConfig(directConfig)
                    .withDbConfig(dbConfig)
                    .withDictRepository(memoryDictRepository)
                    .withEventBatchSize(1000)
                    .withInitialServerId(null)
                    .withMaxBufferedEvents(1000)
                    .withReadWriteStateTable(memoryStateReaderWriter.asStateReaderWriter())
                    .withRecordBatchSize(1)
                    .withRowProcessingStrategy(new StateProcessingStrategy.Builder()
                            .setDictDataCategory(DictDataCategory.CAMPAIGN_NAME)
                            .setIdField("id")
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
            actionProcessor.run();

            Object dictContents =
                    memoryDictRepository.repositoryMap.get(new DictRequest(DictDataCategory.CAMPAIGN_NAME, 1));
            softly.assertThat(dictContents).isEqualTo("r1=1 r2=2 r3=3 r4=4 r5=5");
            List<String> records = memoryActionLogWriteRepository.getAllWrittenRecords().stream()
                    .map(ActionLogRecord::getNewFields)
                    .map(FieldValueList::getFieldsValues)
                    .flatMap(List::stream)
                    .filter(f -> f.getName().equals("value"))
                    .map(FieldValue::getValueAsNonNullString)
                    .collect(Collectors.toList());
            softly.assertThat(records).containsExactly(
                    "r1=1 r2=2 r3=3 r4=4",
                    "r1=1 r2=2 r3=3 r4=4 r5=5");
        }
    }
}
