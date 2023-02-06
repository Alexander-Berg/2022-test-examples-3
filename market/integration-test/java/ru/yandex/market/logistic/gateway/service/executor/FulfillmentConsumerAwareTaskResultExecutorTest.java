package ru.yandex.market.logistic.gateway.service.executor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.ff.client.dto.RequestAcceptDTO;
import ru.yandex.market.ff.client.dto.RequestRejectDTO;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Order;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.service.consumer.FulfillmentConsumerClient;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CancelInboundErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CancelInboundSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CancelOutboundErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CancelOutboundSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CreateInboundErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CreateInboundSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CreateOutboundErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CreateOutboundSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CreateTransferErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CreateTransferSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.GetOrderErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.GetOrderSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.PutReferenceItemsSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.PutRegisterErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.PutRegisterSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.UpdateInboundErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.UpdateInboundSuccessExecutor;
import ru.yandex.market.logistics.iris.client.api.LgwResponseClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class FulfillmentConsumerAwareTaskResultExecutorTest extends TaskResultExecutorTest {

    @MockBean
    private LgwResponseClient lgwResponseClient;

    @Autowired
    private Collection<FulfillmentConsumerClient> consumerClients;

    /**
     * Минимальное тело запроса, достаточное, чтобы тест не упал на валидации запроса. FF экзекьюторы.
     */
    private final Map<Class<? extends TaskExecutor>, String> EXECUTORS_MESSAGES_FULFILLMENT =
        ImmutableMap.<Class<? extends TaskExecutor>, String>builder()
            .put(ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CancelOrderErrorExecutor.class,
                "{\"orderId\":{\"yandexId\":1}, \"partner\" : {\"id\" : 48}}")
            .put(ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CancelOrderSuccessExecutor.class,
                "{\"yandexId\":2, \"partner\" : {\"id\" : 48}}")
            .put(ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CreateOrderErrorExecutor.class,
                "{\"order\":{\"orderId\":{\"yandexId\":3}}}")
            .put(ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CreateOrderSuccessExecutor.class,
                "{\"orderId\":3, \"trackId\" : \"t1\", \"partner\" : { \"id\" : 4}}")

            .put(CancelInboundErrorExecutor.class, "{\"inboundId\":{\"yandexId\":1}}")
            .put(CancelInboundSuccessExecutor.class, "{\"yandexId\":1}")
            .put(CancelOutboundErrorExecutor.class, "{\"outboundId\":{\"yandexId\":2}}")
            .put(CancelOutboundSuccessExecutor.class, "{\"yandexId\":2}")
            .put(CreateInboundErrorExecutor.class, "{\"inbound\":{\"inboundId\":{\"yandexId\":3}}}")
            .put(CreateInboundSuccessExecutor.class, "{\"inboundId\":{\"yandexId\":3,\"partnerId\":3}}")
            .put(CreateOutboundErrorExecutor.class, "{\"outbound\":{\"outboundId\":{\"yandexId\":4}}}")
            .put(CreateOutboundSuccessExecutor.class, "{\"outboundId\":{\"yandexId\":4,\"partnerId\":4}}")
            .put(CreateTransferErrorExecutor.class, "{\"transfer\":{\"transferId\":{\"yandexId\":5}}}")
            .put(CreateTransferSuccessExecutor.class, "{\"transferId\":{\"yandexId\":5,\"partnerId\":5}}")
            .put(PutReferenceItemsSuccessExecutor.class, "{\"createdItems\":[],\"errorItems\":[]}}")
            .put(ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.UpdateOrderErrorExecutor.class,
                "{\"order\": {\"orderId\":{\"yandexId\":5}}, \"partner\" : { \"id\" : 5}}")
            .put(ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.UpdateOrderSuccessExecutor.class,
                "{\"orderId\":4, \"trackId\" : \"t4\", \"partner\" : { \"id\" : 4}}")
            .put(GetOrderErrorExecutor.class, "{\"orderId\":{\"yandexId\":1}, \"partner\" : { \"id\" : 5}}")
            .put(GetOrderSuccessExecutor.class,
                "{\"order\": {\"orderId\":{\"yandexId\":5}}, \"partner\" : { \"id\" : 5}}")
            .put(UpdateInboundErrorExecutor.class, "{\"inbound\":{\"inboundId\":{\"yandexId\":6}}}")
            .put(UpdateInboundSuccessExecutor.class, "{\"inboundId\":{\"yandexId\":7,\"partnerId\":3}}")
            .put(PutRegisterErrorExecutor.class, "{\n" +
                    "  \"register\": {\n" +
                    "    \"transportationRequestId\": {\n" +
                    "      \"yandexId\": \"3\",\n" +
                    "      \"fulfillmentId\": \"3\",\n" +
                    "      \"partnerId\": \"3\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n")
            .put(PutRegisterSuccessExecutor.class, "{\n" +
                    "  \"registerId\": {\n" +
                    "      \"yandexId\": \"3\",\n" +
                    "      \"fulfillmentId\": \"3\",\n" +
                    "      \"partnerId\": \"3\"\n" +
                    "  }\n" +
                    "}\n")
            .build();

    /**
     * Не указываем в ClientTask.consumer никакое значение.
     * Должен вылететь IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void defaultClientCallPerformed() {
        executeAll(null, EXECUTORS_MESSAGES_FULFILLMENT, Collections.emptySet());
    }

    /**
     * В ClientTask.consumer указываем неправильный тип клиента-потребителя.
     * Должно быть выброшено исключение.
     */
    @Test
    public void invalidClientConsumerSpecified() {
        executeAllAndVerifyExceptionThrown(TaskResultConsumer.MDB,
            EXECUTORS_MESSAGES_FULFILLMENT, Collections.emptySet());
        verifyZeroInteractions(fulfillmentWorkflowConsumerClient);
    }

    @Test
    public void clientCallPerformed() {
        Set<TaskResultConsumer> validConsumers = ImmutableSet.of(
            TaskResultConsumer.FF_WF_API,
            TaskResultConsumer.IRIS,
            TaskResultConsumer.LOAD_TESTING,
            TaskResultConsumer.TRACKER,
            TaskResultConsumer.MDB,
            TaskResultConsumer.LOM,
            TaskResultConsumer.TPL,
            TaskResultConsumer.TM
        );

        Set<TaskResultConsumer> calledConsumers = new HashSet<>();
        consumerClients
            .forEach(consumerClient -> {
                calledConsumers.add(consumerClient.getClientName());
                executeAll(consumerClient.getClientName(), EXECUTORS_MESSAGES_FULFILLMENT, Collections.emptySet());
                verifyInteractions(consumerClient);
            });

        // проверяем что не потерялись бины потребителей
        Assertions.assertThat(calledConsumers).isEqualTo(validConsumers);
    }

    private void verifyInteractions(FulfillmentConsumerClient consumerClient) {
        verify(consumerClient).setCancelInboundError(eq("1"), any());
        verify(consumerClient).setCancelInboundSuccess(eq("1"), any());
        verify(consumerClient).setCancelOutboundError(eq("2"), any());
        verify(consumerClient).setCancelOutboundSuccess(eq("2"), any());
        verify(consumerClient).rejectRequestByService(eq(3L), eq(new RequestRejectDTO()));
        verify(consumerClient).acceptRequestByService(eq(3L), eq(new RequestAcceptDTO("3")));
        verify(consumerClient).rejectRequestByService(eq(4L), eq(new RequestRejectDTO()));
        verify(consumerClient).acceptRequestByService(eq(4L), eq(new RequestAcceptDTO("4")));
        verify(consumerClient).rejectRequestByService(eq(5L), eq(new RequestRejectDTO()));
        verify(consumerClient).acceptRequestByService(eq(5L), eq(new RequestAcceptDTO("5")));
        verify(consumerClient).setUpdateOrderSuccess(eq("4"), eq("t4"), eq(4L), eq(PROCESS_ID));
        verify(consumerClient).setUpdateOrderError(eq("5"), eq(5L), eq(PROCESS_ID), isNull());
        verify(consumerClient).setGetOrderSuccess(eq(getOrder("5")), eq(new Partner(5L)), eq(PROCESS_ID));
        verify(consumerClient).rejectRequestUpdating(eq(6L));
        verify(consumerClient).acceptRequestUpdating(eq(7L));
        verify(consumerClient).setPutRegisterSuccess(eq("3"), eq("3"));
        verify(consumerClient).setPutRegisterError(eq("3"));
    }

    private Order getOrder(String yandexId) {
        return new Order.OrderBuilder(ResourceId.builder().setYandexId(yandexId).build(),
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null).build();
    }
}
