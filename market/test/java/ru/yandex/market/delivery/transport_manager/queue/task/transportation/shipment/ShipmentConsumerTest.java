package ru.yandex.market.delivery.transport_manager.queue.task.transportation.shipment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.transport_manager.facade.shipment.ShipmentFacade;
import ru.yandex.market.delivery.transport_manager.queue.base.QueueRegister;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ShipmentConsumerTest {
    @Mock
    ShipmentFacade shipmentFacade;
    @Mock
    QueueRegister queueRegister;
    @Mock
    ObjectMapper objectMapper;
    ShipmentConsumer shipmentConsumer;

    @BeforeEach
    void setUp() {
        shipmentConsumer =
            new ShipmentConsumer(
                queueRegister,
                objectMapper,
                shipmentFacade
            );
    }

    @Test
    void executeTest_success() {
        Task.Builder<ShipmentQueueDto> builder = Task.builder(new QueueShardId("1"));

        Task<ShipmentQueueDto> task = builder
            .withPayload(new ShipmentQueueDto(1L))
            .build();

        TaskExecutionResult result = shipmentConsumer.execute(task);
        assertThat(result).isEqualTo(TaskExecutionResult.finish());
    }
}
