package ru.yandex.direct.hourglass.ydb.schedulerinstances;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.yandex.ydb.table.TableClient;
import com.yandex.ydb.table.transaction.TxControl;
import org.assertj.core.data.TemporalUnitOffset;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.hourglass.InstanceId;
import ru.yandex.direct.hourglass.client.SchedulerInstance;
import ru.yandex.direct.hourglass.implementations.InstanceIdImpl;
import ru.yandex.direct.hourglass.implementations.updateschedule.SchedulerInstanceImpl;
import ru.yandex.direct.hourglass.ydb.YdbInfoHolder;
import ru.yandex.direct.ydb.YdbPath;
import ru.yandex.direct.ydb.table.temptable.TempTableDescription;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.hourglass.ydb.storage.Tables.SCHEDULER_INSTANCES;
import static ru.yandex.direct.ydb.builder.querybuilder.DeleteBuilder.deleteFrom;
import static ru.yandex.direct.ydb.builder.querybuilder.InsertBuilder.upsertInto;
import static ru.yandex.direct.ydb.builder.querybuilder.SelectBuilder.select;
import static ru.yandex.direct.ydb.table.temptable.TempTable.tempTable;

class YdbScheduleInstancesRepositoryTest {

    private static final Duration heartbeatExpiration = Duration.ofDays(120);
    private static final Duration instanceExpiration = Duration.ofDays(1000);
    private static final TemporalUnitOffset OFFSET = new TemporalUnitWithinOffset(1, ChronoUnit.MINUTES);
    private static final Instant goodHeartbeatTime = Instant.now().minusSeconds(15).truncatedTo(SECONDS);
    private static TableClient tableClient;
    private YdbScheduleInstancesRepository schedulerInstancesRepository;
    private static YdbPath db;

    @BeforeAll
    static void initDb() {
        var ydbInfo = YdbInfoHolder.getYdbInfo();
        tableClient = ydbInfo.getClient();
        db = ydbInfo.getDb();
    }

    @BeforeEach
    void before() {
        schedulerInstancesRepository = new YdbScheduleInstancesRepository(tableClient, db, heartbeatExpiration,
                1, instanceExpiration);
        clearRepository();
    }

    /**
     * Тест проверяет, что если неосновной инстанс помечен как основной - у него поменяется значение поля isMain
     * У остальных записей ничего не поменяется
     */
    @Test
    void markInstanceAsMainTest_isMainChanged() {
        var instanceIds = new ArrayList<InstanceId>();

        for (var i = 0; i < 3; i++) {
            instanceIds.add(new InstanceIdImpl());
        }

        var instanceInfoToBeChanged = new SchedulerInstancesInfo(instanceIds.get(1), goodHeartbeatTime, false, "13");
        var instancesInfoNotToBeChanged = List.of(
                new SchedulerInstancesInfo(instanceIds.get(0), goodHeartbeatTime, false, "12"),
                new SchedulerInstancesInfo(instanceIds.get(2), goodHeartbeatTime, true, "13"));
        insertSchedulerInstances(List.of(instanceInfoToBeChanged));

        insertSchedulerInstances(instancesInfoNotToBeChanged);

        schedulerInstancesRepository.markInstanceAsMain(instanceInfoToBeChanged.instanceId);

        var schedulerInstancesInfoChangedGot =
                selectSchedulerInstances(List.of(instanceInfoToBeChanged.instanceId.toString()));

        assertThat(schedulerInstancesInfoChangedGot).hasSize(1);
        assertThat(schedulerInstancesInfoChangedGot.get(0).isMain).isEqualTo(true);

        var schedulerInstancesInfoNotChangedGot =
                selectSchedulerInstances(instancesInfoNotToBeChanged.stream().map(i -> i.instanceId.toString())
                        .collect(Collectors.toList()));

        assertThat(schedulerInstancesInfoNotChangedGot).hasSize(2);
        assertThat(schedulerInstancesInfoNotChangedGot)
                .containsExactlyInAnyOrder(instancesInfoNotToBeChanged.toArray(SchedulerInstancesInfo[]::new));
    }

