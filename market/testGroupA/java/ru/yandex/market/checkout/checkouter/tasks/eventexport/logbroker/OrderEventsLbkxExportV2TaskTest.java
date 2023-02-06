package ru.yandex.market.checkout.checkouter.tasks.eventexport.logbroker;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.shaded.com.google.common.base.Throwables;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
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
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.event.EventService;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.metrics.CompositeMetricsPublisher;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolverStub;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.tasks.Partition;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderHistoryEventViewModel;
import ru.yandex.market.checkout.checkouter.views.services.OrderHistoryEventViewModelService;
import ru.yandex.market.checkout.common.logbroker.LogbrokerEventPublishService;
import ru.yandex.market.checkout.common.tasks.ZooTask;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.tasks.eventexport.OrderEventPublisherTask.EVENT_PUBLISH_TASK;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class OrderEventsLbkxExportV2TaskTest extends AbstractWebTestBase {

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
    @Value("${market.checkout.lbkx.topic.order-event.partition.count}")
    private int partitionsCount;

    private AsyncProducer lbkxAsyncProducer;

    @Test
    public void shouldRun() throws Exception {
        List<ZooTask> tasks = new ArrayList<>(partitionsCount);
        for (int i = 0; i < partitionsCount; i++) {
            tasks.add(taskMap.get(OrderEventsLbkxExportTask.EVENT_EXPORT_TASK + "_" + i));
            orderCreateHelper.createOrder(defaultBlueOrderParameters());
        }

        ArgumentCaptor<AsyncProducerConfig> configCaptor = ArgumentCaptor.forClass(AsyncProducerConfig.class);
        lbkxAsyncProducer = Mockito.mock(AsyncProducer.class);

        Mockito.when(lbkxClientFactory.asyncProducer(configCaptor.capture()))
                .thenReturn(lbkxAsyncProducer);

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        when(lbkxAsyncProducer.write(captor.capture(), Mockito.anyLong()))
                .thenReturn(CompletableFuture.completedFuture(new ProducerWriteResponse(1, 1, false)));
        CompletableFuture<ProducerInitResponse> initFuture
                = CompletableFuture.completedFuture(
                new ProducerInitResponse(Long.MAX_VALUE, "1", 1, "1")
        );
        Mockito.doReturn(initFuture)
                .when(lbkxAsyncProducer).init();


        taskMap.entrySet()
                .stream()
                .filter(stringZooTaskEntry -> stringZooTaskEntry.getKey().contains(EVENT_PUBLISH_TASK))
                .map(Map.Entry::getValue)
                .forEach(ZooTask::runOnce);

        for (ZooTask task : tasks) {
            long lastEventIdBefore = getLastEventId(task);
            task.runOnce();
            long lastEventIdAfter = getLastEventId(task);
            assertThat(lastEventIdAfter, Matchers.greaterThan(lastEventIdBefore));
        }


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


        checkParcelItemDates(events);

        AsyncProducerConfig config = configCaptor.getValue();
        assertEquals("market-checkout/production/checkouter-order-event-log", config.getTopic());
    }

    private void checkParcelItemDates(List<OrderHistoryEvent> events) {
        List<ParcelItem> parcelItems = events.stream()
                .flatMap(e -> e.getOrderAfter().getDelivery().getParcels().get(0).getParcelItems().stream())
                .collect(Collectors.toList());
        assertThat(parcelItems, everyItem(hasProperty("supplierStartDateTime", notNullValue())));
        assertThat(parcelItems, everyItem(hasProperty("supplierShipmentDateTime", notNullValue())));
    }

    private long getLastEventId(ZooTask task) throws Exception {
        String lastEventIdString = new String(curator.getData()
                .forPath(task.getNodePath() + "/lastEventId"), StandardCharsets.US_ASCII
        );
        return Long.parseLong(lastEventIdString);
    }

    @Test
    void shouldNotEraseRouteIfEventTypeNewOrder() {
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
                null
        );

        OrderHistoryEvent event;
        List<OrderHistoryEventViewModel> views;

        //order NEW_ORDER
        event = EntityHelper.getOrderHistoryEvent();
        event.setType(HistoryEventType.NEW_ORDER);
        views = task.mapToViewModel(Collections.singletonList(event));
        assertEquals(1, views.size());
        assertNotNull(views.get(0).getOrderAfter());
        assertTrue(views.get(0).getOrderAfter().getDelivery().getParcels().stream().map(Parcel::getRoute)
                .allMatch(Objects::nonNull));
        assertTrue(views.get(0).getOrderAfter().getDelivery().getParcels().stream().map(Parcel::getCombinatorRouteId)
                .allMatch(r -> Objects.equals(r, new UUID(0, 0))));
        assertNotNull(views.get(0).getOrderBefore());
        assertTrue(views.get(0).getOrderBefore().getDelivery().getParcels().stream().map(Parcel::getRoute)
                .allMatch(Objects::nonNull));
        assertTrue(views.get(0).getOrderBefore().getDelivery().getParcels().stream().map(Parcel::getCombinatorRouteId)
                .allMatch(r -> Objects.equals(r, new UUID(0, 0))));

        //order another type
        event = EntityHelper.getOrderHistoryEvent();
        event.setType(HistoryEventType.CASH_REFUND);
        views = task.mapToViewModel(Collections.singletonList(event));
        assertEquals(1, views.size());
        assertNotNull(views.get(0).getOrderAfter());
        assertTrue(views.get(0).getOrderAfter().getDelivery().getParcels().stream().map(Parcel::getRoute)
                .allMatch(Objects::isNull));
        assertTrue(views.get(0).getOrderAfter().getDelivery().getParcels().stream().map(Parcel::getCombinatorRouteId)
                .allMatch(r -> Objects.equals(r, new UUID(0, 0))));
        assertNotNull(views.get(0).getOrderBefore());
        assertTrue(views.get(0).getOrderBefore().getDelivery().getParcels().stream().map(Parcel::getRoute)
                .allMatch(Objects::isNull));
        assertTrue(views.get(0).getOrderBefore().getDelivery().getParcels().stream().map(Parcel::getCombinatorRouteId)
                .allMatch(r -> Objects.equals(r, new UUID(0, 0))));

    }

    @Test
    void shouldFilterOrdersByPartition() {

        List<Order> singleOrders = new ArrayList<>();
        List<Order> multiOrders = new ArrayList<>();

        // Генерим последовательно одиночные заказы, в количестве равном количеству партиций, чтобы точно хотя бы один
        // заказ попал под экспорт
        for (int i = 0; i < partitionsCount; i++) {
            singleOrders.add(orderCreateHelper.createOrder(defaultBlueOrderParameters()));
        }

        // Генерим по тому же принципу мультизаказы
        for (int i = 0; i < partitionsCount; i++) {
            multiOrders.addAll(orderCreateHelper.createMultiOrder(
                    defaultBlueOrderParameters().addOrder(defaultBlueOrderParameters())
            ).getOrders());
        }

        // Вычисляем, по каким заказам должна экспортировать таска события
        Set<Long> ordersExport = singleOrders.stream()
                .filter(o -> o.getId() % partitionsCount == 0)
                .map(Order::getId)
                .collect(Collectors.toSet());

        ordersExport.addAll(multiOrders.stream()
                .filter(o -> calculateSqlPartition(o.getProperty(OrderPropertyType.MULTI_ORDER_ID)) == 0)
                .map(Order::getId)
                .collect(Collectors.toSet()));

        final LogbrokerEventPublishService<OrderHistoryEventViewModel> logbrokerEventPublishServiceMock =
                Mockito.spy(LogbrokerEventPublishService.class);

        ArgumentCaptor<Collection<OrderHistoryEventViewModel>> captor = ArgumentCaptor.forClass(Collection.class);
        doReturn(Map.of()).when(logbrokerEventPublishServiceMock).publishEvents(captor.capture(), any(), any());
        ZooTask zooTask = taskMap.get(OrderEventsLbkxExportTask.EVENT_EXPORT_TASK + "_0");

        OrderEventsLbkxExportTask task = new OrderEventsLbkxExportTask(eventService,
                orderHistoryEventViewModelService,
                logbrokerEventPublishServiceMock,
                metricsPublisher,
                0.8,
                new CheckouterFeatureResolverStub(),
                null,
                null);
        task.setName("orderEventsLbkxExportTask_0");
        task.setPartition(Partition.of(0));

        task.doJob(zooTask, () -> false);

        Collection<Long> exportedOrderIds = captor.getValue()
                .stream()
                .map(e -> e.getOrderAfter().getId())
                .collect(Collectors.toSet());

        // Проверяем, что в экспорт попали только те заказы, которые подходят для этой партиции
        assertEquals(ordersExport, exportedOrderIds);
    }

    private long calculateSqlPartition(String multiOrderId) {
        return Math.abs((long) multiOrderId.hashCode()) % partitionsCount;
    }
}
