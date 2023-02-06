package ru.yandex.direct.oneshot.core.entity.oneshot.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.oneshot.core.configuration.OneshotCoreTest;
import ru.yandex.direct.oneshot.core.entity.oneshot.repository.testing.TestOneshots;
import ru.yandex.direct.oneshot.core.model.LaunchStatus;
import ru.yandex.direct.oneshot.core.model.OneshotLaunch;
import ru.yandex.direct.oneshot.core.model.OneshotLaunchData;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@OneshotCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class OneshotLaunchDataRepositoryTest {
    private static final LocalDateTime DEFAULT_LAUNCH_TIME = LocalDateTime.of(2020, 5, 11, 0, 0);
    private static final LocalDateTime DEFAULT_LAST_ACTIVE_TIME = LocalDateTime.of(2020, 5, 12, 0, 0);
    private static final LaunchStatus DEFAULT_STATUS = LaunchStatus.READY;

    @Autowired
    private OneshotRepository oneshotRepository;
    @Autowired
    private OneshotLaunchRepository oneshotLaunchRepository;
    @Autowired
    private OneshotLaunchDataRepository oneshotLaunchDataRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    private OneshotLaunch defaultLaunch;

    private int shardCounter = 1;

    @Before
    public void before() {
        var oneshot = TestOneshots.defaultOneshot();
        oneshot.setId(oneshotRepository.add(oneshot));
        defaultLaunch = TestOneshots.defaultLaunch(oneshot.getId());
        defaultLaunch.setId(oneshotLaunchRepository.add(defaultLaunch));
        shardCounter = 1;
    }

    @Test
    public void testIdUpdatedAfterInsert() {
        OneshotLaunchData launchData = createLaunchData();

        assertThat(launchData.getId()).isNull();
        oneshotLaunchDataRepository.add(launchData);
        assertThat(launchData.getId()).isNotNull();
    }

    @Test
    public void testIdUpdatedAfterInsertList() {
        var launchDatas = List.of(createLaunchData(), createLaunchData(), createLaunchData());

        launchDatas.forEach(launchData -> assertThat(launchData.getId()).isNull());
        oneshotLaunchDataRepository.add(launchDatas);
        launchDatas.forEach(launchData -> assertThat(launchData.getId()).isNotNull());
    }


    @Test
    public void insertAndGet() {
        OneshotLaunchData launchData = new OneshotLaunchData()
                .withLaunchId(defaultLaunch.getId())
                .withLaunchStatus(LaunchStatus.READY)
                .withLaunchTime(LocalDateTime.of(2019, 7, 8, 19, 0))
                .withLaunchedRevisions(Set.of("123", "456", "789"))
                .withShard(1)
                .withSpanId(15L);
        oneshotLaunchDataRepository.add(launchData);
        OneshotLaunchData newLaunchData = oneshotLaunchDataRepository.get(launchData.getId());

        assertThat(newLaunchData)
                .usingRecursiveComparison()
                .isEqualTo(launchData);
    }

    @Test
    public void testUpdateLastActiveTime() {
        var launchDatas = List.of(createLaunchData(), createLaunchData());
        oneshotLaunchDataRepository.add(launchDatas);
        var newTime = DEFAULT_LAST_ACTIVE_TIME.plusHours(1);
        dslContextProvider.
                ppcdictTransaction(conf -> oneshotLaunchDataRepository.updateLastActiveTime(conf.dsl(),
                        launchDatas, newTime));

        assertThat(launchDatas.get(0).getLastActiveTime()).isEqualTo(newTime);
        assertThat(launchDatas.get(1).getLastActiveTime()).isEqualTo(newTime);

        var updated = oneshotLaunchDataRepository.get(mapList(launchDatas, OneshotLaunchData::getId));
        assertThat(updated).hasSize(2);
        assertThat(updated.get(0).getLastActiveTime()).isEqualTo(newTime);
        assertThat(updated.get(1).getLastActiveTime()).isEqualTo(newTime);
    }

    @Test
    public void testFindNotActiveLaunchDatas() {
        var launchDatas = StreamEx.of(LaunchStatus.values()).map(this::createLaunchDataWithStatus).toList();
        oneshotLaunchDataRepository.add(launchDatas);


        var notActiveLaunchDatas = dslContextProvider.ppcdictTransactionResult(conf ->
                oneshotLaunchDataRepository.selectNotActiveLaunchDatasForUpdate(conf.dsl(),
                        DEFAULT_LAST_ACTIVE_TIME.plusSeconds(1)));

        var notActiveLaunchDatasEmpty = dslContextProvider.ppcdictTransactionResult(conf ->
                oneshotLaunchDataRepository.selectNotActiveLaunchDatasForUpdate(conf.dsl(),
                        DEFAULT_LAST_ACTIVE_TIME.minusSeconds(1)));

        assertThat(notActiveLaunchDatasEmpty).isEmpty();
        assertThat(mapList(notActiveLaunchDatas, OneshotLaunchData::getLaunchStatus))
                .containsExactlyInAnyOrder(LaunchStatus.PAUSE_REQUESTED, LaunchStatus.IN_PROGRESS);
    }

    @Test
    public void testPauseLaunchDatas() {
        var launchDatas = List.of(
                createLaunchDataWithStatus(LaunchStatus.PAUSE_REQUESTED),
                createLaunchDataWithStatus(LaunchStatus.IN_PROGRESS)
        );
        oneshotLaunchDataRepository.add(launchDatas);

        dslContextProvider.ppcdictTransaction(conf ->
                oneshotLaunchDataRepository.pauseLaunchDatas(conf.dsl(), launchDatas));

        assertThat(launchDatas.get(0).getLaunchStatus()).isEqualTo(LaunchStatus.PAUSED);
        assertThat(launchDatas.get(1).getLaunchStatus()).isEqualTo(LaunchStatus.PAUSED);

        var updated = oneshotLaunchDataRepository.get(mapList(launchDatas, OneshotLaunchData::getId));
        assertThat(updated).hasSize(2);
        assertThat(updated.get(0).getLaunchStatus()).isEqualTo(LaunchStatus.PAUSED);
        assertThat(updated.get(1).getLaunchStatus()).isEqualTo(LaunchStatus.PAUSED);
    }

    private OneshotLaunchData createLaunchDataWithStatus(LaunchStatus status) {
        return createLaunchData(defaultLaunch).withLaunchStatus(status);
    }

    private OneshotLaunchData createLaunchData() {
        return createLaunchData(defaultLaunch);
    }

    private OneshotLaunchData createLaunchData(OneshotLaunch launch) {
        return new OneshotLaunchData()
                .withLaunchId(launch.getId())
                .withLaunchStatus(DEFAULT_STATUS)
                .withLaunchTime(DEFAULT_LAUNCH_TIME)
                .withLastActiveTime(DEFAULT_LAST_ACTIVE_TIME)
                .withLaunchedRevisions(Set.of("123", "456", "789"))
                .withShard(shardCounter++)
                .withSpanId(15L);
    }
}
