package ru.yandex.market.checkout.checkouter.checkout;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;

//FIXME нужно переписывать
@Deprecated
@Disabled
public class CreateOrderBelorussiaTest extends AbstractWebTestBase {

    private static final long MINSK_REGION_ID = 157;

    @Test
    public void shouldCreateOrderInMinskSuccessfully() {
        Order order = OrderProvider.getBlueOrder();
        Parameters parameters = new Parameters(MultiCartProvider.buildMultiCart(
                order, MINSK_REGION_ID, Currency.BYN
        ));

        parameters.getReportParameters().setBuyerCurrency(Currency.BYN);

        Order createdOrder = orderCreateHelper.createOrder(parameters);

        assertThat(createdOrder.getBuyerCurrency(), Matchers.is(Currency.BYN));
    }
}
