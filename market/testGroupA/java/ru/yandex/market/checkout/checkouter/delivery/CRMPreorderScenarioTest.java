package ru.yandex.market.checkout.checkouter.delivery;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Sets;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryOption;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.checkout.checkouter.order.WrongDeliveryOption;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestAvailability;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryServiceCustomerInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.OrderEditPossibility;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.helpers.utils.configuration.MockConfiguration.StockStorageMockType;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.fulfillment.FulfillmentConfigurer;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.CartOffer;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_INTAKE_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

/**
 * @author mmetlov
 */
public class CRMPreorderScenarioTest extends AbstractWebTestBase {

    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private FulfillmentConfigurer fulfillmentConfigurer;

    @Test
    public void fullScenario() throws Exception {
        // делаем синий предзаказ
        Order order = createPreorder(DeliveryType.DELIVERY);

        //приходим в /cart по-CRMному и получаем там опцию доставки в другую службу
        order.setDelivery(DeliveryProvider.getEmptyDelivery());
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(order);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setContext(Context.SANDBOX);
        parameters.setColor(BLUE);
        parameters.setSandbox(true);
        parameters.setApiSettings(ApiSettings.PRODUCTION);
        ActualDelivery actualDelivery = ActualDeliveryProvider.builder()
                .addDelivery(MOCK_INTAKE_DELIVERY_SERVICE_ID, 3)
                .build();
        parameters.getReportParameters().setActualDelivery(actualDelivery);
        reportMock.resetRequests();
        MultiCart crmCart = orderCreateHelper.cart(parameters);

        OrderItem item = order.getItems().stream().findFirst().get();
        CartOffer cartOffer = new CartOffer(
                item.getWareMd5(),
                item.getCount(),
                item.getWeightInKilo(),
                item.getPrice().intValue(),
                CartOffer.formatDimensions(
                        item.getWidth(),
                        item.getHeight(),
                        item.getDepth()
                ),
                item.getWarehouseId(),
                true,
                item.getWarehouseId().longValue()
        );
        assertThat(item.getCargoTypes(), containsInAnyOrder(CoreMatchers.is(1), CoreMatchers.is(2),
                CoreMatchers.is(3)));
        cartOffer.setCargoTypes(item.getCargoTypes());
        assertActualDeliveryWasRequestedWithFakeOffer(cartOffer);

        //идем в изменение условий доставки с новой опцией
        Delivery editDelivery = crmCart.getCarts().get(0).getDeliveryOptions().stream()
                .filter(d -> d.getDeliveryPartnerType() == DeliveryPartnerType.YANDEX_MARKET &&
                        MOCK_INTAKE_DELIVERY_SERVICE_ID.equals(d.getDeliveryServiceId()) &&
                        DeliveryType.DELIVERY.equals(d.getType()))
                .findFirst().get();
        editDelivery.setPrice(null);

        reportMock.resetRequests();
        Order editedOrder = orderDeliveryHelper.updateOrderDelivery(order.getId(), editDelivery);
        assertEquals(MOCK_INTAKE_DELIVERY_SERVICE_ID, editedOrder.getDelivery().getDeliveryServiceId());

        reportMock.resetRequests();

        //переводим в PROCESSING (аналогия buyer-been-called)
        Order orderInProcessing = orderStatusHelper.proceedOrderToStatus(editedOrder, OrderStatus.PROCESSING);
        assertActualDeliveryWasRequestedWithFakeOffer(cartOffer);
        assertNotEquals(editedOrder.getDelivery().getShipment().getShipmentDate(),
                orderInProcessing.getDelivery().getShipment().getShipmentDate());

    }