    /**
     * Тест проверяет, что если основной инстанс помечен как неосновной - у него поменяется значение поля isMain
     * У остальных записей ничего не поменяется
     */
    @Test
    void markInstanceAsNotMainTest() {
        var instanceIds = new ArrayList<InstanceId>();

        for (var i = 0; i < 3; i++) {
            instanceIds.add(new InstanceIdImpl());
        }

        var instanceInfoToBeChanged = new SchedulerInstancesInfo(instanceIds.get(1), goodHeartbeatTime, true, "13");
        var instancesInfoNotToBeChanged = List.of(
                new SchedulerInstancesInfo(instanceIds.get(0), goodHeartbeatTime, false, "12"),
                new SchedulerInstancesInfo(instanceIds.get(2), goodHeartbeatTime, true, "13"));
        insertSchedulerInstances(List.of(instanceInfoToBeChanged));

        insertSchedulerInstances(instancesInfoNotToBeChanged);

        schedulerInstancesRepository.markInstanceAsNotMain(instanceInfoToBeChanged.instanceId);

        var schedulerInstancesInfoChangedGot =
                selectSchedulerInstances(List.of(instanceInfoToBeChanged.instanceId.toString()));

        assertThat(schedulerInstancesInfoChangedGot).hasSize(1);
        assertThat(schedulerInstancesInfoChangedGot.get(0).isMain).isEqualTo(false);

        var schedulerInstancesInfoNotChangedGot =
                selectSchedulerInstances(instancesInfoNotToBeChanged.stream().map(i -> i.instanceId.toString())
                        .collect(Collectors.toList()));

        assertThat(schedulerInstancesInfoNotChangedGot).hasSize(2);
        assertThat(schedulerInstancesInfoNotChangedGot)
                .containsExactlyInAnyOrder(instancesInfoNotToBeChanged.toArray(SchedulerInstancesInfo[]::new));
    }

    /**
     * Тест проверяет, что если в таблице есть версия, у которой строк с полем isMain = 1 больше чем у всех
     * остальных, то она будет лидером, а остальные нет
     */
    @Test
    void isLeaderTest() {
        var instanceIds = new ArrayList<InstanceId>();

        for (var i = 0; i < 3; i++) {
            instanceIds.add(new InstanceIdImpl());
        }
        var leaderVersionCandidate = "13";
        var notLeaderVersionCandidate = "12";
        var instancesInfoToInsert = List.of(
                new SchedulerInstancesInfo(instanceIds.get(0), goodHeartbeatTime, true, leaderVersionCandidate),
                new SchedulerInstancesInfo(instanceIds.get(1), goodHeartbeatTime, true, notLeaderVersionCandidate),
                new SchedulerInstancesInfo(instanceIds.get(2), goodHeartbeatTime, true, leaderVersionCandidate));

        insertSchedulerInstances(instancesInfoToInsert);
        var isLeader = schedulerInstancesRepository.isLeaderVersion(leaderVersionCandidate);
        var isNotLeader = schedulerInstancesRepository.isLeaderVersion(notLeaderVersionCandidate);

        assertThat(isLeader).isTrue();
        assertThat(isNotLeader).isFalse();
    }

    /**
     * Тест проверяет, если версии нет в списке, то она не лидирующая
     */
    @Test
    void isLeaderTest_EmptyCandidates() {
        var isLeader = schedulerInstancesRepository.isLeaderVersion("1");

        assertThat(isLeader).isFalse();
    }

