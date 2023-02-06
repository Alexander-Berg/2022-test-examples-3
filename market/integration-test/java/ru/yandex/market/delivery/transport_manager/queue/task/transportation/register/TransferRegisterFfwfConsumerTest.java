package ru.yandex.market.delivery.transport_manager.queue.task.transportation.register;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.inbound.PutInboundRegisterProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.transfer.ffwf.TransferRegisterFfwfConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.transfer.ffwf.TransferRegisterFfwfDto;
import ru.yandex.market.delivery.transport_manager.util.DbQueueUtils;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// todo: TMDEV-308
public class TransferRegisterFfwfConsumerTest extends AbstractContextualTest {

    public static final long REQUEST_ID = 100L;

    @Autowired
    private TransferRegisterFfwfConsumer consumer;

    @Autowired
    private PutInboundRegisterProducer putInboundRegisterProducer;

    @Test
    @DatabaseSetup("/repository/register/transfer_register.xml")
    @ExpectedDatabase(value = "/repository/register/after/transfer_register.xml", assertionMode =
        DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void successfulTransferRegister() {
        TaskExecutionResult execute = consumer.execute(
            DbQueueUtils.createTask(new TransferRegisterFfwfDto(11L))
        );
        softly.assertThat(execute).isEqualTo(TaskExecutionResult.finish());
        verify(putInboundRegisterProducer).produce(3L);
    }

    @Test
    @DatabaseSetup("/repository/register/transfer_register_null_request_id.xml")
    void noTransferRegisterBecauseOfNullRequestId() {
        TaskExecutionResult execute = consumer.execute(
            DbQueueUtils.createTask(new TransferRegisterFfwfDto(11L))
        );
        softly.assertThat(execute).isEqualTo(TaskExecutionResult.finish());
        verify(putInboundRegisterProducer, never()).produce(any());
    }
}
