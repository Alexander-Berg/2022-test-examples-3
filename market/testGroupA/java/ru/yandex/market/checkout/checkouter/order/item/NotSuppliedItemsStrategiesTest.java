package ru.yandex.market.checkout.checkouter.order.item;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemsCannotBeSuppliedNotification;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.OrderMissingItemsStrategy;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.postpaidBlueOrderParameters;

public class NotSuppliedItemsStrategiesTest extends MissingItemsAbstractTest {

    @Autowired
    CancellationRequestHelper cancellationRequestHelper;

    @Test
    public void test1Order1Item1ItemRemoval() {
        Parameters params = postpaidBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(params);
        OrderItem item = order.getItems().stream().findFirst().get();

        ItemsCannotBeSuppliedNotification notification = ItemsCannotBeSuppliedNotification.builder()
                .withMissingItem(new ItemInfo(item.getSupplierId(), item.getShopSku(), item.getCount()))
                .withOrderId(order.getId())
                .build();
        List<OrderMissingItemsStrategy> result = client.getNotSuppliedItemsStrategies(notification);
        assertEquals(1, result.size());
        OrderMissingItemsStrategy singleStrategy = result.get(0);
        assertEquals(order.getId(), singleStrategy.getOrderId());
        assertEquals(Collections.singleton(new ItemInfo(item.getSupplierId(), item.getShopSku(), 0)),
                singleStrategy.getRemainedItems());
    }

    @Test
    public void test1Order1Item0ItemRemoval() {
        Parameters params = postpaidBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(params);
        OrderItem item = order.getItems().stream().findFirst().get();

        ItemsCannotBeSuppliedNotification notification = ItemsCannotBeSuppliedNotification.builder()
                .withOrderId(order.getId())
                .build();
        assertThrows(ErrorCodeException.class, () -> {
            client.getNotSuppliedItemsStrategies(notification);
        });
    }

    @Test
    public void test1order2itemPcs1missing() {
        Parameters params = postpaidBlueOrderParameters();
        params.getOrder().getItems().stream().findFirst().get().setCount(2);
        Order order = orderCreateHelper.createOrder(params);
        OrderItem item = order.getItems().stream().findFirst().get();

        ItemsCannotBeSuppliedNotification notification = ItemsCannotBeSuppliedNotification.builder()
                .withMissingItem(new ItemInfo(item.getSupplierId(), item.getShopSku(), 1))
                .withOrderId(order.getId())
                .build();
        List<OrderMissingItemsStrategy> result = client.getNotSuppliedItemsStrategies(notification);
        assertEquals(1, result.size());
        OrderMissingItemsStrategy singleStrategy = result.get(0);
        assertEquals(order.getId(), singleStrategy.getOrderId());
        assertEquals(Collections.singleton(new ItemInfo(item.getSupplierId(), item.getShopSku(), 1)),
                singleStrategy.getRemainedItems());
    }

    @Test
    public void test2orderSortByShipmentDate() {
        Parameters params = postpaidBlueOrderParameters();
        ZonedDateTime now = ZonedDateTime.now(getClock());
        setFixedTime(now.plus(1, ChronoUnit.DAYS).toInstant());
        Order order1 = orderCreateHelper.createOrder(params);
        setFixedTime(now.toInstant());
        Order order2 = orderCreateHelper.createOrder(params);
        assertTrue(order2.getCreationDate().before(order1.getCreationDate()));
        assertTrue(order2.getDelivery().getParcels().get(0).getShipmentDate().isBefore(
                order1.getDelivery().getParcels().get(0).getShipmentDate()));
        assertTrue(order1.getBuyerTotal().equals(order2.getBuyerTotal()));
        OrderItem item = order1.getItems().stream().findFirst().get();

        ItemsCannotBeSuppliedNotification notification = ItemsCannotBeSuppliedNotification.builder()
                .withMissingItem(new ItemInfo(item.getSupplierId(), item.getShopSku(), 1))
                .withOrderId(order1.getId())
                .withOrderId(order2.getId())
                .build();
        List<OrderMissingItemsStrategy> result = client.getNotSuppliedItemsStrategies(notification);
        assertThat(result, Matchers.containsInAnyOrder(
                Matchers.allOf(
                        Matchers.hasProperty("orderId", is(order2.getId())),
                        Matchers.hasProperty("remainedItems", is(
                                Collections.singleton(new ItemInfo(item.getSupplierId(), item.getShopSku(), 1))))
                ),

                Matchers.allOf(
                        Matchers.hasProperty("orderId", is(order1.getId())),
                        Matchers.hasProperty("remainedItems", is(
                                Collections.singleton(new ItemInfo(item.getSupplierId(), item.getShopSku(), 0))))
                )
        ));
    }