    /**
     * Тест проверяет, что если две версии имеют одинкаовое количество строк с полем isMain=1, то ни она из них не
     * лидирующая
     */
    @Test
    void isLeaderTest_TwoPossibleCandidates() {
        var instanceIds = new ArrayList<InstanceId>();
        for (var i = 0; i < 4; i++) {
            instanceIds.add(new InstanceIdImpl());
        }
        var leaderVersionCandidate1 = "13";
        var leaderVersionCandidate2 = "12";
        var i = 0;
        var instancesInfoToInsert = List.of(
                new SchedulerInstancesInfo(instanceIds.get(i++), goodHeartbeatTime, true,
                        leaderVersionCandidate1),
                new SchedulerInstancesInfo(instanceIds.get(i++), goodHeartbeatTime, true, leaderVersionCandidate2),
                new SchedulerInstancesInfo(instanceIds.get(i++), goodHeartbeatTime, true, leaderVersionCandidate1),
                new SchedulerInstancesInfo(instanceIds.get(i), goodHeartbeatTime, true, leaderVersionCandidate2));

        insertSchedulerInstances(instancesInfoToInsert);
        var isLeader1 = schedulerInstancesRepository.isLeaderVersion(leaderVersionCandidate1);
        var isLeader2 = schedulerInstancesRepository.isLeaderVersion(leaderVersionCandidate2);

        assertThat(isLeader1).isFalse();
        assertThat(isLeader2).isFalse();
    }

    /**
     * Тест проверяет, строки, у которых поле isMain = true, но heartbeatTime очень старый, будут игнорироваться
     */
    @Test
    void isLeader_ExpiredHeartbeatIsSkippedTest() {
        var instanceIds = new ArrayList<InstanceId>();

        for (var i = 0; i < 5; i++) {
            instanceIds.add(new InstanceIdImpl());
        }

        var badHeartbeatTime = goodHeartbeatTime.minus(heartbeatExpiration);
        var leaderVersionCandidate = "13";
        var notLeaderVersionCandidate = "12";
        var instancesInfoToInsert = List.of(
                new SchedulerInstancesInfo(instanceIds.get(0), goodHeartbeatTime, true, leaderVersionCandidate),
                new SchedulerInstancesInfo(instanceIds.get(1), badHeartbeatTime, true, notLeaderVersionCandidate),
                new SchedulerInstancesInfo(instanceIds.get(2), goodHeartbeatTime, true, leaderVersionCandidate),
                new SchedulerInstancesInfo(instanceIds.get(3), badHeartbeatTime, true, notLeaderVersionCandidate),
                new SchedulerInstancesInfo(instanceIds.get(4), goodHeartbeatTime, true, notLeaderVersionCandidate));

        insertSchedulerInstances(instancesInfoToInsert);
        var isLeader = schedulerInstancesRepository.isLeaderVersion(leaderVersionCandidate);
        var isNotLeader = schedulerInstancesRepository.isLeaderVersion(notLeaderVersionCandidate);

        assertThat(isLeader).isTrue();
        assertThat(isNotLeader).isFalse();
    }

    /**
     * Тест проверяет, что строки с полем isMain=0 не участвуют в выборе лидера
     */
    @Test
    void isLeaderTest_OneIsNotMain() {
        var instanceIds = new ArrayList<InstanceId>();

        for (var i = 0; i < 4; i++) {
            instanceIds.add(new InstanceIdImpl());
        }
        var leaderVersionMain = "13";
        var leaderVersionNotMain = "12";
        var i = 0;
        var instancesInfoToInsert = List.of(
                new SchedulerInstancesInfo(instanceIds.get(i++), goodHeartbeatTime, true, leaderVersionMain),
                new SchedulerInstancesInfo(instanceIds.get(i++), goodHeartbeatTime, false, leaderVersionNotMain),
                new SchedulerInstancesInfo(instanceIds.get(i++), goodHeartbeatTime, true, leaderVersionMain),
                new SchedulerInstancesInfo(instanceIds.get(i), goodHeartbeatTime, true, leaderVersionNotMain));

        insertSchedulerInstances(instancesInfoToInsert);
        var isLeader1 = schedulerInstancesRepository.isLeaderVersion(leaderVersionMain);
        var isLeader2 = schedulerInstancesRepository.isLeaderVersion(leaderVersionNotMain);

        assertThat(isLeader1).isTrue();
        assertThat(isLeader2).isFalse();
    }