    /**
     * Сценарий не совсем полный тк механизм изменения заказа еще не все поддерживает что нужно CRM
     * например изменение службы доставки до PROCESSING
     */
    @Test
    public void fullScenarioWithOrderEdit() throws Exception {
        // делаем синий предзаказ
        Order order = createPreorder(DeliveryType.DELIVERY);

        // проверяем доступность изменений
        OrderEditPossibility possibility = client.getOrderEditPossibilities(Collections.singleton(order.getId()),
                ClientRole.CALL_CENTER_OPERATOR, 123L, Collections.singletonList(BLUE)).get(0);

        assertEquals(ChangeRequestAvailability.ENABLED,
                possibility.getEditPossibilities().stream()
                        .filter(ep -> ep.getType() == ChangeRequestType.DELIVERY_DATES)
                        .findFirst()
                        .orElseThrow()
                        .getAvailability());
        assertEquals(ChangeRequestAvailability.ENABLED,
                possibility.getEditPossibilities().stream()
                        .filter(ep -> ep.getType() == ChangeRequestType.DELIVERY_OPTION)
                        .findFirst()
                        .orElseThrow()
                        .getAvailability());

        final DeliveryServiceCustomerInfo info = possibility.getDeliveryServiceCustomerInfo();
        assertNotNull(info);
        assertThat(info.getPhones(), containsInAnyOrder("+7-(912)-345-67-89", "+7-(912)-345-67-88"));
        assertEquals("www.partner100501-site.ru", info.getTrackOrderSite());

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(order);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setContext(Context.SANDBOX);
        parameters.setColor(BLUE);
        parameters.setSandbox(true);
        parameters.setApiSettings(ApiSettings.PRODUCTION);
        ActualDelivery actualDelivery = ActualDeliveryProvider.builder()
                .addDelivery(MOCK_DELIVERY_SERVICE_ID, 3)
                .build();
        ActualDeliveryOption actualDeliveryOption = actualDelivery.getResults().get(0).getDelivery().get(0);
        actualDeliveryOption.setDayFrom(3);
        actualDeliveryOption.setDayTo(4);
        actualDeliveryOption.setTimeIntervals(singletonList(new DeliveryTimeInterval(LocalTime.of(10, 0),
                LocalTime.of(18, 0))));
        parameters.getReportParameters().setActualDelivery(actualDelivery);
        reportMock.resetRequests();
        orderCreateHelper.initializeMock(parameters);

        // получаем опцию
        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(new DeliveryEditOptionsRequest());
        OrderEditOptions orderEditOptions = client.getOrderEditOptions(order.getId(), ClientRole.CALL_CENTER_OPERATOR,
                123L,
                Collections.singletonList(BLUE), orderEditOptionsRequest);

        OrderItem item = order.getItems().stream().findFirst().orElseThrow();
        CartOffer cartOffer = new CartOffer(
                item.getWareMd5(),
                item.getCount(),
                item.getWeightInKilo(),
                item.getPrice().intValue(),
                CartOffer.formatDimensions(
                        item.getWidth(),
                        item.getHeight(),
                        item.getDepth()
                ),
                item.getWarehouseId(),
                true,
                item.getWarehouseId().longValue()
        );
        assertThat(item.getCargoTypes(), containsInAnyOrder(CoreMatchers.is(1), CoreMatchers.is(2),
                CoreMatchers.is(3)));
        cartOffer.setCargoTypes(item.getCargoTypes());
        assertActualDeliveryWasRequestedWithFakeOffer(cartOffer);

        // редактируем заказ
        DeliveryOption chosenOption = orderEditOptions.getDeliveryOptions().iterator().next();
        TimeInterval chosenInterval = chosenOption.getTimeIntervalOptions().iterator().next();
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .prerequest(orderEditOptionsRequest.getDeliveryEditOptionsRequest())
                .deliveryOption(chosenOption)
                .timeInterval(chosenInterval)
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());
        client.editOrder(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                singletonList(BLUE),
                orderEditRequest
        );

        order = client.getOrder(order.getId(), ClientRole.SYSTEM, 0L);
        reportMock.resetRequests();

