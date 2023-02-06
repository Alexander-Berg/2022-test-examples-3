package ru.yandex.market.checkout.checkouter.checkout;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.util.CheckouterPropertiesImpl;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.DeliveryResponseProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateOrderForcedPostpaidTest extends AbstractWebTestBase {

    @Test
    // у пользователя еще не выбран метод оплаты
    public void shouldForcePostpaidToCartWithoutUserPaymentMethod() throws Exception {
        PaymentMethod forcedMethod = PaymentMethod.CASH_ON_DELIVERY;
        checkouterProperties.setForcePostpaid(CheckouterPropertiesImpl.ForcePostpaid.FORCE);

        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setPaymentMethod(null);
        parameters.setMockPushApi(false);
        parameters.getOrder().setDelivery(DeliveryProvider.shopSelfDelivery().build());

        // мокируем пуш-апи: опции доставки, отсутствие постоплаты
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildDeliveryResponseWithIntervals();
        deliveryResponse.setPaymentOptions(Set.of(PaymentMethod.YANDEX));
        List<PaymentMethod> shopPaymentMethods = getPrepaidMethods();
        pushApiConfigurer.mockCart(parameters.getOrder(),
                Collections.singletonList(deliveryResponse), shopPaymentMethods, false);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        // появилась постоплата среди методов оплаты, среди методов оплаты в доставке
        checkCartHasPostpaid(forcedMethod, multiCart);

        parameters.setPaymentMethod(forcedMethod); // пользователь выбрал этот метод, с фронта пришел еще один /cart
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        // опять мокируем пуш-апи: опции доставки, отсутствие постоплаты
        pushApiConfigurer.mockCart(parameters.getOrder(),
                Collections.singletonList(deliveryResponse), shopPaymentMethods, false);
        multiCart = orderCreateHelper.cart(parameters);
        // появилась постоплата
        checkCartHasPostpaid(forcedMethod, multiCart);

        // опять мокируем пуш-апи уже для чекаута: опции доставки, отсутствие постоплаты
        pushApiConfigurer.mockCart(parameters.getOrder(),
                Collections.singletonList(deliveryResponse), shopPaymentMethods, false
        );
        pushApiConfigurer.mockAccept(parameters.getOrder(), true);
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        OrderRequest orderRequest = OrderRequest.builder(Iterables.getOnlyElement(multiOrder.getOrders()).getId())
                .build();
        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        Order order = client.getOrder(requestClientInfo, orderRequest);
        // создали с выбранным способом постоплата
        assertThat(order.getPaymentMethod(), is(forcedMethod));
    }

    @Test
    // у пользователя выбрана оплата через яндекс
    public void shouldForcePostpaidToCartWithUserPaymentMethod() throws Exception {
        PaymentMethod forcedMethod = PaymentMethod.CASH_ON_DELIVERY;
        checkouterProperties.setForcePostpaid(CheckouterPropertiesImpl.ForcePostpaid.FORCE);

        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setMockPushApi(false);
        parameters.getOrder().setDelivery(DeliveryProvider.shopSelfDelivery().build());

        // мокируем пуш-апи: опции доставки, отсутствие постоплаты
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildDeliveryResponseWithIntervals();
        deliveryResponse.setPaymentOptions(Set.of(PaymentMethod.YANDEX));
        List<PaymentMethod> shopPaymentMethods = getPrepaidMethods();
        pushApiConfigurer.mockCart(parameters.getOrder(),
                Collections.singletonList(deliveryResponse), shopPaymentMethods, false);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        // появилась постоплата
        checkCartHasPostpaid(forcedMethod, multiCart);

        parameters.setPaymentMethod(forcedMethod); // пользователь выбрал этот метод, с фронта пришел еще один /cart
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        // опять мокируем пуш-апи: опции доставки, отсутствие постоплаты
        pushApiConfigurer.mockCart(parameters.getOrder(),
                Collections.singletonList(deliveryResponse), shopPaymentMethods, false);
        multiCart = orderCreateHelper.cart(parameters);
        // появилась постоплата
        checkCartHasPostpaid(forcedMethod, multiCart);

        // опять мокируем пуш-апи уже для чекаута: опции доставки, отсутствие постоплаты
        pushApiConfigurer.mockCart(parameters.getOrder(),
                Collections.singletonList(deliveryResponse), shopPaymentMethods, false
        );
        pushApiConfigurer.mockAccept(parameters.getOrder(), true);
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        OrderRequest orderRequest = OrderRequest.builder(Iterables.getOnlyElement(multiOrder.getOrders()).getId())
                .build();
        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        Order order = client.getOrder(requestClientInfo, orderRequest);
        // создали с выбранным способом постоплата
        assertThat(order.getPaymentMethod(), is(forcedMethod));
    }


    @ParameterizedTest
    @EnumSource(value = CheckouterPropertiesImpl.ForcePostpaid.class, names = {"DISABLE", "LOGGING"})
    public void shouldNotForcePostpaidMethodToCartNotMultiOrder(
            CheckouterPropertiesImpl.ForcePostpaid forcePostpaid
    ) {
        checkouterProperties.setForcePostpaid(forcePostpaid);
        List<PaymentMethod> shopPaymentMethods = getPrepaidMethods();
        PaymentMethod forcedMethod = PaymentMethod.CASH_ON_DELIVERY;
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setMockPushApi(false);
        parameters.getOrder().setDelivery(DeliveryProvider.shopSelfDelivery().build());
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildDeliveryResponseWithIntervals();
        deliveryResponse.setPaymentOptions(Set.of(PaymentMethod.YANDEX));

        pushApiConfigurer.mockCart(parameters.getOrder(),
                Collections.singletonList(deliveryResponse), shopPaymentMethods, false);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        // в самой корзине
        assertThat(multiCart.getPaymentOptions(),
                not(Matchers.hasItem(forcedMethod)));
        // в каждой корзине
        assertTrue(multiCart.getCarts().stream().noneMatch(it -> it.getPaymentOptions().contains(forcedMethod)));
        // в  доставке в каждой корзине
        assertTrue(multiCart.getCarts().stream().flatMap(it -> it.getDeliveryOptions().stream())
                .noneMatch(it -> it.getPaymentOptions().contains(forcedMethod)));
    }

    @Test
    public void shouldNotForcePostpaidYandexInDeliveryOptions() {
        checkouterProperties.setForcePostpaid(CheckouterPropertiesImpl.ForcePostpaid.FORCE);
        List<PaymentMethod> shopPaymentMethods = getPrepaidMethods();
        PaymentMethod forcedMethod = PaymentMethod.CASH_ON_DELIVERY;
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setMockPushApi(false);
        parameters.getOrder().setDelivery(DeliveryProvider.shopSelfDelivery().build());
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildDeliveryResponseWithIntervals();
        deliveryResponse.setPaymentOptions(Set.of(PaymentMethod.YANDEX, PaymentMethod.CARD_ON_DELIVERY));

        pushApiConfigurer.mockCart(parameters.getOrder(),
                Collections.singletonList(deliveryResponse), shopPaymentMethods, false);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        // в самой корзине
        assertThat(multiCart.getPaymentOptions(),
                not(Matchers.hasItem(forcedMethod)));
        // в каждой корзине
        assertTrue(multiCart.getCarts().stream().noneMatch(it -> it.getPaymentOptions().contains(forcedMethod)));
        // в  доставке в каждой корзине
        assertTrue(multiCart.getCarts().stream().flatMap(it -> it.getDeliveryOptions().stream())
                .noneMatch(it -> it.getPaymentOptions().contains(forcedMethod)));
    }

    private List<PaymentMethod> getPrepaidMethods() {
        return Stream.of(PaymentMethod.values())
                .filter(it -> it.getPaymentType() != PaymentType.POSTPAID)
                .collect(Collectors.toList());
    }

    private void checkCartHasPostpaid(PaymentMethod forcedMethod, MultiCart multiCart) {
        // в самой корзине
        assertThat(multiCart.getPaymentOptions(),
                Matchers.hasItem(forcedMethod));
        // в каждой корзине
        assertTrue(multiCart.getCarts().stream().allMatch(it -> it.getPaymentOptions().contains(forcedMethod)));
        // в  доставке в каждой корзине
        assertTrue(multiCart.getCarts().stream().flatMap(it -> it.getDeliveryOptions().stream())
                .allMatch(it -> it.getPaymentOptions().contains(forcedMethod)));
    }
}
