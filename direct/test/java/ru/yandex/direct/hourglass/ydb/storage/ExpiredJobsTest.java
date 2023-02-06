package ru.yandex.direct.hourglass.ydb.storage;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.hourglass.ydb.storage.Tables.SCHEDULED_TASKS;
import static ru.yandex.direct.ydb.builder.querybuilder.DeleteBuilder.deleteFrom;
import static ru.yandex.direct.ydb.builder.querybuilder.InsertBuilder.upsertInto;
import static ru.yandex.direct.ydb.builder.querybuilder.SelectBuilder.select;
import static ru.yandex.direct.ydb.table.temptable.TempTable.tempTable;

class ExpiredJobsTest {
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
                hourglassConfiguration, schedulerId, "1", s -> s);
    }

    @BeforeEach
    void makeSchedule() {
        var className = this.getClass().getName();
        var session = tableClient.createSession().join().expect("Cannot create session for test " + className);

        var deleteQueryAndParams = deleteFrom(SCHEDULED_TASKS).queryAndParams(db);
        session.executeDataQuery(deleteQueryAndParams.getQuery(),
                TxControl.serializableRw().setCommitTx(true), deleteQueryAndParams.getParams()).join().expect("Cannot" +
                " truncate table before test " + className);

        var now = Instant.now();
        var heartbeatTimeInThePast = now.minusSeconds(600);
        var heartbeatRightNow = now.minusSeconds(1);
        var nextRunInThePast = now.minusSeconds(10);

        var insertValues = tempTable(new TempTableDescription("values"), SCHEDULED_TASKS.ID, SCHEDULED_TASKS.NAME,
                SCHEDULED_TASKS.PARAMS, SCHEDULED_TASKS.STATUS,
                SCHEDULED_TASKS.NEED_RESCHEDULE, SCHEDULED_TASKS.HEARTBEAT_TIME,
                SCHEDULED_TASKS.NEXT_RUN, SCHEDULED_TASKS.SCHEDULE_HASH,
                SCHEDULED_TASKS.INSTANCE_ID, SCHEDULED_TASKS.VERSION)
                .createValues()
                .fill("1", "A", "A1", "New", false, heartbeatTimeInThePast, nextRunInThePast, "", "deadbeef", "1")
                .fill("2", "B", "B1", "New", true, heartbeatTimeInThePast, nextRunInThePast, "", "cafebabe", "2")
                .fill("3", "BG", "B1", "New", true, heartbeatTimeInThePast, nextRunInThePast, "", "cafebabe", "1")
                .fill("4", "C", "C1", "Running", false, heartbeatTimeInThePast, nextRunInThePast, "", "deadbeef", "2")
                .fill("5", "D", "C1", "Running", true, heartbeatTimeInThePast, nextRunInThePast, "", "deadbeef", "1")
                .fill("6", "DG", "C1", "Running", true, heartbeatTimeInThePast, nextRunInThePast, "", "cafebabe", "2")
                //Non expired
                .fill("7", "F", "F1", "Paused", true, heartbeatRightNow, nextRunInThePast, "", "deadbeef", "1")
                .fill("8", "E", "E1", "Paused", false, heartbeatRightNow, nextRunInThePast, "", "deadbeef", "2")
                .fill("9", "EG", "E1", "Paused", false, heartbeatRightNow, nextRunInThePast, "", "cafebabe",
                        "1")
                .fill("10", "J", "F1", "Deleted", true, heartbeatRightNow, nextRunInThePast, "", "deadbeef",
                        "1")
                .fill("11", "K", "E1", "Deleted", false, heartbeatRightNow, nextRunInThePast, "", "deadbeef",
                        "2")
                .fill("12", "L", "D1", "New", true, heartbeatRightNow, nextRunInThePast, "", "deadbeef", "2")
                .fill("13", "LG", "D1", "New", true, heartbeatRightNow, nextRunInThePast, "", "cafebabe", "1")
                .fill("14", "M", "F1", "Running", true, heartbeatRightNow, nextRunInThePast, "", "deadbeef",
                        "1")
                .fill("15", "MG", "F1", "Running", true, heartbeatRightNow, nextRunInThePast, "", "cafebabe", "2");

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

        Collection<PrimaryId> primaryIds = storage.find().whereJobStatus(JobStatus.EXPIRED).findPrimaryIds();
        Collection<Job> jobs = storage.find().wherePrimaryIdIn(primaryIds).whereJobStatus(JobStatus.EXPIRED).findJobs();

        Set<String> expectedExpiredIds = new HashSet<>(Set.of("4", "5", "6"));

        assertThat(jobs).hasSize(3);
        var gotExpiredIds = jobs.stream().map(job -> ((YdbPrimaryId) job.primaryId()).getId()).collect(toList());
        assertThat(gotExpiredIds).containsExactlyInAnyOrder(expectedExpiredIds.toArray(String[]::new));

    }

    @Test
    void updateTest() {
        storage.update().whereJobStatus(JobStatus.EXPIRED).setJobStatus(JobStatus.READY).execute();

        var gotIdsToVersions = tableClient.createSession().thenApply(
                sessionResult -> sessionResult.expect("Cannot create session"))
                .thenCompose(session -> {
                            var selectIds =
                                    select(SCHEDULED_TASKS.ID, SCHEDULED_TASKS.VERSION)
                                            .from(SCHEDULED_TASKS)
                                            .where(SCHEDULED_TASKS.STATUS.eq("New").and(SCHEDULED_TASKS.INSTANCE_ID.isNull()).and(SCHEDULED_TASKS.HEARTBEAT_TIME.isNull()))
                                            .queryAndParams(db);

                            return session.executeDataQuery(selectIds.getQuery(),
                                    TxControl.serializableRw().setCommitTx(true), selectIds.getParams());
                        }
                ).thenApply(dataQueryResultResult -> {
                    var resultSet = dataQueryResultResult.expect("failed to select task ids");
                    Map<String, String> idsToHeartbeatTime = new HashMap<>();
                    var reader = resultSet.getResultSet(0);
                    while (reader.next()) {
                        idsToHeartbeatTime.put(reader.getColumn("id").getUtf8(),
                                reader.getColumn("version").getUtf8());
                    }
                    return idsToHeartbeatTime;
                }).join();

        assertThat(gotIdsToVersions).hasSize(3);
        var expectedIdsToVersions = Map.of(
                "4", "2",
                "5", "1",
                "6", "2"
        );
        assertThat(gotIdsToVersions).isEqualTo(expectedIdsToVersions);
    }
}
