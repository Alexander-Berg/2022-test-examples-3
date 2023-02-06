package ru.yandex.market.tsum.release.delivery;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.reactivex.Observable;
import org.apache.curator.framework.CuratorFramework;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.DeliveryMachineEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.pipe.engine.runtime.state.StageGroupDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StageGroupState;
import ru.yandex.market.tsum.release.dao.delivery.DeliveryMachineState;
import ru.yandex.market.tsum.release.dao.delivery.DeliveryMachineStateDao;
import ru.yandex.market.tsum.test_data.TestProjectFactory;
import ru.yandex.market.tsum.test_data.TestStageGroupStateFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 19.02.18
 */
public class DeliveryMachineWatcherTest {
    private static final Logger log = LogManager.getLogger();

    private static final String FIRST_STAGE_GROUP_ID = "first-stage-group-id";
    private static final String SECOND_STAGE_GROUP_ID = "second-stage-group-id";
    private static final String FIRST_PROJECT_ID = "first-some-project";
    private static final String SECOND_PROJECT_ID = "second-some-project";
    private static final String PIPE_ID = "some-pipe";

    private DeliveryMachineWatcher sut;
    private ProjectEntity firstProject;
    private ProjectEntity secondProject;
    private StageGroupDao stageService;
    private DeliveryMachineStateDao deliveryMachineStateDao;
    private CuratorFramework curatorFramework;
    private DeliveryPipelineLauncher pipelineLauncher;
    private ProjectsDao projectsDao;

    @Before
    public void setUp() throws Exception {
        MongoConverter mongoConverter = mock(MongoConverter.class);
        curatorFramework = mock(CuratorFramework.class);
        stageService = mock(StageGroupDao.class);
        deliveryMachineStateDao = mock(DeliveryMachineStateDao.class);
        pipelineLauncher = mock(DeliveryPipelineLauncher.class);

        projectsDao = mock(ProjectsDao.class);
        firstProject = TestProjectFactory.project(FIRST_PROJECT_ID, FIRST_STAGE_GROUP_ID, PIPE_ID);
        secondProject = TestProjectFactory.project(SECOND_PROJECT_ID, SECOND_STAGE_GROUP_ID, PIPE_ID);
        when(projectsDao.stream()).thenReturn(Stream.of(firstProject, secondProject));
        when(projectsDao.get(firstProject.getId())).thenReturn(firstProject);
        when(projectsDao.get(secondProject.getId())).thenReturn(secondProject);

        sut = new DeliveryMachineWatcher(
            curatorFramework, projectsDao, stageService,
            deliveryMachineStateDao, new ComplicatedMonitoring(), pipelineLauncher
        );
    }

    @Test
    public void shouldPassToLauncherOnlyFreshState() {
        // arrange
        State firstState = setupDeliveryMachine(FIRST_STAGE_GROUP_ID);
        State secondState = setupDeliveryMachine(SECOND_STAGE_GROUP_ID);

        // act
        sut.takeLeadership(curatorFramework);
        log.info("Leadership released");

        // assert
        DeliveryMachineEntity deliveryMachineSettings = firstProject.getDeliveryMachine(PIPE_ID);
        Mockito.verify(pipelineLauncher, Mockito.times(1)).maybeTriggerPipelines(
            firstProject, deliveryMachineSettings, firstState.stageGroupState, firstState.machineState
        );

        deliveryMachineSettings = secondProject.getDeliveryMachine(PIPE_ID);
        Mockito.verify(pipelineLauncher, Mockito.times(1)).maybeTriggerPipelines(
            secondProject, deliveryMachineSettings, secondState.stageGroupState, secondState.machineState
        );
    }

    @Test
    public void errorsInOneDeliveryMachineShouldNotAffectOthers() {
        // arrange
        sut.setReloadingObservable(Observable.just(1L));
        State firstState = setupDeliveryMachine(FIRST_STAGE_GROUP_ID);
        State secondState = setupDeliveryMachine(SECOND_STAGE_GROUP_ID);

        when(projectsDao.stream()).thenReturn(
            Stream.of(firstProject), Stream.of(firstProject, secondProject)
        );

        Mockito.doThrow(IllegalStateException.class)
            .when(pipelineLauncher).maybeTriggerPipelines(any(), any(), any(), any());

        // act
        sut.takeLeadership(curatorFramework);

        // assert
        DeliveryMachineEntity deliveryMachineSettings = firstProject.getDeliveryMachine(PIPE_ID);
        Mockito.verify(pipelineLauncher, Mockito.times(1)).maybeTriggerPipelines(
            firstProject, deliveryMachineSettings, firstState.stageGroupState, firstState.machineState
        );

        deliveryMachineSettings = secondProject.getDeliveryMachine(PIPE_ID);
        Mockito.verify(pipelineLauncher, Mockito.times(1)).maybeTriggerPipelines(
            secondProject, deliveryMachineSettings, secondState.stageGroupState, secondState.machineState
        );
    }

    private State setupDeliveryMachine(String stageGroupId) {
        when(stageService.observeVersion(stageGroupId))
            .thenReturn(makeAsync(Observable.just(1L, 2L, 3L)));

        when(deliveryMachineStateDao.observeVersion(stageGroupId))
            .thenReturn(makeAsync(Observable.just(2L, 3L, 4L)));

        DeliveryMachineState machineState = new DeliveryMachineState(stageGroupId, null);
        machineState.setVersion(4L);
        when(deliveryMachineStateDao.getById(stageGroupId)).thenReturn(Optional.of(machineState));

        StageGroupState stageGroupState = TestStageGroupStateFactory.create(stageGroupId);
        stageGroupState.setVersion(3L);
        when(stageService.get(stageGroupId)).thenReturn(stageGroupState);
        return new State(machineState, stageGroupState);
    }

    static class State {
        final DeliveryMachineState machineState;
        final StageGroupState stageGroupState;

        State(DeliveryMachineState machineState, StageGroupState stageGroupState) {
            this.machineState = machineState;
            this.stageGroupState = stageGroupState;
        }
    }

    private <T> Observable<T> makeAsync(Observable<T> observable) {
        return observable.zipWith(Observable.interval(1, TimeUnit.NANOSECONDS), (val, __) -> val);
    }
}
