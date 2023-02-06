package ru.yandex.market.tsum.pipe.engine.runtime.state;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.pipe.engine.definition.stage.Stage;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageGroup;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageRef;
import ru.yandex.market.tsum.pipe.engine.runtime.events.PipeEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.StageGroupChangeEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.state.calculator.commands.LockAndUnlockStageCommand;
import ru.yandex.market.tsum.pipe.engine.runtime.state.calculator.commands.LockStageCommand;
import ru.yandex.market.tsum.pipe.engine.runtime.state.calculator.commands.PipeCommand;
import ru.yandex.market.tsum.pipe.engine.runtime.state.calculator.commands.RemoveFromStageQueueCommand;
import ru.yandex.market.tsum.pipe.engine.runtime.state.calculator.commands.StageGroupCommand;
import ru.yandex.market.tsum.pipe.engine.runtime.state.calculator.commands.UnlockStageCommand;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunchRefImpl;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StageGroupState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.revision.StageGroupStateVersionService;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 27.11.17
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {StageGroupStateCalculatorTest.Config.class, TestMongo.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StageGroupStateCalculatorTest {
    private static final String TESTING = "testing";
    private static final String PRESTABLE = "prestable";
    private static final String STABLE = "stable";
    private static final String STAGE_GROUP_ID = "test-stages";
    private static final String FIRST_PIPE_ID = "first_pipe";
    private static final String SECOND_PIPE_ID = "second_pipe";
    private static final String THIRD_PIPE_ID = "third_pipe";
    private static final String FOURTH_PIPE_ID = "fourth_pipe";

    @Autowired
    private StageGroupDao stageService;

    @Autowired
    private MongoTemplate mongoTemplate;

    private StageGroup stageGroup = new StageGroup(TESTING, PRESTABLE, STABLE);
    private StageGroup rollbackStageGroup = new StageGroup(STABLE);

    private PipeLaunch firstPipeLaunch;
    private PipeLaunch secondPipeLaunch;
    private PipeLaunch thirdPipeLaunch;
    private PipeLaunch rollbackPipeLaunch;

    @Before
    public void setup() {
        mongoTemplate.save(
            new StageGroupState(
                STAGE_GROUP_ID
            )
        );

        firstPipeLaunch = PipeLaunch.builder()
            .withLaunchRef(PipeLaunchRefImpl.create(FIRST_PIPE_ID))
            .withTriggeredBy("someUser")
            .withCreatedDate(new Date())
            .withStagesGroupId("stageGroupId")
            .withStages(stageGroup.getStages())
            .withProjectId("prj")
            .build();

        secondPipeLaunch = PipeLaunch.builder()
            .withLaunchRef(PipeLaunchRefImpl.create(SECOND_PIPE_ID))
            .withTriggeredBy("someUser")
            .withCreatedDate(new Date())
            .withStagesGroupId("stageGroupId")
            .withStages(stageGroup.getStages())
            .withProjectId("prj")
            .build();

        thirdPipeLaunch = PipeLaunch.builder()
            .withLaunchRef(PipeLaunchRefImpl.create(THIRD_PIPE_ID))
            .withTriggeredBy("someUser")
            .withCreatedDate(new Date())
            .withStagesGroupId("stageGroupId")
            .withStages(stageGroup.getStages())
            .withProjectId("prj")
            .build();

        rollbackPipeLaunch = PipeLaunch.builder()
            .withLaunchRef(PipeLaunchRefImpl.create(FOURTH_PIPE_ID))
            .withTriggeredBy("someUser")
            .withCreatedDate(new Date())
            .withStagesGroupId("stageGroupId")
            .withStages(rollbackStageGroup.getStages())
            .withProjectId("prj")
            .build();
    }

    @Test
    public void acquireFreeStage() {

        // act
        List<PipeCommand> commands = unlockAndLockStage(Collections.emptyList(), stageGroup.getStage(TESTING), firstPipeLaunch);

        // assert
        Assert.assertEquals(
            Collections.singletonList(stagesUpdatedEvent(firstPipeLaunch.getIdString())), commands
        );

        assertAcquiredStages(firstPipeLaunch.getIdString(), TESTING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void acquireAlreadyAcquiredStage() {
        // act
        unlockAndLockStage(Collections.emptyList(), stageGroup.getStage(TESTING), firstPipeLaunch);
        unlockAndLockStage(Collections.emptyList(), stageGroup.getStage(TESTING), firstPipeLaunch);
    }

    @Test(expected = IllegalArgumentException.class)
    public void releaseStageThatPipeDoesNotHold() {
        // act
        unlockAndLockStage(
            Collections.singletonList(stageGroup.getStage(TESTING)), stageGroup.getStage(PRESTABLE), firstPipeLaunch
        );
    }

    @Test
    public void acquireBusyStage() {
        unlockAndLockStage(Collections.emptyList(), stageGroup.getStage(TESTING), firstPipeLaunch);

        // act
        List<PipeCommand> commands = unlockAndLockStage(Collections.emptyList(), stageGroup.getStage(TESTING), secondPipeLaunch);

        // assert
        Assert.assertTrue(commands.isEmpty());

        assertAcquiredStages(firstPipeLaunch.getIdString(), TESTING);
        assertNoAcquiredStages(secondPipeLaunch.getIdString());
    }

    @Test
    public void acquireBusyStageWithDifferentStagesPipelines() {
        unlockAndLockStage(Collections.emptyList(), stageGroup.getStage(PRESTABLE), firstPipeLaunch);
        unlockAndLockStage(Collections.emptyList(), stageGroup.getStage(STABLE), firstPipeLaunch);

        // act
        List<PipeCommand> commands = unlockAndLockStage(Collections.emptyList(), stageGroup.getStage(STABLE), rollbackPipeLaunch);

        // assert
        Assert.assertTrue(commands.isEmpty());

        assertAcquiredStages(firstPipeLaunch.getIdString(), PRESTABLE, STABLE);
        assertNoAcquiredStages(rollbackPipeLaunch.getIdString());
    }

    @Test
    public void shouldHoldPreviousStageUntilNextAcquired() {
        unlockAndLockStage(Collections.emptyList(), stageGroup.getStage(PRESTABLE), firstPipeLaunch);
        unlockAndLockStage(Collections.emptyList(), stageGroup.getStage(TESTING), secondPipeLaunch);

        // act
        unlockAndLockStage(singletonList(stageGroup.getStage(TESTING)), stageGroup.getStage(PRESTABLE), secondPipeLaunch);

        // assert
        assertAcquiredStages(firstPipeLaunch.getIdString(), PRESTABLE);
        assertAcquiredStages(secondPipeLaunch.getIdString(), TESTING);
        StageGroupState stageGroupState = stageGroupState();
        Assert.assertEquals(PRESTABLE, stageGroupState.getQueueItem(secondPipeLaunch.getIdString()).get().getDesiredStageId());
    }

    @Test
    public void removePipeFromQueueWhenFinished() {
        unlockAndLockStage(Collections.emptyList(), stageGroup.getStage(TESTING), firstPipeLaunch);
        unlockAndLockStage(Collections.emptyList(), stageGroup.getStage(TESTING), secondPipeLaunch);

        // act
        execCommand(new RemoveFromStageQueueCommand(firstPipeLaunch));

        // assert
        assertAcquiredStages(secondPipeLaunch.getIdString(), TESTING);
        StageGroupState stageGroupState = stageGroupState();
        Assert.assertFalse(stageGroupState.getQueueItem(firstPipeLaunch.getIdString()).isPresent());
    }

    @Test
    public void skipsStagesOnFlag() {
        unlockAndLockStage(Collections.emptyList(), stageGroup.getStage(TESTING), firstPipeLaunch);
        unlockAndLockStage(Collections.emptyList(), stageGroup.getStage(STABLE), secondPipeLaunch, true);
        unlockAndLockStage(Collections.emptyList(), stageGroup.getStage(STABLE), thirdPipeLaunch, true);

        // assert
        assertAcquiredStages(secondPipeLaunch.getIdString(), STABLE);
        Assert.assertEquals(thirdPipeLaunch.getIdString(), stageGroupState().getQueue().get(1).getPipeLaunchId());
    }

    private void assertCommandsAreSame(PipeCommand expected, PipeCommand actual) {
        Assert.assertEquals(expected.getPipeLaunchId(), actual.getPipeLaunchId());
        PipeEvent expectedEvent = expected.getEvent();
        PipeEvent actualEvent = actual.getEvent();
        Assert.assertEquals(expectedEvent.getClass(), actualEvent.getClass());
        Assert.assertEquals(expectedEvent, actualEvent);
    }

    private void assertStageRefsAreSame(StageRef expected, StageRef actual) {
        assertEquals(expected.getId(), actual.getId());
    }

    private void assertNoAcquiredStages(String pipeLaunchId) {
        Assert.assertEquals(
            Collections.emptySet(),
            stageGroupState().getQueueItem(pipeLaunchId).get().getAcquiredStageIds()
        );
    }

    private void assertAcquiredStages(String pipeLaunchId, String... stages) {
        Assert.assertEquals(
            Sets.newHashSet(stages),
            stageGroupState().getQueueItem(pipeLaunchId).get().getAcquiredStageIds()
        );
    }

    private List<PipeCommand> unlockAndLockStage(List<StageRef> stagesToUnlock, StageRef desiredStage,
                                                 PipeLaunch pipeLaunch) {
        return execCommand(new LockAndUnlockStageCommand(stagesToUnlock, desiredStage, pipeLaunch, false));
    }

    private List<PipeCommand> unlockAndLockStage(List<StageRef> stagesToUnlock, StageRef desiredStage,
                                                 PipeLaunch pipeLaunch, boolean skipStagesAllowed) {
        return execCommand(new LockAndUnlockStageCommand(stagesToUnlock, desiredStage, pipeLaunch, skipStagesAllowed));
    }

    private List<PipeCommand> unlockStage(Stage stageToUnlock, PipeLaunch pipeLaunch) {
        return execCommand(new UnlockStageCommand(stageToUnlock, pipeLaunch));
    }

    private Collection<? extends PipeCommand> lockStage(StageRef stage, PipeLaunch pipeLaunch) {
        return execCommand(new LockStageCommand(stage, pipeLaunch, false));
    }

    private List<PipeCommand> execCommand(StageGroupCommand command) {
        StageGroupState stageGroupState = stageGroupState();
        List<PipeCommand> commands = command.execute(stageGroupState);
        stageService.trySave(stageGroupState);
        return commands;
    }

    private PipeCommand stagesUpdatedEvent(String pipeLaunchId) {
        return new PipeCommand(pipeLaunchId, StageGroupChangeEvent.INSTANCE);
    }

    private StageGroupState stageGroupState() {
        return stageService.get(STAGE_GROUP_ID);
    }

    @Configuration
    public static class Config {
        @Autowired
        private MongoTemplate mongoTemplate;

        @Bean
        public StageGroupDao stageService() {
            return new StageGroupDao(
                mongoTemplate, mock(StageGroupStateVersionService.class)
            );
        }
    }

}