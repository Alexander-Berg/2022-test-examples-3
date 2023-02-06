package ru.yandex.market.checkout.checkouter.delivery;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryUpdateProvider;


public class OrderControllerDeliveryUpdateDeliveryTest extends AbstractWebTestBase {

    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private EventsGetHelper eventsGetHelper;
    private Order order;
    private Order updatedOrder;

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(
                updateShopDeliveryId(),
                updateDeliveryType(),
                updateServiceName(),
                updatePrice(),
                updateDeliveryDates(),
                updateDeliveryAddress(),
                updateDeliveryAddressInSameCity()
        )
                .map(Case::toArray)
                .collect(Collectors.toList()).stream().map(Arguments::of);
    }

    /**
     * https://testpalm.yandex-team.ru/testcase/checkouter-183
     */
    private static Case updateShopDeliveryId() {
        return new Case(DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            d.setShopDeliveryId("newShopDeliveryId");
        }), "updateShopDeliveryId") {
            @Override
            public void checkNewDelivery(Delivery delivery) {
                Assertions.assertEquals("newShopDeliveryId", delivery.getShopDeliveryId());
            }
        };
    }

    /**
     * https://testpalm.yandex-team.ru/testcase/checkouter-184
     */
    //TODO: по идее сейчас в outletId хранитья только маркетный идентификатор
    @Deprecated
    private static Case updateDeliveryType() {
        var shopOutletId = Long.parseLong(DeliveryProvider.FREE_SHOP_OUTLET_CODE);
        return new Case(DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            d.setType(DeliveryType.PICKUP);
            d.setOutletId(shopOutletId);
        }), "updateDeliveryType") {
            @Override
            public void checkNewDelivery(Delivery delivery) {
                Assertions.assertEquals(DeliveryType.PICKUP, delivery.getType());
                Assertions.assertEquals(shopOutletId, delivery.getOutletId());
            }
        };
    }

    /**
     * https://testpalm.yandex-team.ru/testcase/checkouter-185
     */
    private static Case updateServiceName() {
        return new Case(DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            d.setServiceName("newServiceName");
        }), "updateServiceName") {
            @Override
            public void checkNewDelivery(Delivery delivery) {
                Assertions.assertEquals("newServiceName", delivery.getServiceName());
            }
        };
    }

    /**
     * https://testpalm.yandex-team.ru/testcase/checkouter-186
     */
    private static Case updatePrice() {
        return new Case(DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            d.setPrice(new BigDecimal("123.45"));
        }), "updatePrice") {
            @Override
            public void checkNewDelivery(Delivery delivery) {
                Assertions.assertEquals(new BigDecimal("123.45"), delivery.getPrice());
                Assertions.assertEquals(new BigDecimal("123.45"), delivery.getBuyerPrice());
            }
        };
    }

    /**
     * https://testpalm.yandex-team.ru/testcase/checkouter-187
     */
    private static Case updateDeliveryDates() {
        DeliveryDates deliveryDates = new DeliveryDates(3, 5);

        return new Case(DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            d.setDeliveryDates(deliveryDates);
        }), "updateDeliveryDates") {
            @Override
            public void checkNewDelivery(Delivery delivery) {
                Assertions.assertEquals(deliveryDates, delivery.getDeliveryDates());
            }
        };
    }

    /**
     * https://testpalm.yandex-team.ru/testcase/checkouter-188
     */
    private static Case updateDeliveryAddress() {
        Address newShopAddress = AddressProvider.getAnotherAddress();

        return new Case(DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            d.setAddress(newShopAddress);
            d.setShopAddress(newShopAddress);
        }), "updateDeliveryAddress") {
            @Override
            public void checkNewDelivery(Delivery delivery) {
                Assertions.assertNull(delivery.getRegionId());
                Assertions.assertNull(delivery.getBuyerAddress());
                Assertions.assertEquals(newShopAddress, delivery.getShopAddress());
            }
        };
    }

    /**
     * https://testpalm.yandex-team.ru/testcase/checkouter-189
     */
    private static Case updateDeliveryAddressInSameCity() {
        Address newShopAddress = AddressProvider.getAnotherAddressWithSameCity();

        return new Case(DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            d.setAddress(newShopAddress);
            d.setShopAddress(newShopAddress);
        }), "updateDeliveryAddressInSameCity") {

            @Override
            public void checkNewDelivery(Delivery delivery) {
                Assertions.assertNotNull(delivery.getRegionId());
                Assertions.assertNull(delivery.getBuyerAddress());
                Assertions.assertEquals(newShopAddress, delivery.getShopAddress());
            }
        };
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @ParameterizedTest(name = "{1}")
    @MethodSource("parameterizedTestData")
    public void test(Case testCase, String caseName) throws Exception {
        var params = WhiteParametersProvider.defaultWhiteParameters();
        params.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        params.setShopId(DeliveryProvider.OUTLET_SHOP_ID);
        order = orderCreateHelper.createOrder(params);
        reportConfigurer.mockOutlets();
        updatedOrder = orderDeliveryHelper.updateOrderDelivery(this.order.getId(), ClientInfo.SYSTEM,
                testCase.deliveryUpdate);
        Delivery newDelivery = updatedOrder.getDelivery();
        testCase.checkNewDelivery(newDelivery);
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @ParameterizedTest(name = "{1}")
    @MethodSource("parameterizedTestData")
    public void shouldSaveHistoryForChangedField(Case testCase, String caseName) throws Exception {
        var params = WhiteParametersProvider.defaultWhiteParameters();
        params.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        params.setShopId(DeliveryProvider.OUTLET_SHOP_ID);
        order = orderCreateHelper.createOrder(params);
        reportConfigurer.mockOutlets();
        updatedOrder = orderDeliveryHelper.updateOrderDelivery(this.order.getId(), ClientInfo.SYSTEM,
                testCase.deliveryUpdate);
        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());
        //
        OrderHistoryEvent lastEvent = Iterables.get(events.getItems(), 0);
        Assertions.assertEquals(order.getDelivery(), lastEvent.getOrderBefore().getDelivery());
        testCase.checkNewDelivery(lastEvent.getOrderAfter().getDelivery());
    }

    private abstract static class Case {

        private final Delivery deliveryUpdate;
        private final String caseName;

        Case(Delivery deliveryUpdate, String caseName) {
            this.deliveryUpdate = deliveryUpdate;
            this.caseName = caseName;
        }

        public Object[] toArray() {
            return new Object[]{this, caseName};
        }

        public abstract void checkNewDelivery(Delivery delivery);
    }
}
