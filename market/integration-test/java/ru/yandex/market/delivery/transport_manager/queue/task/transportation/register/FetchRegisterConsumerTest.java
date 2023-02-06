package ru.yandex.market.delivery.transport_manager.queue.task.transportation.register;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.util.DbQueueUtils;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;

import static org.mockito.ArgumentMatchers.eq;

class FetchRegisterConsumerTest extends AbstractContextualTest {

    @Autowired
    private FetchRegisterConsumer consumer;

    @Autowired
    private FulfillmentWorkflowClientApi ffwfClient;

    @Test
    @DisplayName("У дефолтного юнита без стратегии улетит запрос в FFWF")
    @DatabaseSetup("/repository/dbqueue/transportation.xml")
    void executeTask() {
        consumer.executeTask(
            DbQueueUtils.createTask(new FetchRegisterQueueDto(1L, true))
        );

        Mockito.verify(ffwfClient).getRegistries(eq(12L));
    }
}
