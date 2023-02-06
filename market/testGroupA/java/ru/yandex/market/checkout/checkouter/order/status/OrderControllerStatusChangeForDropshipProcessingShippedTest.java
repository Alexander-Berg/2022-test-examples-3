package ru.yandex.market.checkout.checkouter.order.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.OrderTypeUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.LocalDeliveryOptionProvider;
import ru.yandex.market.checkout.util.CheckoutRequestUtils;
import ru.yandex.market.checkout.util.ClientHelper;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.yandexDelivery;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.offerOf;

public class OrderControllerStatusChangeForDropshipProcessingShippedTest extends AbstractWebTestBase {

    private Order order;

    // временный тест https://st.yandex-team.ru/MARKETCHECKOUT-23901
    // тикет на выпил https://st.yandex-team.ru/MARKETCHECKOUT-23957

    @BeforeEach
    private void setUp() {
        createDropShipOrder();
        assertTrue(OrderTypeUtils.isFBS(order));
    }

    @Test
    public void shouldNotFailOnShopProcessingShippedAlreadyInProcessingShipped() throws Exception {
        checkShopProcessingStatusUpdate(order, ClientHelper.shopClientFor(order));
    }

    @Test
    public void shouldNotFailOnSystemProcessingShippedAlreadyInProcessingShipped() throws Exception {
        checkShopProcessingStatusUpdate(order, ClientInfo.SYSTEM);
    }

    private void checkShopProcessingStatusUpdate(Order order, ClientInfo clientInfo) throws Exception {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.PROCESSING, OrderSubstatus.SHIPPED);
        orderStatusHelper.updateOrderStatusForActions(order.getId(), clientInfo,
                OrderStatus.PROCESSING, OrderSubstatus.SHIPPED).andExpect(status().isOk());
    }

    @Test
    public void shouldNotAllowUpdateInProcessingShippedForShop() throws Exception {
        checkouterProperties.setForbiddenProcessingShippedForShops(true);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        orderStatusHelper.updateOrderStatus(order.getId(), ClientInfo.SYSTEM,
                OrderStatus.PROCESSING, OrderSubstatus.READY_TO_SHIP);

        orderStatusHelper.updateOrderStatusForActions(order.getId(), ClientHelper.shopClientFor(order),
                OrderStatus.PROCESSING, OrderSubstatus.SHIPPED).andExpect(status().isForbidden());
    }

    private void createDropShipOrder() {
        var supplierShipmentDateTime = Instant.parse("2021-10-14T13:40:00Z");
        var item = orderItemWithSortingCenter().offer("some offer");
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .deliveryBuilder(yandexDelivery()
                        .serviceId(MOCK_DELIVERY_SERVICE_ID)
                        .shipmentDate(supplierShipmentDateTime))
                .itemBuilder(item)
                .property(OrderPropertyType.ASYNC_OUTLET_ANTIFRAUD, true)
        );

        Parameters parameters = CheckoutRequestUtils.shopRequestFor(cart, List.of(offerOf(item)
                        .isFulfillment(false)
                        .atSupplierWarehouse(true)
                        .warehouseId(145)
                        .build()),
                ShopSettingsHelper::getDefaultMeta, optionsConfigurer -> optionsConfigurer.add(item,
                        LocalDeliveryOptionProvider.getMarDoLocalDeliveryOption(
                                MOCK_DELIVERY_SERVICE_ID, 1, supplierShipmentDateTime,
                                Duration.ofHours(23))), null, null, null);

        order = orderCreateHelper.createOrder(parameters);
    }
}
