package ru.yandex.direct.hourglass.ydb.storage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.yandex.ydb.table.TableClient;
import com.yandex.ydb.table.transaction.TxControl;
import org.assertj.core.data.TemporalUnitWithinOffset;
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
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.hourglass.ydb.storage.Tables.SCHEDULED_TASKS;
import static ru.yandex.direct.ydb.builder.querybuilder.DeleteBuilder.deleteFrom;
import static ru.yandex.direct.ydb.builder.querybuilder.InsertBuilder.upsertInto;
import static ru.yandex.direct.ydb.builder.querybuilder.SelectBuilder.select;
import static ru.yandex.direct.ydb.table.temptable.TempTable.tempTable;

class ReadyJobsTest {
    private static final String VERSION = "1";
    private static final Instant NOW = Instant.now();
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

        storage = new YdbStorageImpl(tableClient, db, YdbSessionProperties.builder().build(),
                HourglassProperties.builder().build(),
                schedulerId, VERSION, s -> s);
    }

    @BeforeEach
    void makeSchedule() {
        var className = this.getClass().getName();
        var session = tableClient.createSession().join().expect("Cannot create session for test " + className);

        var deleteQueryAndParams = deleteFrom(SCHEDULED_TASKS).queryAndParams(db);
        session.executeDataQuery(deleteQueryAndParams.getQuery(),
                TxControl.serializableRw().setCommitTx(true), deleteQueryAndParams.getParams()).join().expect("Cannot" +
                " truncate table before test " + className);

        var nowSec = NOW;
        var nextRunInTheFuture = NOW.plus(1, HOURS);
        var nextRunInTheClosePast = NOW.minusSeconds(10);
        var nextRunOneMinuteAgo = NOW.minus(1, MINUTES);
        var nextRunRightNow = NOW.minusSeconds(1);
        var heartbeatTimeInThePast = NOW.minus(2, MINUTES);

        var insertValues = tempTable(new TempTableDescription("values"), SCHEDULED_TASKS.ID, SCHEDULED_TASKS.NAME,
                SCHEDULED_TASKS.PARAMS, SCHEDULED_TASKS.STATUS,
                SCHEDULED_TASKS.NEED_RESCHEDULE, SCHEDULED_TASKS.HEARTBEAT_TIME,
                SCHEDULED_TASKS.NEXT_RUN, SCHEDULED_TASKS.SCHEDULE_HASH,
                SCHEDULED_TASKS.INSTANCE_ID, SCHEDULED_TASKS.VERSION)
                .createValues()
                .fill("1", "A", "A1", "New", false, nowSec, nextRunInTheClosePast, "", null, "1")
                .fill("2", "B", "A1", "New", false, nowSec, nextRunRightNow, "", null, "1")
                .fill("3", "C", "A1", "New", true, nowSec, nextRunRightNow, "", null, "1")
                .fill("4", "D", "A1", "New", false, nowSec, nextRunInTheClosePast, "", "deadbeef", "1")
                .fill("5", "E", "A1", "New", false, nowSec, nextRunInTheClosePast, "", "cafrbabe", "1")
                .fill("6", "F", "A1", "New", false, nowSec, nextRunInTheFuture, "", null, "1")
                .fill("7", "G", "A1", "Paused", false, nowSec, nextRunOneMinuteAgo, "", null, "1")
                .fill("8", "J", "A1", "Running", false, heartbeatTimeInThePast, nextRunInTheClosePast, "", "deadbeef",
                        "1")
                .fill("9", "M", "P1", "New", false, nowSec, nextRunInTheClosePast, "", null, "2");

        var insertQueryAndParams = upsertInto(SCHEDULED_TASKS)
                .selectAll()
                .from(insertValues)
                .queryAndParams(db);
        session.executeDataQuery(insertQueryAndParams.getQuery(), TxControl.serializableRw().setCommitTx(true),
                insertQueryAndParams.getParams()).join().expect("Cannot insert " +
                "tasks for test " + className);
    }

    @Test
    void findTest() {

        Collection<PrimaryId> primaryIds =
                storage.find().whereNextRunLeNow().whereJobStatus(JobStatus.READY).findPrimaryIds();

        Collection<Job> jobs = storage.find().wherePrimaryIdIn(primaryIds).whereJobStatus(JobStatus.READY).findJobs();

        assertThat(jobs).hasSize(3);
        var expectedIds = List.of("1", "2", "3");
        var gotIds = jobs.stream().map(job -> ((YdbPrimaryId) job.primaryId()).getId()).collect(toList());

        assertThat(gotIds).containsExactlyInAnyOrder(expectedIds.toArray(String[]::new));
    }

    @Test
    void findRescheduleTest() {

        Collection<PrimaryId> primaryIds =
                storage.find().whereNextRunLeNow()
                        .whereNeedReschedule(false)
                        .whereJobStatus(JobStatus.READY)
                        .findPrimaryIds();

        Collection<Job> jobs = storage.find()
                .wherePrimaryIdIn(primaryIds)
                .whereJobStatus(JobStatus.READY).findJobs();

        assertThat(jobs).hasSize(2);
        var expectedIds = List.of("1", "2");
        var gotIds = jobs.stream().map(job -> ((YdbPrimaryId) job.primaryId()).getId()).collect(toList());
        assertThat(gotIds).containsExactlyInAnyOrder(expectedIds.toArray(String[]::new));
    }

    @Test
    void updateTest() {
        storage.update().whereNextRunLeNow().whereJobStatus(JobStatus.READY).setJobStatus(JobStatus.LOCKED)
                .execute();

        Map<String, LocalDateTime> result = tableClient.createSession().thenApply(
                sessionResult -> sessionResult.expect("Cannot create session"))
                .thenCompose(session -> {
                            var selectIds = select(SCHEDULED_TASKS.ID, SCHEDULED_TASKS.HEARTBEAT_TIME)
                                    .from(SCHEDULED_TASKS)
                                    .where(SCHEDULED_TASKS.STATUS.eq("Running").and(SCHEDULED_TASKS.INSTANCE_ID.eq(schedulerId.toString())))
                                    .queryAndParams(db);
                            return session.executeDataQuery(selectIds.getQuery(),
                                    TxControl.serializableRw().setCommitTx(true), selectIds.getParams());
                        }
                ).thenApply(dataQueryResultResult -> {
                    var resultSet = dataQueryResultResult.expect("failed to select task ids");
                    Map<String, LocalDateTime> idsToHeartbeatTime = new HashMap<>();
                    var reader = resultSet.getResultSet(0);
                    while (reader.next()) {
                        idsToHeartbeatTime.put(reader.getColumn("id").getUtf8(),
                                reader.getColumn("heartbeat_time").getDatetime());
                    }
                    return idsToHeartbeatTime;
                }).join();

        assertThat(result).hasSize(4);
        var expectedUpdatedIds = List.of("1", "2", "3");
        var expectedNotUpdatedIds = List.of("8");
        assertThat(result).containsKeys(expectedUpdatedIds.toArray(String[]::new));
        assertThat(result).containsKeys(expectedNotUpdatedIds.toArray(String[]::new));

        var offset = new TemporalUnitWithinOffset(30, ChronoUnit.SECONDS);
        for (var expectedUpdatedId : expectedUpdatedIds) {
            assertThat(result.get(expectedUpdatedId)).isCloseTo(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"
            )), offset);
        }

        for (var expectedNotUpdatedId : expectedNotUpdatedIds) {
            assertThat(result.get(expectedNotUpdatedId)).isBefore(LocalDateTime.ofInstant(NOW.minus(1, MINUTES),
                    ZoneId.of("UTC")));
        }
    }
}