        //переводим в PROCESSING (аналогия buyer-been-called)
        Order orderInProcessing = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        assertActualDeliveryWasRequestedWithFakeOffer(cartOffer);
        assertNotEquals(order.getDelivery().getShipment().getShipmentDate(),
                orderInProcessing.getDelivery().getShipment().getShipmentDate());

    }

    private Order createPreorder(DeliveryType deliveryType) {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(BLUE)
                .withDeliveryType(deliveryType)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .buildParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> {
                    oi.setCargoTypes(Sets.newHashSet(1, 2, 3));
                    oi.setPreorder(true);
                });
        parameters.setStockStorageMockType(StockStorageMockType.PREORDER_OK);

        Order order = orderCreateHelper.createOrder(parameters);
        return orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PENDING);
    }

    private void assertActualDeliveryWasRequestedWithFakeOffer(CartOffer fakeOffer) {
        ServeEvent actualDeliveryServeEvent = reportMock.getServeEvents().getRequests().stream()
                .filter(req -> req.getRequest().getQueryParams().get("place").isSingleValued()
                        && req.getRequest().getQueryParams().get("place").firstValue().equals("actual_delivery"))
                .findFirst().get();
        assertEquals(fakeOffer.toRequestFormat(), CollectionUtils.getOnlyElement(
                actualDeliveryServeEvent.getRequest().getQueryParams().get("offers-list").values()));
        assertEquals("1", CollectionUtils.getOnlyElement(
                actualDeliveryServeEvent.getRequest().getQueryParams().get("ignore-has-gone").values()));
        assertNull(actualDeliveryServeEvent.getRequest().getQueryParams().get("inlet-shipment-day"), "1");
    }

    @Test
    public void shouldReturn4xxWhenDeliveryIsntActualOnBuyerBeenCalled() {
        Assertions.assertThrows(WrongDeliveryOption.class, () -> {
            //создаем заказ
            Order order = createPreorder(DeliveryType.PICKUP);
            order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PENDING);

            //меняем ответ actual_delivery на другую службу
            Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(order);
            parameters.setPaymentMethod(PaymentMethod.YANDEX);
            parameters.setContext(Context.SANDBOX);
            parameters.setColor(BLUE);
            parameters.setSandbox(true);
            parameters.setApiSettings(ApiSettings.PRODUCTION);
            ActualDelivery actualDelivery = ActualDeliveryProvider.builder()
                    .addPickup(MOCK_INTAKE_DELIVERY_SERVICE_ID, 3)
                    .build();
            parameters.getReportParameters().setActualDelivery(actualDelivery);
            parameters.getReportParameters().setConfigurePreciseActualDelivery(false);
            orderCreateHelper.initializeMock(parameters);

            //переводим в PROCESSING (аналогия buyer-been-called)
            orderStatusHelper.updateOrderStatus(order.getId(), ClientInfo.SYSTEM,
                    OrderStatus.PROCESSING, null, new ResultActionsContainer()
                            .andExpect(status().is(400))
                            .andExpect(content().json("{\"status\":400," +
                                    "\"code\":\"CURRENT_DELIVERY_OPTION_EXPIRED\"," +
                                    "\"message\":\"Order " + order.getId() + " has expired delivery option.\"}")),
                    null
            );

            client.updateOrderStatus(order.getId(), ClientRole.SYSTEM, 0L, SHOP_ID_WITH_SORTING_CENTER,
                    OrderStatus.PROCESSING, null);
        });
    }

    @Test
    public void shouldReturn4xxWhenDeliveryIsntActualOnUpdatedeliveryOutlet() {
        Assertions.assertThrows(WrongDeliveryOption.class, () -> {
            //создаем заказ
            Order order = createPreorder(DeliveryType.DELIVERY);

            //пытаемся обновить аутлет на тот которого нет в actual_delivery
            Delivery editDelivery = new Delivery();
            editDelivery.setOutletId(12312304L);
            editDelivery.setRegionId(DeliveryProvider.REGION_ID);
            editDelivery.setType(DeliveryType.PICKUP);
            orderDeliveryHelper.updateOrderDeliveryForActions(order.getId(), ClientInfo.SYSTEM, editDelivery)
                    .andExpect(status().is(400)).andExpect(content().json("{\"status\":400," +
                    "\"code\":\"DELIVERY_UPDATE_FAILED\"," +
                    "\"message\":\"Delivery service " + MOCK_INTAKE_DELIVERY_SERVICE_ID +
                    " is not available to the shop: " + SHOP_ID_WITH_SORTING_CENTER + "\"}"));

            client.updateOrderDelivery(order.getId(), ClientRole.SYSTEM, 0L, editDelivery);
        });
    }

    @Test
    public void testRequestReportWithFakeOffer() {
        final String realPromoCode = "REAL-PROMO-CODE";
        final BigDecimal ffSubsidy1 = BigDecimal.valueOf(123.45);

        Parameters parameters = defaultBlueOrderParameters();

        OrderItem orderItem = OrderItemProvider.getOrderItem();
        orderItem.setWeight(1000L);
        orderItem.setWidth(10L);
        orderItem.setHeight(10L);
        orderItem.setDepth(10L);

        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode(realPromoCode));
        parameters.getOrder().setItems(Collections.singletonList(orderItem));
        LoyaltyDiscount discount = LoyaltyDiscount.builder()
                .discount(ffSubsidy1)
                .promoType(PromoType.MARKET_PROMOCODE)
                .build();
        parameters.getLoyaltyParameters().addLoyaltyDiscount(orderItem, discount);
        parameters.setMockLoyalty(true);

        parameters.getReportParameters().setShopSupportsSubsidies(false);
        fulfillmentConfigurer.configure(parameters);

        // делаем заказ, применяя промокоды
        Order order = orderCreateHelper.createOrder(parameters);

        reportMock.resetAll();
        // еще раз делаем /cart, чтобы убедиться, что в репорт уезжает запрос с ценой до применения скидок по промокодам
        Parameters orderParameters = defaultBlueOrderParameters(order);
        orderParameters.setSandbox(true);

        orderCreateHelper.cart(orderParameters);
        orderItem = orderService.getOrder(order.getId()).getItems().iterator().next();
        CartOffer cartOffer = new CartOffer(
                orderItem.getWareMd5(),
                orderItem.getCount(),
                orderItem.getWeightInKilo(),
                orderItem.getBuyerPrice().intValue(),
                CartOffer.formatDimensions(
                        orderItem.getWidth(),
                        orderItem.getHeight(),
                        orderItem.getDepth()
                ),
                orderItem.getWarehouseId(),
                true,
                orderItem.getWarehouseId().longValue()
        );

        String expected = cartOffer.toRequestFormat();
        String actual = reportMock.getServeEvents().getRequests().stream()
                .filter(req -> req.getRequest().getQueryParams().get("place").isSingleValued()
                        && req.getRequest().getQueryParams().get("place").firstValue().equals("actual_delivery"))
                .filter(req -> req.getRequest().getQueryParams().get("offers-list").isPresent())
                .map(rq -> CollectionUtils.getOnlyElement(rq.getRequest().getQueryParams().get("offers-list").values()))
                .findAny()
                .orElseThrow(() -> new AssertionError("actual cart offer string does not found"));

        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void outletChangeScenario() throws Exception {
        //создаем заказ на самовывоз
        Order order = createPreorder(DeliveryType.PICKUP);

        //приходим в /cart по-CRMному и получаем там опцию доставки в другую службу в другой пункт
        order.setDelivery(new Delivery(DeliveryProvider.REGION_ID));
        Parameters crmCartParameters = BlueParametersProvider.defaultBlueOrderParameters(order);
        crmCartParameters.setPaymentMethod(PaymentMethod.YANDEX);
        crmCartParameters.setContext(Context.SANDBOX);
        crmCartParameters.setColor(BLUE);
        crmCartParameters.setSandbox(true);
        crmCartParameters.setApiSettings(ApiSettings.PRODUCTION);
        ActualDelivery actualDelivery = ActualDeliveryProvider.builder()
                .addPickup(MOCK_INTAKE_DELIVERY_SERVICE_ID, 3, Collections.singletonList(12312304L))
                .build();
        crmCartParameters.getReportParameters().setActualDelivery(actualDelivery);
        MultiCart crmCart = orderCreateHelper.cart(crmCartParameters);

        //идем в изменение условий доставки с новой опцией
        Delivery editDelivery = crmCart.getCarts().get(0).getDeliveryOptions().stream()
                .filter(d -> d.getDeliveryPartnerType() == DeliveryPartnerType.YANDEX_MARKET &&
                        MOCK_INTAKE_DELIVERY_SERVICE_ID.equals(d.getDeliveryServiceId()) &&
                        DeliveryType.PICKUP.equals(d.getType()))
                .findFirst().get();

        editDelivery.setOutletId(12312304L);
        Order editedOrder = orderDeliveryHelper.updateOrderDelivery(order.getId(), editDelivery);
        assertEquals(MOCK_INTAKE_DELIVERY_SERVICE_ID, editedOrder.getDelivery().getDeliveryServiceId());
        assertEquals(Long.valueOf(12312304L), editedOrder.getDelivery().getOutletId());
        assertEquals("testOutletCode", editedOrder.getDelivery().getOutletCode());

        //переводим в PROCESSING (аналогия buyer-been-called)
        Order orderInProcessing = orderStatusHelper.proceedOrderToStatus(editedOrder, OrderStatus.PROCESSING);

        assertNotEquals(editedOrder.getDelivery().getShipment().getShipmentDate(),
                orderInProcessing.getDelivery().getShipment().getShipmentDate());

    }
}
