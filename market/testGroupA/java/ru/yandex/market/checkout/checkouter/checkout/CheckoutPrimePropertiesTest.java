package ru.yandex.market.checkout.checkouter.checkout;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

public class CheckoutPrimePropertiesTest extends AbstractWebTestBase {

    public static final String YANDEX_PLUS_PERK = "yandex_plus";

    private static Parameters yandexPlusParameters() {
        Parameters parameters = new Parameters();
        parameters.configuration().cart().request().setPerks(YANDEX_PLUS_PERK);
        parameters.setYandexPlus(true);
        return parameters;
    }

    @Test
    public void shouldCheckYaPlusOnCheckout() throws Exception {
        Parameters parameters = yandexPlusParameters();
        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        Boolean property = checkout.getCarts().get(0).getProperty(OrderPropertyType.YANDEX_PLUS);
        assertThat("order.property.prime", property, allOf(notNullValue(), is(true)));
    }

    @Test
    public void shouldNotCheckYaPlusOnCart() {
        MultiCart cart = orderCreateHelper.cart(yandexPlusParameters());
        Boolean property = cart.getCarts().get(0).getProperty(OrderPropertyType.YANDEX_PLUS);
        assertThat("order.property.prime", property, is(nullValue()));
    }

    @Test
    public void shouldWriteYaplusIntoOrder() {
        Order order = orderCreateHelper.createOrder(yandexPlusParameters());
        Boolean prime = order.getProperty(OrderPropertyType.YANDEX_PLUS);
        assertThat("order.property.prime", prime, allOf(notNullValue(), is(true)));
    }

    @Test
    public void shouldSendYaPlusToReport() {
        orderCreateHelper.createOrder(yandexPlusParameters());
        var reportEvents = reportMock.getServeEvents();
        assertThat(
                reportEvents.getServeEvents()
                        .stream()
                        .filter(se -> se.getRequest().queryParameter("place")
                                .containsValue(MarketReportPlace.OFFER_INFO.getId()))
                        .filter(se -> se.getRequest().queryParameter("perks")
                                .containsValue(YANDEX_PLUS_PERK))
                        .collect(Collectors.toList()),
                hasSize(greaterThanOrEqualTo(1))
        );
    }
}
