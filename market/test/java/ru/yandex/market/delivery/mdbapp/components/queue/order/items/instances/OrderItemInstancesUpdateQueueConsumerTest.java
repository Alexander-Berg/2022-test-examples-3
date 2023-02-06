package ru.yandex.market.delivery.mdbapp.components.queue.order.items.instances;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstancesPutRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItems;
import ru.yandex.market.delivery.mdbapp.components.service.LogisticsOrderService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.delivery.mdbapp.integration.converter.BlueOrderItemConverter;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

@ExtendWith(SpringExtension.class)
class OrderItemInstancesUpdateQueueConsumerTest {

    private static final long LOM_ORDER_ID = 10433;
    private static final long ORDER_ITEM_ID = 234234;
    private static final long CHECKOUT_ORDER_ID = 234;
    private static final String ARTICLE_SHOP_SKU = "test";
    private static final String CIS_TEST_VALUE = "testCis";
    private static final String CIS_FULL_TEST_VALUE = "testCisFull";
    private static final String IMEI_TEST_VALUE = "testImei";
    private static final String SN_TEST_VALUE = "testSn";
    private static final String UIT_TEST_VALUE = "testUit";
    private static final long SUPPLIER_VENDOR_ID = 456L;

    @MockBean
    private LogisticsOrderService logisticsOrderService;
    @MockBean
    private CheckouterOrderService checkouterOrderService;

    private OrderItemInstancesUpdateQueueConsumer orderItemInstancesUpdateQueueConsumer;

    private final BlueOrderItemConverter orderItemConverter = new BlueOrderItemConverter(null, null, null);

    @BeforeEach
    void setUp() {
        orderItemInstancesUpdateQueueConsumer = new OrderItemInstancesUpdateQueueConsumer(
            checkouterOrderService,
            logisticsOrderService,
            orderItemConverter
        );
    }

