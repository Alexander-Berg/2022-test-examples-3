package ru.yandex.market.logistics.lom.jobs.executor;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.producer.trust.CheckTrustBasketProducer;
import ru.yandex.market.logistics.lom.service.trust.TrustPaymentService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.mockito.Mockito.mock;

@DisplayName("Запрос статуса платежа в Балансе")
class CheckTrustBasketExecutorTest extends AbstractContextualTest {

    @Autowired
    private TrustPaymentService trustPaymentService;

    @Autowired
    private CheckTrustBasketProducer checkTrustBasketProducer;

    private final JobExecutionContext context = mock(JobExecutionContext.class);
    private CheckTrustBasketExecutor executor;

    @BeforeEach
    void setup() {
        executor = new CheckTrustBasketExecutor(trustPaymentService, checkTrustBasketProducer);
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/service/trust/before/check_basket.xml")
    void success() {
        executor.doJob(context);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHECK_TRUST_BASKET,
            PayloadFactory.createOrderIdPayload(1L, 1L)
        );
    }

    @Test
    @DisplayName("Платеж в неподдерживаемом статусе")
    @DatabaseSetup("/service/trust/before/check_basket_wrong_status.xml")
    void wrongStatus() {
        executor.doJob(context);
        queueTaskChecker.assertNoQueueTasksCreated();
    }

}
