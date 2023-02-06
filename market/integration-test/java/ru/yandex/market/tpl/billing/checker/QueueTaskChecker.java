package ru.yandex.market.tpl.billing.checker;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.billing.model.entity.QueueTask;
import ru.yandex.market.tpl.billing.queue.model.QueueType;
import ru.yandex.market.tpl.billing.repository.QueueTaskRepository;
import ru.yandex.market.tpl.common.db.queue.model.ExecutionQueueItemPayload;

@Component
@RequiredArgsConstructor
public class QueueTaskChecker {

    private final ObjectMapper objectMapper;
    private final QueueTaskRepository queueTaskRepository;

    @SneakyThrows
    public void assertQueueTaskCreated(QueueType queueType, ExecutionQueueItemPayload task) {
        String payloadAsString = objectMapper.writeValueAsString(task);

        List<QueueTask> queueTasks = queueTaskRepository.findByQueueNameAndTask(queueType, payloadAsString);
        int taskCount = queueTasks.size();
        if (taskCount == 1) {
            return;
        }

        String message = String.format(
            "Expected exactly one task with queueType = %s and payload = %s",
            queueType,
            payloadAsString
        );

        assertError(taskCount, message);
    }

    @SneakyThrows
    public void assertQueueTaskCreatedWithDelay(
        QueueType queueType,
        ExecutionQueueItemPayload payload,
        Duration delay
    ) {
        String payloadAsString = objectMapper.writeValueAsString(payload);

        List<QueueTask> queueTasks = queueTaskRepository.findByQueueNameAndTask(queueType, payloadAsString);
        int taskCount = queueTasks.size();

        if (taskCount == 1) {
            QueueTask queueTask = queueTasks.get(0);
            Assertions.assertThat(Duration.between(queueTask.getCreateTime(), queueTask.getProcessTime()))
                .isEqualTo(delay);
            return;
        }

        String message = String.format(
            "Expected exactly one task with queueType = %s, payload = %s and delay = %s",
            queueType,
            payloadAsString,
            delay
        );

        assertError(taskCount, message);
    }

    public void assertNoQueueTasksCreated() {
        Assertions.assertThat(queueTaskRepository.findAll())
            .as("Expected no tasks in queue")
            .isEmpty();
    }

    public void assertNoQueueTasksCreatedExcept(Set<QueueType> exceptedTasks) {
        Assertions.assertThat(
            queueTaskRepository.findAll().stream()
                .filter(queueTask -> !exceptedTasks.contains(queueTask.getQueueName()))
        )
            .as("Expected no tasks in queue")
            .isEmpty();
    }

    @SneakyThrows
    public void assertQueueTaskNotCreated(QueueType queueType, ExecutionQueueItemPayload payload) {
        String payloadAsString = objectMapper.writeValueAsString(payload);
        List<QueueTask> queueTasks = queueTaskRepository.findByQueueNameAndTask(queueType, payloadAsString);
        assertQueueTaskNotCreated(
            queueTasks,
            () -> String.format(
                "Expected no tasks with queueType = %s and payload = %s",
                queueType,
                payloadAsString
            )
        );
    }

    @SneakyThrows
    public void assertQueueTaskNotCreated(QueueType queueType) {
        List<QueueTask> queueTasks = queueTaskRepository.findByQueueName(queueType);
        assertQueueTaskNotCreated(
            queueTasks,
            () -> String.format(
                "Expected no tasks with queueType = %s",
                queueType
            )
        );
    }

    private void assertQueueTaskNotCreated(
        List<QueueTask> queueTasks,
        Supplier<String> messageSupplier
    ) {
        int taskCount = queueTasks.size();

        if (taskCount == 0) {
            return;
        }

        Assertions.fail(messageSupplier.get() + ", but found. " + allTasksDebugMessage());
    }

    private void assertError(Integer taskCount, String message) {
        String allTasksDebugMessage = allTasksDebugMessage();
        if (taskCount < 1) {
            Assertions.fail(message + ", but none found. " + allTasksDebugMessage);
            return;
        }

        Assertions.fail(message + ", but found " + taskCount + ". " + allTasksDebugMessage);
    }

    private String allTasksDebugMessage() {
        List<QueueTask> allTasks = queueTaskRepository.findAll();
        if (allTasks.isEmpty()) {
            return "";
        }
        return allTasks.stream().map(String::valueOf)
            .collect(Collectors.joining("\n", "Other tasks in queue:\n", ""));
    }
}
