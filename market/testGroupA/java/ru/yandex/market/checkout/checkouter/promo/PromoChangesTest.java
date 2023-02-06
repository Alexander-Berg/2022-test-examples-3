package ru.yandex.market.checkout.checkouter.promo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.report.ItemInfo;

import static java.math.BigDecimal.TEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author sergeykoles
 * Created on: 29.06.18
 */
public class PromoChangesTest extends AbstractWebTestBase {

    private Parameters parameters;

    @Test
    @DisplayName("При изменении цены айтема с промо должно присутствовать change=PRICE")
    public void shouldGiveChangeOnPriceUpdateOfPromoOrder() throws Exception {
        MultiCart cart = orderCreateHelper.cart(parameters);
        cart.getCarts().iterator().next().getItems().forEach(
                oi -> assertThat(oi.getChanges(), contains(ItemChange.PRICE))
        );
        parameters.setMultiCartAction(
                mc -> mc.getCarts().forEach(
                        c -> c.getItems().forEach(
                                orderItem -> orderItem.setBuyerPrice(orderItem.getBuyerPrice().add(TEN)
                                )
                        )
                )
        );
        orderCreateHelper.checkout(cart, parameters);
    }

    @Test
    @DisplayName("При изменении цены айтема с промо должен быть запрещён чекаут")
    public void shouldForbidCheckoutOnPriceUpdateOfPromoOrder() throws Exception {
        parameters.setMultiCartAction(
                mc -> mc.getCarts().forEach(
                        c -> c.getItems().forEach(
                                // сделаем вид, что мы клиент, который не умеет работать с item.prices
                                oi -> oi.getPrices().setBuyerPriceNominal(null)
                        )
                )
        );
        parameters.setCheckOrderCreateErrors(false);
        orderCreateHelper.createOrder(parameters);
    }

    @BeforeEach
    public void setUp() {
        parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        parameters.getOrder().getItems().forEach(
                orderItem -> {
                    ItemInfo.Prices reportPrices =
                            parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId())
                            .getPrices();
                    reportPrices.value = orderItem.getBuyerPrice().subtract(TEN);
                    reportPrices.discountOldMin = orderItem.getBuyerPrice().add(TEN);
                }
        );
        // эта штука по умолчанию проверяет, что заказ не создался. Вот такая вот вешчь.
        parameters.setCheckCartErrors(false);
    }
}
