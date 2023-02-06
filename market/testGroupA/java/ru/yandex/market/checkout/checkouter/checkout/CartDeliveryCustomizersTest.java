package ru.yandex.market.checkout.checkouter.checkout;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryCustomizer;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

public class CartDeliveryCustomizersTest extends AbstractWebTestBase {

    @Test
    public void shouldReturnDeliveryOptionsWithCustomizers() {
        var parameters = WhiteParametersProvider.defaultWhiteParameters();
        var deliveryCustomizers = new ArrayList<DeliveryCustomizer>();
        deliveryCustomizers.add(new DeliveryCustomizer("leave_at_the_door", "Оставить у двери", "boolean"));
        deliveryCustomizers.add(new DeliveryCustomizer("no_call", "Не звонить", "boolean"));
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .customizers(deliveryCustomizers)
                                .buildActualDeliveryOption()
                        )
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .customizers(null)
                                .buildActualDeliveryOption()
                        ).build()
        );

        var actualMultiCart = orderCreateHelper.cart(parameters);

        var actualOrder = actualMultiCart.getCarts().get(0);
        var deliveryOptions = actualOrder.getDeliveryOptions();

        assertThat(deliveryOptions, hasSize(2));
        var delivery1 = deliveryOptions.get(0);
        var delivery2 = deliveryOptions.get(1);

        assertThat(delivery1.getCustomizers(), hasSize(2));
        var customizers = delivery1.getCustomizers();
        assertThat(customizers, containsInAnyOrder(
                new DeliveryCustomizer("leave_at_the_door", "Оставить у двери", "boolean"),
                new DeliveryCustomizer("no_call", "Не звонить", "boolean")
        ));

        assertThat(delivery2.getCustomizers(), hasSize(0));
    }
}
