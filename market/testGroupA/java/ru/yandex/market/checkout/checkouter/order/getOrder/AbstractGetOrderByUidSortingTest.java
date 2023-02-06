package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.RequestBuilder;

import ru.yandex.bolts.collection.Option;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.RecentOrderViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.containers.PagedOrderViewModel;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.YaLavkaHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.util.balance.ShopSettingsHelper.getDefaultMeta;

public abstract class AbstractGetOrderByUidSortingTest extends AbstractWebTestBase {

    @Resource(name = "checkouterAnnotationObjectMapper")
    protected ObjectMapper objectMapper;
    private Order unpaidOrder;
    private Order deferredCourierLastMileStartedOrder;
    private Order deferredCourierReadyForLastMileOrder;
    private Order onDemandReadyForLastMileOrder;
    private Order pickupInOutletOrder;
    private Order deliveryFirstOrder;
    private Order deliverySecondOrder;
    private Order pickupThirdOrder;
    private Order deliveryUserReceivedFirstOrder;
    private Order pickupUserReceivedSecondOrder;
    private Order deliveredFirstOrder;
    private Order cancelledSecondOrder;
    private static final int ORDERS_NUMBER = 12;
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    protected YaLavkaHelper yaLavkaHelper;

    protected void createShuffledOrders() throws Exception {
        List<TestOrderCreator> orderCreators = new ArrayList<>();
        orderCreators.add(unpaidOrderCreator());
        orderCreators.add(deferredCourierLastMileStartedOrderCreator());
        orderCreators.add(deferredCourierReadyForLastMileOrderCreator());
        orderCreators.add(onDemandReadyForLastMileOrderCreator());
        orderCreators.add(pickupInOutletOrderCreator());
        orderCreators.add(deliveryFirstOrderCreator());
        orderCreators.add(deliverySecondOrderCreator());
        orderCreators.add(pickupThirdOrderCreator());
        orderCreators.add(deliveryUserReceivedFirstOrderCreator());
        orderCreators.add(pickupUserReceivedSecondOrderCreator());
        orderCreators.add(deliveredFirstOrderCreator());
        orderCreators.add(cancelledSecondOrderCreator());

        Collections.shuffle(orderCreators);
        for (TestOrderCreator orderCreator : orderCreators) {
            orderCreator.create();
        }
    }

