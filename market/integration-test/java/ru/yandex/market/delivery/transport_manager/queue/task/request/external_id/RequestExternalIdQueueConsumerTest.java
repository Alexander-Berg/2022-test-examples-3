package ru.yandex.market.delivery.transport_manager.queue.task.request.external_id;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.ShopRequestDetailsDTO;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@DatabaseSetup("/repository/transportation_unit/before/transportation_unit_accepted.xml")
public class RequestExternalIdQueueConsumerTest extends AbstractContextualTest {

    private static final Long REQUEST_ID = 15L;
    private static final String EXTERNAL_ID = "ABC123";

    @Autowired
    private RequestExternalIdQueueConsumer consumer;
    @Autowired
    private FulfillmentWorkflowClientApi ffwfClient;


    @Test
    @ExpectedDatabase(
        value = "/repository/transportation_unit/after/transportation_unit_with_external_id.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testSuccess() {
        Mockito.when(ffwfClient.getRequest(eq(15L)))
            .thenReturn(shopRequestDetailsDTO(REQUEST_ID, EXTERNAL_ID));

        softly.assertThat(consumer.execute(task(REQUEST_ID)))
            .isEqualTo(TaskExecutionResult.finish());
    }

    @Test
    void testErrorRequestNotFound() {
        Mockito.when(ffwfClient.getRequest(any()))
            .thenReturn(null);

        softly.assertThatThrownBy(() -> consumer.execute(task(REQUEST_ID)));
    }

    @Test
    void testErrorNullExternalId() {
        Mockito.when(ffwfClient.getRequest(any()))
            .thenReturn(shopRequestDetailsDTO(REQUEST_ID, null));

        softly.assertThatThrownBy(() -> consumer.execute(task(REQUEST_ID)));
    }


    private ShopRequestDetailsDTO shopRequestDetailsDTO(Long requestId, String externalId) {
        var res = new ShopRequestDetailsDTO();
        res.setServiceRequestId(externalId);
        res.setId(requestId);
        return res;
    }

    private Task<RequestExternalIdQueueDto> task(Long requestId) {
        return Task.<RequestExternalIdQueueDto>builder(new QueueShardId("123"))
            .withPayload(new RequestExternalIdQueueDto(requestId))
            .build();
    }
}
