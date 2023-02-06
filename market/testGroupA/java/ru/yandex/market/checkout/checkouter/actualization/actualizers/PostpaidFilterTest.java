package ru.yandex.market.checkout.checkouter.actualization.actualizers;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentIntegerFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentCollectionFeatureType.PAYMENT_DISABLE_METHOD;

public class PostpaidFilterTest extends AbstractWebTestBase {

    @Autowired
    private OrderCreateHelper orderCreateHelper;

    @AfterEach
    public void tearDown() {
        checkouterFeatureWriter.writeValue(PAYMENT_DISABLE_METHOD, Set.of());
        checkouterFeatureWriter.writeValue(PermanentIntegerFeatureType.PAYMENT_DISABLE_USER_PERCENT, 0);
    }

    @Test
    public void shouldFilterPostpayOptionWithExp() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        assertTrue(actualCart.getPaymentOptions().stream()
                .anyMatch(paymentMethod -> PaymentType.POSTPAID.equals(paymentMethod.getPaymentType())));

        parameters.setExperiments(Experiments.POSTPAY_FILTER);
        actualCart = orderCreateHelper.cart(parameters);

        Order actualOrder = actualCart.getCarts().get(0);
        assertThat(actualOrder.getPaymentOptions(), everyItem(
                hasProperty("paymentType", Matchers.is(PaymentType.PREPAID))
        ));
    }

    @Test
    public void shouldNotFilterPostpayOptionWithoutExp() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        assertTrue(actualOrder.getPaymentOptions().stream()
                .anyMatch(paymentMethod -> PaymentType.POSTPAID.equals(paymentMethod.getPaymentType())));
    }

    @Test
    public void shouldNotFilterPostpayOptionWithtExpIfPrepayOptionEmpty() {

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setExperiments(Experiments.POSTPAY_FILTER);

        Set<PaymentMethod> paymentPrepaidMethods = Stream.of(PaymentMethod.values()).
                filter(paymentMethod -> PaymentType.PREPAID.equals(paymentMethod.getPaymentType())).
                collect(Collectors.toSet());

        checkouterFeatureWriter.writeValue(PermanentIntegerFeatureType.PAYMENT_DISABLE_USER_PERCENT, 100);
        checkouterFeatureWriter.writeValue(PAYMENT_DISABLE_METHOD, paymentPrepaidMethods.stream()
                .map(PaymentMethod::name)
                .collect(Collectors.toSet()));
        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);

        assertThat(actualOrder.getPaymentOptions(), everyItem(
                hasProperty("paymentType", Matchers.is(PaymentType.POSTPAID))
        ));
    }

    @Test
    public void availablePaymentMethodsTests() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setExperiments(Experiments.POSTPAY_FILTER);

        Set<PaymentMethod> paymentPrepaidMethods = Stream.of(PaymentMethod.values()).
                filter(paymentMethod -> PaymentType.PREPAID.equals(paymentMethod.getPaymentType())).
                collect(Collectors.toSet());
        checkouterFeatureWriter.writeValue(PermanentIntegerFeatureType.PAYMENT_DISABLE_USER_PERCENT, 100);
        checkouterFeatureWriter.writeValue(PAYMENT_DISABLE_METHOD, paymentPrepaidMethods.stream()
                .map(PaymentMethod::name)
                .collect(Collectors.toSet()));

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);

        assertNotNull(actualOrder.getProperty(OrderPropertyType.PARTNER_PAYMENT_METHODS));
    }
}
