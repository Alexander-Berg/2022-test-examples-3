package ru.yandex.market.mbo.taskqueue;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;


@SuppressWarnings("checkstyle:MagicNumber")
@Transactional
public class TaskQueueRegistratorTest extends BaseTaskQueueTest {

    private static final int SLOT = 0;

    @Autowired
    private TaskQueueRepository taskQueueRepository;

    private TaskQueueHandlerRegistry handlerRegistry;
    private TaskQueueRegistrator taskQueueRegistrator;
    private List<String> taskTypes;

    @Before
    public void setUp() {
        TestTaskHandler taskHandler = new TestTaskHandler();
        ObjectMapper objectMapper = new ObjectMapper();
        handlerRegistry = TaskQueueHandlerRegistry.newBuilder()
            .addGroup(1, taskHandler)
            .buildRegistry();
        taskQueueRegistrator = new TaskQueueRegistrator(taskQueueRepository, objectMapper);
        taskTypes = Collections.singletonList(handlerRegistry.getTaskType(taskHandler));
    }

    @Test
    public void shouldRegisterTaskWithoutLock() {
        TaskQueueTask task = new TestTaskHandler.Task(SLOT, "test");
        taskQueueRegistrator.registerTask(task);
        Optional<TaskRecord> maybeTaskRecord = taskQueueRepository.findNextTask(false, taskTypes);
        assertThat(maybeTaskRecord).isPresent();
        TaskRecord taskRecord = maybeTaskRecord.get();
        assertThat(taskRecord.getTaskType()).isEqualTo(handlerRegistry.getTaskType(task));
        assertThat(taskRecord.getTaskState()).isEqualTo(TaskRecord.TaskState.ACTIVE);
        assertThat(taskRecord.getLockName()).isNull();
    }

    @Test
    public void shouldRegisterTaskWithLock() {
        TaskQueueTask task = new TestTaskHandler.Task(SLOT, "test");
        String lock = "lock";
        taskQueueRegistrator.registerTask(task, lock);
        Optional<TaskRecord> maybeTaskRecord = taskQueueRepository.findNextTask(true, taskTypes);
        assertThat(maybeTaskRecord).isPresent();
        TaskRecord taskRecord = maybeTaskRecord.get();
        assertThat(taskRecord.getTaskType()).isEqualTo(handlerRegistry.getTaskType(task));
        assertThat(taskRecord.getTaskState()).isEqualTo(TaskRecord.TaskState.ACTIVE);
        assertThat(taskRecord.getLockName()).isEqualTo(lock);
    }

    @Test
    public void testRegisterBatchTasks() {
        String lock = "lock";
        List<TaskQueueTask> tasks = IntStream.range(0, 10)
                .mapToObj(id -> new TestTaskHandler.Task(SLOT, "test " + id))
                .collect(Collectors.toList());

        taskQueueRegistrator.registerTasks(tasks, lock);

        List<TaskRecord> actualTasks = taskQueueRepository.findAll();
        assertThat(actualTasks).hasSize(10);
        assertThat(actualTasks).extracting(s -> s.getTaskState()).allMatch(s -> s.equals(TaskRecord.TaskState.ACTIVE));
        assertThat(actualTasks).extracting(s -> s.getLockName()).allMatch(l -> lock.equals(l));
    }

    @Test
    public void testRegisterBatchTasksWithLock() {
        List<TaskQueueTask> tasks = IntStream.range(0, 10)
                .mapToObj(id -> new TestTaskHandler.Task(SLOT, "test " + id))
                .collect(Collectors.toList());

        taskQueueRegistrator.registerTasks(tasks);

        List<TaskRecord> actualTasks = taskQueueRepository.findAll();
        assertThat(actualTasks).hasSize(10);
        assertThat(actualTasks).extracting(s -> s.getTaskState()).allMatch(s -> s.equals(TaskRecord.TaskState.ACTIVE));
        assertThat(actualTasks).extracting(s -> s.getLockName()).allMatch(Objects::isNull);
    }
}