    /**
     * Тест проверяет, что если есть протухшие инстансы, они удалятся при выборе лидера
     */
    @Test
    void isLeaderTest_ExpiredInstancesRemovedTest() {
        var instanceIds = new ArrayList<InstanceId>();

        for (var i = 0; i < 2; i++) {
            instanceIds.add(new InstanceIdImpl());
        }
        var i = 0;
        var heartbeatTimeToDeleteInstance = Instant.now().minus(instanceExpiration).minus(1, DAYS);
        var heartbeatTimeToStayInstance = Instant.now().minus(instanceExpiration).plus(1, DAYS);
        var instancesInfoToInsert = List.of(
                new SchedulerInstancesInfo(instanceIds.get(i++), heartbeatTimeToDeleteInstance, true, ""),
                new SchedulerInstancesInfo(instanceIds.get(i), heartbeatTimeToStayInstance, true, ""));

        insertSchedulerInstances(instancesInfoToInsert);
        schedulerInstancesRepository.isLeaderVersion("");

        var gotSchedulerInstancesInfo = selectSchedulerInstances(List.of(instanceIds.get(0).toString(),
                instanceIds.get(1).toString()));

        assertThat(gotSchedulerInstancesInfo).hasSize(1);
        assertThat(gotSchedulerInstancesInfo.get(0).instanceId).isEqualTo(instanceIds.get(1));
    }

    /**
     * Тест проверяет, что если в таблице есть версия, у которой строк с полем isMain = 1 больше чем у всех
     * остальных, но это значение не превышает минимальный порог, то она не будет лидером
     */
    @Test
    void isLeader_LoaderBorderNotReachedTest() {
        var instanceIds = new ArrayList<InstanceId>();
        var ydbSchedulerInstancesRepository = YdbScheduleInstancesRepository.builder(tableClient, db)
                .withHeartbeatExpiration(heartbeatExpiration)
                .withInstancesExpiration(instanceExpiration)
                .withLeaderVotingLowerBound(3)
                .build();

        for (var i = 0; i < 5; i++) {
            instanceIds.add(new InstanceIdImpl());
        }
        var leaderVersionCandidate = "13";
        int i = 0;
        var instancesInfoToInsert = List.of(
                new SchedulerInstancesInfo(instanceIds.get(i++), goodHeartbeatTime, true, leaderVersionCandidate),
                new SchedulerInstancesInfo(instanceIds.get(i), goodHeartbeatTime, true, leaderVersionCandidate));

        insertSchedulerInstances(instancesInfoToInsert);
        var isLeader = ydbSchedulerInstancesRepository.isLeaderVersion(leaderVersionCandidate);

        assertThat(isLeader).isFalse();
    }

    /**
     * Тест проверяет, что если в таблице есть версия, у которой строк с полем isMain = 1 больше чем у всех
     * остальных и это значение превышает минимальный порог, то она будет лидером, а остальные нет
     */
    @Test
    void isLeader_LoaderBorderReachedTest() {
        var instanceIds = new ArrayList<InstanceId>();
        var ydbSchedulerInstancesRepository = YdbScheduleInstancesRepository.builder(tableClient, db)
                .withHeartbeatExpiration(heartbeatExpiration)
                .withInstancesExpiration(instanceExpiration)
                .withLeaderVotingLowerBound(2)
                .build();

        for (var i = 0; i < 3; i++) {
            instanceIds.add(new InstanceIdImpl());
        }
        var leaderVersionCandidate = "13";
        var notLeaderVersionCandidate = "12";
        int i = 0;
        var instancesInfoToInsert = List.of(
                new SchedulerInstancesInfo(instanceIds.get(i++), goodHeartbeatTime, true, leaderVersionCandidate),
                new SchedulerInstancesInfo(instanceIds.get(i++), goodHeartbeatTime, true, notLeaderVersionCandidate),
                new SchedulerInstancesInfo(instanceIds.get(i), goodHeartbeatTime, true, leaderVersionCandidate));

        insertSchedulerInstances(instancesInfoToInsert);
        var isLeader = ydbSchedulerInstancesRepository.isLeaderVersion(leaderVersionCandidate);
        var isNotLeader = ydbSchedulerInstancesRepository.isLeaderVersion(notLeaderVersionCandidate);

        assertThat(isLeader).isTrue();
        assertThat(isNotLeader).isFalse();
    }

