package ru.yandex.market.logistics.logistics4shops.utils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import ru.yandex.market.logistics.dbqueue.DbQueueProcessor;
import ru.yandex.market.logistics.dbqueue.QueueProcessor;
import ru.yandex.market.logistics.dbqueue.entity.QueueTaskEntity;
import ru.yandex.market.logistics.dbqueue.payload.QueuePayload;
import ru.yandex.market.logistics.dbqueue.repository.QueueTaskRepository;

@Component
@RequiredArgsConstructor
public class QueueTaskChecker {
    private final ObjectMapper objectMapper;
    private final QueueTaskRepository queueTaskRepository;

    @SneakyThrows
    public <T extends QueuePayload> void assertQueueTaskCreatedWithDelay(
        Class<? extends QueueProcessor<T>> queueProcessor,
        T payload,
        Duration delay
    ) {
        String payloadAsString = objectMapper.writeValueAsString(payload);
        String queueName = Optional.ofNullable(AnnotationUtils.findAnnotation(
                queueProcessor,
                DbQueueProcessor.class
            ))
            .map(DbQueueProcessor::value)
            .orElse(null);

        List<QueueTaskEntity> queueTasks =
            queueTaskRepository.findAllByQueueNameAndPayload(queueName, payloadAsString);
        int taskCount = queueTasks.size();

        if (taskCount == 1) {
            QueueTaskEntity queueTask = queueTasks.get(0);
            Assertions.assertThat(Duration.between(queueTask.getCreatedAt(), queueTask.getNextProcessAt()))
                .isEqualTo(delay);
            return;
        }

        String message = String.format(
            "Expected exactly one task with queueType = %s, payload = %s and delay = %s",
            queueName,
            payloadAsString,
            delay
        );

        assertError(taskCount, message);
    }

    private void assertError(Integer taskCount, String message) {
        String allTasksDebugMessage = allTasksDebugMessage();
        if (taskCount < 1) {
            Assertions.fail(message + ", but none found. " + allTasksDebugMessage);
            return;
        }

        Assertions.fail(message + ", but found " + taskCount + ". " + allTasksDebugMessage);
    }

    @Nonnull
    private String allTasksDebugMessage() {
        List<QueueTaskEntity> allTasks = queueTaskRepository.findAll();
        if (allTasks.isEmpty()) {
            return "";
        }
        return allTasks.stream().map(String::valueOf)
            .collect(Collectors.joining("\n", "Other tasks in queue:\n", ""));
    }
}
