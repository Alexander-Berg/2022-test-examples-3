package ru.yandex.market.checkout.checkouter.tasks.eventexport.logbroker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.shaded.com.google.common.base.Throwables;
import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.event.EventService;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.metrics.CompositeMetricsPublisher;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolverStub;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderHistoryEventViewModel;
import ru.yandex.market.checkout.checkouter.views.services.OrderHistoryEventViewModelService;
import ru.yandex.market.checkout.common.logbroker.LogbrokerEventPublishService;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.resale.ResaleSpecs;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_YANDEX_I_COOKIE;
import static ru.yandex.market.checkout.checkouter.tasks.eventexport.OrderEventPublisherTask.EVENT_PUBLISH_TASK;
import static ru.yandex.market.checkout.test.providers.ResaleSpecsProvider.getResaleSpecs;

public class OrderEventsLbkxExportTaskTest extends AbstractWebTestBase {

    private static final String I_AM_YANDEX_COOKIE = "I am YANDEX COOKIE";
    @Autowired
    private Map<String, ZooTask> taskMap;
    @Autowired
    private HttpMessageConverter checkouterJsonMessageConverter;
    @Autowired
    private CuratorFramework curator;
    @Autowired
    private EventService eventService;
    @Autowired
    private OrderHistoryEventViewModelService orderHistoryEventViewModelService;
    @Autowired
    private CompositeMetricsPublisher metricsPublisher;
    @Value("${market.checkout.lbkx.topic.order-event.partition.count:1}")
    private int partitionsCount;

    private AsyncProducer lbkxAsyncProducer;

    @Test
    public void shouldRun() throws Exception {
        ArgumentCaptor<AsyncProducerConfig> configCaptor = ArgumentCaptor.forClass(AsyncProducerConfig.class);
        lbkxAsyncProducer = Mockito.mock(AsyncProducer.class);

        when(lbkxClientFactory.asyncProducer(configCaptor.capture())).thenReturn(lbkxAsyncProducer);

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        when(lbkxAsyncProducer.write(captor.capture(), Mockito.anyLong()))
                .thenReturn(CompletableFuture.completedFuture(new ProducerWriteResponse(1, 1, false)));
        CompletableFuture<ProducerInitResponse> initFuture
                = CompletableFuture.completedFuture(new ProducerInitResponse(0, "1", 1, "1"));
        Mockito.doReturn(initFuture).when(lbkxAsyncProducer).init();

        final Buyer buyer = BuyerProvider.getDefaultBuyer(534534534825L);

        final Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(buyer);

        checkouterFeatureWriter.writeValue(ENABLE_YANDEX_I_COOKIE, true);
        parameters.configuration().cart().request().setICookie(I_AM_YANDEX_COOKIE);
        buyer.setICookie(I_AM_YANDEX_COOKIE);

        Order order = orderCreateHelper.createOrder(parameters);

        taskMap.entrySet()
                .stream()
                .filter(stringZooTaskEntry -> stringZooTaskEntry.getKey().contains(EVENT_PUBLISH_TASK))
                .map(Map.Entry::getValue)
                .forEach(ZooTask::runOnce);

        ZooTask task = taskMap.entrySet()
                .stream()
                .filter(stringZooTaskEntry -> stringZooTaskEntry
                        .getKey()
                        .equals(OrderEventsLbkxExportTask.EVENT_EXPORT_TASK + "_"
                                + order.calculatePartitionIndex(partitionsCount)))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow();

        long lastEventIdBefore = getLastEventId(task);

        task.runOnce();

        long lastEventIdAfter = getLastEventId(task);

        List<byte[]> values = captor.getAllValues();
        assertThat(values, CoreMatchers.not(Matchers.empty()));

        List<OrderHistoryEvent> events = values.stream()
                .map(bytes -> {
                    try {
                        return (OrderHistoryEvent) checkouterJsonMessageConverter.read(OrderHistoryEvent.class,
                                new MockHttpInputMessage(bytes));
                    } catch (Exception ex) {
                        throw Throwables.propagate(ex);
                    }
                })
                .collect(Collectors.toList());

        assertThat(lastEventIdAfter, Matchers.greaterThan(lastEventIdBefore));

        checkParcelItemDates(events);
        checkQuantPriceAndQuantity(events);
        checkPersonalInformation(buyer, events);

        AsyncProducerConfig config = configCaptor.getValue();
        assertEquals(config.getTopic(), "market-checkout/production/checkouter-order-event-log");
    }

