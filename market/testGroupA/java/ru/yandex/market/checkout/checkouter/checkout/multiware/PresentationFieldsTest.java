package ru.yandex.market.checkout.checkouter.checkout.multiware;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.common.report.model.ActualDeliveryResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class PresentationFieldsTest extends AbstractWebTestBase {

    public static final LoyaltyDiscount LOYALTY_FREE_DELIVERY = new LoyaltyDiscount(BigDecimal.valueOf(5000L),
            PromoType.FREE_DELIVERY_THRESHOLD);

    @Test
    public void tariffWarningInMultiCart() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Parameters anotherParameters = BlueParametersProvider.defaultBlueOrderParameters();
        addDisclaimers(anotherParameters);
        parameters.addOrder(anotherParameters);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getPresentationFields(), notNullValue());
        assertThat(multiCart.getPresentationFields().isTariffWarning(), equalTo(true));
    }

    @Test
    public void noTariffWarningForFreeDelivery() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getLoyaltyParameters().addDeliveryDiscount(LOYALTY_FREE_DELIVERY);
        Parameters anotherParameters = BlueParametersProvider.defaultBlueOrderParameters();
        anotherParameters.getLoyaltyParameters().addDeliveryDiscount(LOYALTY_FREE_DELIVERY);
        addDisclaimers(anotherParameters);
        parameters.addOrder(anotherParameters);
        parameters.setMockLoyalty(true);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getPresentationFields(), notNullValue());
        assertThat(multiCart.getPresentationFields().isTariffWarning(), equalTo(false));
    }

    private void addDisclaimers(Parameters parameters) {
        parameters.getReportParameters().getActualDelivery().getResults().stream()
                .map(ActualDeliveryResult::getDelivery)
                .flatMap(Collection::stream)
                .forEach(d -> d.setDisclaimers(Collections.singletonList("tariffWarning")));
    }
}
