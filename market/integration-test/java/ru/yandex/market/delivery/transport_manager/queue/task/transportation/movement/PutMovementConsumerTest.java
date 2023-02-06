package ru.yandex.market.delivery.transport_manager.queue.task.transportation.movement;

import com.fasterxml.jackson.databind.ObjectMapper;
import javassist.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.base.QueueRegister;
import ru.yandex.market.delivery.transport_manager.queue.base.exception.DbQueueTaskExecutionException;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.movement.put.PutMovementConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.movement.put.PutMovementQueueDto;
import ru.yandex.market.delivery.transport_manager.service.external.lgw.MovementCreatorService;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class PutMovementConsumerTest extends AbstractContextualTest {
    private PutMovementConsumer putMovementConsumer;

    @Autowired
    private QueueRegister queueRegister;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private MovementCreatorService movementCreatorService;

    @BeforeEach
    void init() throws NotFoundException {
        doNothing().when(movementCreatorService).create(1L);
        doThrow(new NotFoundException("Not found")).when(movementCreatorService).create(2L);
        doThrow(new RuntimeException("Gateway error")).when(movementCreatorService).create(3L);

        putMovementConsumer = new PutMovementConsumer(
            queueRegister,
            objectMapper,
            movementCreatorService
        );
    }

    @Test
    void testSuccessExecution() throws NotFoundException {
        TaskExecutionResult result = putMovementConsumer.execute(task(1L));

        verify(movementCreatorService).create(1L);
        softly.assertThat(result)
            .isEqualTo(TaskExecutionResult.finish());
    }

    @Test
    void testNotFoundExecution() {
        softly.assertThatThrownBy(() -> putMovementConsumer.execute(task(2L)))
            .isInstanceOf(DbQueueTaskExecutionException.class)
            .hasMessage("Not found");
    }

    @Test
    void testGatewayApiExceptionExecution() {
        softly.assertThatThrownBy(() -> putMovementConsumer.execute(task(3L)))
            .isInstanceOf(DbQueueTaskExecutionException.class)
            .hasMessage("Gateway error");
    }

    private static Task<PutMovementQueueDto> task(Long transportationId) {
        var dto = new PutMovementQueueDto();
        dto.setTransportationId(transportationId);

        return Task.<PutMovementQueueDto>builder(new QueueShardId("123"))
            .withPayload(dto)
            .build();
    }
}
