package ru.yandex.market.delivery.transport_manager.queue.task.transportation.checker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.facade.TransportationUpdateFacade;
import ru.yandex.market.delivery.transport_manager.facade.transportation.TransportationFacade;
import ru.yandex.market.delivery.transport_manager.queue.base.QueueRegister;
import ru.yandex.market.delivery.transport_manager.service.checker.TransportationChecker;
import ru.yandex.market.delivery.transport_manager.service.checker.dto.EnrichedTransportation;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

class TransportationCheckerConsumerTest extends AbstractContextualTest {
    @Autowired
    private QueueRegister queueRegister;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private TransportationChecker checker;

    @Mock
    private TransportationFacade transportationFacade;

    @Mock
    private TransportationUpdateFacade transportationUpdateFacade;

    private TransportationCheckerConsumer consumer;

    @BeforeEach
    void init() {
        doAnswer(args -> {
            Transportation t = args.getArgument(0);
            if (t.getId() == 1) {
                return new EnrichedTransportation().setTransportation(t);
            }
            throw new RuntimeException();
        }).when(checker).check(
            Mockito.any()
        );
        doAnswer(invocation -> new Transportation().setId(invocation.getArgument(0)))
            .when(transportationFacade).getById(Mockito.anyLong());

        consumer = new TransportationCheckerConsumer(
            queueRegister,
            objectMapper,
            checker,
            transportationFacade,
            transportationUpdateFacade
        );
    }

    @Test
    void testSuccessExecution() {
        TaskExecutionResult result = consumer.execute(task(1L));

        verify(checker).check(
            Mockito.argThat((ArgumentMatcher<Transportation>) transportation -> transportation.getId().equals(1L))
        );
        softly.assertThat(result)
            .isEqualTo(TaskExecutionResult.finish());
    }

    @Test
    void testFailedExecution() {
        softly.assertThatThrownBy(() -> consumer.execute(task(2L)));
    }

    private static Task<TransportationCheckerDto> task(Long transportationId) {
        var dto = new TransportationCheckerDto();
        dto.setTransportationId(transportationId);

        return Task.<TransportationCheckerDto>builder(new QueueShardId("123"))
            .withPayload(dto)
            .build();
    }
}