    /**
     * Тест проверяет, что если в таблице есть версия, у которой строк с полем isMain = 1 больше чем у всех
     * остальных и это значение превышает минимальный порог, но номер этой версии не максимальный,
     * то она будет лидером, а остальные нет
     */
    @Test
    void isLeader_LeaderWithLessVersionTest() {
        var instanceIds = new ArrayList<InstanceId>();
        var ydbSchedulerInstancesRepository = YdbScheduleInstancesRepository.builder(tableClient, db)
                .withHeartbeatExpiration(heartbeatExpiration)
                .withInstancesExpiration(instanceExpiration)
                .withLeaderVotingLowerBound(2)
                .build();

        for (var i = 0; i < 3; i++) {
            instanceIds.add(new InstanceIdImpl());
        }
        var leaderVersionCandidate = "12";
        var notLeaderVersionCandidate = "13";
        int i = 0;
        var instancesInfoToInsert = List.of(
                new SchedulerInstancesInfo(instanceIds.get(i++), goodHeartbeatTime, true, leaderVersionCandidate),
                new SchedulerInstancesInfo(instanceIds.get(i++), goodHeartbeatTime, true, notLeaderVersionCandidate),
                new SchedulerInstancesInfo(instanceIds.get(i), goodHeartbeatTime, true, leaderVersionCandidate));

        insertSchedulerInstances(instancesInfoToInsert);
        var isLeader = ydbSchedulerInstancesRepository.isLeaderVersion(leaderVersionCandidate);
        var isNotLeader = ydbSchedulerInstancesRepository.isLeaderVersion(notLeaderVersionCandidate);

        assertThat(isLeader).isTrue();
        assertThat(isNotLeader).isFalse();
    }

    @Test
    void getSchedulerInstancesInfoTest() {
        var instanceIds = new ArrayList<InstanceId>();
        var now = Instant.now().truncatedTo(SECONDS);
        for (var i = 0; i < 2; i++) {
            instanceIds.add(new InstanceIdImpl());
        }
        var i = 0;
        var heartbeatTime1 = now.minus(1, MINUTES);
        var heartbeatTime2 = now.minus(2, MINUTES);
        var instancesInfoToInsert = List.of(
                new SchedulerInstancesInfo(instanceIds.get(i++), heartbeatTime1, true, "1", "meta1"),
                new SchedulerInstancesInfo(instanceIds.get(i), heartbeatTime2, false, "5", "meta2"));

        insertSchedulerInstances(instancesInfoToInsert);
        var gotSchedulerInstances = schedulerInstancesRepository.getSchedulerInstancesInfo();

        var expectedSchedulerInstances = new SchedulerInstance[]{
                new SchedulerInstanceImpl(instanceIds.get(0), "1", true, heartbeatTime1,
                        "meta1", true),
                new SchedulerInstanceImpl(instanceIds.get(1), "5", false, heartbeatTime2,
                        "meta2", true),
        };

        assertThat(gotSchedulerInstances).hasSize(2);
        assertThat(gotSchedulerInstances).containsExactlyInAnyOrder(expectedSchedulerInstances);
    }

