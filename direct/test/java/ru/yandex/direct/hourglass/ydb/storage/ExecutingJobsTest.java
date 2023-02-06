package ru.yandex.direct.hourglass.ydb.storage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

class ExecutingJobsTest {
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

        schedulerId = mock(InstanceId.class);

        when(schedulerId.toString()).thenReturn("deadbeef");

        storage = new YdbStorageImpl(tableClient, db,
                YdbSessionProperties.builder().build(), HourglassProperties.builder().build(),
                schedulerId, "1", s -> s);
    }

    @BeforeEach
    void makeSchedule() {
        var className = this.getClass().getName();
        var session = tableClient.createSession().join().expect("Cannot create session for test " + className);

        var deleteQueryAndParams = deleteFrom(SCHEDULED_TASKS).queryAndParams(db);
        session.executeDataQuery(deleteQueryAndParams.getQuery(),
                TxControl.serializableRw().setCommitTx(true)).join().expect("Cannot truncate table before" +
                " test " + className);

        var heartbeatTime = Instant.now().minusSeconds(10);
        var nextRunTime = Instant.now().minusSeconds(10);
        var insertValues = tempTable(new TempTableDescription("values"), SCHEDULED_TASKS.ID, SCHEDULED_TASKS.NAME,
                SCHEDULED_TASKS.PARAMS, SCHEDULED_TASKS.STATUS,
                SCHEDULED_TASKS.NEED_RESCHEDULE, SCHEDULED_TASKS.HEARTBEAT_TIME,
                SCHEDULED_TASKS.NEXT_RUN, SCHEDULED_TASKS.SCHEDULE_HASH,
                SCHEDULED_TASKS.INSTANCE_ID, SCHEDULED_TASKS.VERSION)
                .createValues()
                .fill("1", "A", "A1", "Running", false, heartbeatTime, nextRunTime, "", "deadbeef", "1")
                /* Во время выполнения поменялась версия расписания, по окончании работы версия не должна
                        сброситься */
                .fill("2", "B", "B1", "Running", true, heartbeatTime, nextRunTime, "", "deadbeef", "2")
                //Not executing jobs
                .fill("3", "C", "B1", "Running", false, heartbeatTime, nextRunTime, "", "cafebabe", "2")

                .fill("4", "D", "B1", "New", false, null, nextRunTime, "", null, "1")

                .fill("5", "E", "B1", "Paused", false, null, nextRunTime, "", null, "2")

                .fill("6", "F", "B1", "New", true, null, nextRunTime, "", "deadbeef", "2");
        var queryAndParams = upsertInto(SCHEDULED_TASKS).selectAll().from(insertValues).queryAndParams(db);

        session.executeDataQuery(queryAndParams.getQuery(), TxControl.serializableRw().setCommitTx(true),
                queryAndParams.getParams()).join().expect("Cannot insert " +
                "tasks for test " + className);
        session.close();
    }

    @Test
    void findTest() {

        Collection<PrimaryId> primaryIds = storage.find().whereJobStatus(JobStatus.LOCKED).findPrimaryIds();
        Collection<Job> jobs =
                storage.find().wherePrimaryIdIn(primaryIds).whereJobStatus(JobStatus.LOCKED).findJobs();

        Set<String> expectedIds = new HashSet<>(Set.of("1", "2"));

        assertThat(jobs).hasSize(2);
        var gotIds = jobs.stream().map(job -> ((YdbPrimaryId) job.primaryId()).getId()).collect(toList());
        assertThat(gotIds).containsExactlyInAnyOrder(expectedIds.toArray(String[]::new));
    }

    @Test
    void updateTest() {
        Instant now = Instant.now().truncatedTo(SECONDS);
        storage.update().whereJobStatus(JobStatus.LOCKED).setJobStatus(JobStatus.LOCKED).execute();

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

        assertThat(result).hasSize(2);

        var expectedIds = List.of("1", "2");
        var gotIds = result.keySet();
        assertThat(gotIds).containsExactlyInAnyOrder(expectedIds.toArray(String[]::new));
        var heartbeatTimes = result.values();

        for (var heartbeatTime : heartbeatTimes) {
            assertThat(heartbeatTime).isAfterOrEqualTo(LocalDateTime.ofInstant(now, ZoneId.of("UTC")));
        }
    }

    @Test
    void executingToReadyTest() {
        Instant nextRun = Instant.now().truncatedTo(SECONDS);
        storage.update()
                .whereJobStatus(JobStatus.LOCKED)
                .setNextRun(nextRun)
                .setJobStatus(JobStatus.READY)
                .execute();

        List<RescheduledInfo> got =
                tableClient.createSession().thenApply(
                        sessionResult -> sessionResult.expect("Cannot create session"))
                        .thenCompose(session -> {
                                    var selectIds = select(SCHEDULED_TASKS.ID, SCHEDULED_TASKS.NEED_RESCHEDULE,
                                            SCHEDULED_TASKS.VERSION)
                                            .from(SCHEDULED_TASKS)
                                            .where(SCHEDULED_TASKS.STATUS.eq("New")
                                                    .and(SCHEDULED_TASKS.INSTANCE_ID.isNull())
                                                    .and(SCHEDULED_TASKS.HEARTBEAT_TIME.isNull())
                                                    .and(SCHEDULED_TASKS.NEXT_RUN.eq(nextRun)))
                                            .queryAndParams(db);

                                    return session.executeDataQuery(selectIds.getQuery(),
                                            TxControl.serializableRw().setCommitTx(true), selectIds.getParams());
                                }
                        ).thenApply(dataQueryResultResult -> {
                    var resultSet = dataQueryResultResult.expect("failed to select task ids");
                    List<RescheduledInfo> rescheduledInfos = new ArrayList<>();
                    var reader = resultSet.getResultSet(0);
                    while (reader.next()) {
                        rescheduledInfos.add(new RescheduledInfo(reader.getColumn("id").getUtf8(),
                                reader.getColumn("need_reschedule").getBool(), reader.getColumn("version").getUtf8()));
                    }
                    return rescheduledInfos;
                }).join();

        assertThat(got).hasSize(2);
        var expected = new RescheduledInfo[]{
                new RescheduledInfo("1", false, "1"),
                new RescheduledInfo("2", true, "2")
        };
        assertThat(got).containsExactlyInAnyOrder(expected);
    }

    private class RescheduledInfo {
        String id;
        boolean needReschedule;
        String version;

        RescheduledInfo(String id, boolean needReschedule, String version) {
            this.id = id;
            this.needReschedule = needReschedule;
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RescheduledInfo that = (RescheduledInfo) o;
            return needReschedule == that.needReschedule &&
                    Objects.equals(id, that.id) &&
                    Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, needReschedule, version);
        }

        @Override
        public String toString() {
            return "RescheduledInfo{" +
                    "id='" + id + '\'' +
                    ", needReschedule=" + needReschedule +
                    ", version='" + version + '\'' +
                    '}';
        }
    }
}
