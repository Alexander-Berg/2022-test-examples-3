package ru.yandex.market.checkout.checkouter.delivery;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryUpdateProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderControllerDeliveryUpdatePickupTest extends AbstractWebTestBase {

    private static final long INVALID_OUTLET_ID = 9999L;

    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;

    private Order order;

    @BeforeEach
    public void setUp() {
        Parameters parameters = new Parameters();
        parameters.setColor(Color.WHITE);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setOutletId(DeliveryProvider.FREE_MARKET_OUTLET_ID);
        parameters.setDeliveryType(DeliveryType.PICKUP);

        order = orderCreateHelper.createOrder(parameters);
    }

    /**
     * https://testpalm.yandex-team.ru/testcase/checkouter-190
     */
    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя изменить тип доставки на DELIVERY, не указав адрес")
    @Test
    public void shouldNotAllowToUpdateDeliveryTypeIfAddressIsNotSpecified() throws Exception {
        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            d.setType(DeliveryType.DELIVERY);
        });

        orderDeliveryHelper.updateOrderDeliveryForActions(this.order.getId(), ClientInfo.SYSTEM, deliveryUpdate)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing address"));
    }

    /**
     * https://testpalm.yandex-team.ru/testcase/checkouter-191
     */
    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Можно изменить тип доставки на DELIVERY, указав адрес")
    @Test
    public void shouldUpdateDeliveryType() throws Exception {
        Address anotherAddress = AddressProvider.getAnotherAddress();

        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            d.setType(DeliveryType.DELIVERY);
            d.setAddress(anotherAddress);
        });

        Order updated = orderDeliveryHelper.updateOrderDelivery(this.order, ClientInfo.SYSTEM, deliveryUpdate);
        Delivery updatedDelivery = updated.getDelivery();

        assertEquals(DeliveryType.DELIVERY, updatedDelivery.getType());
        assertNull(updatedDelivery.getBuyerAddress());
        assertEquals(anotherAddress, updatedDelivery.getShopAddress());
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Можно изменить тип доставки на POST, указав адрес")
    @Test
    public void shouldUpdateDeliveryTypeToPost() throws Exception {
        Address anotherAddress = AddressProvider.getAnotherAddress();

        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            d.setType(DeliveryType.POST);
            d.setAddress(anotherAddress);
        });

        Order updated = orderDeliveryHelper.updateOrderDelivery(this.order, ClientInfo.SYSTEM, deliveryUpdate);
        Delivery updatedDelivery = updated.getDelivery();

        assertEquals(DeliveryType.POST, updatedDelivery.getType());
        assertNull(updatedDelivery.getBuyerAddress());
        assertEquals(anotherAddress, updatedDelivery.getShopAddress());
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя передать адрес для самовоывоза")
    @Test
    public void shouldNotAllowToUpdateAddressForPickup() throws Exception {
        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            d.setAddress(AddressProvider.getAnotherAddress());
        });

        orderDeliveryHelper.updateOrderDeliveryForActions(this.order.getId(), ClientInfo.SYSTEM, deliveryUpdate)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Buyer delivery address is not appropriate for delivery type " +
                        "PICKUP"));
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Не должны разрешать способ доставки на POST, если статус уже PICKUP")
    @Test
    public void shouldNotAllowToUpdateDeliveryTypeToPostIfStatusIsPickup() throws Exception {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);

        orderDeliveryHelper.updateOrderDeliveryForActions(
                order.getId(),
                ClientInfo.SYSTEM,
                DeliveryUpdateProvider.createDeliveryUpdate(d -> {
                    d.setType(DeliveryType.POST);
                })).andExpect(status().isBadRequest());
    }
}

