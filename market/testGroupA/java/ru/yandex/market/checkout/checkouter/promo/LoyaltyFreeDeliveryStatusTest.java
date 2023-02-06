package ru.yandex.market.checkout.checkouter.promo;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class LoyaltyFreeDeliveryStatusTest extends AbstractWebTestBase {

    @Test
    public void freeDeliveryStatusInCartAndCheckout() throws Exception {
        Parameters params = BlueParametersProvider.defaultBlueOrderParameters();
        params.getLoyaltyParameters().setPriceLeftForFreeDelivery(BigDecimal.valueOf(1000L));
        params.getLoyaltyParameters().setFreeDeliveryReason(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY);
        params.getLoyaltyParameters().setFreeDeliveryStatus(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS);
        loyaltyConfigurer.mockCalcsWithDynamicResponse(params);
        MultiCart multiCart = orderCreateHelper.cart(params);
        assertThat(multiCart.getFreeDeliveryReason(), is(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY));
        assertThat(multiCart.getFreeDeliveryStatus(), is(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, params);
        assertThat(multiOrder.getFreeDeliveryReason(), is(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY));
        assertThat(multiOrder.getFreeDeliveryStatus(), is(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));
    }

    @Test
    public void freeDeliveryThresholdInCartAndCheckout() throws Exception {
        Parameters params = BlueParametersProvider.defaultBlueOrderParameters();
        BigDecimal threshold = BigDecimal.TEN;
        params.getLoyaltyParameters().setFreeDeliveryThreshold(threshold);
        loyaltyConfigurer.mockCalcsWithDynamicResponse(params);
        MultiCart multiCart = orderCreateHelper.cart(params);
        assertThat(multiCart.getFreeDeliveryThreshold(), is(threshold));
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, params);
        assertThat(multiOrder.getFreeDeliveryThreshold(), is(threshold));
    }
}