    @Test
    public void test2orderSortByCreationDate() {
        Parameters params = postpaidBlueOrderParameters();
        LocalDate today = LocalDate.now(getClock());
        setFixedTime(today.atTime(10, 00).atZone(ZoneId.systemDefault()).toInstant());
        Order order1 = orderCreateHelper.createOrder(params);
        setFixedTime(today.atTime(11, 00).atZone(ZoneId.systemDefault()).toInstant());
        Order order2 = orderCreateHelper.createOrder(params);
        assertTrue(order1.getCreationDate().before(order2.getCreationDate()));
        assertTrue(order1.getDelivery().getParcels().get(0).getShipmentDate().equals(
                order2.getDelivery().getParcels().get(0).getShipmentDate()));
        assertTrue(order1.getBuyerTotal().equals(order2.getBuyerTotal()));
        OrderItem item = order1.getItems().stream().findFirst().get();

        ItemsCannotBeSuppliedNotification notification = ItemsCannotBeSuppliedNotification.builder()
                .withMissingItem(new ItemInfo(item.getSupplierId(), item.getShopSku(), 1))
                .withOrderId(order1.getId())
                .withOrderId(order2.getId())
                .build();
        List<OrderMissingItemsStrategy> result = client.getNotSuppliedItemsStrategies(notification);
        assertThat(result, Matchers.containsInAnyOrder(
                Matchers.allOf(
                        Matchers.hasProperty("orderId", is(order1.getId())),
                        Matchers.hasProperty("remainedItems", is(
                                Collections.singleton(new ItemInfo(item.getSupplierId(), item.getShopSku(), 1))))
                ),

                Matchers.allOf(
                        Matchers.hasProperty("orderId", is(order2.getId())),
                        Matchers.hasProperty("remainedItems", is(
                                Collections.singleton(new ItemInfo(item.getSupplierId(), item.getShopSku(), 0))))
                )
        ));
    }

    @Test
    public void test2orderSortByTotal() {
        Parameters params = postpaidBlueOrderParameters();
        setFixedTime(getClock().instant());
        Order order1 = orderCreateHelper.createOrder(params);
        params.getOrder().getItems().stream().findFirst().get().setCount(2);
        Order order2 = orderCreateHelper.createOrder(params);
        assertTrue(order1.getCreationDate().equals(order2.getCreationDate()));
        assertTrue(order1.getDelivery().getParcels().get(0).getShipmentDate().equals(
                order2.getDelivery().getParcels().get(0).getShipmentDate()));
        assertTrue(order1.getBuyerTotal().compareTo(order2.getBuyerTotal()) < 0);
        OrderItem item = order1.getItems().stream().findFirst().get();

        ItemsCannotBeSuppliedNotification notification = ItemsCannotBeSuppliedNotification.builder()
                .withMissingItem(new ItemInfo(item.getSupplierId(), item.getShopSku(), 1))
                .withOrderId(order1.getId())
                .withOrderId(order2.getId())
                .build();
        List<OrderMissingItemsStrategy> result = client.getNotSuppliedItemsStrategies(notification);
        assertThat(result, Matchers.containsInAnyOrder(
                Matchers.allOf(
                        Matchers.hasProperty("orderId", is(order2.getId())),
                        Matchers.hasProperty("remainedItems", is(
                                Collections.singleton(new ItemInfo(item.getSupplierId(), item.getShopSku(), 2))))
                ),

                Matchers.allOf(
                        Matchers.hasProperty("orderId", is(order1.getId())),
                        Matchers.hasProperty("remainedItems", is(
                                Collections.singleton(new ItemInfo(item.getSupplierId(), item.getShopSku(), 0))))
                )
        ));
    }

