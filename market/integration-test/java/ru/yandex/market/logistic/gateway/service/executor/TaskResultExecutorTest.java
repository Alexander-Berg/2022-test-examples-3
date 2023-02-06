package ru.yandex.market.logistic.gateway.service.executor;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.FulfillmentWorkflowConsumerClient;
import ru.yandex.market.logistic.gateway.service.consumer.FulfillmentWorkflowDeliveryConsumerClient;
import ru.yandex.market.logistic.gateway.service.consumer.IrisFulfillmentConsumerClient;
import ru.yandex.market.logistic.gateway.service.consumer.LmsDeliveryConsumerClient;
import ru.yandex.market.logistic.gateway.service.consumer.LoadTestingDeliveryConsumerClient;
import ru.yandex.market.logistic.gateway.service.consumer.LoadTestingFulfillmentConsumerClient;
import ru.yandex.market.logistic.gateway.service.consumer.LomDeliveryConsumerClient;
import ru.yandex.market.logistic.gateway.service.consumer.LomFulfillmentConsumerClient;
import ru.yandex.market.logistic.gateway.service.consumer.MdbDeliveryConsumerClient;
import ru.yandex.market.logistic.gateway.service.consumer.MdbFulfillmentConsumerClient;
import ru.yandex.market.logistic.gateway.service.consumer.TMDeliveryConsumerClient;
import ru.yandex.market.logistic.gateway.service.consumer.TMFulfillmentConsumerClient;
import ru.yandex.market.logistic.gateway.service.consumer.TplDeliveryConsumerClient;
import ru.yandex.market.logistic.gateway.service.consumer.TplFulfillmentConsumerClient;
import ru.yandex.market.logistic.gateway.service.consumer.TrackerDeliveryConsumerClient;
import ru.yandex.market.logistic.gateway.service.consumer.TrackerFulfillmentConsumerClient;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.tpl.client.ff.FulfillmentResponseConsumerClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Часть TaskResultExecutor'ов пушит результирующие данные клиенту, другая часть - нет.
 * Какому клиенту пушить данные - зависит от поля ClientTask.consumer.
 * Тест проверяет, что вызываются методы нужного клиента.
 */
public abstract class TaskResultExecutorTest extends AbstractIntegrationTest {

    private static final long CLIENT_TASK_ID = 1L;

    protected static final String PROCESS_ID = "123";

    @Autowired
    protected ApplicationContext applicationContext;

    @MockBean
    protected ClientTaskRepository clientTaskRepository;

    @MockBean
    private MdbClient mdbClient;

    @MockBean(name = "lomHttpTemplate")
    private HttpTemplate lomHttpClient;

    @MockBean(name = "tmHttpTemplate")
    private HttpTemplate tmHttpTemplate;

    @MockBean(name = "tplRestTemplate")
    private RestTemplate tplRestTemplate;

    @SpyBean
    protected MdbDeliveryConsumerClient mdbDeliveryConsumerClient;

    @SpyBean
    protected MdbFulfillmentConsumerClient mdbFulfillmentConsumerClient;

    @SpyBean
    protected LomDeliveryConsumerClient lomConsumerClient;

    @SpyBean
    protected LomFulfillmentConsumerClient lomFulfillmentConsumerClient;

    @MockBean
    private FulfillmentWorkflowClientApi fulfillmentWorkflowClient;

    @SpyBean
    protected FulfillmentWorkflowConsumerClient fulfillmentWorkflowConsumerClient;

    @SpyBean
    protected FulfillmentWorkflowDeliveryConsumerClient fulfillmentWorkflowDeliveryConsumerClient;

    @SpyBean
    protected LmsDeliveryConsumerClient lmsDeliveryConsumerClient;

    @SpyBean
    protected TrackerDeliveryConsumerClient trackerDeliveryConsumerClient;

    @SpyBean
    protected TrackerFulfillmentConsumerClient trackerFulfillmentConsumerClient;