    protected void checkByImportanceOrderSorting(RequestBuilder requestBuilder) throws Exception {
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*]", hasSize(ORDERS_NUMBER)))
                .andExpect(jsonPath("$.orders[0].id").value(unpaidOrder.getId()))
                .andExpect(jsonPath("$.orders[1].id").value(deferredCourierLastMileStartedOrder.getId()))
                .andExpect(jsonPath("$.orders[2].id").value(deferredCourierReadyForLastMileOrder.getId()))
                .andExpect(jsonPath("$.orders[3].id").value(onDemandReadyForLastMileOrder.getId()))
                .andExpect(jsonPath("$.orders[4].id").value(pickupInOutletOrder.getId()))
                .andExpect(jsonPath("$.orders[5].id").value(deliveryFirstOrder.getId()))
                .andExpect(jsonPath("$.orders[6].id").value(deliverySecondOrder.getId()))
                .andExpect(jsonPath("$.orders[7].id").value(pickupThirdOrder.getId()))
                .andExpect(jsonPath("$.orders[8].id").value(deliveryUserReceivedFirstOrder.getId()))
                .andExpect(jsonPath("$.orders[9].id").value(pickupUserReceivedSecondOrder.getId()))
                .andExpect(jsonPath("$.orders[10].id").value(deliveredFirstOrder.getId()))
                .andExpect(jsonPath("$.orders[11].id").value(cancelledSecondOrder.getId()));
    }

    protected void checkByImportanceOrderSortingRecent(RequestBuilder requestBuilder) throws Exception {
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*]", hasSize(ORDERS_NUMBER)))
                .andExpect(jsonPath("$[0].id").value(unpaidOrder.getId()))
                .andExpect(jsonPath("$[1].id").value(deferredCourierLastMileStartedOrder.getId()))
                .andExpect(jsonPath("$[2].id").value(deferredCourierReadyForLastMileOrder.getId()))
                .andExpect(jsonPath("$[3].id").value(onDemandReadyForLastMileOrder.getId()))
                .andExpect(jsonPath("$[4].id").value(pickupInOutletOrder.getId()))
                .andExpect(jsonPath("$[5].id").value(deliveryFirstOrder.getId()))
                .andExpect(jsonPath("$[6].id").value(deliverySecondOrder.getId()))
                .andExpect(jsonPath("$[7].id").value(pickupThirdOrder.getId()))
                .andExpect(jsonPath("$[8].id").value(deliveryUserReceivedFirstOrder.getId()))
                .andExpect(jsonPath("$[9].id").value(pickupUserReceivedSecondOrder.getId()))
                .andExpect(jsonPath("$[10].id").value(deliveredFirstOrder.getId()))
                .andExpect(jsonPath("$[11].id").value(cancelledSecondOrder.getId()));
    }

    protected void checkByDateOrderSortingRecent(RequestBuilder requestBuilder) throws Exception {
        String response = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<RecentOrderViewModel> recentOrders = Arrays.asList(objectMapper.readValue(response,
                RecentOrderViewModel[].class));

        assertEquals(ORDERS_NUMBER, recentOrders.size());

        List<Date> creationDates = recentOrders.stream()
                .map(RecentOrderViewModel::getCreationDate)
                .collect(Collectors.toList());

        checkCreationDateList(creationDates);
    }

    protected void checkByDateOrderSorting(RequestBuilder requestBuilder) throws Exception {
        String response = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PagedOrderViewModel pagedOrders = objectMapper.readValue(response, PagedOrderViewModel.class);
        List<Date> creationDates = pagedOrders.getItems().stream()
                .map(OrderViewModel::getCreationDate)
                .collect(Collectors.toList());

        checkCreationDateList(creationDates);
    }

    private void checkCreationDateList(List<Date> creationDates) {
        assertEquals(creationDates.stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList()), creationDates);
    }


    private TestOrderCreator unpaidOrderCreator() {
        return () ->
                unpaidOrder = orderServiceHelper.saveOrder(
                        OrderProvider.getPrepaidOrder(o -> o.setBuyer(BuyerProvider.getBuyer())));
    }

    private TestOrderCreator deferredCourierLastMileStartedOrderCreator() {
        return () -> {
            deferredCourierLastMileStartedOrder =
                    createDeferredCourierOrderInDelivery(OrderSubstatus.LAST_MILE_STARTED);
        };
    }

    private TestOrderCreator deferredCourierReadyForLastMileOrderCreator() {
        return () -> {
            deferredCourierReadyForLastMileOrder =
                    createDeferredCourierOrderInDelivery(OrderSubstatus.READY_FOR_LAST_MILE);
        };
    }

    private Order createDeferredCourierOrderInDelivery(OrderSubstatus substatus) throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                Collections.<String>emptySet()));
        Parameters parameters = yaLavkaHelper.buildParametersForDeferredCourier(1);
        Order order = orderCreateHelper.createOrder(parameters);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        return orderStatusHelper.requestStatusUpdate(
                order.getId(),
                ClientRole.SYSTEM, "1234",
                OrderStatus.DELIVERY, substatus);
    }

    private TestOrderCreator onDemandReadyForLastMileOrderCreator() {
        return () -> {
            ShopSettingsHelper.createShopSettings(shopService, OrderProvider.SHOP_ID, getDefaultMeta());
            onDemandReadyForLastMileOrder = OrderProvider.getPrepaidOrder(o -> {
                        o.setDelivery(DeliveryProvider.getOnDemandDelivery());
                        o.setBuyer(BuyerProvider.getBuyer());
                    }
            );
            onDemandReadyForLastMileOrder = orderServiceHelper.saveOrder(onDemandReadyForLastMileOrder);
            orderStatusHelper.proceedOrderToStatus(onDemandReadyForLastMileOrder, OrderStatus.DELIVERY);
            onDemandReadyForLastMileOrder = orderStatusHelper.requestStatusUpdate(onDemandReadyForLastMileOrder.getId(),
                    ClientRole.SYSTEM, "1234",
                    OrderStatus.DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);
        };
    }

    private TestOrderCreator pickupInOutletOrderCreator() {
        return () -> {
            pickupInOutletOrder = OrderProvider.getBlueOrder(o -> {
                o.setDelivery(DeliveryProvider.getYandexMarketPickupDelivery());
                o.setBuyer(BuyerProvider.getBuyer());
            });
            pickupInOutletOrder = orderServiceHelper.saveOrder(pickupInOutletOrder);
            orderStatusHelper.proceedOrderToStatus(pickupInOutletOrder, OrderStatus.DELIVERY);
            pickupInOutletOrder = orderStatusHelper.requestStatusUpdate(pickupInOutletOrder.getId(), ClientRole.SYSTEM,
                    "1234", OrderStatus.PICKUP, OrderSubstatus.PICKUP_SERVICE_RECEIVED);
        };
    }

    private TestOrderCreator deliveryFirstOrderCreator() {
        return () ->
                deliveryFirstOrder =
                        createOrderWithStatusDelivery(DeliveryProvider
                                .yandexDelivery()
                                .nextDays(1)
                                .build());
    }

    private TestOrderCreator deliverySecondOrderCreator() {
        return () ->
                deliverySecondOrder =
                        createOrderWithStatusDelivery(DeliveryProvider
                                .yandexDelivery()
                                .nextDays(2)
                                .build());
    }

    private TestOrderCreator pickupThirdOrderCreator() {
        return () ->
                pickupThirdOrder =
                        createOrderWithStatusDelivery(DeliveryProvider
                                .yandexPickupDelivery()
                                .nextDays(3)
                                .build());
    }

    private TestOrderCreator deliveryUserReceivedFirstOrderCreator() {
        return () -> {
            deliveryUserReceivedFirstOrder =
                    createOrderWithStatusDelivery(DeliveryProvider
                            .yandexDelivery()
                            .today()
                            .build());

            deliveryUserReceivedFirstOrder =
                    orderStatusHelper.requestStatusUpdate(deliveryUserReceivedFirstOrder.getId(), ClientRole.SYSTEM,
                            "1234", OrderStatus.DELIVERY, OrderSubstatus.USER_RECEIVED);
        };
    }

    private TestOrderCreator pickupUserReceivedSecondOrderCreator() {
        return () -> {
            pickupUserReceivedSecondOrder =
                    createOrderWithStatusDelivery(DeliveryProvider
                            .yandexPickupDelivery()
                            .nextDays(2)
                            .build());

            orderStatusHelper.proceedOrderToStatus(pickupUserReceivedSecondOrder, OrderStatus.PICKUP);
            pickupUserReceivedSecondOrder =
                    orderStatusHelper.requestStatusUpdate(pickupUserReceivedSecondOrder.getId(), ClientRole.SYSTEM,
                            "1234", OrderStatus.PICKUP, OrderSubstatus.PICKUP_USER_RECEIVED);
        };
    }

    private TestOrderCreator deliveredFirstOrderCreator() {
        return () -> {
            // если cancelledSecondOrder уже создан, нужно гарантированно создать
            // deliveredFirstOrder после него
            Instant creationTime = Option.ofNullable(cancelledSecondOrder)
                    .map(Order::getCreationDate)
                    .map(date -> date.toInstant().plus(1, ChronoUnit.MINUTES))
                    .orElseGet(() -> getClock().instant().plus(1, ChronoUnit.MINUTES));

            setFixedTime(creationTime);

            deliveredFirstOrder = OrderProvider.getBlueOrder((order -> {
                order.setBuyer(BuyerProvider.getBuyer());
                order.setDelivery(DeliveryProvider.getYandexMarketDelivery(true));
            }));
            deliveredFirstOrder = orderServiceHelper.saveOrder(deliveredFirstOrder);
            orderStatusHelper.proceedOrderToStatus(deliveredFirstOrder, OrderStatus.DELIVERY);
            deliveredFirstOrder = orderStatusHelper.requestStatusUpdate(deliveredFirstOrder.getId(), ClientRole.SYSTEM,
                    "1234", OrderStatus.DELIVERED, OrderSubstatus.DELIVERY_SERVICE_DELIVERED);

        };
    }

    private TestOrderCreator cancelledSecondOrderCreator() {
        return () -> {
            // если deliveredFirstOrder уже создан, нужно гарантированно создать
            // cancelledSecondOrder до него
            Instant creationTime = Option.ofNullable(deliveredFirstOrder)
                    .map(Order::getCreationDate)
                    .map(date -> date.toInstant().minus(1, ChronoUnit.MINUTES))
                    .orElseGet(() -> getClock().instant().plus(1, ChronoUnit.MINUTES));

            setFixedTime(creationTime);

            cancelledSecondOrder = OrderProvider.getBlueOrder((order -> {
                order.setBuyer(BuyerProvider.getBuyer());
                order.setDelivery(DeliveryProvider.getYandexMarketDelivery(true));
            }));
            cancelledSecondOrder = orderServiceHelper.saveOrder(cancelledSecondOrder);
            orderStatusHelper.proceedOrderToStatus(cancelledSecondOrder, OrderStatus.DELIVERY);
            cancelledSecondOrder = orderStatusHelper.requestStatusUpdate(cancelledSecondOrder.getId(),
                    ClientRole.SYSTEM,
                    "1234", OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND);

        };
    }

    private Order createOrderWithStatusDelivery(Delivery delivery) {
        Order order = OrderProvider.getBlueOrder(o -> {
            o.setDelivery(delivery);
            o.setBuyer(BuyerProvider.getBuyer());
        });

        order = orderServiceHelper.saveOrder(order);
        return orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
    }

    @FunctionalInterface
    private interface TestOrderCreator {

        void create() throws Exception;
    }
}
