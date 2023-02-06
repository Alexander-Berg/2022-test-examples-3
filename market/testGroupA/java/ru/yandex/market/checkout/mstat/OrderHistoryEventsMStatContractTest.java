package ru.yandex.market.checkout.mstat;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.checkout.checkouter.mstat.validators.OrderHistoryEventMStatContractValidator;
import ru.yandex.market.checkout.checkouter.tasks.eventexport.logbroker.OrderEventsLbkxExportTask;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderHistoryEventsMStatContractTest
        extends AbstractMStatContractTest<OrderHistoryEventMStatContractValidator> {

    @Autowired
    protected Map<String, ZooTask> taskMap;
    @Autowired
    @Qualifier("checkouterAnnotationObjectMapper")
    private ObjectMapper mapper;
    @Autowired
    private OrderHistoryEventMStatContractValidator validator;
    @Autowired
    private LogbrokerClientFactory lbkxClientFactory;
    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;

    private AsyncProducer lbkxAsyncProducer;

    @BeforeEach
    public void setUp() throws InterruptedException {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndGps();
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, true);
        lbkxAsyncProducer = Mockito.mock(AsyncProducer.class);
        Mockito.when(lbkxClientFactory.asyncProducer(Mockito.any(AsyncProducerConfig.class)))
                .thenReturn(lbkxAsyncProducer);
    }

    @AfterEach
    void resetMocks() {
        b2bCustomersMockConfigurer.resetAll();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль значений перечислений для таски " +
            "OrderEventsLbkxExportTask")
    void possibleEnumValuesTest() {
        validatePossibleEnumValues();
    }

    @Test
    public void contractTest() throws Exception {

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        CompletableFuture<ProducerWriteResponse> future = CompletableFuture.completedFuture(null);

        Mockito.doReturn(future)
                .when(lbkxAsyncProducer).write(captor.capture(), Mockito.anyLong());

        CompletableFuture<ProducerInitResponse> initFuture
                = CompletableFuture.completedFuture(
                new ProducerInitResponse(Long.MAX_VALUE, "1", 1, "1")
        );
        Mockito.doReturn(initFuture)
                .when(lbkxAsyncProducer).init();

        orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());

        taskMap.entrySet().stream()
                .filter(stringZooTaskEntry ->
                        stringZooTaskEntry.getKey().contains(OrderEventsLbkxExportTask.EVENT_EXPORT_TASK))
                .map(Map.Entry::getValue)
                .forEach(ZooTask::runOnce);

        List<byte[]> values = captor.getAllValues();

        JsonNode expected = mapper.readTree(this.getClass().getClassLoader().getResource("order_event_history.json"));
        JsonNode actual = mapper.readTree(values.get(0));

        assertNodeRecursive(expected, actual);
    }

    private void assertNodeRecursive(JsonNode expected, JsonNode actual) {
        assertNotNull(actual);

        Iterator<Map.Entry<String, JsonNode>> fields = expected.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> nodeEntry = fields.next();

            if (nodeEntry.getValue().isObject()) {
                assertNodeRecursive(nodeEntry.getValue(), actual.get(nodeEntry.getKey()));
            } else if (nodeEntry.getValue().isArray() && nodeEntry.getValue().size() > 0) {
                assertTrue(actual.get(nodeEntry.getKey()).size() > 0);
                assertNodeRecursive(nodeEntry.getValue().get(0), actual.get(nodeEntry.getKey()).get(0));
            } else {
                assertNode(actual, nodeEntry);
            }
        }
    }

    private void assertNode(JsonNode actual, Map.Entry<String, JsonNode> nodeEntry) {
        assertNotNull(actual.get(nodeEntry.getKey()));
        assertEquals(actual.get(nodeEntry.getKey()).getClass(), nodeEntry.getValue().getClass());
    }

    @Override
    protected OrderHistoryEventMStatContractValidator getValidator() {
        return validator;
    }

    @Override
    protected Set<String> getTableNecessaryColumns() {
        return Set.of();
    }
}