    @SpyBean
    protected IrisFulfillmentConsumerClient irisFulfillmentConsumerClient;

    @SpyBean
    protected LoadTestingDeliveryConsumerClient loadTestingConsumerClient;

    @SpyBean
    protected LoadTestingFulfillmentConsumerClient loadTestingFulfillmentConsumerClient;

    @MockBean
    private FulfillmentResponseConsumerClient fulfillmentResponseConsumerClient;

    @SpyBean
    protected TplFulfillmentConsumerClient tplFulfillmentConsumerClient;

    @SpyBean
    protected TplDeliveryConsumerClient tplDeliveryConsumerClient;

    @SpyBean
    protected TMDeliveryConsumerClient tmDeliveryConsumerClient;

    @SpyBean
    protected TMFulfillmentConsumerClient tmFulfillmentConsumerClient;

    protected void executeAllAndVerifyExceptionThrown(
        TaskResultConsumer consumer,
        Map<Class<? extends TaskExecutor>, String> executorsMessages,
        Set<Class<? extends TaskExecutor>> executorsNotUsingClientConsumer
    ) {
        executorsMessages.keySet().stream()
            .map(applicationContext::getBean)
            .filter(executor -> isUsingClientConsumer(executor, executorsNotUsingClientConsumer))
            .forEach(executor -> {
                try {
                    mockAndExecute(consumer, executor, executorsMessages);
                } catch (IllegalArgumentException e) {
                    softAssert.assertThat(e.getMessage())
                        .isEqualTo(String.format("No consumer for name [%s] found", consumer));
                } catch (UnsupportedOperationException e) {
                    softAssert.assertThat(e.getMessage()).contains("Unsupported method");
                } catch (Exception e) {
                    softAssert.fail("Unexpected exception thrown", e);
                }
            });
    }

    protected void executeAll(TaskResultConsumer consumer,
                              Map<Class<? extends TaskExecutor>, String> executorsMessages,
                              Set<Class<? extends TaskExecutor>> executorsNotUsingClientConsumer) {
        executorsMessages.keySet().stream()
            .map(applicationContext::getBean)
            .filter(executor -> isUsingClientConsumer(executor, executorsNotUsingClientConsumer))
            .forEach(executor -> mockAndExecute(consumer, executor, executorsMessages));
    }

    protected void mockAndExecute(TaskResultConsumer consumer,
                                  TaskExecutor executor,
                                  Map<Class<? extends TaskExecutor>, String> executorsMessages) {
        when(clientTaskRepository.findTask(eq(CLIENT_TASK_ID)))
            .thenReturn(createClientTask(consumer, executor, executorsMessages));
        // Метод клиента-потребителя может быть не реализован. Тогда выбросится исключение.
        try {
            executor.execute(new ExecutorTaskWrapper().setId(CLIENT_TASK_ID));
        } catch (UnsupportedOperationException e) {
            softAssert.assertThat(e.getMessage()).startsWith("Unsupported method");
        }
    }

    private boolean isUsingClientConsumer(TaskExecutor executor,
                                          Set<Class<? extends TaskExecutor>> executorsNotUsingClientConsumer) {
        return executorsNotUsingClientConsumer.stream()
            .noneMatch(executorClass -> executorClass.isInstance(executor));
    }

    private ClientTask createClientTask(TaskResultConsumer consumer,
                                        TaskExecutor executor,
                                        Map<Class<? extends TaskExecutor>, String> executorsMessages) {
        return new ClientTask()
            .setId(CLIENT_TASK_ID)
            .setRootId(CLIENT_TASK_ID)
            .setParentId(CLIENT_TASK_ID)
            .setMessage(executorsMessages.getOrDefault(executorsMessages.keySet().stream()
                .filter(executorClass -> executorClass.isInstance(executor))
                .findFirst()
                .orElse(executor.getClass()), "{}"))
            .setConsumer(consumer)
            .setProcessId(PROCESS_ID);
    }
}
