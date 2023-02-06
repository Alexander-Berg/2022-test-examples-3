package ru.yandex.market.logistic.gateway.client;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.ClientTaskWrapper;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.client.ClientTaskConsumer;
import ru.yandex.market.logistic.gateway.service.client.ClientTaskProducer;
import ru.yandex.market.logistic.gateway.service.flow.FlowService;
import ru.yandex.market.logistic.gateway.service.util.probe.SqsQueueActionProcessingTimeProbe;
import ru.yandex.market.logistic.gateway.service.util.probe.SqsQueueActionProcessingTimeProbeAspect;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

public class ClientTaskConsumerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ClientTaskConsumer clientTaskConsumer;

    @Autowired
    private ClientTaskRepository taskRepository;

    @SpyBean
    private SqsQueueActionProcessingTimeProbeAspect aspect;

    @SpyBean
    private FlowService flowService;

    @MockBean
    private ClientTaskProducer clientTaskProducer;

    @Test
    public void usualBehaviorWorksCorrect() throws Throwable {

        RequestFlow flow = RequestFlow.DS_CREATE_INTAKE;
        String message = "message";

        ClientTaskWrapper clientTaskWrapper = new ClientTaskWrapper(100L, flow, message, null, null, null);
        TaskMessage expectedTaskMessage = new TaskMessage(message);

        clientTaskConsumer.consumeClientTask(clientTaskWrapper, "100");

        verify(aspect).executeWithLogging(any(ProceedingJoinPoint.class), any(SqsQueueActionProcessingTimeProbe.class));
        verify(flowService).startFlow(eq(flow), isNull(), eq(expectedTaskMessage), isNull());

        ClientTask savedTask = taskRepository.findTask(1L);
        softAssert.assertThat(savedTask.getConsumer()).isEqualTo(null);
    }
}
