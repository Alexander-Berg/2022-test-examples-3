package ru.yandex.market.checkout.checkouter.order.changerequest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.OnDemandType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderTypeUtils;
import ru.yandex.market.checkout.checkouter.order.changerequest.ondemand.ConvertToOnDemandRequest;
import ru.yandex.market.checkout.checkouter.trace.OrderEditContextHolder;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.YaLavkaHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.yalavka.YaLavkaDeliveryServiceConfigurer;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.helpers.YaLavkaHelper.lavkaOption;
import static ru.yandex.market.checkout.helpers.YaLavkaHelper.normalOption;

public class ConvertToOnDemandRequestTest extends AbstractWebTestBase {

    @Autowired
    private EventsGetHelper eventsGetHelper;
    @Autowired
    private YaLavkaHelper yaLavkaHelper;
    @Autowired
    private YaLavkaDeliveryServiceConfigurer yaLavkaDSConfigurer;

    @BeforeEach
    public void before() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndGps();
    }

    @ParameterizedTest
    @EnumSource(value = OnDemandType.class, mode = EXCLUDE, names = {"UNKNOWN"})
    public void convertToOnDemandRequest_forOrdinaryOrderWithToggle_shouldNotEdit(OnDemandType onDemandType)
            throws Exception {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        var order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        var toOnDemandRequest = new ConvertToOnDemandRequest();
        toOnDemandRequest.setOnDemandType(onDemandType);
        var editRequest = new OrderEditRequest();
        editRequest.setConvertToOnDemandRequest(toOnDemandRequest);
        // Act & Assert
        assertOrderCantBeConverted(order, editRequest);
        assertIsNotOnDemand(order);
        assertNoDeliveryUpdatedEvent(order);
    }

    @ParameterizedTest
    @EnumSource(value = OnDemandType.class, mode = EXCLUDE, names = {"UNKNOWN"})
    public void convertToOnDemandRequest_forDeferredCourierOrder_shouldChangeFeaturesAndRaiseEvent(
            OnDemandType onDemandType)
            throws Exception {
        // Assign
        OrderEditContextHolder.OrderEditContextAttributesHolder holder =
                new OrderEditContextHolder.OrderEditContextAttributesHolder();
        var parameters = BlueParametersProvider.blueOrderWithDeferredCourierDelivery();
        var order = orderCreateHelper.createOrder(parameters);
        assertTrue(OrderTypeUtils.isDeferredCourierDelivery(order));
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        var toOnDemandRequest = new ConvertToOnDemandRequest();
        toOnDemandRequest.setOnDemandType(onDemandType);
        var editRequest = new OrderEditRequest();
        editRequest.setConvertToOnDemandRequest(toOnDemandRequest);
        var expectedDeliveryFeature = onDemandType == OnDemandType.YALAVKA
                ? DeliveryFeature.ON_DEMAND_YALAVKA
                : (onDemandType == OnDemandType.MARKET_PICKUP
                ? DeliveryFeature.ON_DEMAND_MARKET_PICKUP
                : DeliveryFeature.UNKNOWN);
        // Act
        var response = client.editOrder(order.getId(), ClientRole.SYSTEM, BuyerProvider.UID,
                singletonList(BLUE), editRequest);
        // Assert
        order = orderService.getOrder(order.getId());
        assertThat(response, hasSize(1));
        assertEquals(ChangeRequestStatus.APPLIED, response.get(0).getStatus());
        assertEquals(ChangeRequestType.CONVERT_TO_ON_DEMAND, response.get(0).getType());
        assertIsOnDemand(order, expectedDeliveryFeature);
        assertFalse(OrderTypeUtils.isDeferredCourierDelivery(order));

        PagedEvents orderHistoryEvents = eventsGetHelper.getOrderHistoryEvents(order.getId(), Integer.MAX_VALUE);
        assertTrue(orderHistoryEvents.getItems().stream()
                .anyMatch(e -> HistoryEventType.ORDER_DELIVERY_UPDATED == e.getType()));

        Assertions.assertThat(holder.getAttributes())
                .containsEntry("changeRequestStatus", ChangeRequestStatus.APPLIED);
        Assertions.assertThat(holder.getAttributes())
                .containsEntry("orderStatus", OrderStatus.DELIVERY);
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryType.class, names = {"PICKUP", "POST"})
    public void convertToOnDemandRequest_shouldNotEditForNonCourierDelivery(DeliveryType deliveryType)
            throws Exception {
        // Assign
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setDeliveryType(deliveryType);
        var order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        var toOnDemandRequest = new ConvertToOnDemandRequest();
        toOnDemandRequest.setOnDemandType(OnDemandType.YALAVKA);
        var editRequest = new OrderEditRequest();
        editRequest.setConvertToOnDemandRequest(toOnDemandRequest);
        // Act & Assert
        assertOrderCantBeConverted(order, editRequest);
        assertIsNotOnDemand(order);
        assertNoDeliveryUpdatedEvent(order);
    }

    @Test
    public void convertToOnDemandRequest_shouldNotEditForOnDemandAlready() throws Exception {
        // Assign
        yaLavkaDSConfigurer.configureOrderReservationRequest(HttpStatus.OK);
        var parameters = yaLavkaHelper.buildParameters(true,
                normalOption(0),
                lavkaOption(0)
        );

        var order = orderCreateHelper.createOrder(parameters);
        assertIsOnDemand(order, DeliveryFeature.ON_DEMAND_YALAVKA);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        var toOnDemandRequest = new ConvertToOnDemandRequest();
        toOnDemandRequest.setOnDemandType(OnDemandType.YALAVKA);
        var editRequest = new OrderEditRequest();
        editRequest.setConvertToOnDemandRequest(toOnDemandRequest);
        // Act & Assert
        assertOrderCantBeConverted(order, editRequest);
        assertNoDeliveryUpdatedEvent(order);
    }

    private void assertIsOnDemand(Order order, DeliveryFeature expectedDeliveryFeature) {
        assertTrue(OrderTypeUtils.isOnDemandDelivery(order));
        assertTrue(OrderTypeUtils.getDeliveryFeatures(order).contains(expectedDeliveryFeature));
    }

    private void assertOrderCantBeConverted(Order order, OrderEditRequest editRequest) {
        var orderId = order.getId();
        var e = assertThrows(ErrorCodeException.class, () ->
                // Act
                client.editOrder(orderId, ClientRole.SYSTEM, BuyerProvider.UID, singletonList(BLUE), editRequest)
        );
        assertEquals("Order can't be converted to On Demand", e.getMessage());
        assertEquals(400, e.getStatusCode());
    }

    private void assertIsNotOnDemand(Order order) {
        order = orderService.getOrder(order.getId());
        assertFalse(OrderTypeUtils.isOnDemandDelivery(order));
        assertFalse(OrderTypeUtils.getDeliveryFeatures(order).contains(DeliveryFeature.ON_DEMAND_YALAVKA));
        assertFalse(OrderTypeUtils.getDeliveryFeatures(order).contains(DeliveryFeature.ON_DEMAND_MARKET_PICKUP));
    }

    private void assertNoDeliveryUpdatedEvent(Order order) throws Exception {
        var orderHistoryEvents = eventsGetHelper.getOrderHistoryEvents(order.getId(), Integer.MAX_VALUE);
        assertFalse(orderHistoryEvents.getItems().stream()
                .anyMatch(event -> HistoryEventType.ORDER_DELIVERY_UPDATED == event.getType()));
    }
}
