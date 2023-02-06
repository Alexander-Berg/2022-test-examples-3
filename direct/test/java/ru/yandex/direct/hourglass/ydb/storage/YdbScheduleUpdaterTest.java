package ru.yandex.direct.hourglass.ydb.storage;

import java.util.ArrayList;
import java.util.List;

import com.yandex.ydb.table.TableClient;
import com.yandex.ydb.table.transaction.TxControl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.hourglass.implementations.updateschedule.ScheduleRecord;
import ru.yandex.direct.hourglass.ydb.YdbInfoHolder;
import ru.yandex.direct.ydb.YdbPath;
import ru.yandex.direct.ydb.table.temptable.TempTableDescription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static ru.yandex.direct.hourglass.ydb.storage.Tables.SCHEDULED_TASKS;
import static ru.yandex.direct.ydb.builder.querybuilder.DeleteBuilder.deleteFrom;
import static ru.yandex.direct.ydb.builder.querybuilder.InsertBuilder.insertInto;
import static ru.yandex.direct.ydb.builder.querybuilder.SelectBuilder.select;
import static ru.yandex.direct.ydb.table.temptable.TempTable.tempTable;

class YdbScheduleUpdaterTest {
    private static final String NEW_VERSION = "new_version";

    private static YdbScheduleUpdater updater;
    private static TableClient tableClient;
    private static YdbPath db;

    @BeforeAll
    static void initDb() {
        var ydbInfo = YdbInfoHolder.getYdbInfo();
        tableClient = ydbInfo.getClient();
        db = ydbInfo.getDb();

        updater = new YdbScheduleUpdater(
                tableClient,
                db,
                NEW_VERSION,
                null);
    }

    /*
     * Мы будем проверять что произойдет с заданиями в разных состояниях при изменении расписания
     */
    @BeforeEach
    void truncate() {
        var className = this.getClass().getName();
        var session = tableClient.createSession().join().expect("Cannot create session for test " + className);

        var deleteQueryAndParams = deleteFrom(SCHEDULED_TASKS).queryAndParams(db);
        session.executeDataQuery(deleteQueryAndParams.getQuery(),
                TxControl.serializableRw().setCommitTx(true), deleteQueryAndParams.getParams()).join().expect("Cannot" +
                " truncate table before test " + className);

        session.close();
    }

    /**
     * Тест проверяет, что если к моменту вставки новых задач кто-то уже их вставил, это будет не ошибкой
     */
    @Test
    void concurrentAddNewTasksTest() {
        var schedulerRecord =
                new ScheduleRecord().setName("name").setParam("param").setScheduleHashSum("hash").setMeta("meta");
        YdbScheduleUpdater.ScheduleRecordWithId scheduleRecordWithId = new YdbScheduleUpdater.ScheduleRecordWithId(
                schedulerRecord,
                "1"
        );

        updater.addNewTasks(List.of(scheduleRecordWithId)).join();
        var insertedIds = getTasksIds();

        assertThat(insertedIds).hasSize(1);
        assertThat(insertedIds.get(0)).isEqualTo("1");

        assertThatCode(() -> updater.addNewTasks(List.of(scheduleRecordWithId)).join()).doesNotThrowAnyException();

        insertedIds = getTasksIds();

        assertThat(insertedIds).hasSize(1);
        assertThat(insertedIds.get(0)).isEqualTo("1");
    }

    @Test
    void allTaskHasStorageVersion_EmptyScheduleTest() {
        boolean allTasksHasStorageVersion = updater.allTasksHasStorageVersion();
        assertThat(allTasksHasStorageVersion).isFalse();
    }

    @Test
    void allTaskHasStorageVersion() {
        var tempInsertTable =
                tempTable(new TempTableDescription(), SCHEDULED_TASKS.ID, SCHEDULED_TASKS.VERSION).createValues()
                        .fill("1", NEW_VERSION)
                        .fill("2", NEW_VERSION)
                        .fill("3", NEW_VERSION);
        var queryAndParams = insertInto(SCHEDULED_TASKS).selectAll().from(tempInsertTable).queryAndParams(db);
        tableClient.createSession().thenApply(
                sessionResult -> sessionResult.expect("Cannot create session for insert values"))
                .thenCompose(session ->
                        session.executeDataQuery(queryAndParams.getQuery(), TxControl.serializableRw(),
                                queryAndParams.getParams())
                ).join();
        boolean allTasksHasStorageVersion = updater.allTasksHasStorageVersion();
        assertThat(allTasksHasStorageVersion).isTrue();
    }

    @Test
    void allTaskHasStorageVersion_NotAllTasks() {
        var tempInsertTable =
                tempTable(new TempTableDescription(), SCHEDULED_TASKS.ID, SCHEDULED_TASKS.VERSION).createValues()
                        .fill("1", "old")
                        .fill("2", NEW_VERSION)
                        .fill("3", NEW_VERSION);
        var queryAndParams = insertInto(SCHEDULED_TASKS).selectAll().from(tempInsertTable).queryAndParams(db);
        tableClient.createSession().thenApply(
                sessionResult -> sessionResult.expect("Cannot create session for insert values"))
                .thenCompose(session ->
                        session.executeDataQuery(queryAndParams.getQuery(), TxControl.serializableRw(),
                                queryAndParams.getParams())
                ).join();
        boolean allTasksHasStorageVersion = updater.allTasksHasStorageVersion();
        assertThat(allTasksHasStorageVersion).isFalse();
    }

    @Test
    void allTaskHasStorageVersion_AllTasksAnotherVersion() {
        var tempInsertTable =
                tempTable(new TempTableDescription(), SCHEDULED_TASKS.ID, SCHEDULED_TASKS.VERSION).createValues()
                        .fill("1", "old")
                        .fill("2", "old")
                        .fill("3", "old");
        var queryAndParams = insertInto(SCHEDULED_TASKS).selectAll().from(tempInsertTable).queryAndParams(db);
        tableClient.createSession().thenApply(
                sessionResult -> sessionResult.expect("Cannot create session for insert values"))
                .thenCompose(session ->
                        session.executeDataQuery(queryAndParams.getQuery(), TxControl.serializableRw(),
                                queryAndParams.getParams())
                ).join();
        boolean allTasksHasStorageVersion = updater.allTasksHasStorageVersion();
        assertThat(allTasksHasStorageVersion).isFalse();
    }

    private List<String> getTasksIds() {

        var selectQueryAndParams =
                select(SCHEDULED_TASKS.ID).from(SCHEDULED_TASKS).queryAndParams(db);
        return tableClient.createSession()
                .thenApply(
                        sessionResult -> sessionResult.expect("Cannot create session for select tasks"))
                .thenCompose(session ->
                        session.executeDataQuery(selectQueryAndParams.getQuery(),
                                TxControl.serializableRw().setCommitTx(true), selectQueryAndParams.getParams()))
                .thenApply(dataQueryResultResult -> {
                    var resultSetReader =
                            dataQueryResultResult.expect("Failed to select ids").getResultSet(0);
                    List<String> ids = new ArrayList<>();
                    while (resultSetReader.next()) {
                        ids.add(resultSetReader.getColumn("id").getUtf8());
                    }
                    return ids;
                }).join();

    }
}
