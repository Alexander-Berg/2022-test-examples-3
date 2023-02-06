package ru.yandex.direct.hourglass.ydb.storage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.yandex.ydb.table.TableClient;
import com.yandex.ydb.table.result.ResultSetReader;
import com.yandex.ydb.table.transaction.TxControl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.hourglass.HourglassProperties;
import ru.yandex.direct.hourglass.implementations.InstanceIdImpl;
import ru.yandex.direct.hourglass.implementations.updateschedule.ScheduleRecord;
import ru.yandex.direct.hourglass.ydb.YdbInfoHolder;
import ru.yandex.direct.ydb.YdbPath;
import ru.yandex.direct.ydb.client.YdbSessionProperties;
import ru.yandex.direct.ydb.column.Column;
import ru.yandex.direct.ydb.table.temptable.TempTableDescription;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.hourglass.ydb.storage.Tables.SCHEDULED_TASKS;
import static ru.yandex.direct.ydb.builder.querybuilder.DeleteBuilder.deleteFrom;
import static ru.yandex.direct.ydb.builder.querybuilder.InsertBuilder.upsertInto;
import static ru.yandex.direct.ydb.builder.querybuilder.SelectBuilder.select;
import static ru.yandex.direct.ydb.table.temptable.TempTable.tempTable;

class ScheduleUpdateTest {

    private static final String OLD_VERSION = "old_version";
    private static final String NEW_VERSION = "new_version";
    private static final List<Column> scheduledTaskColumns = List.of(SCHEDULED_TASKS.ID,
            SCHEDULED_TASKS.NAME,
            SCHEDULED_TASKS.PARAMS,
            SCHEDULED_TASKS.STATUS,
            SCHEDULED_TASKS.INSTANCE_ID,
            SCHEDULED_TASKS.HEARTBEAT_TIME,
            SCHEDULED_TASKS.SCHEDULE_HASH,
            SCHEDULED_TASKS.NEED_RESCHEDULE,
            SCHEDULED_TASKS.NEXT_RUN,
            SCHEDULED_TASKS.LAST_START_TIME,
            SCHEDULED_TASKS.LAST_FINISH_TIME,
            SCHEDULED_TASKS.VERSION,
            SCHEDULED_TASKS.META);
    private static YdbStorageImpl storage;
    private static TableClient tableClient;
    private static YdbPath db;