    @Test
    @DisplayName("Сервисы возвращают пустые данные")
    void executeFail() {
        var result = orderItemInstancesUpdateQueueConsumer.execute(getTask());
        Assertions.assertEquals(TaskExecutionResult.fail(), result);
        Mockito.verify(checkouterOrderService, Mockito.never()).putOrderItemInstances(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("Пустой заказ")
    void executeEmptyOrder() {
        Mockito
            .when(logisticsOrderService.getByIdOrThrow(Mockito.eq(LOM_ORDER_ID), Mockito.anySet()))
            .thenReturn(getLomOrder());
        Mockito.when(checkouterOrderService.getOrder(Mockito.eq(CHECKOUT_ORDER_ID)))
            .thenReturn(getOrder());

        var result = orderItemInstancesUpdateQueueConsumer.execute(getTask());
        Assertions.assertEquals(TaskExecutionResult.finish(), result);
        Mockito.verify(checkouterOrderService, Mockito.never()).putOrderItemInstances(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("Не нашли айтемов в чекаутере, которые нужно обновить")
    void executeNotEmptyOrder() {
        Mockito
            .when(logisticsOrderService.getByIdOrThrow(Mockito.eq(LOM_ORDER_ID), Mockito.anySet()))
            .thenReturn(getLomOrder());
        var order = getOrder();

        order.setItems(List.of(getOrderItem("NONONO", 999L)));

        Mockito.when(checkouterOrderService.getOrder(Mockito.eq(CHECKOUT_ORDER_ID)))
            .thenReturn(order);

        var result = orderItemInstancesUpdateQueueConsumer.execute(getTask());
        Assertions.assertEquals(TaskExecutionResult.finish(), result);
        Mockito.verify(checkouterOrderService, Mockito.never()).putOrderItemInstances(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("Находим один айтем из трёх пришедших и обновляем его маркировки")
    void executeOk() {
        Mockito.when(logisticsOrderService.getByIdOrThrow(Mockito.eq(LOM_ORDER_ID), Mockito.anySet()))
            .thenReturn(getLomOrder());

        var order = getOrder();
        order.setItems(List.of(getOrderItem(ARTICLE_SHOP_SKU, SUPPLIER_VENDOR_ID)));
        Mockito.when(checkouterOrderService.getOrder(Mockito.eq(CHECKOUT_ORDER_ID))).thenReturn(order);

        var requestCaptor = ArgumentCaptor.forClass(OrderItemInstancesPutRequest.class);
        Mockito.when(
                checkouterOrderService.putOrderItemInstances(
                    Mockito.eq(CHECKOUT_ORDER_ID),
                    requestCaptor.capture()
                )
            )
            .thenReturn(new OrderItems());

        var result = orderItemInstancesUpdateQueueConsumer.execute(getTask());
        Assertions.assertEquals(TaskExecutionResult.finish(), result);

        Mockito.verify(checkouterOrderService)
            .putOrderItemInstances(Mockito.eq(CHECKOUT_ORDER_ID), Mockito.any(OrderItemInstancesPutRequest.class));

        OrderItemInstancesPutRequest request = requestCaptor.getValue();
        List<OrderItemInstance> instances = request
            .getItems()
            .stream()
            .flatMap(i -> i.getInstances().stream())
            .collect(Collectors.toList());

        OrderItemInstance orderItemInstance1 = new OrderItemInstance();
        orderItemInstance1.setCis(CIS_FULL_TEST_VALUE);
        orderItemInstance1.setImei(IMEI_TEST_VALUE);
        orderItemInstance1.setSn(SN_TEST_VALUE);
        orderItemInstance1.setUit(UIT_TEST_VALUE);

        OrderItemInstance orderItemInstance2 = new OrderItemInstance();
        orderItemInstance2.setCis(CIS_TEST_VALUE + "-1");

        Assertions.assertAll(
            () -> Assertions.assertEquals(1, request.getItems().size(), "Только один товар"),
            () -> Assertions.assertEquals(2, instances.size(), "Только 2 набора маркировок"),
            () -> Assertions.assertTrue(instances.contains(orderItemInstance1), "Первый набор маркировок"),
            () -> Assertions.assertTrue(instances.contains(orderItemInstance2), "Второй набор маркировок")
        );
    }

    OrderItem getOrderItem(String article, Long vendorId) {
        var orderItem = new OrderItem();
        orderItem.setId(ORDER_ITEM_ID);
        orderItem.setShopSku(article);
        orderItem.setSupplierId(vendorId);
        orderItem.setCount(2);
        return orderItem;
    }

    Task<OrderItemInstancesUpdateDto> getTask() {
        var dto = new OrderItemInstancesUpdateDto(LOM_ORDER_ID);
        return new Task<>(
            new QueueShardId("order.delivery.option.change"),
            dto,
            3,
            ZonedDateTime.now(),
            null,
            null
        );
    }

    private OrderDto getLomOrder() {
        return new OrderDto()
            .setId(LOM_ORDER_ID)
            .setItems(List.of(
                ItemDto.builder()
                    .vendorId(SUPPLIER_VENDOR_ID)
                    .article(ARTICLE_SHOP_SKU)
                    .count(3)
                    .instances(List.of(
                        Map.of(
                            OrderItemInstanceKeys.CIS, CIS_TEST_VALUE,
                            OrderItemInstanceKeys.CIS_FULL, CIS_FULL_TEST_VALUE,
                            OrderItemInstanceKeys.UIT, UIT_TEST_VALUE,
                            OrderItemInstanceKeys.IMEI, IMEI_TEST_VALUE,
                            OrderItemInstanceKeys.SN, SN_TEST_VALUE
                        ),
                        Map.of(OrderItemInstanceKeys.CIS, CIS_TEST_VALUE + "-1")
                    ))
                    .build()
            ))
            .setExternalId(String.valueOf(CHECKOUT_ORDER_ID));
    }

    public Order getOrder() {
        Delivery delivery = new Delivery();
        delivery.setPrice(BigDecimal.TEN);

        Order order = new Order();
        order.setId(CHECKOUT_ORDER_ID);
        order.setShopId(CHECKOUT_ORDER_ID);
        order.setDelivery(delivery);
        return order;
    }
}
