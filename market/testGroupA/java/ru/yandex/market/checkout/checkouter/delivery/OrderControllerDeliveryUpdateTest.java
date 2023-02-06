package ru.yandex.market.checkout.checkouter.delivery;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.shipment.ShipmentUpdateNotAllowedException;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.builders.ParcelBuilder;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryUpdateProvider;
import ru.yandex.market.checkout.test.providers.ParcelItemProvider;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.INVALID_REQUEST;

public class OrderControllerDeliveryUpdateTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    /**
     * https://testpalm.yandex-team.ru/testcase/checkouter-195
     */
    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя менять стоимость доставки, если заказ был предоплачен")
    @Test
    public void shouldNotAllowToUpdateDeliveryPriceIfPrepaid() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);

        Delivery delivery = DeliveryUpdateProvider.createDeliveryUpdate(d -> {
            d.setPrice(new BigDecimal("34.56"));
        });

        orderDeliveryHelper.updateOrderDeliveryForActions(order.getId(), ClientInfo.SYSTEM, delivery)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Delivery price change is not allowed for prepaid order " +
                                order.getId()));
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя передать ид. аутлета для курьерской доставки")
    @Test
    public void shouldNotAllowToUpdateOutletIdForDelivery() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters());

        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate(delivery -> {
            delivery.setRegionId(DeliveryProvider.REGION_ID);
            delivery.setOutletId(DeliveryProvider.FREE_MARKET_OUTLET_ID);
        });

        orderDeliveryHelper.updateOrderDeliveryForActions(order.getId(), ClientInfo.SYSTEM, deliveryUpdate)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(
                        "Delivery outlet is not appropriate for delivery type " +
                                "DELIVERY"));
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Можно изменить ид. аутлета для самовывоза")
    @Test
    public void shouldAllowToUpdateOutletIdForPickup() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        Order order = orderCreateHelper.createOrder(parameters);

        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate(delivery -> {
            delivery.setOutletId(DeliveryProvider.FREE_MARKET_OUTLET_ID);
        });

        orderDeliveryHelper.updateOrderDeliveryForActions(order.getId(), ClientInfo.SYSTEM, deliveryUpdate)
                .andExpect(status().isOk());
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя изменить адрес доставки МарДо заказу в процессинг")
    @Issue("MARKETCHECKOUT-5518")
    @Test
    public void shouldNotAllowToUpdateAddressForMardoProcessing() throws Exception {
        Order marDoOrder = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .build();
        orderPayHelper.payForOrder(marDoOrder);

        Address oldShopAddress = orderService.getOrder(marDoOrder.getId()).getDelivery().getShopAddress();

        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate(d -> {
            d.setRegionId(DeliveryProvider.REGION_ID);
            d.setAddress(AddressProvider.getAnotherAddress());
        });

        orderDeliveryHelper.updateOrderDeliveryForActions(marDoOrder.getId(), ClientInfo.SYSTEM, deliveryUpdate)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(
                        "Cannot change delivery address for MarDo order in status " +
                                "PROCESSING, should be PENDING"));

        Address shopAddress = orderService.getOrder(marDoOrder.getId()).getDelivery().getShopAddress();
        assertEquals(oldShopAddress, shopAddress);
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Можно изменить адрес доставки обычному заказу в процессинг")
    @Issue("MARKETCHECKOUT-5518")
    @Test
    public void shouldAllowToUpdateAddress() throws Exception {
        Order order = orderCreateHelper
                .createOrder(BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters());
        Address oldShopAddress = orderService.getOrder(order.getId()).getDelivery().getShopAddress();

        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate(d -> {
            d.setRegionId(DeliveryProvider.REGION_ID);
            d.setAddress(AddressProvider.getAnotherAddress());
        });

        orderDeliveryHelper.updateOrderDeliveryForActions(order.getId(), ClientInfo.SYSTEM, deliveryUpdate)
                .andExpect(status().isOk());
        Address shopAddress = orderService.getOrder(order.getId()).getDelivery().getShopAddress();
        assertNotEquals(oldShopAddress, shopAddress);

    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя изменить адрес доставки МарДошному ФФ заказу в пендинг без региона")
    @Issue("MARKETCHECKOUT-5518")
    @Test
    public void shouldNotAllowToUpdateAddressForMardoPendingNullRegion() throws Exception {
        Date fakeNow = DateUtil.convertDotDateFormat("07.07.2027");
        int shipmentDays = (int) Math.ceil(DateUtil.diffInDays(fakeNow, DateUtil.now()));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(shipmentDays)
                .withPartnerInterface(true)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addDelivery(MOCK_DELIVERY_SERVICE_ID, shipmentDays)
                                .withFreeDelivery()
                                .build()
                )
                .buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        Address oldShopAddress = orderService.getOrder(order.getId()).getDelivery().getShopAddress();

        orderPayHelper.payForOrder(order);

        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate(d -> {
            d.setAddress(AddressProvider.getAnotherAddress());
        });

        orderDeliveryHelper.updateOrderDeliveryForActions(order.getId(), ClientInfo.SYSTEM, deliveryUpdate)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Cannot change Delivery address for MarDo " +
                                                "without specifying regionId"
                                )
                );
        Address shopAddress = orderService.getOrder(order.getId()).getDelivery().getShopAddress();
        assertEquals(oldShopAddress, shopAddress);

    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя изменить ПВЗ МарДошному ФФ заказу в процессинг")
    @Test
    public void shouldNotAllowUpdateOutletForMardoProcessingOrder() throws Exception {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(2)
                .withPartnerInterface(true)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(MOCK_DELIVERY_SERVICE_ID, 2,
                                        Arrays.asList(12312302L, 12312303L))
                                .build()
                )
                .buildParameters();
        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder multiOrder = orderCreateHelper.mapCartToOrder(cart, parameters);
        Order order = Iterables.getOnlyElement(orderCreateHelper.checkout(multiOrder, parameters).getOrders());
        assertThat(order.isFulfilment(), is(true));
        orderPayHelper.payForOrder(order);
        assertEquals(PROCESSING, orderService.getOrder(order.getId()).getStatus());

        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate(delivery -> {
            delivery.setOutletId(741258L);
        });
        orderDeliveryHelper.updateOrderDeliveryForActions(order.getId(), ClientInfo.SYSTEM, deliveryUpdate)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(INVALID_REQUEST.name()))
                .andExpect(jsonPath("$.message").value("Cannot change delivery address for " +
                        "MarDo order in status " +
                        "PROCESSING, should be PENDING"));
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя изменить ПВЗ МарДошному ФФ заказу в пендинг, если указан несуществующий аутлет")
    @Issue("MARKETCHECKOUT-5471")
    @Test
    public void shouldNotAllowUpdateToUnknownOutletForMardoPendingOrder() throws Exception {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(2)
                .withPartnerInterface(true)
                .buildParameters();
        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder multiOrder = orderCreateHelper.mapCartToOrder(cart, parameters);
        Order order = Iterables.getOnlyElement(orderCreateHelper.checkout(multiOrder, parameters).getOrders());
        orderPayHelper.payForOrder(order);

        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate(delivery -> {
            delivery.setOutletId(789789L);
        });
        orderDeliveryHelper.updateOrderDeliveryForActions(order.getId(), ClientInfo.SYSTEM, deliveryUpdate)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(INVALID_REQUEST.name()))
                .andExpect(jsonPath("$.message").value("Outlet is not found: 789789"));
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Изменить службу доставки для доставки магазином")
    @Issue("MARKETCHECKOUT-16350")
    @Test
    public void updateDeliveryServiceIdForShopOrder() throws Exception {
        Parameters parameters = new Parameters();
        Order order = orderCreateHelper.createOrder(parameters);
        assertEquals(PROCESSING, orderService.getOrder(order.getId()).getStatus());

        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            d.setDeliveryServiceId(order.getDelivery().getDeliveryServiceId() + 1);
        });

        orderDeliveryHelper.updateOrderDeliveryForActions(order.getId(), ClientInfo.SYSTEM, deliveryUpdate)
                .andExpect(status().isOk());

        assertEquals(
                order.getDelivery().getDeliveryServiceId() + 1,
                orderService.getOrder(order.getId()).getDelivery().getDeliveryServiceId()
        );
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Изменить службу доставки для доставки магазином в статусе DELIVERY")
    @Issue("MARKETCHECKOUT-16350")
    @Test
    public void shouldNotAllowToUpdateDeliveryServiceIdForShopOrderInStatusDelivery() throws Exception {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        assertEquals(DELIVERY, orderService.getOrder(order.getId()).getStatus());


        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            d.setDeliveryServiceId(order.getDelivery().getDeliveryServiceId() + 1);
        });

        orderDeliveryHelper.updateOrderDeliveryForActions(order.getId(), ClientInfo.SYSTEM, deliveryUpdate)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STATUS_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("Action is not allowed for order "
                        + order.getId() +
                        " with status DELIVERY"));
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя изменить службу доставки МарДошному заказу в ПВЗ")
    @Issue("MARKETCHECKOUT-5661")
    @Test
    public void shouldNotAllowUpdateDeliveryServiceIdForMardoProcessingOrder() throws Exception {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(2)
                .withPartnerInterface(false)
                .buildParameters();
        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder multiOrder = orderCreateHelper.mapCartToOrder(cart, parameters);
        Order order = Iterables.getOnlyElement(orderCreateHelper.checkout(multiOrder, parameters).getOrders());
        orderPayHelper.payForOrder(order);
        assertEquals(PROCESSING, orderService.getOrder(order.getId()).getStatus());

        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate(delivery -> {
            delivery.setDeliveryServiceId(order.getDelivery().getDeliveryServiceId() + 1);
        });
        orderDeliveryHelper.updateOrderDeliveryForActions(order.getId(), ClientInfo.SYSTEM, deliveryUpdate)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(INVALID_REQUEST.name()))
                .andExpect(jsonPath("$.message").value("Delivery service change not allowed " +
                        "for PICKUP delivery type"));
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Можно менять стоимость доставки, если заказ с постоплатой")
    @Test
    public void shouldAllowToUpdateDeliveryPriceIfPostpaid() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

        Order order = orderCreateHelper.createOrder(parameters);

        Delivery delivery = DeliveryUpdateProvider.createDeliveryUpdate(d -> {
            d.setPrice(BigDecimal.ZERO);
        });

        orderDeliveryHelper.updateOrderDeliveryForActions(order.getId(), ClientInfo.SYSTEM, delivery)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery.price").value("0"))
                .andExpect(jsonPath("$.delivery.buyerPrice").value("0"));
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Можно поменять только регион доставки")
    @Test
    public void shouldAllowToChangeOnlyDeliveryRegionId() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        Order order = orderCreateHelper.createOrder(parameters);

        Delivery delivery = DeliveryUpdateProvider.createDeliveryUpdate(d -> {
            d.setRegionId(2L);
        });

        Order updated = orderDeliveryHelper.updateOrderDelivery(order.getId(), delivery);

        assertThat(updated.getDelivery().getRegionId(), is(2L));
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Можно поменять адрес для почтовой доставки")
    @Test
    public void shouldAllowToChangeAddressForPostDeliveryType() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setColor(Color.WHITE);
        parameters.setDeliveryType(DeliveryType.POST);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getDelivery().getType(), is(DeliveryType.POST));

        Delivery delivery = DeliveryUpdateProvider.createDeliveryUpdate(d -> {
            d.setAddress(AddressProvider.getAnotherAddress());
        });

        Order updated = orderDeliveryHelper.updateOrderDelivery(order.getId(), delivery);

        assertThat(updated.getDelivery().getShopAddress(), is(AddressProvider.getAnotherAddress()));
    }

    /**
     * FIXME: проверка почему то есть только в магазинной доставке
     */
    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя изменять delivery для заказов в статусе CANCELLED.")
    @Test
    public void disabledDeliveryUpdatesForCancelled() throws Exception {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, CANCELLED);

        assertThat(order.getStatus(), is(CANCELLED));

        OrderItem itemSaved = order.getItems().iterator().next();

        Parcel parcel = ParcelBuilder.instance()
                .withParcelItems(
                        Arrays.asList(
                                ParcelItemProvider.buildParcelItem(
                                        itemSaved.getId(),
                                        itemSaved.getCount())
                        )
                )
                .build();
        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdateWithParcels(parcel);


        ErrorCodeException updateError = orderDeliveryHelper.updateOrderDeliveryFailed(
                order.getId(),
                ClientInfo.SYSTEM,
                deliveryUpdate
        );

        assertThat(updateError.getCode(), is(ShipmentUpdateNotAllowedException.ERROR_CODE));
        assertThat(
                updateError.getMessage(),
                is(String.format("Updating Shipments is disabled for order with id=%d in status=CANCELLED",
                        order.getId()))
        );
    }

}
