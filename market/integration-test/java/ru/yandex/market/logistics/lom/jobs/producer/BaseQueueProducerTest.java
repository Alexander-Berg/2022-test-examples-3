package ru.yandex.market.logistics.lom.jobs.producer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.DbQueueConfiguration;
import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.exception.LomException;
import ru.yandex.market.logistics.lom.service.process.BusinessProcessStateService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@DisplayName("Тесты на базовый функционал продюсеров тасок для очереди DbQueue")
class BaseQueueProducerTest extends AbstractContextualTest {
    @MockBean(answer = Answers.CALLS_REAL_METHODS)
    BusinessProcessStateService businessProcessStateService;

    @Autowired
    DbQueueConfiguration dbQueueConfiguration;

    @Autowired
    private OrderExternalValidationProducer fakeProducer;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("запись в лог и в очередь - атомарны")
    void atomicEnqueueAndLog() {
        doThrow(new RuntimeException("ooops")).when(businessProcessStateService)
            .save(any(BusinessProcessState.class), anyString());

        softly.assertThatThrownBy(
            // Все продьюсеры вызываются из сервисов и их код исполняется внутри транзакции
            () -> transactionTemplate.execute(
                status -> {
                    fakeProducer.produceTask(123213);
                    return null;
                }
            )
        )
            .isInstanceOf(LomException.class)
            .hasMessageContaining("ooops");

        queueTaskChecker.assertNoQueueTasksCreated();
    }
}