    /**
     * Тест проверяет, что поле isActive заполняется верно
     */
    @Test
    void IsActiveTest() {
        var instanceIds = new ArrayList<InstanceId>();

        for (var i = 0; i < 2; i++) {
            instanceIds.add(new InstanceIdImpl(i + ""));
        }

        var i = 0;

        var oldHeartbeat = Instant.now().minus(instanceExpiration);
        var freshHeartbeat = Instant.now().plus(1, DAYS);

        var instancesInfoToInsert = List.of(
                new SchedulerInstancesInfo(instanceIds.get(i++), oldHeartbeat, true, ""),
                new SchedulerInstancesInfo(instanceIds.get(i), freshHeartbeat, true, ""));

        insertSchedulerInstances(instancesInfoToInsert);

        List<SchedulerInstance> schedulerInstances = schedulerInstancesRepository.getSchedulerInstancesInfo();

        assertThat(schedulerInstances).hasSize(2);

        var firstInstance =
                schedulerInstances.stream().filter(e -> e.getInstanceId().toString().equals("0")).findFirst();
        var secondInstance =
                schedulerInstances.stream().filter(e -> e.getInstanceId().toString().equals("1")).findFirst();

        assertThat(firstInstance).isPresent();
        assertThat(secondInstance).isPresent();

        assertThat(firstInstance.orElseThrow().isActive()).isFalse();
        assertThat(secondInstance.orElseThrow().isActive()).isTrue();
    }

    @Test
    void getPingInstanceTest() {
        var instanceIds = new ArrayList<InstanceId>();
        var now = Instant.now().truncatedTo(SECONDS);
        for (var i = 0; i < 2; i++) {
            instanceIds.add(new InstanceIdImpl());
        }
        var i = 0;
        var heartbeatTime1 = now.minus(2, DAYS);
        var heartbeatTime2 = now.minus(1, DAYS);
        var instancesInfoToInsert = List.of(
                new SchedulerInstancesInfo(instanceIds.get(i++), heartbeatTime1, true, "1", "meta1"),
                new SchedulerInstancesInfo(instanceIds.get(i), heartbeatTime2, false, "5", "meta2"));

        insertSchedulerInstances(instancesInfoToInsert);
        schedulerInstancesRepository.pingInstance(instanceIds.get(0));

        var schedulerInstanceIdToInfo = selectSchedulerInstances(List.of(instanceIds.get(0).toString(),
                instanceIds.get(1).toString()))
                .stream()
                .collect(toMap(schedulerInstanceInfo -> schedulerInstanceInfo.instanceId,
                        schedulerInstanceInfo -> schedulerInstanceInfo));

        var changedSchedulerInstanceInfo = schedulerInstanceIdToInfo.get(instanceIds.get(0));
        assertThat(changedSchedulerInstanceInfo.heartbeatTime).isCloseTo(now, OFFSET);
        assertThat(changedSchedulerInstanceInfo.isMain).isEqualTo(true);
        assertThat(changedSchedulerInstanceInfo.version).isEqualTo("1");
        assertThat(changedSchedulerInstanceInfo.meta).isEqualTo("meta1");
        assertThat(schedulerInstanceIdToInfo.get(instanceIds.get(1))).isEqualTo(instancesInfoToInsert.get(1));
    }

    private void insertSchedulerInstances(List<SchedulerInstancesInfo> schedulerInstancesInfoList) {
        var insertValues = tempTable(new TempTableDescription("values"), SCHEDULER_INSTANCES.INSTANCE_ID,
                SCHEDULER_INSTANCES.HEARTBEAT_TIME, SCHEDULER_INSTANCES.IS_MAIN,
                SCHEDULER_INSTANCES.VERSION, SCHEDULER_INSTANCES.META).createValues();
        for (var schedulerInstancesInfo : schedulerInstancesInfoList) {
            insertValues.fill(schedulerInstancesInfo.instanceId.toString(), schedulerInstancesInfo.heartbeatTime,
                    schedulerInstancesInfo.isMain, schedulerInstancesInfo.version, schedulerInstancesInfo.meta);
        }

        var insertQueryAndParams = upsertInto(SCHEDULER_INSTANCES)
                .selectAll()
                .from(insertValues)
                .queryAndParams(db);

        var session = tableClient.createSession().join().expect("Failed to create session for insert scheduler " +
                "instances");

        session.executeDataQuery(insertQueryAndParams.getQuery(), TxControl.serializableRw().setCommitTx(true),
                insertQueryAndParams.getParams()).join().expect("Failed to insert scheduler instances");
    }

