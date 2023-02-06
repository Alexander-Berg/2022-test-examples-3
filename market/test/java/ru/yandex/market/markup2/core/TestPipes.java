package ru.yandex.market.markup2.core;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.config.TaskConfigState;
import ru.yandex.market.markup2.entries.group.TaskConfigGroupInfo;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.task.TaskState;
import ru.yandex.market.markup2.entries.type.TaskTypeInfo;
import ru.yandex.market.markup2.processors.task.ProgressStatus;
import ru.yandex.market.markup2.processors.task.TaskProgressState;
import ru.yandex.market.markup2.processors.task.TaskStatus;
import ru.yandex.market.markup2.workflow.generation.RequestsGenerator;
import ru.yandex.market.markup2.workflow.pipe.Pipe;
import ru.yandex.market.markup2.workflow.pipe.Pipes;
import ru.yandex.market.markup2.workflow.requestSender.RequestsSender;
import ru.yandex.market.markup2.workflow.responseReceiver.ResponsesReceiver;
import ru.yandex.market.markup2.workflow.resultMaker.ResultsMaker;
import ru.yandex.market.markup2.workflow.taskDataUnique.TaskDataUniqueStrategy;
import ru.yandex.market.markup2.workflow.taskFinalizer.TaskFinalizer;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static ru.yandex.market.markup2.processors.task.TaskProgressState.GENERATION;
import static ru.yandex.market.markup2.processors.task.TaskProgressState.PROCESS_RESULT;
import static ru.yandex.market.markup2.processors.task.TaskProgressState.RECEIVE_RESPONSE;
import static ru.yandex.market.markup2.processors.task.TaskProgressState.SEND_REQUEST;
import static ru.yandex.market.markup2.processors.task.TaskProgressState.TASK_FINALIZATION;
/**
 * @author york
 * @since 24.10.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class TestPipes {
    private int idSeq = 1;

    private RequestsGeneratorMock requestsGenerator;
    private RequestsSenderMock requestsSender;
    private ResponsesReceiverMock responsesReceiver;
    private ResultMakerMock resultMaker;
    private TaskFinalizerMock taskFinalizer;

    private Map<TaskProgressState, BiFunction<Pipe, TaskInfo, Boolean>> canStatesMapping;
    private Map<TaskProgressState, BiFunction<Pipe, TaskInfo, Boolean>> shouldStatesMapping;

    private interface ITestStepProcessor {
        TaskStatus.Builder verifyRequestsStatus(TaskInfo taskInfo, TaskStatus.Builder progrees,
                                                AtomicBoolean nextScheduled);

    }

    class RequestsGeneratorMock extends RequestsGenerator implements ITestStepProcessor {
        @Override
        public TaskStatus.Builder verifyRequestsStatus(TaskInfo taskInfo, TaskStatus.Builder progrees,
                                                        AtomicBoolean nextScheduled) {
            nextScheduled.set(false);
            IntConsumer ic = isScheduleNextStep(taskInfo) ? i -> nextScheduled.set(true) : i -> { };
            return verifyRequestsProgress(taskInfo, progrees, ic);
        }
    }
    class RequestsSenderMock extends RequestsSender implements ITestStepProcessor {
        @Override
        public TaskStatus.Builder verifyRequestsStatus(TaskInfo taskInfo, TaskStatus.Builder progrees,
                                                       AtomicBoolean nextScheduled) {
            nextScheduled.set(false);
            IntConsumer ic = isScheduleNextStep(taskInfo) ? i -> nextScheduled.set(true) : i -> { };
            return verifyRequestsProgress(taskInfo, progrees, ic);
        }
    }
    class ResponsesReceiverMock extends ResponsesReceiver implements ITestStepProcessor {
        @Override
        public TaskStatus.Builder verifyRequestsStatus(TaskInfo taskInfo, TaskStatus.Builder progrees,
                                                       AtomicBoolean nextScheduled) {
            nextScheduled.set(false);
            IntConsumer ic = isScheduleNextStep(taskInfo) ? i -> nextScheduled.set(true) : i -> { };
            return verifyRequestsProgress(taskInfo, progrees, ic);
        }
    }
    class ResultMakerMock extends ResultsMaker implements ITestStepProcessor {
        @Override
        public TaskStatus.Builder verifyRequestsStatus(TaskInfo taskInfo, TaskStatus.Builder progrees,
                                                       AtomicBoolean nextScheduled) {
            nextScheduled.set(false);
            IntConsumer ic = isScheduleNextStep(taskInfo) ? i -> nextScheduled.set(true) : i -> { };
            return verifyRequestsProgress(taskInfo, progrees, ic);
        }
    }
    class TaskFinalizerMock extends TaskFinalizer implements ITestStepProcessor {
        @Override
        public TaskStatus.Builder verifyRequestsStatus(TaskInfo taskInfo, TaskStatus.Builder progrees,
                                                       AtomicBoolean nextScheduled) {
            nextScheduled.set(false);
            IntConsumer ic = isScheduleNextStep(taskInfo) ? i -> nextScheduled.set(true) : i -> { };
            return verifyRequestsProgress(taskInfo, progrees, ic);
        }
    }


    @Before
    public void setUp() {
        canStatesMapping = ImmutableMap.of(
            GENERATION, (pipe, i) -> pipe.canGenerateRequests(i),
            SEND_REQUEST, (pipe, i) -> pipe.canSendRequests(i),
            RECEIVE_RESPONSE, (pipe, i) -> pipe.canReceiveResponses(i),
            PROCESS_RESULT, (pipe, i) -> pipe.canMakeResult(i),
            TASK_FINALIZATION, (pipe, i) -> pipe.canFinalizeTask(i)
        );
        shouldStatesMapping = ImmutableMap.of(
            GENERATION, (pipe, i) -> pipe.shouldGenerateRequestsNow(i),
            SEND_REQUEST, (pipe, i) -> pipe.shouldSendRequestsNow(i),
            RECEIVE_RESPONSE, (pipe, i) -> pipe.shouldReceiveResponsesNow(i),
            PROCESS_RESULT, (pipe, i) -> pipe.shouldMakeResultNow(i),
            TASK_FINALIZATION, (pipe, i) -> pipe.shouldFinalizeTaskNow(i)
        );
        requestsGenerator = new RequestsGeneratorMock();
        requestsSender = new RequestsSenderMock();
        responsesReceiver = new ResponsesReceiverMock();
        resultMaker = new ResultMakerMock();
        taskFinalizer = new TaskFinalizerMock();
    }

    private TaskInfo taskInfo(Pipe pipe) {
        TaskTypeInfo taskTypeInfo = new TaskTypeInfo(idSeq++, "dummy", "hitman-mitman", 1, 1,
            pipe, TaskDataUniqueStrategy.TYPE_CATEGORY);

        TaskConfigGroupInfo groupConfigInfo = new TaskConfigGroupInfo.Builder()
            .setCategoryId(1000)
            .setTypeInfo(taskTypeInfo)
            .setId(idSeq++)
            .build();
        TaskConfigInfo taskConfigInfo = new TaskConfigInfo.Builder()
            .setCount(10)
            .setGroupInfo(groupConfigInfo)
            .setId(idSeq++)
            .setState(TaskConfigState.ACTIVE)
            .build();
        TaskInfo taskInfo = new TaskInfo.Builder()
            .setId(idSeq++)
            .setConfig(taskConfigInfo)
            .setState(TaskState.RUNNING)
            .build();

        return taskInfo;
    }

    @Test
    public void testSequential() throws Exception {
        Pipe pipe = Pipes.SEQUENTIALLY;
        TaskInfo taskInfo = taskInfo(pipe);
        AtomicBoolean nextScheduled = new AtomicBoolean();

        checkCanAndShould(taskInfo, GENERATION);

        updateState(
            requestsGenerator,
            taskInfo,
            p -> p.incGeneratedCount(1)
                 .incGeneratedEntitiesCount(1),
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());
        checkCanAndShould(taskInfo, GENERATION);

        updateState(
            requestsGenerator,
            taskInfo,
            p -> p.incGeneratedCount(taskInfo.getConfig().getCount() - 1)
                .incGeneratedEntitiesCount(taskInfo.getConfig().getCount() - 1),
            nextScheduled);
        Assert.assertTrue(nextScheduled.get());
        checkCanAndShould(taskInfo, SEND_REQUEST);

        updateState(
            requestsSender,
            taskInfo,
            p ->  p.incSendedCount(1),
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());
        checkCanAndShould(taskInfo, new TaskProgressState[]{SEND_REQUEST, RECEIVE_RESPONSE},
            new TaskProgressState[]{SEND_REQUEST, RECEIVE_RESPONSE});

        updateState(
            requestsSender,
            taskInfo,
            p ->  p.incSendedCount(p.getGeneratedCount() - 1),
            nextScheduled);
        Assert.assertTrue(nextScheduled.get());
        checkCanAndShould(taskInfo, RECEIVE_RESPONSE);

        updateState(
            responsesReceiver,
            taskInfo,
            p ->  p.incReceivedCount(1),
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());
        checkCanAndShould(taskInfo, RECEIVE_RESPONSE);

        updateState(
            responsesReceiver,
            taskInfo,
            p ->  p.incReceivedCount(p.getSentCount() - 1),
            nextScheduled);
        Assert.assertTrue(nextScheduled.get());
        checkCanAndShould(taskInfo, PROCESS_RESULT);

        updateState(
            resultMaker,
            taskInfo,
            p -> p.incFailedProcessingCount(1)
                  .incCannotCount(2)
                  .incProcessedCount(p.getReceivedCount() - 3),
            nextScheduled);
        Assert.assertTrue(nextScheduled.get());
        checkCanAndShould(taskInfo, TASK_FINALIZATION);

        updateState(
            taskFinalizer,
            taskInfo,
            p -> {
                p.setTaskFinalizationStatus(ProgressStatus.FINISHED);
                taskInfo.setState(TaskState.COMPLETED);
            },
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());
        checkCanAndShould(taskInfo, new TaskProgressState[0], new TaskProgressState[0]);
    }

    @Test
    public void testParallelGenerate() throws Exception {
        Pipe pipe = Pipes.PARALLEL_GENERATE_PROCESS;
        TaskInfo taskInfo = taskInfo(pipe);
        AtomicBoolean nextScheduled = new AtomicBoolean();

        checkCanAndShould(taskInfo, new TaskProgressState[] {GENERATION, SEND_REQUEST, RECEIVE_RESPONSE},
            new TaskProgressState[] {GENERATION});

        updateState(
            requestsGenerator,
            taskInfo,
            p -> p.incGeneratedCount(2)
                .incGeneratedEntitiesCount(2),
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());
        checkCanAndShould(taskInfo, new TaskProgressState[] {GENERATION, SEND_REQUEST, RECEIVE_RESPONSE},
            new TaskProgressState[] {GENERATION, SEND_REQUEST});

        updateState(
            requestsSender,
            taskInfo,
            p -> p.incSendedCount(2),
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());

        updateState(
            requestsGenerator,
            taskInfo,
            p -> p.incGeneratedCount(taskInfo.getConfig().getCount() - p.getGeneratedCount())
                  .incGeneratedEntitiesCount(taskInfo.getConfig().getCount() - p.getGeneratedEntitiesCount()),
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());

        checkCanAndShould(taskInfo, new TaskProgressState[] {SEND_REQUEST, RECEIVE_RESPONSE},
            new TaskProgressState[] {SEND_REQUEST, RECEIVE_RESPONSE});

        updateState(
            requestsSender,
            taskInfo,
            p -> p.incReceivedCount(1),
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());
        checkCanAndShould(taskInfo, new TaskProgressState[]{SEND_REQUEST, RECEIVE_RESPONSE},
            new TaskProgressState[]{SEND_REQUEST, RECEIVE_RESPONSE});

        updateState(
            requestsSender,
            taskInfo,
            p -> p.incSendedCount(p.getGeneratedCount() - p.getSentCount()),
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());
        checkCanAndShould(taskInfo, RECEIVE_RESPONSE);

        updateState(
            responsesReceiver,
            taskInfo,
            p -> p.incReceivedCount(p.getSentCount() - p.getReceivedCount()),
            nextScheduled);
        Assert.assertTrue(nextScheduled.get());
        checkCanAndShould(taskInfo, PROCESS_RESULT);

        updateState(
            resultMaker,
            taskInfo,
            p -> p.incProcessedCount(p.getReceivedCount()),
            nextScheduled);
        Assert.assertTrue(nextScheduled.get());
        checkCanAndShould(taskInfo, TASK_FINALIZATION);

        updateState(
            taskFinalizer,
            taskInfo,
            p -> {
                p.setTaskFinalizationStatus(ProgressStatus.FINISHED);
                taskInfo.setState(TaskState.COMPLETED);
            },
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());
        checkCanAndShould(taskInfo, new TaskProgressState[0], new TaskProgressState[0]);
    }

    @Test
    public void testFullyParallel() throws Exception {
        Pipe pipe = Pipes.FULLY_PARALLEL;
        TaskInfo taskInfo = taskInfo(pipe);
        AtomicBoolean nextScheduled = new AtomicBoolean();

        checkCanAndShould(taskInfo, new TaskProgressState[] {GENERATION, SEND_REQUEST,
                RECEIVE_RESPONSE, PROCESS_RESULT},
            new TaskProgressState[] {GENERATION});

        updateState(
            requestsGenerator,
            taskInfo,
            p -> p.incGeneratedCount(1)
                  .incGeneratedEntitiesCount(1),
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());
        checkCanAndShould(taskInfo, new TaskProgressState[] {GENERATION, SEND_REQUEST,
                RECEIVE_RESPONSE, PROCESS_RESULT},
            new TaskProgressState[] {GENERATION, SEND_REQUEST});

        updateState(
            requestsSender,
            taskInfo,
            p -> p.incSendedCount(2),
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());

        updateState(
            requestsGenerator,
            taskInfo,
            p -> p.incGeneratedCount(taskInfo.getConfig().getCount() - 1)
                  .incGeneratedEntitiesCount(taskInfo.getConfig().getCount() - 1),
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());

        checkCanAndShould(taskInfo, new TaskProgressState[] {SEND_REQUEST, RECEIVE_RESPONSE, PROCESS_RESULT},
            new TaskProgressState[] {SEND_REQUEST, RECEIVE_RESPONSE});

        updateState(
            responsesReceiver,
            taskInfo,
            p -> p.incReceivedCount(1),
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());
        checkCanAndShould(taskInfo, new TaskProgressState[]{SEND_REQUEST, RECEIVE_RESPONSE, PROCESS_RESULT},
            new TaskProgressState[]{SEND_REQUEST, RECEIVE_RESPONSE, PROCESS_RESULT});

        updateState(
            requestsSender,
            taskInfo,
            p -> p.incSendedCount(p.getGeneratedCount() - p.getSentCount()),
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());
        checkCanAndShould(taskInfo, new TaskProgressState[]{RECEIVE_RESPONSE, PROCESS_RESULT},
            new TaskProgressState[]{RECEIVE_RESPONSE, PROCESS_RESULT});

        updateState(
            responsesReceiver,
            taskInfo,
            p -> p.incReceivedCount(p.getSentCount() - p.getReceivedCount()),
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());
        checkCanAndShould(taskInfo, PROCESS_RESULT);

        updateState(
            resultMaker,
            taskInfo,
            p -> p.incProcessedCount(p.getReceivedCount()),
            nextScheduled);
        Assert.assertTrue(nextScheduled.get());
        checkCanAndShould(taskInfo, TASK_FINALIZATION);

        updateState(
            taskFinalizer,
            taskInfo,
            p -> {
                p.setTaskFinalizationStatus(ProgressStatus.FINISHED);
                taskInfo.setState(TaskState.COMPLETED);
            },
            nextScheduled);
        Assert.assertFalse(nextScheduled.get());
        checkCanAndShould(taskInfo, new TaskProgressState[0], new TaskProgressState[0]);
    }


    private void checkCanAndShould(TaskInfo taskInfo, TaskProgressState state) {
        checkCanAndShould(taskInfo, new TaskProgressState[]{state}, new TaskProgressState[]{state});
    }

    private void checkCanAndShould(TaskInfo taskInfo, TaskProgressState[] can, TaskProgressState[] should) {
        Set<TaskProgressState> cannot = EnumSet.allOf(TaskProgressState.class);
        checkStates(taskInfo, Arrays.asList(can), true, "can", canStatesMapping);
        cannot.removeAll(Arrays.asList(can));
        checkStates(taskInfo, cannot, false, "can", canStatesMapping);

        Set<TaskProgressState> shouldnt = EnumSet.allOf(TaskProgressState.class);
        checkStates(taskInfo, Arrays.asList(should), true, "should", shouldStatesMapping);
        shouldnt.removeAll(Arrays.asList(should));
        checkStates(taskInfo, shouldnt, false, "should", shouldStatesMapping);

    }

    private void checkStates(TaskInfo taskInfo, Collection<TaskProgressState> states,
                                boolean expected, String type,
                                Map<TaskProgressState, BiFunction<Pipe, TaskInfo, Boolean>> mapping) {

        for (TaskProgressState state : states) {
            Assert.assertEquals(type + " " + state, expected, mapping.get(state)
                .apply(taskInfo.getPipe(), taskInfo));
        }
    }

    private void updateState(ITestStepProcessor processor,
                             TaskInfo taskInfo,
                             Consumer<TaskStatus.Builder> tp,
                             AtomicBoolean nextStep) {
        taskInfo.getProgress().updateTaskProgress(p -> {
            tp.accept(p);
            p = processor.verifyRequestsStatus(taskInfo, p, nextStep);
            return p;
        });
    }
}
