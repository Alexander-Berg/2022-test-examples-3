package ru.yandex.market.checkout.checkouter.order;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckoutParametersBuilder;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.util.OrderUtils.firstOrder;

public class OrderPpTest extends AbstractWebTestBase {

    /**
     * Проверяет, что новое поле pp пишется в базу при вызове checkout.
     */
    @Test
    public void shouldReturnOfferPpAfterCart() {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        var cart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.mapCartToOrder(cart, parameters);
        // хакнул pp, потому что он удаляется перед отправкой респонса cart(), а в логике тестов используется именно он
        multiOrder.getCarts().get(0).getItems().iterator().next().setPp(1000);
        pushApiConfigurer.mockAccept(multiOrder.getCarts().get(0), true);
        var multiOrderAfterCheckout = client.checkout(multiOrder,
                CheckoutParametersBuilder.aCheckoutParameters()
                        .withUid(parameters.getBuyer().getUid())
                        .withContext(Context.MARKET)
                        .withHitRateGroup(HitRateGroup.UNLIMIT)
                        .withApiSettings(ApiSettings.PRODUCTION)
                        .withRgb(Color.BLUE)
                        .build());
        var actualOrderFromDatabase = orderService.getOrder(firstOrder(multiOrderAfterCheckout).getId());
        var actualItemFromDatabase = actualOrderFromDatabase.getItems().iterator().next();
        assertThat(actualItemFromDatabase.getPp(), is(1000));
    }
}
