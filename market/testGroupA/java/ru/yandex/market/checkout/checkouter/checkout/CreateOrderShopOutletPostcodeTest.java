package ru.yandex.market.checkout.checkouter.checkout;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CreateOrderShopOutletPostcodeTest extends AbstractWebTestBase {

    @Test
    public void shouldCreateShopOutletPostcode() {
        Parameters parameters = BlueParametersProvider.clickAndCollectOrderParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setOutletId(419586L);
        parameters.setPushApiDeliveryResponse(DeliveryProvider.shopSelfPickupDeliveryByOutletCode()
                .outletCode(null)
                .outletId(DeliveryProvider.SHOP_OUTLET_POSTCODE_ID)
                .buildResponse(DeliveryResponse::new));

        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDelivery());

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getDelivery().getOutlet().getPostcode(), is("123456"));
    }
}
