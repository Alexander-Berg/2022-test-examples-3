package ru.yandex.market.delivery.transport_manager.queue.task.xdoc.submit_date;

import java.time.LocalDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.util.TimeUtil;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.XDocFinalInboundDateDTO;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

@DatabaseSetup({
    "/repository/transportation/xdoc_items_virtual_transportation.xml",
})
public class XDocSubmitInboundDateConsumerTest extends AbstractContextualTest {
    @Autowired
    private FulfillmentWorkflowClientApi fulfillmentWorkflowClient;

    @Autowired
    private XDocSubmitInboundDateConsumer consumer;

    @ExpectedDatabase(
        value = "/repository/transportation_unit/after/xdoc_inbound_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void commitXDocFinalInboundDate() {
        LocalDateTime outboundDate = LocalDateTime.of(2021, 5, 7, 12, 0);
        long ffwfId = 111L;
        XDocSubmitInboundDateDto payload = new XDocSubmitInboundDateDto(ffwfId, 3L, 147L, 172L, outboundDate);
        Task<XDocSubmitInboundDateDto> task = Task.<XDocSubmitInboundDateDto>builder(new QueueShardId("1"))
            .withPayload(payload)
            .build();
        consumer.executeTask(task);

        Mockito.verify(fulfillmentWorkflowClient).commitXDocFinalInboundDate(
            new XDocFinalInboundDateDTO()
                .setShopRequestId(ffwfId)
                .setDate(TimeUtil.getOffsetTimeFromLocalDateTime(outboundDate.plusDays(3)))
        );
        Mockito.verifyNoMoreInteractions(fulfillmentWorkflowClient);
    }

    @ExpectedDatabase(
        value = "/repository/transportation/xdoc_items_virtual_transportation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void commitXDocFinalInboundDateEmptyPayload() {
        Task<XDocSubmitInboundDateDto> task = Task.<XDocSubmitInboundDateDto>builder(new QueueShardId("1"))
            .build();

        softly
            .assertThatThrownBy(() -> consumer.executeTask(task))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Empty dto for submit inbound date for XDoc");

        Mockito.verifyNoMoreInteractions(fulfillmentWorkflowClient);
    }
}