    @BeforeAll
    static void initDb() {
        var ydbInfo = YdbInfoHolder.getYdbInfo();
        tableClient = ydbInfo.getClient();
        db = ydbInfo.getDb();
        var instanceId = new InstanceIdImpl("instance");

        storage = new YdbStorageImpl(tableClient, db, YdbSessionProperties.builder().build(),
                HourglassProperties.builder().build(),
                instanceId, NEW_VERSION, s -> s);
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
     * Тест проверяет, что расписние менсяется только у несовпадающий версий
     */
    @Test
    void setNewSchedule_OnlyDifferentVersionUpdateTest() {

        var recordWithSameVersion = new ScheduledTasksRecord();
        recordWithSameVersion.setId("name1_param");
        recordWithSameVersion.setName("name1");
        recordWithSameVersion.setParams("param");
        recordWithSameVersion.setStatus("New");
        recordWithSameVersion.setNeedReschedule(false);
        recordWithSameVersion.setHeartbeatTime(LocalDateTime.of(2019, 9, 30, 1, 2).toInstant(ZoneOffset.UTC));
        recordWithSameVersion.setNextRun(LocalDateTime.of(2019, 9, 30, 1, 2, 30).toInstant(ZoneOffset.UTC));
        recordWithSameVersion.setScheduleHash("hash");
        recordWithSameVersion.setVersion(NEW_VERSION);
        recordWithSameVersion.setInstanceId(null);
        recordWithSameVersion.setLastFinishTime(LocalDateTime.of(2019, 9, 30, 0, 0, 30).toInstant(ZoneOffset.UTC));
        recordWithSameVersion.setLastStartTime(LocalDateTime.of(2019, 9, 30, 0, 0, 20).toInstant(ZoneOffset.UTC));
        recordWithSameVersion.setMeta("meta1_old");

        var recordWithAnotherVersion = new ScheduledTasksRecord();
        recordWithAnotherVersion.setId("name2_");
        recordWithAnotherVersion.setName("name2");
        recordWithAnotherVersion.setParams("");
        recordWithAnotherVersion.setStatus("New");
        recordWithAnotherVersion.setNeedReschedule(false);
        recordWithAnotherVersion.setHeartbeatTime(LocalDateTime.of(2019, 9, 30, 0, 59, 21).toInstant(ZoneOffset.UTC));
        recordWithAnotherVersion.setNextRun(LocalDateTime.of(2019, 9, 30, 4, 2, 36).toInstant(ZoneOffset.UTC));
        recordWithAnotherVersion.setScheduleHash("hash_old");
        recordWithAnotherVersion.setVersion(OLD_VERSION);
        recordWithSameVersion.setInstanceId(null);
        recordWithAnotherVersion.setLastFinishTime(LocalDateTime.of(2019, 9, 30, 3, 29, 30).toInstant(ZoneOffset.UTC));
        recordWithAnotherVersion.setLastStartTime(LocalDateTime.of(2019, 9, 30, 3, 13, 45).toInstant(ZoneOffset.UTC));
        recordWithAnotherVersion.setMeta("meta2_old");

        insertSchedule(recordWithSameVersion, recordWithAnotherVersion);

        var newSchedule = List.of(
                new ScheduleRecord().setName("name1").setParam("param").setScheduleHashSum("hash").setNameHashSum(
                        "nameHash").setMeta("meta1_old"),
                new ScheduleRecord().setName("name2").setParam("").setScheduleHashSum("hash_new").setNameHashSum(
                        "nameHash").setMeta("meta2_new")
        );

        storage.setNewSchedule(newSchedule);

        var idToScheduleRecord = getIdScheduleMap();

        assertThat(idToScheduleRecord).containsKeys("name1_param", "name2_");
        assertRecordsEquals(idToScheduleRecord.get("name1_param"), recordWithSameVersion, Set.of());
        var differentScheduleRecord = idToScheduleRecord.get("name2_");
        assertRecordsEquals(differentScheduleRecord, recordWithAnotherVersion,
                Set.of("need_reschedule", "version", "schedule_hash", "meta"));
        assertThat(differentScheduleRecord.getNeedReschedule()).isEqualTo(true);
        assertThat(differentScheduleRecord.getVersion()).isEqualTo(NEW_VERSION);
        assertThat(differentScheduleRecord.getScheduleHash()).isEqualTo("hash_new");
        assertThat(differentScheduleRecord.getMeta()).isEqualTo("meta2_new");
    }

    /**
     * Тест проверяет, что расписние менсяется только у задачи, у которых новое расписание отличается от старого, но
     * версия обновится у всех∆
     */
    @Test
    void setNewSchedule_OnlyWhenDifferentScheduleSumUpdateTest() {

        var recordWithSameVersion = new ScheduledTasksRecord();
        recordWithSameVersion.setId("name1_param");
        recordWithSameVersion.setName("name1");
        recordWithSameVersion.setParams("param");
        recordWithSameVersion.setStatus("New");
        recordWithSameVersion.setNeedReschedule(false);
        recordWithSameVersion.setHeartbeatTime(LocalDateTime.of(2019, 9, 30, 1, 2).toInstant(ZoneOffset.UTC));
        recordWithSameVersion.setNextRun(LocalDateTime.of(2019, 9, 30, 1, 2, 30).toInstant(ZoneOffset.UTC));
        recordWithSameVersion.setScheduleHash("hash");
        recordWithSameVersion.setVersion(OLD_VERSION);
        recordWithSameVersion.setLastFinishTime(LocalDateTime.of(2019, 9, 30, 1, 0, 30).toInstant(ZoneOffset.UTC));
        recordWithSameVersion.setLastStartTime(LocalDateTime.of(2019, 9, 30, 0, 0, 30).toInstant(ZoneOffset.UTC));


        var recordWithAnotherVersion = new ScheduledTasksRecord();
        recordWithAnotherVersion.setId("name2_");
        recordWithAnotherVersion.setName("name2");
        recordWithAnotherVersion.setParams("");
        recordWithAnotherVersion.setStatus("New");
        recordWithAnotherVersion.setNeedReschedule(false);
        recordWithAnotherVersion.setHeartbeatTime(LocalDateTime.of(2019, 9, 30, 0, 59, 21).toInstant(ZoneOffset.UTC));
        recordWithAnotherVersion.setNextRun(LocalDateTime.of(2019, 9, 30, 4, 2, 36).toInstant(ZoneOffset.UTC));
        recordWithAnotherVersion.setScheduleHash("hash_old");
        recordWithAnotherVersion.setVersion(OLD_VERSION);
        recordWithAnotherVersion.setLastFinishTime(LocalDateTime.of(2019, 9, 30, 2, 17, 11).toInstant(ZoneOffset.UTC));
        recordWithAnotherVersion.setLastStartTime(LocalDateTime.of(2019, 9, 30, 2, 15, 1).toInstant(ZoneOffset.UTC));


        insertSchedule(recordWithSameVersion, recordWithAnotherVersion);

        var newSchedule = List.of(
                new ScheduleRecord().setName("name1").setParam("param").setScheduleHashSum("hash").setNameHashSum(
                        "nameHash"),
                new ScheduleRecord().setName("name2").setParam("").setScheduleHashSum("hash_new").setNameHashSum(
                        "nameHash")
        );

        storage.setNewSchedule(newSchedule);

        var idToScheduleRecord = getIdScheduleMap();

        assertThat(idToScheduleRecord).hasSize(2);
        assertThat(idToScheduleRecord).containsKeys("name1_param", "name2_");
        var sameScheduleRecord = idToScheduleRecord.get("name1_param");
        assertRecordsEquals(sameScheduleRecord, recordWithSameVersion, Set.of("version"));
        assertThat(sameScheduleRecord.getVersion()).isEqualTo(NEW_VERSION);

        var differentScheduleRecord = idToScheduleRecord.get("name2_");
        assertRecordsEquals(differentScheduleRecord, recordWithAnotherVersion,
                Set.of("need_reschedule", "version", "schedule_hash"));
        assertThat(differentScheduleRecord.getNeedReschedule()).isEqualTo(true);
        assertThat(differentScheduleRecord.getVersion()).isEqualTo(NEW_VERSION);
        assertThat(differentScheduleRecord.getScheduleHash()).isEqualTo("hash_new");
    }

    /**
     * Тест проверяет, вставку новой задачи
     */
    @Test
    void setNewSchedule_InsertNewTask() {

        var newSchedule = List.of(
                new ScheduleRecord().setName("name1").setParam("param").setScheduleHashSum("hash").setNameHashSum(
                        "nameHash")
        );

        storage.setNewSchedule(newSchedule);

        var idToScheduleRecord = getIdScheduleMap();

        assertThat(idToScheduleRecord).hasSize(1);
        var id = idToScheduleRecord.keySet().iterator().next();
        var gotRecord = idToScheduleRecord.get(id);
        var expectedRecord = new ScheduledTasksRecord();
        expectedRecord.setId(id);
        expectedRecord.setName("name1");
        expectedRecord.setParams("param");
        expectedRecord.setScheduleHash("hash");
        expectedRecord.setNeedReschedule(true);
        expectedRecord.setVersion(NEW_VERSION);
        expectedRecord.setStatus("New");

        assertRecordsEquals(gotRecord, expectedRecord, Set.of());
    }

    /**
     * Тест проверяет, что статусы Running и Paused не изменятся при обновлении расписания
     */
    @Test
    void setNewSchedule_StatusNotChangedTest() {

        var taskWithStatusRunningBeforeUpdate = new ScheduledTasksRecord();
        taskWithStatusRunningBeforeUpdate.setId("name1_param");
        taskWithStatusRunningBeforeUpdate.setName("name1");
        taskWithStatusRunningBeforeUpdate.setParams("param");
        taskWithStatusRunningBeforeUpdate.setStatus("Running");
        taskWithStatusRunningBeforeUpdate.setNeedReschedule(false);
        taskWithStatusRunningBeforeUpdate.setHeartbeatTime(LocalDateTime.of(2019, 9, 30, 1, 2).toInstant(ZoneOffset.UTC));
        taskWithStatusRunningBeforeUpdate.setNextRun(LocalDateTime.of(2019, 9, 30, 1, 2, 30).toInstant(ZoneOffset.UTC));
        taskWithStatusRunningBeforeUpdate.setScheduleHash("hash_old1");
        taskWithStatusRunningBeforeUpdate.setInstanceId("some_instance");
        taskWithStatusRunningBeforeUpdate.setVersion(OLD_VERSION);
        taskWithStatusRunningBeforeUpdate.setLastFinishTime(LocalDateTime.of(2019, 9, 30, 1, 0, 30).toInstant(ZoneOffset.UTC));
        taskWithStatusRunningBeforeUpdate.setLastStartTime(LocalDateTime.of(2019, 9, 30, 0, 0, 30).toInstant(ZoneOffset.UTC));


        var taskWithStatusPausedBeforeUpdate = new ScheduledTasksRecord();
        taskWithStatusPausedBeforeUpdate.setId("name2_");
        taskWithStatusPausedBeforeUpdate.setName("name2");
        taskWithStatusPausedBeforeUpdate.setParams("");
        taskWithStatusPausedBeforeUpdate.setStatus("Paused");
        taskWithStatusPausedBeforeUpdate.setNeedReschedule(false);
        taskWithStatusPausedBeforeUpdate.setHeartbeatTime(LocalDateTime.of(2019, 9, 30, 0, 59, 21).toInstant(ZoneOffset.UTC));
        taskWithStatusPausedBeforeUpdate.setNextRun(LocalDateTime.of(2019, 9, 30, 4, 2, 36).toInstant(ZoneOffset.UTC));
        taskWithStatusPausedBeforeUpdate.setScheduleHash("hash_old2");
        taskWithStatusPausedBeforeUpdate.setVersion(OLD_VERSION);
        taskWithStatusPausedBeforeUpdate.setLastFinishTime(LocalDateTime.of(2019, 9, 30, 1, 0, 30).toInstant(ZoneOffset.UTC));
        taskWithStatusPausedBeforeUpdate.setLastStartTime(LocalDateTime.of(2019, 9, 30, 0, 0, 30).toInstant(ZoneOffset.UTC));


        insertSchedule(taskWithStatusRunningBeforeUpdate, taskWithStatusPausedBeforeUpdate);

        var newSchedule = List.of(
                new ScheduleRecord().setName("name1").setParam("param").setScheduleHashSum("hash_new1").setNameHashSum(
                        "nameHash"),
                new ScheduleRecord().setName("name2").setParam("").setScheduleHashSum("hash_new2").setNameHashSum(
                        "nameHash")
        );

        storage.setNewSchedule(newSchedule);

        var idToScheduleRecord = getIdScheduleMap();

        assertThat(idToScheduleRecord).containsKeys("name1_param", "name2_");
        var taskWithStatusRunningAfterUpdate = idToScheduleRecord.get("name1_param");
        assertRecordsEquals(taskWithStatusRunningAfterUpdate, taskWithStatusRunningBeforeUpdate,
                Set.of("need_reschedule", "version", "schedule_hash"));
        assertThat(taskWithStatusRunningAfterUpdate.getVersion()).isEqualTo(NEW_VERSION);
        assertThat(taskWithStatusRunningAfterUpdate.getScheduleHash()).isEqualTo("hash_new1");
        assertThat(taskWithStatusRunningAfterUpdate.getNeedReschedule()).isEqualTo(true);

        var taskWithStatusPausedAfterUpdate = idToScheduleRecord.get("name2_");
        assertRecordsEquals(taskWithStatusPausedAfterUpdate, taskWithStatusPausedBeforeUpdate,
                Set.of("need_reschedule", "version", "schedule_hash"));
        assertThat(taskWithStatusPausedAfterUpdate.getNeedReschedule()).isEqualTo(true);
        assertThat(taskWithStatusPausedAfterUpdate.getVersion()).isEqualTo(NEW_VERSION);
        assertThat(taskWithStatusPausedAfterUpdate.getScheduleHash()).isEqualTo("hash_new2");
    }

    /**
     * Тест проверяет, что задача есть в базе, но в новом расписании ее нет - она заархивируется, но версия у нее
     * обновится
     */
    @Test
    void setNewSchedule_ArchiveTaskTest() {

        var taskToBeArchived = new ScheduledTasksRecord();
        taskToBeArchived.setId("name1_param");
        taskToBeArchived.setName("name1");
        taskToBeArchived.setParams("param");
        taskToBeArchived.setStatus("New");
        taskToBeArchived.setNeedReschedule(false);
        taskToBeArchived.setHeartbeatTime(LocalDateTime.of(2019, 9, 30, 1, 2).toInstant(ZoneOffset.UTC));
        taskToBeArchived.setNextRun(LocalDateTime.of(2019, 9, 30, 1, 2, 30).toInstant(ZoneOffset.UTC));
        taskToBeArchived.setScheduleHash("hash_old1");
        taskToBeArchived.setVersion(OLD_VERSION);
        taskToBeArchived.setLastFinishTime(LocalDateTime.of(2019, 9, 30, 1, 0, 30).toInstant(ZoneOffset.UTC));
        taskToBeArchived.setLastStartTime(LocalDateTime.of(2019, 9, 30, 0, 0, 30).toInstant(ZoneOffset.UTC));


        insertSchedule(taskToBeArchived);

        var newSchedule = List.of(
                new ScheduleRecord().setName("name2").setParam("").setScheduleHashSum("hash_new2").setNameHashSum(
                        "nameHash")
        );

        storage.setNewSchedule(newSchedule);

        var idToScheduleRecord = getIdScheduleMap();

        assertThat(idToScheduleRecord).containsKey("name1_param");
        var taskWithStatusRunningAfterUpdate = idToScheduleRecord.get("name1_param");
        assertRecordsEquals(taskWithStatusRunningAfterUpdate, taskToBeArchived, Set.of("status", "version"));
        assertThat(taskWithStatusRunningAfterUpdate.getVersion()).isEqualTo(NEW_VERSION);
        assertThat(taskWithStatusRunningAfterUpdate.getStatus()).isEqualTo("Deleted");
    }

    /**
     * Тест проверяет, что при обновлении расписания статус needReschedule не сброится на false
     */
    @Test
    void setNewSchedule_NeedRescheduleNotResetTest() {

        var needRescheduleTask = new ScheduledTasksRecord();
        needRescheduleTask.setId("name1_param");
        needRescheduleTask.setName("name1");
        needRescheduleTask.setParams("param");
        needRescheduleTask.setStatus("New");
        needRescheduleTask.setNeedReschedule(false);
        needRescheduleTask.setHeartbeatTime(LocalDateTime.of(2019, 9, 30, 1, 2).toInstant(ZoneOffset.UTC));
        needRescheduleTask.setNextRun(LocalDateTime.of(2019, 9, 30, 1, 2, 30).toInstant(ZoneOffset.UTC));
        needRescheduleTask.setScheduleHash("hash_old1");
        needRescheduleTask.setNeedReschedule(true);
        needRescheduleTask.setVersion(OLD_VERSION);
        needRescheduleTask.setLastFinishTime(LocalDateTime.of(2019, 9, 30, 1, 0, 30).toInstant(ZoneOffset.UTC));
        needRescheduleTask.setLastStartTime(LocalDateTime.of(2019, 9, 30, 0, 0, 30).toInstant(ZoneOffset.UTC));

        var needRescheduleButNotChangedTask = new ScheduledTasksRecord();
        needRescheduleButNotChangedTask.setId("name2_");
        needRescheduleButNotChangedTask.setName("name2");
        needRescheduleButNotChangedTask.setParams("");
        needRescheduleButNotChangedTask.setStatus("New");
        needRescheduleButNotChangedTask.setNeedReschedule(true);
        needRescheduleButNotChangedTask.setHeartbeatTime(LocalDateTime.of(2019, 9, 30, 1, 2).toInstant(ZoneOffset.UTC));
        needRescheduleButNotChangedTask.setNextRun(LocalDateTime.of(2019, 9, 30, 1, 2, 30).toInstant(ZoneOffset.UTC));
        needRescheduleButNotChangedTask.setScheduleHash("hash");
        needRescheduleButNotChangedTask.setNeedReschedule(true);
        needRescheduleButNotChangedTask.setVersion(OLD_VERSION);
        needRescheduleButNotChangedTask.setLastFinishTime(LocalDateTime.of(2019, 9, 30, 1, 0, 30).toInstant(ZoneOffset.UTC));
        needRescheduleButNotChangedTask.setLastStartTime(LocalDateTime.of(2019, 9, 30, 0, 0, 30).toInstant(ZoneOffset.UTC));

        insertSchedule(needRescheduleTask, needRescheduleButNotChangedTask);

        var newSchedule = List.of(
                new ScheduleRecord().setName("name1").setParam("param").setScheduleHashSum("hash_new2").setNameHashSum(
                        "nameHash"),
                new ScheduleRecord().setName("name2").setParam("").setScheduleHashSum("hash").setNameHashSum(
                        "nameHash2")
        );

        storage.setNewSchedule(newSchedule);

        var idToScheduleRecord = getIdScheduleMap();

        assertThat(idToScheduleRecord).hasSize(2);
        assertThat(idToScheduleRecord).containsKeys("name1_param", "name2_");
        var needRescheduleTaskGot = idToScheduleRecord.get("name1_param");
        assertRecordsEquals(needRescheduleTaskGot, needRescheduleTask, Set.of("schedule_hash", "version"));
        assertThat(needRescheduleTaskGot.getScheduleHash()).isEqualTo("hash_new2");
        assertThat(needRescheduleTaskGot.getVersion()).isEqualTo(NEW_VERSION);

        /* needReschedule не должет сброситься */
        var needRescheduleButNotChangedTaskGot = idToScheduleRecord.get("name2_");
        assertRecordsEquals(needRescheduleButNotChangedTaskGot, needRescheduleButNotChangedTaskGot,
                Set.of("version"));
        assertThat(needRescheduleButNotChangedTaskGot.getVersion()).isEqualTo(NEW_VERSION);
    }

    @Test
    void setNewSchedule_UnArchiveTaskTest() {

        var taskToBeUnarchived = new ScheduledTasksRecord();
        taskToBeUnarchived.setId("name1_param");
        taskToBeUnarchived.setName("name1");
        taskToBeUnarchived.setParams("param");
        taskToBeUnarchived.setStatus("Deleted");
        taskToBeUnarchived.setNeedReschedule(false);
        taskToBeUnarchived.setHeartbeatTime(LocalDateTime.of(2019, 9, 30, 1, 2).toInstant(ZoneOffset.UTC));
        taskToBeUnarchived.setNextRun(LocalDateTime.of(2019, 9, 30, 1, 2, 30).toInstant(ZoneOffset.UTC));
        taskToBeUnarchived.setScheduleHash("hash_old1");
        taskToBeUnarchived.setVersion(OLD_VERSION);
        taskToBeUnarchived.setLastFinishTime(LocalDateTime.of(2019, 9, 30, 1, 0, 30).toInstant(ZoneOffset.UTC));
        taskToBeUnarchived.setLastStartTime(LocalDateTime.of(2019, 9, 30, 0, 0, 30).toInstant(ZoneOffset.UTC));


        insertSchedule(taskToBeUnarchived);

        var newSchedule = List.of(
                new ScheduleRecord().setName("name1").setParam("param").setScheduleHashSum("hash_new").setNameHashSum(
                        "nameHash")
        );

        storage.setNewSchedule(newSchedule);

        var idToScheduleRecord = getIdScheduleMap();

        assertThat(idToScheduleRecord).containsKey("name1_param");
        var taskWithStatusRunningAfterUpdate = idToScheduleRecord.get("name1_param");
        assertRecordsEquals(taskWithStatusRunningAfterUpdate, taskToBeUnarchived, Set.of("status",
                "version", "schedule_hash", "need_reschedule"));
        assertThat(taskWithStatusRunningAfterUpdate.getVersion()).isEqualTo(NEW_VERSION);
        assertThat(taskWithStatusRunningAfterUpdate.getStatus()).isEqualTo("New");
        assertThat(taskWithStatusRunningAfterUpdate.getScheduleHash()).isEqualTo("hash_new");
        assertThat(taskWithStatusRunningAfterUpdate.getNeedReschedule()).isEqualTo(true);
    }

    /**
     *
     */
    @Test
    void setNewSchedule_InsertSeveralTimesNewTask() {

        var newSchedule = List.of(
                new ScheduleRecord().setName("name1").setParam("param").setScheduleHashSum("hash").setNameHashSum(
                        "nameHash")
        );

        storage.setNewSchedule(newSchedule);

        var idToScheduleRecord = getIdScheduleMap();

        assertThat(idToScheduleRecord).hasSize(1);
        var id = idToScheduleRecord.keySet().iterator().next();
        var gotRecord = idToScheduleRecord.get(id);
        var expectedRecord = new ScheduledTasksRecord();
        expectedRecord.setId(id);
        expectedRecord.setName("name1");
        expectedRecord.setParams("param");
        expectedRecord.setScheduleHash("hash");
        expectedRecord.setNeedReschedule(true);
        expectedRecord.setVersion(NEW_VERSION);
        expectedRecord.setStatus("New");

        assertRecordsEquals(gotRecord, expectedRecord, Set.of());
    }

    private void assertRecordsEquals(ScheduledTasksRecord actual,
                                     ScheduledTasksRecord expected, Set<String> fieldsNotEquals) {
        for (var column : scheduledTaskColumns) {
            var colName = column.getColumnName();
            if (!fieldsNotEquals.contains(colName)) {
                assertThat(actual.get(colName)).as(colName + " fields expected to be equal").isEqualTo(expected.get(colName));
            }
        }
    }

    private class ScheduledTasksRecord {
        private Map<String, Object> record = new HashMap<>();

        private void setId(String id) {
            record.put("id", id);
        }

        private void setName(String name) {
            record.put("name", name);
        }

        private void setParams(String params) {
            record.put("params", params);
        }

        private void setStatus(String status) {
            record.put("status", status);
        }

        private void setNeedReschedule(boolean needReschedule) {
            record.put("need_reschedule", needReschedule);
        }

        private void setHeartbeatTime(Instant heartbeatTime) {
            record.put("heartbeat_time", heartbeatTime);
        }

        private void setNextRun(Instant nextRun) {
            record.put("next_run", nextRun);
        }

        private void setScheduleHash(String scheduleHash) {
            record.put("schedule_hash", scheduleHash);
        }

        private void setInstanceId(String instanceId) {
            record.put("instance_id", instanceId);
        }

        private void setVersion(String version) {
            record.put("version", version);
        }

        private void setLastFinishTime(Instant lastFinishTime) {
            record.put("last_finish_time", lastFinishTime);
        }

        private void setLastStartTime(Instant lastStartTime) {
            record.put("last_start_time", lastStartTime);
        }

        private void setMeta(String meta) {
            record.put("meta", meta);
        }

        private void set(ResultSetReader reader) {
            record.put("id", reader.getColumn("id").getUtf8());
            record.put("name", reader.getColumn("name").getUtf8());
            record.put("params", reader.getColumn("params").getUtf8());
            record.put("status", reader.getColumn("status").getUtf8());
            record.put("need_reschedule", reader.getColumn("need_reschedule").getBool());
            record.put("heartbeat_time", getOptionalDateTime(reader, "heartbeat_time"));
            record.put("next_run", getOptionalDateTime(reader, "next_run"));
            record.put("schedule_hash", reader.getColumn("schedule_hash").getUtf8());
            record.put("instance_id", getOptionalUtf8(reader, "instance_id"));
            record.put("version", reader.getColumn("version").getUtf8());
            record.put("last_finish_time", getOptionalDateTime(reader, "last_finish_time"));
            record.put("last_start_time", getOptionalDateTime(reader, "last_start_time"));
            record.put("meta", getOptionalUtf8(reader, "meta"));

        }

        private String getOptionalUtf8(ResultSetReader reader, String name) {
            var optionalItem = reader.getColumn(name).getOptionalItem();
            return optionalItem.isOptionalItemPresent() ? optionalItem.getUtf8() : null;
        }

        private Instant getOptionalDateTime(ResultSetReader reader, String name) {
            var optionalItem = reader.getColumn(name).getOptionalItem();
            return optionalItem.isOptionalItemPresent() ? optionalItem.getDatetime().toInstant(ZoneOffset.UTC) :
                    null;
        }

        private Boolean getNeedReschedule() {
            return (Boolean) record.get("need_reschedule");
        }

        private String getVersion() {
            return (String) record.get("version");
        }

        private String getScheduleHash() {
            return (String) record.get("schedule_hash");
        }

        private String getInstanceId() {
            return (String) record.get("instance_id");
        }

        private String getId() {
            return (String) record.get("id");
        }

        private String getMeta() {
            return (String) record.get("meta");
        }

        private String getName() {
            return (String) record.get("name");
        }

        private String getParams() {
            return (String) record.get("params");
        }

        private Instant getHeartbeatTime() {
            return (Instant) record.get("heartbeat_time");
        }

        private Instant getNextRun() {
            return (Instant) record.get("next_run");
        }

        private Instant getLastFinishTime() {
            return (Instant) record.get("last_finish_time");
        }

        private Instant getLastStartTime() {
            return (Instant) record.get("last_start_time");
        }

        private String getStatus() {
            return (String) record.get("status");
        }

        private Object get(String name) {
            return record.get(name);
        }

        @Override
        public String toString() {
            return "ScheduledTasksRecord{" +
                    "record=" + record +
                    '}';
        }
    }

    private void insertSchedule(ScheduledTasksRecord... scheduledTasksRecords) {
        var insertValues = tempTable(new TempTableDescription("values"), SCHEDULED_TASKS.ID, SCHEDULED_TASKS.NAME,
                SCHEDULED_TASKS.PARAMS, SCHEDULED_TASKS.STATUS,
                SCHEDULED_TASKS.NEED_RESCHEDULE,
                SCHEDULED_TASKS.HEARTBEAT_TIME, SCHEDULED_TASKS.NEXT_RUN,
                SCHEDULED_TASKS.SCHEDULE_HASH, SCHEDULED_TASKS.INSTANCE_ID, SCHEDULED_TASKS.LAST_START_TIME,
                SCHEDULED_TASKS.LAST_FINISH_TIME, SCHEDULED_TASKS.META,
                SCHEDULED_TASKS.VERSION).createValues();


        for (var scheduledTasksRecord : scheduledTasksRecords) {
            insertValues.fill(scheduledTasksRecord.getId(), scheduledTasksRecord.getName(),
                    scheduledTasksRecord.getParams(), scheduledTasksRecord.getStatus(),
                    scheduledTasksRecord.getNeedReschedule(), scheduledTasksRecord.getHeartbeatTime(),
                    scheduledTasksRecord.getNextRun(), scheduledTasksRecord.getScheduleHash(),
                    scheduledTasksRecord.getInstanceId(), scheduledTasksRecord.getLastStartTime(),
                    scheduledTasksRecord.getLastFinishTime(), scheduledTasksRecord.getMeta(),
                    scheduledTasksRecord.getVersion());
        }

        var session =
                tableClient.createSession().join().expect("Cannot create session for test " + this.getClass().getName());

        var insertQueryAndParams = upsertInto(SCHEDULED_TASKS)
                .selectAll()
                .from(insertValues)
                .queryAndParams(db);
        session.executeDataQuery(insertQueryAndParams.getQuery(), TxControl.serializableRw().setCommitTx(true),
                insertQueryAndParams.getParams()).join().expect("Cannot insert " +
                "tasks for test ");
    }

    private Map<String, ScheduledTasksRecord> getIdScheduleMap() {
        var selectQueryAndParams =
                select(scheduledTaskColumns.toArray(Column[]::new)).from(SCHEDULED_TASKS).queryAndParams(db);

        return tableClient.createSession().thenApply(
                sessionResult -> sessionResult.expect("Cannot create session"))
                .thenCompose(session -> session.executeDataQuery(selectQueryAndParams.getQuery(),
                        TxControl.serializableRw().setCommitTx(true), selectQueryAndParams.getParams())
                ).thenApply(dataQueryResultResult -> {
                    var resultSet = dataQueryResultResult.expect("failed to select task ids");
                    Map<String, ScheduledTasksRecord> idsToRecords = new HashMap<>();
                    var reader = resultSet.getResultSet(0);
                    while (reader.next()) {
                        var record = new ScheduledTasksRecord();
                        record.set(reader);
                        idsToRecords.put(record.getId(), record);
                    }
                    return idsToRecords;
                }).join();
    }
}
