package ru.yandex.direct.oneshot.worker;

import java.time.LocalDateTime;
import java.time.Month;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.oneshot.app.OneshotVersionControl;
import ru.yandex.direct.oneshot.configuration.OneshotTest;
import ru.yandex.direct.oneshot.core.entity.oneshot.repository.OneshotLaunchDataRepository;
import ru.yandex.direct.oneshot.core.entity.oneshot.repository.OneshotLaunchRepository;
import ru.yandex.direct.oneshot.core.entity.oneshot.repository.OneshotRepository;
import ru.yandex.direct.oneshot.core.entity.oneshot.repository.testing.TestOneshots;
import ru.yandex.direct.oneshot.core.model.LaunchStatus;
import ru.yandex.direct.oneshot.core.model.OneshotLaunch;

import static org.assertj.core.api.Assertions.assertThat;


@OneshotTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ExecutionWorkerTest {

    @Autowired
    private ExecutionWorker executionWorker;
    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private OneshotRepository oneshotRepository;
    @Autowired
    private OneshotLaunchRepository oneshotLaunchRepository;
    @Autowired
    private OneshotLaunchDataRepository launchDataRepository;

    @Autowired
    private OneshotVersionControl oneshotVersionControl;

    private OneshotLaunch defaultLaunch;

    @Before
    public void before() {
        var oneshot = TestOneshots.defaultOneshot();
        oneshot.setId(oneshotRepository.add(oneshot));
        defaultLaunch = TestOneshots.defaultLaunch(oneshot.getId());
        defaultLaunch.setId(oneshotLaunchRepository.add(defaultLaunch));
    }

    @Test
    public void testUpdateLaunchDataForStart() {
        var launchData = TestOneshots.defaultLaunchData(defaultLaunch.getId())
                .withLaunchStatus(LaunchStatus.READY);
        launchDataRepository.add(launchData);

        assertThat(launchData.getLaunchTime()).isNull();
        assertThat(launchData.getLastActiveTime()).isNull();

        dslContextProvider.ppcdict().transaction(conf ->
                executionWorker.updateLaunchDataForStart(conf.dsl(), launchData));

        var updated = launchDataRepository.get(launchData.getId());

        assertThat(updated.getLaunchStatus()).isEqualTo(LaunchStatus.IN_PROGRESS);
        assertThat(updated.getLastActiveTime()).isNotNull();
        assertThat(updated.getLaunchTime()).isNotNull();
        assertThat(updated.getLaunchedRevisions()).contains(oneshotVersionControl.getCurrentVersion());
    }

    @Test
    public void testSafeOneshotUpdateLaunchDataForStart() {
        var oneshot = TestOneshots.defaultOneshot()
                .withSafeOneshot(true);
        oneshot.setId(oneshotRepository.add(oneshot));
        defaultLaunch = TestOneshots.defaultLaunch(oneshot.getId())
                .withApprover(null)
                .withApprovedRevision(null);
        defaultLaunch.setId(oneshotLaunchRepository.add(defaultLaunch));

        var launchData = TestOneshots.defaultLaunchData(defaultLaunch.getId())
                .withLaunchStatus(LaunchStatus.READY);
        launchDataRepository.add(launchData);

        assertThat(launchData.getLaunchTime()).isNull();
        assertThat(launchData.getLastActiveTime()).isNull();

        dslContextProvider.ppcdict().transaction(conf ->
                executionWorker.updateLaunchDataForStart(conf.dsl(), launchData));

        var updated = launchDataRepository.get(launchData.getId());

        assertThat(updated.getLaunchStatus()).isEqualTo(LaunchStatus.IN_PROGRESS);
        assertThat(updated.getLastActiveTime()).isNotNull();
        assertThat(updated.getLaunchTime()).isNotNull();
        assertThat(updated.getLaunchedRevisions()).contains(oneshotVersionControl.getCurrentVersion());
    }

    @Test
    public void testUpdateLaunchDataForStartPaused() {
        var oldTime = LocalDateTime.of(2000, Month.JANUARY, 1, 0, 0);
        var launchData = TestOneshots.defaultLaunchData(defaultLaunch.getId())
                .withLaunchStatus(LaunchStatus.PAUSED)
                .withLaunchTime(oldTime)
                .withLastActiveTime(oldTime);
        launchDataRepository.add(launchData);

        dslContextProvider.ppcdict().transaction(conf ->
                executionWorker.updateLaunchDataForStart(conf.dsl(), launchData));

        var updated = launchDataRepository.get(launchData.getId());

        assertThat(updated.getLaunchStatus()).isEqualTo(LaunchStatus.IN_PROGRESS);
        assertThat(updated.getLastActiveTime()).isNotEqualTo(oldTime);
        assertThat(updated.getLaunchTime()).isEqualTo(oldTime);
        assertThat(updated.getLaunchedRevisions()).contains(oneshotVersionControl.getCurrentVersion());
    }
}
