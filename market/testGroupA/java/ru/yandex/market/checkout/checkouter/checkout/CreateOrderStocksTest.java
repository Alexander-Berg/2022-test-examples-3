package ru.yandex.market.checkout.checkouter.checkout;

import io.qameta.allure.junit4.Tag;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CreateOrderStocksTest extends AbstractWebTestBase {

    @Tag(Tags.CROSSBORDER)
    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    public void shouldSetIgnoreStocksCorrectly(boolean ignoreStocks, boolean isCrossborder) {
        Parameters parameters = new Parameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.getReportParameters().setIgnoreStocks(ignoreStocks);
        parameters.getReportParameters().setCrossborder(isCrossborder);

        Order order = orderCreateHelper.createOrder(parameters);
        order = orderService.getOrder(order.getId());

        assertThat(order.isIgnoreStocks(), is(ignoreStocks));
        if (isCrossborder) {
            assertThat(order.getProperty(OrderPropertyType.IS_CROSSBORDER), is(isCrossborder));
        } else {
            assertThat(order.getProperty(OrderPropertyType.IS_CROSSBORDER), Matchers.nullValue());
        }
    }
}
