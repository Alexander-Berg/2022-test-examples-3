package ru.yandex.market.delivery.transport_manager.queue.task.transportation.register;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.exception.FFWFRestrictionsBrokenException;
import ru.yandex.market.delivery.transport_manager.queue.base.exception.DbQueueTaskExecutionException;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.inbound.PutInboundRegisterConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.inbound.PutInboundRegisterDto;
import ru.yandex.market.delivery.transport_manager.util.DbQueueUtils;
import ru.yandex.market.ff.client.FulfillmentWorkflowAsyncClientApi;
import ru.yandex.market.ff.client.dto.ResourceIdDto;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PutInboundRegisterConsumerTest extends AbstractContextualTest {

    public static final long UNIT_ID = 3L;

    @Autowired
    private PutInboundRegisterConsumer consumer;

    @Autowired
    private FulfillmentWorkflowAsyncClientApi ffwfClient;

    @Test
    @DatabaseSetup("/repository/register/put_inbound_register.xml")
    @ExpectedDatabase(value = "/repository/register/after/put_inbound_register.xml", assertionMode =
        DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void successfulPutInboundRegister() {
        when(ffwfClient.putFfInboundRegistry(any())).thenReturn(ResourceIdDto.builder().yandexId("123").build());

        TaskExecutionResult execute = consumer.execute(
            DbQueueUtils.createTask(new PutInboundRegisterDto(UNIT_ID))
        );
        softly.assertThat(execute).isEqualTo(TaskExecutionResult.finish());

        verify(ffwfClient).putFfInboundRegistry(any());
        verifyNoMoreInteractions(ffwfClient);
    }

    @Test
    @DatabaseSetup("/repository/register/put_inbound_register_incorrect_status.xml")
    @ExpectedDatabase(
        value = "/repository/register/put_inbound_register_incorrect_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void badStatusPutInboundRegister() {
        when(ffwfClient.putFfInboundRegistry(any())).thenReturn(ResourceIdDto.builder().yandexId("123").build());

        softly.assertThatThrownBy(() -> consumer.execute(
                DbQueueUtils.createTask(new PutInboundRegisterDto(UNIT_ID))
            ))
            .isInstanceOf(DbQueueTaskExecutionException.class)
            .hasCauseExactlyInstanceOf(FFWFRestrictionsBrokenException.class);

        verifyNoMoreInteractions(ffwfClient);
    }
}