    private List<SchedulerInstancesInfo> selectSchedulerInstances(List<String> instanceIds) {
        var session = tableClient.createSession().join().expect("Failed to create session for select scheduler " +
                "instances");
        var queryAndParams = select(SCHEDULER_INSTANCES.INSTANCE_ID, SCHEDULER_INSTANCES.HEARTBEAT_TIME,
                SCHEDULER_INSTANCES.IS_MAIN, SCHEDULER_INSTANCES.VERSION, SCHEDULER_INSTANCES.META)
                .from(SCHEDULER_INSTANCES)
                .where(SCHEDULER_INSTANCES.INSTANCE_ID.in(instanceIds))
                .queryAndParams(db);

        return session.executeDataQuery(queryAndParams.getQuery(), TxControl.serializableRw().setCommitTx(true),
                queryAndParams.getParams())
                .thenApply(resultSet -> {
                    var reader = resultSet.expect("Cannot select scheduler instances").getResultSet(0);
                    List<SchedulerInstancesInfo> schedulerInstancesInfoList = new ArrayList<>();
                    while (reader.next()) {
                        schedulerInstancesInfoList.add(
                                new SchedulerInstancesInfo(
                                        new InstanceIdImpl(reader.getColumn("instance_id").getUtf8()),
                                        reader.getColumn("heartbeat_time").getDatetime().toInstant(ZoneOffset.UTC),
                                        reader.getColumn("is_main").getBool(),
                                        reader.getColumn("version").getUtf8(),
                                        reader.getColumn("meta").getUtf8()));
                    }
                    return schedulerInstancesInfoList;
                })
                .join();
    }

    private void clearRepository() {
        var className = this.getClass().getName();
        var session = tableClient.createSession().join().expect("Cannot create session for test " + className);
        var deleteQueryAndParams = deleteFrom(SCHEDULER_INSTANCES).queryAndParams(db);
        session.executeDataQuery(deleteQueryAndParams.getQuery(), TxControl.serializableRw().setCommitTx(true),
                deleteQueryAndParams.getParams()).join().expect("Cannot" +
                " truncate table before  test " + className);
        session.close();
    }

    private static class SchedulerInstancesInfo {
        InstanceId instanceId;
        Instant heartbeatTime;
        boolean isMain;
        String version;
        String meta;

        SchedulerInstancesInfo(InstanceId instanceId, Instant heartbeatTime, boolean isMain, String version) {
            this(instanceId, heartbeatTime, isMain, version, "");
        }

        SchedulerInstancesInfo(InstanceId instanceId, Instant heartbeatTime, boolean isMain, String version,
                               String meta) {
            this.instanceId = instanceId;
            this.heartbeatTime = heartbeatTime;
            this.isMain = isMain;
            this.version = version;
            this.meta = meta;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SchedulerInstancesInfo that = (SchedulerInstancesInfo) o;
            return isMain == that.isMain &&
                    Objects.equals(instanceId, that.instanceId) &&
                    Objects.equals(heartbeatTime, that.heartbeatTime) &&
                    Objects.equals(version, that.version) &&
                    Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(instanceId, heartbeatTime, isMain, version, meta);
        }

        @Override
        public String toString() {
            return "SchedulerInstancesInfo{" +
                    "instanceId=" + instanceId +
                    ", heartbeatTime=" + heartbeatTime +
                    ", isMain=" + isMain +
                    ", version='" + version + '\'' +
                    ", meta='" + meta + '\'' +
                    '}';
        }
    }
}
