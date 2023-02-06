package ru.yandex.market.checkout.checkouter.delivery;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletPhone;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.RUSPOST_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class OutletSerializeTest extends AbstractWebTestBase {

    public static final String SHOP_OUTLET_CODE = "asdasd";

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @ParameterizedTest
    @CsvSource({"false", "true"})
    public void shouldCorrectReturnOutletNameAndPhonesForPost(boolean enable) throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_POST_OUTLET_ACTUALIZER, enable);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(RUSPOST_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.POST)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withPartnerInterface(true)
                .buildParameters();

        Order order = orderCreateHelper.createOrder(parameters);

        ShopOutlet postOutlet = order.getDelivery().getPostOutlet();
        Assertions.assertNotNull(postOutlet);

        assertEquals("Почтовое отделение", postOutlet.getName());
        assertThat(postOutlet.getPhones(), contains(new ShopOutletPhone("7", "495", "334-3343", "")));
    }

    @Test
    public void shouldCorrectReturnOutletNameAndPhonesForPickup() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setShopId(775L);
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDelivery());
        parameters.setPushApiDeliveryResponse(DeliveryProvider.shopSelfPickupDeliveryByOutletCode()
                .outletCode(SHOP_OUTLET_CODE)
                .buildResponse(DeliveryResponse::new));
        parameters.setDeliveryType(DeliveryType.PICKUP);

        Order order = orderCreateHelper.createOrder(parameters);

        ShopOutlet outlet = order.getDelivery().getOutlet();
        Assertions.assertNotNull(outlet);

        assertEquals("Ионотека", outlet.getName());
        assertThat(outlet.getPhones(), contains(new ShopOutletPhone("7", "950", "272-36452", "8888")));
    }
}