    private void checkParcelItemDates(List<OrderHistoryEvent> events) {
        List<ParcelItem> parcelItems = events.stream()
                .flatMap(e -> e.getOrderAfter().getDelivery().getParcels().get(0).getParcelItems().stream())
                .collect(Collectors.toList());
        assertThat(parcelItems, everyItem(hasProperty("supplierStartDateTime", notNullValue())));
        assertThat(parcelItems, everyItem(hasProperty("supplierShipmentDateTime", notNullValue())));
    }

    private void checkQuantPriceAndQuantity(List<OrderHistoryEvent> events) {
        List<OrderItem> parcelItems = events.stream()
                .flatMap(e -> e.getOrderAfter().getItems().stream())
                .collect(Collectors.toList());
        assertThat(parcelItems, everyItem(hasProperty("quantity", notNullValue())));
        assertThat(parcelItems, everyItem(hasProperty("quantPrice", notNullValue())));
    }

    private long getLastEventId(ZooTask orderEventsLbkxExportTask) throws Exception {
        String lastEventIdString = new String(curator.getData()
                .forPath(orderEventsLbkxExportTask.getNodePath() + "/lastEventId"), StandardCharsets.US_ASCII
        );
        return Long.parseLong(lastEventIdString);
    }

    private void checkPersonalInformation(Buyer original, List<OrderHistoryEvent> events) {
        events.stream()
                .map(e -> e.getOrderAfter().getBuyer())
                .forEach(buyer -> assertTrue(EqualsBuilder.reflectionEquals(buyer, original,
                        "id", "ipRegionId", "dontCall", "assessor", "userAgent", "normalizedPhone",
                        "unreadImportantEvents", "yandexEmployee", "icookie")));

    }

    @Test
    void shouldUseMultiOrderIdIfExists() {
        final LogbrokerEventPublishService<OrderHistoryEventViewModel> logbrokerEventPublishServiceMock =
                Mockito.mock(LogbrokerEventPublishService.class);

        final OrderEventsLbkxExportTask task = new OrderEventsLbkxExportTask(
                eventService,
                orderHistoryEventViewModelService,
                logbrokerEventPublishServiceMock,
                metricsPublisher,
                0.8,
                new CheckouterFeatureResolverStub(),
                null,
                null);

        OrderHistoryEvent event1 = EntityHelper.getOrderHistoryEvent();
        OrderHistoryEvent event2 = EntityHelper.getOrderHistoryEvent();
        OrderHistoryEvent event3 = EntityHelper.getOrderHistoryEvent();
        OrderHistoryEvent multiOrderEvent1 = EntityHelper.getOrderHistoryEvent();
        OrderHistoryEvent multiOrderEvent2 = EntityHelper.getOrderHistoryEvent();
        long orderId1 = 1L;
        long orderId2 = 2L;
        long orderId3 = 3L;
        long multiOrderId1 = 4L;
        long multiOrderId2 = 5L;
        event1.getOrderAfter().setId(orderId1);
        event2.getOrderAfter().setId(orderId2);
        event3.getOrderAfter().setId(orderId3);
        multiOrderEvent1.getOrderAfter().setId(multiOrderId1);
        multiOrderEvent2.getOrderAfter().setId(multiOrderId2);
        List<OrderHistoryEvent> events = List.of(event1, event2, event3, multiOrderEvent1, multiOrderEvent2);
        events.stream().map(OrderHistoryEvent::getOrderAfter).forEach(o -> o.setCreationDate(new Date()));
        String multiOrderId = "6430634d-fe74-42f9-9a57-647fc58ece0b";
        Assertions.assertThat(multiOrderId.hashCode()).isNegative();
        Stream.of(multiOrderEvent1, multiOrderEvent2).map(OrderHistoryEvent::getOrderAfter)
                .forEach(o -> o.setProperty(OrderPropertyType.MULTI_ORDER_ID, multiOrderId));

        when(logbrokerEventPublishServiceMock.publishEvents(any(), any(), any())).then(invocation -> {
            List<OrderHistoryEventViewModel> models = (List<OrderHistoryEventViewModel>) invocation.getArguments()[0];
            assertEquals(
                    Set.of(orderId1, orderId2, orderId3, multiOrderId1, multiOrderId2),
                    models.stream().map(model -> model.getOrderAfter().getId()).collect(Collectors.toSet())
            );
            return emptyMap();
        });

        task.init();
        task.doExport(events);
        task.finish();
    }

