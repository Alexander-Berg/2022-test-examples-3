package ru.yandex.market.logistics.logistics4go.utils;

import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.springframework.stereotype.Component;

import ru.yandex.market.logistics.dbqueue.entity.QueueTaskEntity;
import ru.yandex.market.logistics.dbqueue.payload.QueuePayload;
import ru.yandex.market.logistics.dbqueue.repository.QueueTaskRepository;

@Component
@RequiredArgsConstructor
public class QueueTaskChecker {
    private final ObjectMapper objectMapper;
    private final QueueTaskRepository queueTaskRepository;

    public <T extends QueuePayload> void assertAnyTaskWithPayload(
        String queueName,
        T expectedPayload
    ) {
        List<QueueTaskEntity> tasks = queueTaskRepository.findAll();
        String serializedPayload = toString(expectedPayload);
        Assertions.assertThat(tasks)
            .filteredOn(task -> queueName.equals(task.getQueueName()))
            .map(QueueTaskEntity::getPayload)
            .contains(serializedPayload);
    }

    public void assertTasksCount(String queueName, int expectedCount) {
        List<QueueTaskEntity> tasks = queueTaskRepository.findAll();
        Assertions.assertThat(tasks)
            .filteredOn(task -> queueName.equals(task.getQueueName()))
            .hasSize(expectedCount);
    }

    @Nonnull
    private String toString(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            String message = String.format("Unable to write as string, payload: %s", payload);
            throw new RuntimeException(message, e);
        }
    }
}
