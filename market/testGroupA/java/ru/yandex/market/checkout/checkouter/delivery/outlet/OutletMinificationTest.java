package ru.yandex.market.checkout.checkouter.delivery.outlet;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckoutParameters;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OutletMinificationTest extends AbstractWebTestBase {

    private static final List<Function<ShopOutlet, Object>> MINIFIED_PROPERTIES = Arrays.asList(
            ShopOutlet::getPhones,
            ShopOutlet::getBlock,
            ShopOutlet::getBuilding,
            ShopOutlet::getCity,
            ShopOutlet::getEstate,
            ShopOutlet::getGps,
            ShopOutlet::getHouse,
            ShopOutlet::getName,
            ShopOutlet::getNotes,
            ShopOutlet::getStreet
    );

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Ручки /cart и /checkout возвращают не минифицированные аутлеты, если не указан флаг minifyOutlets")
    @Test
    public void cartReturnsFullOutletContentWhenNoMinifyOutletsOption() throws Exception {
        Parameters parameters = pickupParameters();
        MultiCart cart = orderCreateHelper.cart(parameters);
        assertOutletContent(cart.getCarts(), false);

        MultiOrder order = orderCreateHelper.checkout(cart, parameters);
        assertNull(order.getCartFailures());
        assertOutletContent(order.getCarts(), true);
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Ручки /cart и /checkout возвращают минифицированные аутлеты, если указан флаг minifyOutlets")
    @Test
    public void cartReturnsMinifiedOutletContentWhenHasMinifyOutletsOption() throws Exception {
        Parameters parameters = pickupParameters();
        parameters.setMinifyOutlets(true);
        MultiCart cart = orderCreateHelper.cart(parameters);
        assertOutletContent(cart.getCarts(), true);

        MultiOrder order = orderCreateHelper.checkout(cart, parameters);
        assertNull(order.getCartFailures());
        assertOutletContent(order.getCarts(), true);
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Клиент возвращает не минифицированные аутлеты, если не указан флаг minifyOutlets")
    @Test
    public void clientReturnsFullOutletContentWhenNoMinifyOutletsOption() {
        Parameters parameters = pickupParameters();
        orderCreateHelper.cart(parameters);
        MultiCart cart = client.cart(parameters.getBuiltMultiCart(), parameters.getBuyer().getUid());
        assertOutletContent(cart.getCarts(), false);

        MultiOrder order = orderCreateHelper.mapCartToOrder(cart, parameters);
        pushApiConfigurer.mockAccept(order.getCarts().get(0));
        order = client.checkout(order, parameters.getBuyer().getUid());
        assertNull(order.getCartFailures());
        assertOutletContent(order.getCarts(), false);
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Клиент возвращает минифицированные аутлеты, если указан флаг minifyOutlets")
    @Test
    public void clientReturnsMinifiedOutletContentWhenHasMinifyOutletsOption() {
        Parameters parameters = pickupParameters();
        parameters.setMinifyOutlets(true);
        orderCreateHelper.cart(parameters);
        MultiCart cart = client.cart(parameters.getBuiltMultiCart(), parameters.getBuyer().getUid(), false,
                Context.MARKET, ApiSettings.PRODUCTION, null, HitRateGroup.LIMIT, null,
                true);
        assertOutletContent(cart.getCarts(), true);

        MultiOrder order = orderCreateHelper.mapCartToOrder(cart, parameters);
        pushApiConfigurer.mockAccept(order.getCarts().get(0));
        CheckoutParameters checkoutParameters = CheckoutParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withSandbox(false)
                .withReserveOnly(false)
                .withContext(Context.MARKET)
                .withApiSettings(ApiSettings.PRODUCTION)
                .withHitRateGroup(HitRateGroup.LIMIT)
                .withMinifyOutlets(true)
                .build();
        order = client.checkout(order, checkoutParameters);
        assertNull(order.getCartFailures());
        assertOutletContent(order.getCarts(), true);
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("При ошибке создания заказа аутлеты минифицируются, если указан флаг minifyOutlets")
    @Test
    public void createOrderWithErrorReturnMinifiedOutletsForMinifyOutletsSwitch() throws Exception {
        Parameters parameters = pickupParameters();
        parameters.setMultiCartAction(cart -> cart.getCarts().get(0).getDelivery().setOutletId(123L));
        parameters.turnOffErrorChecks();
        parameters.setUseErrorMatcher(false);
        parameters.setMinifyOutlets(true);

        MultiOrder order = orderCreateHelper.createMultiOrder(parameters);
        assertNotNull(order.getCartFailures());
        assertOutletContent(getFailureOrders(order), true);
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("При ошибке создания заказа аутлеты не минифицируются, если не указан флаг minifyOutlets")
    @Test
    public void createOrderWithErrorReturnFullOutletsWithoutMinifyOutletsSwitch() throws Exception {
        Parameters parameters = pickupParameters();
        parameters.setMultiCartAction(cart ->
                cart.getCarts().get(0).getDelivery().setOutletId(123L));
        parameters.turnOffErrorChecks();
        parameters.setUseErrorMatcher(false);
        parameters.setMinifyOutlets(false);

        MultiOrder order = orderCreateHelper.createMultiOrder(parameters);
        assertNotNull(order.getCartFailures());
        assertOutletContent(getFailureOrders(order), false);
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("При ошибке создания заказа через клиент аутлеты минифицируются, если указан флаг minifyOutlets")
    @Test
    public void clientCheckoutWithErrorReturnFullOutletsWithoutMinifyOutletsSwitch() throws Exception {
        Parameters parameters = pickupParameters();
        orderCreateHelper.cart(parameters);
        MultiCart cart = client.cart(parameters.getBuiltMultiCart(), parameters.getBuyer().getUid(), false,
                Context.MARKET, ApiSettings.PRODUCTION, null, HitRateGroup.LIMIT, null,
                false);
        assertOutletContent(cart.getCarts(), false);

        MultiOrder order = orderCreateHelper.mapCartToOrder(cart, parameters);
        pushApiConfigurer.mockAccept(order.getCarts().get(0));
        order.getCarts().get(0).getDelivery().setOutletId(123L);
        order = client.checkout(order, parameters.getBuyer().getUid());
        assertNotNull(order.getCartFailures());
        assertOutletContent(getFailureOrders(order), false);
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("При ошибке создания заказа через клиент аутлеты не минифицируются, если не указан флаг minifyOutlets")
    @Test
    public void clientCheckoutWithErrorReturnMinifiedOutletsForMinifyOutletsSwitch() throws Exception {
        Parameters parameters = pickupParameters();
        orderCreateHelper.cart(parameters);
        MultiCart cart = client.cart(parameters.getBuiltMultiCart(), parameters.getBuyer().getUid(), false,
                Context.MARKET, ApiSettings.PRODUCTION, null, HitRateGroup.LIMIT, null,
                false);
        assertOutletContent(cart.getCarts(), false);

        MultiOrder order = orderCreateHelper.mapCartToOrder(cart, parameters);
        pushApiConfigurer.mockAccept(order.getCarts().get(0));
        order.getCarts().get(0).getDelivery().setOutletId(123L);
        CheckoutParameters checkoutParameters = CheckoutParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withSandbox(false)
                .withReserveOnly(false)
                .withContext(Context.MARKET)
                .withApiSettings(ApiSettings.PRODUCTION)
                .withHitRateGroup(HitRateGroup.LIMIT)
                .withMinifyOutlets(true)
                .build();
        order = client.checkout(order, checkoutParameters);
        assertNotNull(order.getCartFailures());
        assertOutletContent(getFailureOrders(order), true);
    }

    private List<Order> getFailureOrders(MultiCart cart) {
        return cart.getCartFailures().stream().map(OrderFailure::getOrder).collect(toList());
    }

    private void assertOutletContent(List<Order> orders, boolean isMinified) {
        assertNotNull(orders);
        orders.forEach(order -> assertOutletContent(order, isMinified));
    }

    private void assertOutletContent(Order order, boolean isMinified) {
        assertDeliveryOutletContent(order.getDeliveryOptions(), isMinified);
    }

    private void assertDeliveryOutletContent(List<? extends Delivery> deliveries, boolean isMinified) {
        if (deliveries == null) {
            return;
        }
        deliveries.stream()
                .filter(option -> option.getOutlets() != null)
                .forEach(option -> {
                    option.getOutlets().forEach(outlet -> {
                        assertNotDefaultValues(outlet, isMinified);
                    });
                });
    }

    private void assertNotDefaultValues(ShopOutlet outlet, boolean isMinified) {
        boolean hasAnyNotMinifiedProperty = MINIFIED_PROPERTIES.stream()
                .map(p -> p.apply(outlet))
                .anyMatch(Objects::nonNull);
        assertNotEquals(isMinified, hasAnyNotMinifiedProperty);
    }

    private Parameters pickupParameters() {
        Parameters parameters = new Parameters();
        parameters.setColor(Color.WHITE);
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.getReportParameters().setDeliveryPartnerTypes(List.of("SHOP"));
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.getOrder().setDelivery(DeliveryProvider.shopSelfPickupDeliveryByMarketOutletId().build());
        return parameters;
    }
}
