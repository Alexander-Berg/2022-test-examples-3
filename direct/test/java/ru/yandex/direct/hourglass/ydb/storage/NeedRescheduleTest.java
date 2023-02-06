package ru.yandex.direct.hourglass.ydb.storage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.yandex.ydb.table.TableClient;
import com.yandex.ydb.table.transaction.TxControl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.hourglass.HourglassProperties;
import ru.yandex.direct.hourglass.InstanceId;
import ru.yandex.direct.hourglass.storage.Job;
import ru.yandex.direct.hourglass.storage.JobStatus;
import ru.yandex.direct.hourglass.storage.PrimaryId;
import ru.yandex.direct.hourglass.ydb.YdbInfoHolder;
import ru.yandex.direct.ydb.YdbPath;
import ru.yandex.direct.ydb.client.YdbSessionProperties;
import ru.yandex.direct.ydb.table.temptable.TempTableDescription;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.hourglass.storage.JobStatus.READY;
import static ru.yandex.direct.hourglass.ydb.storage.Tables.SCHEDULED_TASKS;
import static ru.yandex.direct.ydb.builder.querybuilder.DeleteBuilder.deleteFrom;
import static ru.yandex.direct.ydb.builder.querybuilder.InsertBuilder.upsertInto;
import static ru.yandex.direct.ydb.builder.querybuilder.SelectBuilder.select;
import static ru.yandex.direct.ydb.table.temptable.TempTable.tempTable;

class NeedRescheduleTest {
    private static TableClient tableClient;
    private static YdbPath db;
    private static YdbStorageImpl storage;
    private static InstanceId schedulerId;


    @BeforeAll
    static void initDb() {
        var ydbInfo = YdbInfoHolder.getYdbInfo();
        tableClient = ydbInfo.getClient();
        db = ydbInfo.getDb();
        schedulerId = mock(InstanceId.class);

        when(schedulerId.toString()).thenReturn("deadbeef");
        var hourglassConfiguration = HourglassProperties.builder().setMaxHeartbeatAge(60, SECONDS).build();

        storage = new YdbStorageImpl(tableClient, db, YdbSessionProperties.builder().build(),
                hourglassConfiguration, schedulerId, "1",
                s -> s);
    }

    @BeforeEach
    void makeSchedule() {
        var className = this.getClass().getName();
        var session = tableClient.createSession().join().expect("Cannot create session for test " + className);

        var deleteQueryAndParams = deleteFrom(SCHEDULED_TASKS).queryAndParams(db);
        session.executeDataQuery(deleteQueryAndParams.getQuery(),
                TxControl.serializableRw().setCommitTx(true), deleteQueryAndParams.getParams()).join().expect("Cannot" +
                " truncate table before" +
                " test " + className);

        var heartbeatTime = Instant.now().minusSeconds(600);
        var nextRunTime = Instant.now().minusSeconds(10);
        var insertValues = tempTable(new TempTableDescription("values"), SCHEDULED_TASKS.ID, SCHEDULED_TASKS.NAME,
                SCHEDULED_TASKS.PARAMS, SCHEDULED_TASKS.STATUS,
                SCHEDULED_TASKS.NEED_RESCHEDULE, SCHEDULED_TASKS.HEARTBEAT_TIME,
                SCHEDULED_TASKS.NEXT_RUN, SCHEDULED_TASKS.SCHEDULE_HASH,
                SCHEDULED_TASKS.INSTANCE_ID, SCHEDULED_TASKS.VERSION)
                .createValues()
                .fill("1", "A", "A1", "New", true, heartbeatTime, nextRunTime, "", null, "1")
                .fill("2", "B", "B1", "Running", true, heartbeatTime, nextRunTime, "", "cafebabe", "1")
                .fill("3", "BG", "B1", "New", false, heartbeatTime, nextRunTime, "", null, "1")
                .fill("4", "C", "C1", "New", true, heartbeatTime, nextRunTime, "", "cafebabe", "2")
                .fill("5", "D", "D1", "New", true, heartbeatTime, nextRunTime, "", "deadbeef", "2")
                .fill("6", "ANOTHER_VERSION_TASK", "A1", "New", true, heartbeatTime, nextRunTime, "", null, "2");
        var insertQueryAndParams = upsertInto(SCHEDULED_TASKS)
                .selectAll()
                .from(insertValues)
                .queryAndParams(db);

        session.executeDataQuery(insertQueryAndParams.getQuery(), TxControl.serializableRw().setCommitTx(true),
                insertQueryAndParams.getParams()).join().expect("Cannot insert tasks for test " + className);
        session.close();
    }

    @Test
    void findTest() {

        Collection<PrimaryId> primaryIds = storage.find()
                .whereJobStatus(JobStatus.READY)
                .whereNeedReschedule(true)
                .findPrimaryIds();

        Collection<Job> jobs =
                storage.find().wherePrimaryIdIn(primaryIds)
                        .whereJobStatus(JobStatus.READY)
                        .whereNeedReschedule(true)
                        .findJobs();

        assertThat(jobs).hasSize(1);
        assertThat(((YdbPrimaryId) jobs.iterator().next().primaryId()).getId()).isEqualTo("1");
    }

    @Test
    void updateTest() {
        Instant nextRun = Instant.now().plus(1, HOURS).truncatedTo(SECONDS);

        storage.update()
                .wherePrimaryIdIn(List.of(new YdbPrimaryId("1")))
                .whereJobStatus(READY)
                .whereNeedReschedule(true)
                .setNextRun(nextRun)
                .setNeedReschedule(false)
                .execute();

        List<String> gotIds = tableClient.createSession().thenApply(
                sessionResult -> sessionResult.expect("Cannot create session"))
                .thenCompose(session -> {
                            var selectIds =
                                    select(SCHEDULED_TASKS.ID)
                                            .from(SCHEDULED_TASKS)
                                            .where(SCHEDULED_TASKS.STATUS.eq("New")
                                                    .and(SCHEDULED_TASKS.NEED_RESCHEDULE.eq(false))
                                                    .and(SCHEDULED_TASKS.NEXT_RUN.eq(nextRun).and(SCHEDULED_TASKS.INSTANCE_ID.isNull())))
                                            .queryAndParams(db);

                            return session.executeDataQuery(selectIds.getQuery(),
                                    TxControl.serializableRw().setCommitTx(true), selectIds.getParams());
                        }
                ).thenApply(dataQueryResultResult -> {
                    var resultSet = dataQueryResultResult.expect("failed to select task ids");
                    List<String> ids = new ArrayList<>();
                    var reader = resultSet.getResultSet(0);
                    while (reader.next()) {
                        ids.add(reader.getColumn("id").getUtf8());
                    }
                    return ids;
                }).join();


        assertThat(gotIds).hasSize(1);

        assertThat(gotIds.get(0)).isEqualTo("1");
    }
}
