package ru.yandex.market.checkout.checkouter.checkout;

import java.util.Arrays;
import java.util.stream.Stream;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * checkouter-13: Успешное создание постоплатного заказа
 *
 * @link https://testpalm.yandex-team.ru/testcase/checkouter-13
 */
public class CreateOrderPostpaidSuccessTest extends AbstractWebTestBase {


    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(new Object[][]{
                {
                        /**
                         * Создать заказ с
                         * “paymentType”: “POSTPAID”
                         * “paymentMethod”: “CARD_ON_DELIVERY”
                        */
                        PaymentType.POSTPAID,
                        PaymentMethod.CARD_ON_DELIVERY
                },
                {
                        /**
                         * Создать заказ с
                         * “paymentType”: “POSTPAID”
                         * “paymentMethod”: “CASH_ON_DELIVERY”
                        */
                        PaymentType.POSTPAID,
                        PaymentMethod.CASH_ON_DELIVERY
                },
        }).stream().map(Arguments::of);
    }


    /**
     * checkouter-13: Успешное создание постоплатного заказа
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-13
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Успешное создание постоплатного заказа")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void createOrderPostpaidCard(PaymentType paymentType, PaymentMethod paymentMethod) throws Exception {
        Parameters parameters = new Parameters();
        parameters.getOrder().setPaymentType(PaymentType.POSTPAID);
        parameters.getOrder().setPaymentMethod(paymentMethod);
        parameters.getBuiltMultiCart().setPaymentType(paymentType);
        parameters.getBuiltMultiCart().setPaymentMethod(paymentMethod);
        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getId(), notNullValue());
        assertEquals(OrderStatus.PROCESSING, order.getStatus());

        assertEquals(paymentType, order.getPaymentType());
        assertEquals(paymentMethod, order.getPaymentMethod());
    }

}