    @Test
    public void test2orderSortByCancelled() {
        Parameters params = postpaidBlueOrderParameters();
        ZonedDateTime now = ZonedDateTime.now(getClock());
        setFixedTime(now.plus(1, ChronoUnit.DAYS).toInstant());
        Order order1 = orderCreateHelper.createOrder(params);
        setFixedTime(now.toInstant());
        Order order2 = orderCreateHelper.createOrder(params);
        assertTrue(order2.getCreationDate().before(order1.getCreationDate()));
        assertTrue(order2.getDelivery().getParcels().get(0).getShipmentDate().isBefore(
                order1.getDelivery().getParcels().get(0).getShipmentDate()));
        assertTrue(order1.getBuyerTotal().equals(order2.getBuyerTotal()));
        order2 = orderStatusHelper.updateOrderStatus(order2.getId(), OrderStatus.CANCELLED,
                OrderSubstatus.USER_CHANGED_MIND);

        OrderItem item = order1.getItems().stream().findFirst().get();

        ItemsCannotBeSuppliedNotification notification = ItemsCannotBeSuppliedNotification.builder()
                .withMissingItem(new ItemInfo(item.getSupplierId(), item.getShopSku(), 1))
                .withOrderId(order1.getId())
                .withOrderId(order2.getId())
                .build();
        List<OrderMissingItemsStrategy> result = client.getNotSuppliedItemsStrategies(notification);
        assertThat(result, Matchers.containsInAnyOrder(
                Matchers.allOf(
                        Matchers.hasProperty("orderId", is(order2.getId())),
                        Matchers.hasProperty("remainedItems", is(
                                Collections.singleton(new ItemInfo(item.getSupplierId(), item.getShopSku(), 0))))
                ),

                Matchers.allOf(
                        Matchers.hasProperty("orderId", is(order1.getId())),
                        Matchers.hasProperty("remainedItems", is(
                                Collections.singleton(new ItemInfo(item.getSupplierId(), item.getShopSku(), 1))))
                )
        ));
    }

    @Test
    public void test3orderSortByCancelledAndCancellationRequest() throws Exception {
        Parameters params = postpaidBlueOrderParameters();
        ZonedDateTime now = ZonedDateTime.now(getClock());
        setFixedTime(now.plus(2, ChronoUnit.DAYS).toInstant());
        Order order1 = orderCreateHelper.createOrder(params);
        setFixedTime(now.plus(1, ChronoUnit.DAYS).toInstant());
        Order order2 = orderCreateHelper.createOrder(params);
        setFixedTime(now.toInstant());
        Order order3 = orderCreateHelper.createOrder(params);
        assertTrue(order2.getCreationDate().before(order1.getCreationDate()));
        assertTrue(order2.getDelivery().getParcels().get(0).getShipmentDate().isBefore(
                order1.getDelivery().getParcels().get(0).getShipmentDate()));
        assertTrue(order1.getBuyerTotal().equals(order2.getBuyerTotal()));
        order2 = cancellationRequestHelper.createCancellationRequest(order2.getId(),
                new CancellationRequest(OrderSubstatus.USER_CHANGED_MIND, ""),
                new ClientInfo(ClientRole.USER, BuyerProvider.UID));

        order3 = orderStatusHelper.updateOrderStatus(order3.getId(), OrderStatus.CANCELLED,
                OrderSubstatus.USER_CHANGED_MIND);

        OrderItem item = order1.getItems().stream().findFirst().get();

        ItemsCannotBeSuppliedNotification notification = ItemsCannotBeSuppliedNotification.builder()
                .withMissingItem(new ItemInfo(item.getSupplierId(), item.getShopSku(), 2))
                .withOrderId(order1.getId())
                .withOrderId(order2.getId())
                .withOrderId(order3.getId())
                .build();
        List<OrderMissingItemsStrategy> result = client.getNotSuppliedItemsStrategies(notification);
        assertThat(result, Matchers.containsInAnyOrder(
                Matchers.allOf(
                        Matchers.hasProperty("orderId", is(order3.getId())),
                        Matchers.hasProperty("remainedItems", is(
                                Collections.singleton(new ItemInfo(item.getSupplierId(), item.getShopSku(), 0))))
                ),

                Matchers.allOf(
                        Matchers.hasProperty("orderId", is(order2.getId())),
                        Matchers.hasProperty("remainedItems", is(
                                Collections.singleton(new ItemInfo(item.getSupplierId(), item.getShopSku(), 0))))
                ),

                Matchers.allOf(
                        Matchers.hasProperty("orderId", is(order1.getId())),
                        Matchers.hasProperty("remainedItems", is(
                                Collections.singleton(new ItemInfo(item.getSupplierId(), item.getShopSku(), 1))))
                )
        ));
    }
}
