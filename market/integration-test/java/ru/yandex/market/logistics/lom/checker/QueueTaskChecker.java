package ru.yandex.market.logistics.lom.checker;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.springframework.stereotype.Component;

import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.entity.QueueTask;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.filter.BusinessProcessStateFilter;
import ru.yandex.market.logistics.lom.jobs.model.ExecutionQueueItemPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.BusinessProcessStateRepository;
import ru.yandex.market.logistics.lom.repository.QueueTaskRepository;
import ru.yandex.market.logistics.lom.specification.BusinessProcessStateSpecification;

@Component
@RequiredArgsConstructor
public class QueueTaskChecker {

    private final ObjectMapper objectMapper;
    private final QueueTaskRepository queueTaskRepository;
    private final BusinessProcessStateRepository businessProcessStateRepository;
    private final BusinessProcessStateSpecification businessProcessStateSpecification;

    @SneakyThrows
    public void assertExactlyOneQueueTaskCreated(QueueType queueType) {
        assertQueueTasksCreated(queueType, 1);
    }

    @SneakyThrows
    public void assertQueueTasksCreated(QueueType queueType, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative, count = " + count);
        }
        List<QueueTask> queueTasks = queueTaskRepository.findByQueueType(queueType);
        int taskCount = queueTasks.size();
        if (taskCount == count) {
            return;
        }

        String message = String.format(
            "Expected exactly %d task%s with queueType = %s",
            count,
            count == 1 ? "" : "s",
            queueType
        );

        assertError(taskCount, message);
    }

    @SneakyThrows
    public void assertQueueTaskCreated(QueueType queueType, ExecutionQueueItemPayload payload) {
        assertQueueTaskCreated(queueType, payload, payload.getSequenceId() != null ? payload.getSequenceId() : 1);
    }

    @SneakyThrows
    public void assertQueueTaskCreated(QueueType queueType, ExecutionQueueItemPayload payload, long sequenceId) {
        payload.setSequenceId(sequenceId);
        String payloadAsString = objectMapper.writeValueAsString(payload);

        List<QueueTask> queueTasks = queueTaskRepository.findByQueueTypeAndPayload(queueType, payloadAsString);
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

        List<QueueTask> queueTasks = queueTaskRepository.findByQueueTypeAndPayload(queueType, payloadAsString);
        int taskCount = queueTasks.size();

        if (taskCount == 1) {
            QueueTask queueTask = queueTasks.get(0);
            Assertions.assertThat(Duration.between(queueTask.getCreated(), queueTask.getProcessAfter()))
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

    @SneakyThrows
    public void assertQueueTaskCreatedWithDelay(
        QueueType queueType,
        Duration delay
    ) {
        List<QueueTask> queueTasks = queueTaskRepository.findByQueueType(queueType);
        int taskCount = queueTasks.size();

        if (taskCount == 1) {
            QueueTask queueTask = queueTasks.get(0);
            Assertions.assertThat(Duration.between(queueTask.getCreated(), queueTask.getProcessAfter()))
                .isEqualTo(delay);
            return;
        }

        String message = String.format(
            "Expected exactly one task with queueType = %s and delay = %s",
            queueType,
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
                .filter(queueTask -> !exceptedTasks.contains(queueTask.getQueueType()))
        )
            .as("Expected no tasks in queue")
            .isEmpty();
    }

    @SneakyThrows
    public void assertQueueTaskNotCreated(QueueType queueType, ExecutionQueueItemPayload payload) {
        String payloadAsString = objectMapper.writeValueAsString(payload);
        List<QueueTask> queueTasks = queueTaskRepository.findByQueueTypeAndPayload(queueType, payloadAsString);
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
        List<QueueTask> queueTasks = queueTaskRepository.findByQueueType(queueType);
        assertQueueTaskNotCreated(
            queueTasks,
            () -> String.format(
                "Expected no tasks with queueType = %s",
                queueType
            )
        );
    }

    @Nonnull
    public <T> T getProducedTaskPayload(QueueType queueType, Class<T> payloadClass) {
        return businessProcessStateRepository.findAll(
            businessProcessStateSpecification.fromFilter(
                BusinessProcessStateFilter.builder()
                    .queueTypes(EnumSet.of(queueType))
                    .statuses(EnumSet.of(BusinessProcessStatus.ENQUEUED))
                    .build()
            )
        )
            .stream()
            .findFirst()
            .map(BusinessProcessState::getPayload)
            .map(payload -> parsePayload(payloadClass, payload))
            .orElseThrow();
    }

    @Nonnull
    @SneakyThrows
    private <T> T parsePayload(Class<T> payloadClass, String payload) {
        return objectMapper.readValue(payload, payloadClass);
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
