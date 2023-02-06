package ru.yandex.market.logistic.gateway.service.executor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.service.consumer.DeliveryConsumerClient;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CancelOrderSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CancelParcelSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CreateIntakeErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CreateIntakeSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CreateOrderErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CreateOrderSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CreateRegisterErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CreateRegisterSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CreateSelfExportErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CreateSelfExportSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.GetAttachedDocsErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.GetAttachedDocsSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.GetLabelsErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.GetLabelsSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.UpdateOrderDeliveryDateSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.UpdateOrderErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.UpdateOrderItemsErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.UpdateOrderItemsSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.UpdateOrderSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.UpdateRecipientErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.UpdateRecipientSuccessExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class DeliveryConsumerAwareTaskResultExecutorTest extends TaskResultExecutorTest {
    @Autowired
    private Collection<DeliveryConsumerClient> consumerClients;

    /**
     * Множество DS экзекьюторов, которые не используют clientConsumer.
     * Если какой-то из экзекьюторов станет использовать clientConsumer'a, то ожидается, что тест сломается.
     */
    protected final Set<Class<? extends TaskExecutor>> EXECUTORS_NOT_USING_CLIENT_CONSUMER_DELIVERY = Set.of();

    /**
     * Минимальное тело запроса, достаточное, чтобы тест не упал на валидации запроса. DS экзекьюторы.
     */
    private final Map<Class<? extends TaskExecutor>, String> EXECUTORS_MESSAGES_DELIVERY =
        ImmutableMap.<Class<? extends TaskExecutor>, String>builder()
            .put(CreateOrderErrorExecutor.class,
                "{\"order\":{\"orderId\":{\"yandexId\":\"1\"},\"parcelId\":{\"yandexId\":2}}," +
                    "\"error\":\"error message\"}")
            .put(CreateOrderSuccessExecutor.class, "{\"orderId\":1}")
            .put(CreateRegisterSuccessExecutor.class,
                "{\"registerId\":{\"yandexId\":\"test-yandex-id\",\"deliveryId\":\"test-delivery-id\"}," +
                    "\"yandexRegisterId\":\"test-yandex-id\"}")
            .put(CreateRegisterErrorExecutor.class,
                "{\"register\":{\"registerId\":{\"yandexId\":\"test-yandex-id\"," +
                    "\"deliveryId\":\"test-delivery-id\"}},\"error\":\"error message\"}")
            .put(GetLabelsSuccessExecutor.class, "{\"orderId\":1}")
            .put(GetLabelsErrorExecutor.class, "{\"ordersId\" : [{ \"orderId\" : { \"yandexId\" : \"1\" } }] , " +
                "\"partner\" : {\"id\" : 1}}")
            .put(UpdateOrderSuccessExecutor.class, "{\"orderId\":{\"yandexId\":1}}")
            .put(
                UpdateOrderErrorExecutor.class,
                "{\"order\":{\"orderId\":{\"yandexId\":\"1\"}},\"partner\":{\"id\":1},\"error\":\"error message\"}"
            )
            .put(UpdateOrderItemsSuccessExecutor.class, "{\"orderId\":{\"yandexId\":1}, \"partner\" : {\"id\" : 1}}")
            .put(UpdateOrderItemsErrorExecutor.class, "{\"orderItems\":{\"orderId\":{\"yandexId\":1}}, \"partner\" : " +
                "{\"id\" : 1},\"error\":\"error message\"}")
            .put(CreateIntakeSuccessExecutor.class, "{\"partnerId\":\"123\",\"intakeId\":" +
                "{\"yandexId\":\"321\", \"deliveryId\":\"abc\"}," +
                "\"partner\":{\"id\":777}}")
            .put(CreateSelfExportSuccessExecutor.class,
                "{\"partnerId\":\"123\",\"selfExportId\":" +
                "{\"yandexId\":\"321\", \"deliveryId\":\"abc\"}," +
                "\"partner\":{\"id\":777}}")
            .put(CreateIntakeErrorExecutor.class,
                "{\"intake\":{\"intakeId\":{\"yandexId\":\"321\"}}," +
                    "\"error\":\"error message\"," +
                    "\"partner\":{\"id\":777}}")
            .put(CreateSelfExportErrorExecutor.class,
                "{\"selfExport\":{\"selfExportId\":{\"yandexId\":\"321\"}}," +
                    "\"error\":\"error message\"," +
                    "\"partner\":{\"id\":777}}")
            .put(GetAttachedDocsSuccessExecutor.class, "{\n" +
                "  \"shipmentType\": 0,\n" +
                "  \"shipmentDate\": \"2018-08-22T00:00:00+03:00\",\n" +
                "  \"url\": \"http://pdf.url/label/url.pdf\",\n" +
                "  \"partner\": {\"id\":145}\n" +
                "}")
            .put(GetAttachedDocsErrorExecutor.class, "{\n" +
                "  \"attachedDocsData\": {\n" +
                "    \"ordersId\": [],\n" +
                "    \"shipmentType\": 0,\n" +
                "    \"shipmentDate\": \"2018-08-22T00:00:00+03:00\"},\n" +
                "  \"partner\": {\n" +
                "    \"id\": 145\n" +
                "  }\n" +
                "}")
            .put(CancelParcelSuccessExecutor.class, "{\"orderId\":\"1\", \"parcelId\" : 1, \"cancelStatus\" : " +
                "\"SUCCESS\"}")
            .put(CancelOrderSuccessExecutor.class, "{\"yandexId\":1, \"cancelStatus\" : \"SUCCESS\", \"partner\" : " +
                "{\"id\" : 48}}")
            .put(UpdateOrderDeliveryDateSuccessExecutor.class, "{\"orderId\":{\"yandexId\":\"8265683\"}," +
                "\"partner\":{\"id\":1003937},\"updateRequestId\":777}")
            .put(UpdateRecipientErrorExecutor.class, "{\"orderId\":{\"yandexId\":\"8265683\"}," +
                "\"partner\":{\"id\":1003937},\"updateRequestId\":777}")
            .put(UpdateRecipientSuccessExecutor.class, "{\"orderId\":{\"yandexId\":\"8265683\"}," +
                "\"partner\":{\"id\":1003937},\"updateRequestId\":777}")
            .build();

    private final Map<Class<? extends TaskExecutor>, String> SINGLE_EXECUTOR_CHECK =
        ImmutableMap.<Class<? extends TaskExecutor>, String>builder()
            .put(CreateOrderSuccessExecutor.class, "{\"orderId\":1,\"partner\":{\"id\":1}}").build();

    /**
     * Не указываем в ClientTask.consumer никакое значение.
     * Должен быть выброшен IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void defaultClientCallPerformed() {
        executeAll(null, SINGLE_EXECUTOR_CHECK, EXECUTORS_NOT_USING_CLIENT_CONSUMER_DELIVERY);
    }

    /**
     * В ClientTask.consumer указываем неправильный тип клиента-потребителя.
     * Должно быть выброшено исключение.
     */
    @Test
    public void invalidClientConsumerSpecified() {
        executeAllAndVerifyExceptionThrown(TaskResultConsumer.FF_WF_API,
            EXECUTORS_MESSAGES_DELIVERY, EXECUTORS_NOT_USING_CLIENT_CONSUMER_DELIVERY);
        verifyNoInteractions(mdbDeliveryConsumerClient);
    }

    @Test
    public void clientCallPerformed() {
        Set<TaskResultConsumer> validConsumers = ImmutableSet.of(
            TaskResultConsumer.MDB,
            TaskResultConsumer.LOM,
            TaskResultConsumer.LOAD_TESTING,
            TaskResultConsumer.TRACKER,
            TaskResultConsumer.LMS,
            TaskResultConsumer.TM,
            TaskResultConsumer.FF_WF_API,
            TaskResultConsumer.TPL
        );

        Set<TaskResultConsumer> calledConsumers = new HashSet<>();
        consumerClients
            .forEach(consumerClient -> {
                calledConsumers.add(consumerClient.getClientName());
                executeAll(
                    consumerClient.getClientName(),
                    EXECUTORS_MESSAGES_DELIVERY,
                    EXECUTORS_NOT_USING_CLIENT_CONSUMER_DELIVERY
                );
                verifyInteractions(consumerClient);
            });

        // проверяем что не потерялись бины потребителей
        Assertions.assertThat(calledConsumers).isEqualTo(validConsumers);
    }

    @Test
    public void clientConsumerCallNotPerformed() {
        EXECUTORS_NOT_USING_CLIENT_CONSUMER_DELIVERY.stream()
            .map(executorClass -> applicationContext.getBean(executorClass))
            .forEach(executor -> mockAndExecute(TaskResultConsumer.MDB, executor, EXECUTORS_MESSAGES_DELIVERY));

        verifyNoInteractions(mdbDeliveryConsumerClient);
        verifyNoInteractions(loadTestingConsumerClient);
        verifyNoInteractions(fulfillmentWorkflowConsumerClient);
        verifyNoInteractions(loadTestingFulfillmentConsumerClient);
        verifyNoInteractions(lomConsumerClient);
    }

    private void verifyInteractions(DeliveryConsumerClient consumerClient) {
        verify(consumerClient).setCreateIntakeSuccess(any(), any(), any(), eq(PROCESS_ID));
        verify(consumerClient).setCreateIntakeError(any(), any(), eq(PROCESS_ID), eq("error message"));
        verify(consumerClient).setCreateOrderError(eq("1"), eq(2L), any(), eq(PROCESS_ID), eq("error message"));
        verify(consumerClient).setCreateOrderSuccess(eq("1"), any(), any(), any(), eq(PROCESS_ID));
        verify(consumerClient).setCreateRegisterError(any(), eq(PROCESS_ID), eq("error message"));
        verify(consumerClient).setCreateRegisterSuccess(any(), any(), eq(PROCESS_ID));
        verify(consumerClient).setGetLabelsSuccess(eq("1"), any(), any(), any(), eq(PROCESS_ID));
        verify(consumerClient).setGetLabelsError(eq("1"), any(), eq(PROCESS_ID));
        verify(consumerClient).setUpdateOrderSuccess(eq("1"), any(), any(), any(), eq(PROCESS_ID));
        verify(consumerClient).setUpdateOrderError(eq("1"), eq(1L), eq(PROCESS_ID), eq("error message"));
        verify(consumerClient).setUpdateOrderItemsSuccess(eq("1"), any(), eq(PROCESS_ID));
        verify(consumerClient).setUpdateOrderItemsError(eq("1"), any(), eq(PROCESS_ID), eq("error message"));
        verify(consumerClient).setCreateSelfExportError(any(), any(), eq(PROCESS_ID), eq("error message"));
        verify(consumerClient).setCreateSelfExportSuccess(any(), any(), any(), eq(PROCESS_ID));
        verify(consumerClient).setParcelCancelStatus(any(), anyLong(), any());
        verify(consumerClient).setCancelOrderSuccess(any(), any(), eq(PROCESS_ID));
        verify(consumerClient).setGetAttachedDocsSuccess(any(), any(), any(), any(), any(), eq(PROCESS_ID));
        verify(consumerClient).setGetAttachedDocsError(any(), any(), any(), eq(PROCESS_ID));
        verify(consumerClient).setUpdateDeliveryDateSuccess(anyString(), anyLong(), anyString());
        verify(consumerClient).setUpdateRecipientSuccess(anyString(), anyLong(), anyString());
    }

    private void verifySingleInteraction(DeliveryConsumerClient consumerClient) {
        verify(consumerClient).setCreateOrderSuccess(any(), any(), any(), anyLong(), eq(PROCESS_ID));
    }
}
