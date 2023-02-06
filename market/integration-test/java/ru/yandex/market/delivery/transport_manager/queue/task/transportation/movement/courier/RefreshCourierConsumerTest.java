package ru.yandex.market.delivery.transport_manager.queue.task.transportation.movement.courier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.base.QueueRegister;
import ru.yandex.market.delivery.transport_manager.queue.base.TaskExecutionResult;
import ru.yandex.market.delivery.transport_manager.service.movement.courier.MovementCourierRefresher;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

class RefreshCourierConsumerTest extends AbstractContextualTest {
    private RefreshCourierConsumer consumer;

    @Autowired
    private QueueRegister queueRegister;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MovementCourierRefresher courierRefresher;

    @Test
    void executeTask() {
        consumer = new RefreshCourierConsumer(queueRegister, objectMapper, courierRefresher);
        TaskExecutionResult taskExecutionResult = consumer.executeTask(task(1L));

        Mockito.verify(courierRefresher).refresh(1L);

        softly.assertThat(taskExecutionResult).isEqualTo(TaskExecutionResult.finish());
    }

    private static Task<RefreshCourierDto> task(Long stateId) {
        var dto = new RefreshCourierDto();
        dto.setMovementCourierSendingStateId(stateId);

        return Task.<RefreshCourierDto>builder(new QueueShardId("123"))
            .withPayload(dto)
            .build();
    }
}