    @Test
    @DisplayName("POSITIVE: События монотаски, должны выгружаться с другим source_id при включенной мультитаске")
    void multipartitionTaskSourceIdTest() {

        final LogbrokerEventPublishService<OrderHistoryEventViewModel> logbrokerEventPublishServiceMock =
                Mockito.mock(LogbrokerEventPublishService.class);

        final OrderEventsLbkxExportTask task = new OrderEventsLbkxExportTask(
                eventService,
                orderHistoryEventViewModelService,
                logbrokerEventPublishServiceMock,
                metricsPublisher,
                0.8,
                new CheckouterFeatureResolverStub(),
                null,
                null);

        OrderHistoryEvent event1 = EntityHelper.getOrderHistoryEvent();
        OrderHistoryEvent event2 = EntityHelper.getOrderHistoryEvent();
        OrderHistoryEvent event3 = EntityHelper.getOrderHistoryEvent();

        event1.getOrderAfter().setId(1L);
        event2.getOrderAfter().setId(2L);
        event3.getOrderAfter().setId(3L);


        when(logbrokerEventPublishServiceMock.publishEvents(any(), any(), any()))
                .thenReturn(emptyMap());
        task.init();
        task.doExport(List.of(event1, event2, event3));
        task.finish();

    }

    @Test
    public void shouldSendResaleSpecsInEvents() throws Exception {
        ArgumentCaptor<AsyncProducerConfig> configCaptor = ArgumentCaptor.forClass(AsyncProducerConfig.class);
        lbkxAsyncProducer = Mockito.mock(AsyncProducer.class);

        when(lbkxClientFactory.asyncProducer(configCaptor.capture())).thenReturn(lbkxAsyncProducer);

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        when(lbkxAsyncProducer.write(captor.capture(), Mockito.anyLong()))
                .thenReturn(CompletableFuture.completedFuture(new ProducerWriteResponse(1, 1, false)));
        CompletableFuture<ProducerInitResponse> initFuture
                = CompletableFuture.completedFuture(new ProducerInitResponse(0, "1", 1, "1"));
        Mockito.doReturn(initFuture).when(lbkxAsyncProducer).init();

        OrderItem firstItem = OrderItemProvider.defaultOrderItem();
        Parameters params = BlueParametersProvider.defaultBlueOrderParametersWithItems(firstItem);
        params.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(firstItem)
                .resaleSpecs(getResaleSpecs(null)).build()));

        Order order = orderCreateHelper.createOrder(params);

        ZooTask task = taskMap.entrySet()
                .stream()
                .filter(stringZooTaskEntry -> stringZooTaskEntry
                        .getKey()
                        .equals(OrderEventsLbkxExportTask.EVENT_EXPORT_TASK + "_"
                                + order.calculatePartitionIndex(partitionsCount)))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow();

        task.runOnce();
        List<byte[]> values = captor.getAllValues();
        ObjectMapper mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        ResaleSpecs expectedResaleSpecs = getResaleSpecs(order.getItems().iterator().next().getId());
        for (byte[] byteArray : values) {
            assertResaleSpecs(new String(byteArray, StandardCharsets.UTF_8),
                    expectedResaleSpecs, mapper);
        }
    }

    private void assertResaleSpecs(String json, ResaleSpecs resaleSpecs, ObjectMapper mapper) throws IOException {
        JsonNode resaleSpecsJsonNode = mapper.readTree(json);

        JsonNode orderItemNode = ((ArrayNode) resaleSpecsJsonNode.get("orderAfter").get("items")).get(0);
        JsonNode resaleSpecsNode = orderItemNode.get("resaleSpecs");
        assertTrue(orderItemNode.get("isResale").booleanValue());
        JsonNode conditionNode = resaleSpecsNode.get("condition");
        JsonNode reasonNode = resaleSpecsNode.get("reason");
        assertEquals(resaleSpecs.getConditionValue(), conditionNode.get("value").textValue());
        assertEquals(resaleSpecs.getConditionText(), conditionNode.get("text").textValue());
        assertEquals(resaleSpecs.getReasonValue(), reasonNode.get("value").textValue());
        assertEquals(resaleSpecs.getReasonText(), reasonNode.get("text").textValue());
    }
}
