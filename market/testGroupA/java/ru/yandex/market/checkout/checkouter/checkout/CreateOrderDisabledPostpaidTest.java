package ru.yandex.market.checkout.checkouter.checkout;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.checkouter.order.Color.WHITE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;

/**
 * @author zagidullinri
 * @date 02.09.2021
 */
public class CreateOrderDisabledPostpaidTest extends AbstractWebTestBase {

    private static final Set<Long> SHOP_IDS_WITH_DISABLED_POSTPAID = Set.of(1L, 2L, 3L);
    private static final Set<Long> SHOP_IDS_WITH_NOT_DISABLED_POSTPAID = Set.of(4L, 5L, 6L);

    public static Stream<Arguments> disabledPostpaidTestData() {
        return Arrays.stream(CheckouterProperties.ForcePostpaid.values())
                .flatMap(forcePostpaid -> SHOP_IDS_WITH_DISABLED_POSTPAID.stream()
                        .map(shopId -> new Object[]{
                                forcePostpaid, shopId
                        }))
                .map(Arguments::of);
    }

    public static Stream<Arguments> notDisabledPostpaidTestData() {
        return Arrays.stream(CheckouterProperties.ForcePostpaid.values())
                .flatMap(forcePostpaid -> SHOP_IDS_WITH_NOT_DISABLED_POSTPAID.stream()
                        .map(shopId -> new Object[]{
                                forcePostpaid, shopId
                        }))
                .map(Arguments::of);
    }

    @BeforeEach
    public void setUp() {
        checkouterProperties.setDisabledPostpaidMethodsShopIds(SHOP_IDS_WITH_DISABLED_POSTPAID);
    }

    @ParameterizedTest(name = "ForcePostpaid: {0}, Shop: {1}")
    @MethodSource("disabledPostpaidTestData")
    /*
      Проверяем что постоплата исчезает при любом типе форсинга постоплат для магазинов из списка
     */
    public void shouldDisablePostpaidToCart(CheckouterProperties.ForcePostpaid forcePostpaid, long shopId) {
        checkouterProperties.setForcePostpaid(forcePostpaid);
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.getOrder().setShopId(shopId);
        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertThatAllPaymentMethodsMatchesMatcher(multiCart, Matchers.not(Matchers.hasItem(PaymentType.POSTPAID)));
    }

    @ParameterizedTest(name = "ForcePostpaid: {0}, Shop: {1}")
    @MethodSource("disabledPostpaidTestData")
    /*
      Проверяем что постоплата исчезает при любом типе форсинга постоплат для магазинов из списка
     */
    public void shouldDisablePostpaidToOrderEditOptions(CheckouterProperties.ForcePostpaid forcePostpaid, long shopId) {
        checkouterProperties.setForcePostpaid(forcePostpaid);
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.getOrder().setShopId(shopId);
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), CoreMatchers.equalTo(UNPAID));

        OrderEditOptionsRequest request = new OrderEditOptionsRequest();
        request.setChangeRequestTypes(Set.of(ChangeRequestType.PAYMENT_METHOD));
        OrderEditOptions editOptions = client.getOrderEditOptions(order.getId(), ClientRole.USER,
                order.getBuyer().getUid(), singletonList(WHITE), request);
        assertThatAllPaymentMethodsMatchesMatcher(editOptions.getPaymentOptions(),
                Matchers.not(Matchers.hasItem(PaymentType.POSTPAID)));
    }

    @ParameterizedTest(name = "ForcePostpaid: {0}, Shop: {1}")
    @MethodSource("notDisabledPostpaidTestData")
    /*
      Проверяем что постоплата не исчезает при любом типе форсинга постоплат для магазинов не из списка
     */
    public void shouldNotDisablePostpaidToCart(CheckouterProperties.ForcePostpaid forcePostpaid, long shopId) {
        checkouterProperties.setForcePostpaid(forcePostpaid);
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.getOrder().setShopId(shopId);
        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertThatAllPaymentMethodsMatchesMatcher(multiCart, Matchers.hasItem(PaymentType.POSTPAID));
    }

    private void assertThatAllPaymentMethodsMatchesMatcher(MultiCart multiCart,
                                                           Matcher<Iterable<? super PaymentType>> matcher) {
        // в самой корзине
        assertThatAllPaymentMethodsMatchesMatcher(multiCart.getPaymentOptions(), matcher);

        // в каждой корзине
        Set<PaymentMethod> paymentMethodsInCarts = multiCart.getCarts()
                .stream()
                .flatMap(it -> it.getPaymentOptions().stream())
                .collect(Collectors.toSet());
        assertThatAllPaymentMethodsMatchesMatcher(paymentMethodsInCarts, matcher);

        // в  доставке в каждой корзине
        Set<PaymentMethod> paymentMethodsInDeliveries = multiCart.getCarts()
                .stream()
                .flatMap(it -> it.getDeliveryOptions().stream())
                .flatMap(it -> it.getPaymentOptions().stream())
                .collect(Collectors.toSet());
        assertThatAllPaymentMethodsMatchesMatcher(paymentMethodsInDeliveries, matcher);
    }

    private void assertThatAllPaymentMethodsMatchesMatcher(Collection<PaymentMethod> paymentOptions,
                                                           Matcher<Iterable<? super PaymentType>> matcher) {
        assertThat(paymentOptions.stream().map(PaymentMethod::getPaymentType).collect(Collectors.toSet()), matcher);
    }
}
