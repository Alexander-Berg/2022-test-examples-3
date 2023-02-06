package ru.yandex.market.tpl.core.domain.clientreturn.dbqueue;

import java.time.ZonedDateTime;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.service.sqs.SendClientReturnEventToSqsService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@RequiredArgsConstructor
public class ClientReturnDeliveredToScConsumerTest extends TplAbstractTest {

    private final ClientReturnDeliveredToScConsumer consumer;
    private final ClientReturnGenerator generator;

    @MockBean
    private final SendClientReturnEventToSqsService sendClientReturnEventToSqsService;

    @Test
    void executeTest() {
        ClientReturn clientReturn = generator.generate(198L);
        ClientReturnDeliveredToScPayload payload = new ClientReturnDeliveredToScPayload("1", clientReturn.getId(),
                1651667813, "321");
        Task<ClientReturnDeliveredToScPayload> task = new Task(new QueueShardId("fake-shard-id"), payload, 0,
                ZonedDateTime.now(), null, "actor");

        consumer.execute(task);
        verify(sendClientReturnEventToSqsService).sendSynchronously(
                argThat(arg -> arg.equals("321")),
                Mockito.anyLong(),
                any(),
                Mockito.anyString());
    }
}
