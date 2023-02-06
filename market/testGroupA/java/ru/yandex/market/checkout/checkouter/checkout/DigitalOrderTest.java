package ru.yandex.market.checkout.checkouter.checkout;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.digital.DeliverDigitalItemRequest;
import ru.yandex.market.checkout.checkouter.order.digital.DigitalItemContent;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.json.JsonTest;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DigitalOrderTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer persNotifyMock;

    @Test
    void shouldNotAllowCancelPaidDigitalOrder() {
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        assertThrows(
                ErrorCodeException.class,
                () -> client.updateOrderStatus(order.getId(),
                        ClientRole.USER, order.getBuyer().getUid(), null,
                        OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND
                )
        );
    }

    @Test
    void shouldAllowCancelPaidDigitalOrderForSystem() {
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        client.updateOrderStatus(order.getId(),
                ClientRole.SYSTEM, 1L, order.getShopId(),
                OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND
        );
    }

    @Test
    void shouldDeliverDigitalGoods() {
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        persNotifyMock.stubFor(WireMock.any(WireMock.anyUrl())
                .willReturn(new ResponseDefinitionBuilder().withStatus(201)));
        client.deliverDigitalGoods(order.getId(), createDeliverDigitalItemRequest(order));
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.DELIVERY, order.getStatus());
    }

    @Test
    void shouldSendPersonalEmailId() {
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        persNotifyMock.stubFor(WireMock.any(WireMock.anyUrl())
                .willReturn(new ResponseDefinitionBuilder().withStatus(201)));
        client.deliverDigitalGoods(order.getId(), createDeliverDigitalItemRequest(order));
        List<ServeEvent> allServeEvents = persNotifyMock.getAllServeEvents();
        assertThat(allServeEvents, hasSize(1));
        ServeEvent event = allServeEvents.iterator().next();

        JsonTest.checkJson(
                event.getRequest().getBodyAsString(),
                "$.personalEmailId",
                BuyerProvider.PERSONAL_EMAIL_ID);
    }

    @Test
    void shouldValidateMissingDigitalItem() {
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        DeliverDigitalItemRequest deliverDigitalItemRequest = createDeliverDigitalItemRequest(order);
        deliverDigitalItemRequest.getItems().forEach((key, value) -> value.clear());
        ErrorCodeException errorCodeException = assertThrows(ErrorCodeException.class,
                () -> client.deliverDigitalGoods(order.getId(), deliverDigitalItemRequest));

        assertEquals("INVALID_REQUEST", errorCodeException.getCode());
        assertEquals(400, errorCodeException.getStatusCode());
        assertNotNull(errorCodeException.getMessage());
        OrderItem onlyItem = order.getItems().iterator().next();
        assertEquals("Required=" + onlyItem.getCount() + " digital items, found=0 for item.id="
                        + onlyItem.getId(),
                errorCodeException.getMessage());
    }

    @Test
    void shouldValidateMissingActivateTill() {
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        DeliverDigitalItemRequest deliverDigitalItemRequest = createDeliverDigitalItemRequest(
                order,
                "code",
                "description",
                null
        );
        ErrorCodeException errorCodeException = assertThrows(ErrorCodeException.class,
                () -> client.deliverDigitalGoods(order.getId(), deliverDigitalItemRequest));

        assertEquals("INVALID_REQUEST", errorCodeException.getCode());
        assertEquals(400, errorCodeException.getStatusCode());
        assertNotNull(errorCodeException.getMessage());
        assertEquals("Missing activateTill;", errorCodeException.getMessage());
    }

    @Test
    void shouldValidateMissingCode() {
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        DeliverDigitalItemRequest deliverDigitalItemRequest = createDeliverDigitalItemRequest(
                order,
                null,
                "description",
                nextDay()
        );
        ErrorCodeException errorCodeException = assertThrows(ErrorCodeException.class,
                () -> client.deliverDigitalGoods(order.getId(), deliverDigitalItemRequest));

        assertEquals("INVALID_REQUEST", errorCodeException.getCode());
        assertEquals(400, errorCodeException.getStatusCode());
        assertNotNull(errorCodeException.getMessage());
        assertEquals("Missing code;", errorCodeException.getMessage());
    }

    @Test
    void shouldValidateTooLongCode() {
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        DeliverDigitalItemRequest deliverDigitalItemRequest = createDeliverDigitalItemRequest(
                order,
                "String with more then 256 characters. " +
                        "ABCDEFGHIGKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890" +
                        "String with more then 256 characters. " +
                        "ABCDEFGHIGKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890" +
                        "String with more then 256 characters. " +
                        "ABCDEFGHIGKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890",
                "description",
                nextDay()
        );
        ErrorCodeException errorCodeException = assertThrows(ErrorCodeException.class,
                () -> client.deliverDigitalGoods(order.getId(), deliverDigitalItemRequest));

        assertEquals("INVALID_REQUEST", errorCodeException.getCode());
        assertEquals(400, errorCodeException.getStatusCode());
        assertNotNull(errorCodeException.getMessage());
        assertEquals("Code length should be less then 256;", errorCodeException.getMessage());
    }

    @Test
    void shouldValidateMissingSlip() {
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        DeliverDigitalItemRequest deliverDigitalItemRequest = createDeliverDigitalItemRequest(
                order,
                "code",
                null,
                nextDay()
        );
        ErrorCodeException errorCodeException = assertThrows(ErrorCodeException.class,
                () -> client.deliverDigitalGoods(order.getId(), deliverDigitalItemRequest));

        assertEquals("INVALID_REQUEST", errorCodeException.getCode());
        assertEquals(400, errorCodeException.getStatusCode());
        assertNotNull(errorCodeException.getMessage());
        assertEquals("Missing slip;", errorCodeException.getMessage());
    }

    @Test
    void shouldValidateThatOrderIsDigital() {
        Parameters whiteParameters = WhiteParametersProvider.defaultWhiteParameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        DeliverDigitalItemRequest deliverDigitalItemRequest = createDeliverDigitalItemRequest(order);
        ErrorCodeException errorCodeException = assertThrows(ErrorCodeException.class,
                () -> client.deliverDigitalGoods(order.getId(), deliverDigitalItemRequest));

        assertEquals("NON_DIGITAL_ORDER", errorCodeException.getCode());
        assertEquals(400, errorCodeException.getStatusCode());
        assertNotNull(errorCodeException.getMessage());
        assertEquals("Order.id=" + order.getId() + " is not digital.", errorCodeException.getMessage());
    }

    @Test
    void shouldValidateOrderStatus() {
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        DeliverDigitalItemRequest deliverDigitalItemRequest = createDeliverDigitalItemRequest(order);
        ErrorCodeException errorCodeException = assertThrows(ErrorCodeException.class,
                () -> client.deliverDigitalGoods(order.getId(), deliverDigitalItemRequest));

        assertEquals("INVALID_REQUEST", errorCodeException.getCode());
        assertEquals(400, errorCodeException.getStatusCode());
        assertNotNull(errorCodeException.getMessage());
        assertEquals("Invalid order status: expected=PROCESSING, got=CANCELLED", errorCodeException.getMessage());
    }

    @Test
    void shouldValidateOnlyOrderItemsRequested() {
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        DeliverDigitalItemRequest deliverDigitalItemRequest = createDeliverDigitalItemRequest(order);
        Long nonExistingId = deliverDigitalItemRequest.getItems().keySet().stream()
                .max(Long::compareTo)
                .map(id -> id + 1)
                .orElse(1L);
        deliverDigitalItemRequest.getItems()
                .put(nonExistingId, singletonList(new DigitalItemContent("code2", "test", nextDay())));

        ErrorCodeException errorCodeException = assertThrows(ErrorCodeException.class,
                () -> client.deliverDigitalGoods(order.getId(), deliverDigitalItemRequest));

        assertEquals("INVALID_REQUEST", errorCodeException.getCode());
        assertEquals(400, errorCodeException.getStatusCode());
        assertNotNull(errorCodeException.getMessage());
        assertEquals(String.format("Order %s doesn't contain requested items", order.getId()),
                errorCodeException.getMessage());
    }

    private DeliverDigitalItemRequest createDeliverDigitalItemRequest(Order order) {
        return createDeliverDigitalItemRequest(order, "code", "description", nextDay());
    }

    private DeliverDigitalItemRequest createDeliverDigitalItemRequest(Order order,
                                                                      String code,
                                                                      String slip,
                                                                      Date date) {
        Map<Long, List<DigitalItemContent>> items = new HashMap<>();
        order.getItems().forEach(item -> {
            items.put(item.getId(), Stream.generate(() -> new DigitalItemContent(code, slip, date))
                    .limit(item.getCount()).collect(Collectors.toList()));
        });

        return new DeliverDigitalItemRequest(items);
    }

    private Date nextDay() {
        return Date.from(
                Instant.now().plus(1, ChronoUnit.DAYS).atZone(ZoneId.systemDefault()).toInstant()
        );
    }
}
