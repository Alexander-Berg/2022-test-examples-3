package ru.yandex.market.checkout.checkouter.checkout;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class CartDeliveryIsTryingAvailableTest extends AbstractWebTestBase {

    @Test
    public void shouldReturnDeliveryOptionsWithIsTryingAvailable() {
        var parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .buildActualDeliveryOption()
                        )
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .isTryingAvailable(true)
                                .buildActualDeliveryOption()
                        ).build()
        );

        var actualMultiCart = orderCreateHelper.cart(parameters);

        var actualOrder = actualMultiCart.getCarts().get(0);
        var deliveryOptions = actualOrder.getDeliveryOptions();

        assertThat(deliveryOptions, hasSize(2));
        var delivery1 = deliveryOptions.get(0);
        var delivery2 = deliveryOptions.get(1);

        Assertions.assertNotEquals(Boolean.TRUE, delivery1.getTryingAvailable());
        Assertions.assertEquals(Boolean.TRUE, delivery2.getTryingAvailable());
    }

}
