package ru.yandex.market.logistics.lrm.utils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.springframework.stereotype.Component;

import ru.yandex.market.logistics.dbqueue.entity.QueueTaskEntity;
import ru.yandex.market.logistics.dbqueue.payload.QueuePayload;
import ru.yandex.market.logistics.dbqueue.repository.QueueTaskRepository;

import static org.assertj.core.api.Assertions.assertThat;

@Component
@RequiredArgsConstructor
@ParametersAreNonnullByDefault
public class QueueTasksChecker {

    private final ObjectMapper objectMapper;

    private final QueueTaskRepository queueTaskRepository;

    @SneakyThrows
    public void assertSingleQueueTaskPayload(String queueName, QueuePayload expectedPayload) {
        checkForQueueTasksAmount(1, queueName);

        QueuePayload actualPayload = objectMapper.readValue(
            queueTaskRepository.findAll().get(0).getPayload(),
            QueuePayload.class
        );
        assertThat(actualPayload).usingRecursiveComparison().ignoringFields("requestId").isEqualTo(expectedPayload);
    }

    public void assertNoQueueTasksCreated(String queueName) {
        checkForQueueTasksAmount(0, queueName);
    }

    @SneakyThrows
    private void checkForQueueTasksAmount(int tasksAmount, String queueName) {
        if (tasksAmount < 0) {
            throw new IllegalArgumentException("amount must not be negative, amount = " + tasksAmount);
        }

        long queueTasksCount = queueTaskRepository.findAll()
            .stream()
            .filter(task -> Objects.equals(task.getQueueName(), queueName))
            .count();
        if (tasksAmount == queueTasksCount) {
            return;
        }

        String message = "Expected exactly %d tasks with queueName = %s".formatted(tasksAmount, queueName);
        assertError(queueTasksCount, message);
    }

    private void assertError(long queueTasksCount, String message) {
        String allTasksDebugMessage = allTasksDebugMessage();
        if (queueTasksCount < 1) {
            Assertions.fail(message + ", but none found. " + allTasksDebugMessage);
            return;
        }

        Assertions.fail(message + ", but found " + queueTasksCount + ". " + allTasksDebugMessage);
    }

    @Nonnull
    private String allTasksDebugMessage() {
        List<QueueTaskEntity> allTasks = queueTaskRepository.findAll();
        if (allTasks.isEmpty()) {
            return "";
        }

        return allTasks.stream()
            .map(String::valueOf)
            .collect(Collectors.joining("\n", "Other tasks in queue:\n", ""));
    }
}
