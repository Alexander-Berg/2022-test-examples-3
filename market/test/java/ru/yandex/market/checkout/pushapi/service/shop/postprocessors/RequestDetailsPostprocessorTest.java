package ru.yandex.market.checkout.pushapi.service.shop.postprocessors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.util.DeliveryUtil;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;
import ru.yandex.market.checkout.pushapi.out.shopApi.json.DeliveryResponseJsonDeserializer;
import ru.yandex.market.checkout.pushapi.providers.PushApiCartProvider;
import ru.yandex.market.checkout.pushapi.service.shop.CartContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.defaultOrderItem;

/**
 * @author Nicolai Iusiumbeli <mailto:armor@yandex-team.ru>
 * date: 12/05/2017
 */
public class RequestDetailsPostprocessorTest {
    private RequestDetailsPostprocessor postprocessor = new RequestDetailsPostprocessor();
    private DeliveryResponseJsonDeserializer deserializer = new DeliveryResponseJsonDeserializer();

    @BeforeEach
    public void setUp() throws Exception {
        postprocessor.setCheckoutDateFormat(new CheckoutDateFormat());
    }

    @Test
    public void testMergeDeliveryOptions() throws Exception {
        DeliveryResponse delivery1 = JsonTestUtil.deserialize(deserializer,
                "{'type':'DELIVERY'," +
                        " 'id': '12345'," +
                        " 'shopDeliveryId': '54321'," +
                        " 'hash': 'vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='," +
                        " 'price': 2345," +
                        " 'dates': {" +
                        "   'toDate': '23-05-2013'," +
                        "   'fromDate': '20-05-2013'" +
                        "  }," +
                        " 'outlets':[{'id': 3456}, {'code': 'Ny-Ny'}]," +
                        " 'serviceName': '234'," +
                        " 'paymentMethods': ['CASH_ON_DELIVERY', 'CARD_ON_DELIVERY']}"
        );
        DeliveryResponse delivery2 = JsonTestUtil.deserialize(deserializer,
                "{'type':'DELIVERY'," +
                        " 'id': '12345'," +
                        " 'shopDeliveryId': '54321'," +
                        " 'hash': 'vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='," +
                        " 'price': 2345," +
                        " 'dates': {" +
                        "   'toDate': '23-05-2013'," +
                        "   'fromDate': '20-05-2013'" +
                        "  }," +
                        " 'outlets':[ {'id': 4567}, {'id': 5678}, {'code': 'Ny-Ny'}]," +
                        " 'serviceName': '234'," +
                        " 'paymentMethods': ['CASH_ON_DELIVERY', 'CARD_ON_DELIVERY']}"
        );
        List<DeliveryResponse> shopResponses = Arrays.asList(delivery1, delivery2);
        shopResponses.forEach(d -> {
            d.setDeliveryServiceId(99L);
            d.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        });
        List<DeliveryResponse> actual = postprocessor.mergeDeliveryOptions(774, shopResponses);


        DeliveryResponse expected = JsonTestUtil.deserialize(deserializer,
                "{'type':'DELIVERY'," +
                        " 'id': '12345'," +
                        " 'shopDeliveryId': '54321'," +
                        " 'hash': 'vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='," +
                        " 'deliveryOptionId': '774_DELIVERY_234_20-05-2013_noreserve_23-05-2013_54321_24_99_SHOP_null_TRYING_OFF" +
                        "'," +
                        " 'price': 2345," +
                        " 'dates': {" +
                        "   'toDate': '23-05-2013'," +
                        "   'fromDate': '20-05-2013'" +
                        "  }," +
                        " 'outlets':[{'id': 3456}, {'id': 4567}, {'id': 5678}, {'code': 'Ny-Ny'}]," +
                        " 'outletIds':[3456, 4567, 5678]," +
                        " 'serviceName': '234'," +
                        " 'deliveryServiceId': 99," +
                        " 'deliveryPartnerType': 'SHOP', " +
                        " 'paymentMethods': ['CASH_ON_DELIVERY', 'CARD_ON_DELIVERY']}"

        );
        expected.setDeliveryPartnerType(DeliveryPartnerType.SHOP);

        assertEquals(Collections.singletonList(expected), actual);
    }

    @Test
    public void testMergeDifferentDeliveryOptions() throws Exception {
        String delivery1Src = "{'type':'DELIVERY'," +
                " 'id': '12345'," +
                " 'shopDeliveryId': '54321'," +
                " 'hash': 'vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='," +
                " 'price': 2345," +
                " 'dates': {" +
                "   'toDate': '23-05-2013'," +
                "   'fromDate': '20-05-2013'" +
                "  }," +
                " 'outlets':[{'id': 3456}]," +
                " 'serviceName': '234'," +
                " 'paymentMethods': ['CASH_ON_DELIVERY']}";

        String delivery2Src = "{'type':'DELIVERY'," +
                " 'id': '12345'," +
                " 'shopDeliveryId': '54321'," +
                " 'hash': 'vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='," +
                " 'price': 2345," +
                " 'dates': {" +
                "   'toDate': '23-05-2013'," +
                "   'fromDate': '20-05-2013'" +
                "  }," +
                " 'outlets':[ {'id': 4567}, {'id': 5678}]," +
                " 'serviceName': '234'," +
                " 'paymentMethods': ['CARD_ON_DELIVERY']}";

        checkMergeDifferentDeliveryOptions(delivery1Src, delivery2Src);

    }

    @Test
    public void testMergeDifferentDeliveryOptionsCodeOutlet() throws Exception {
        String delivery1Src = "{'type':'DELIVERY'," +
                " 'id': '12345'," +
                " 'shopDeliveryId': '54321'," +
                " 'hash': 'vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='," +
                " 'price': 2345," +
                " 'dates': {" +
                "   'toDate': '23-05-2013'," +
                "   'fromDate': '20-05-2013'" +
                "  }," +
                " 'outlets':[{'code': '3456str'}]," +
                " 'serviceName': '234'," +
                " 'paymentMethods': ['CASH_ON_DELIVERY']}";

        String delivery2Src = "{'type':'DELIVERY'," +
                " 'id': '12345'," +
                " 'shopDeliveryId': '54321'," +
                " 'hash': 'vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='," +
                " 'price': 2345," +
                " 'dates': {" +
                "   'toDate': '23-05-2013'," +
                "   'fromDate': '20-05-2013'" +
                "  }," +
                " 'outlets':[ {'code': '4567str'}, {'code': 'Янтарь'}, {'id': 5678}]," +
                " 'serviceName': '234'," +
                " 'paymentMethods': ['CARD_ON_DELIVERY']}";

        checkMergeDifferentDeliveryOptions(delivery1Src, delivery2Src);
    }

    @Test
    public void testDeliveryServiceIdSubstitution() throws Exception {
        DeliveryResponse option = JsonTestUtil.deserialize(deserializer,
                "{'type':'DELIVERY'," +
                        " 'id': '12345'," +
                        " 'shopDeliveryId': '54321'," +
                        " 'hash': 'vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='," +
                        " 'deliveryOptionId': '774_DELIVERY_234_20-05-2013_noreserve_23-05-2013_54321_24_99_SHOP" +
                        "'," +
                        " 'price': 2345," +
                        " 'dates': {" +
                        "   'toDate': '23-05-2013'," +
                        "   'fromDate': '20-05-2013'" +
                        "  }," +
                        " 'outlets':[{'id': 3456}, {'id': 4567}, {'id': 5678}, {'code': 'Ny-Ny'}]," +
                        " 'outletIds':[3456, 4567, 5678]," +
                        " 'serviceName': '234'," +
                        " 'deliveryServiceId': 155," +
                        " 'deliveryPartnerType': 'SHOP', " +
                        " 'paymentMethods': ['CASH_ON_DELIVERY', 'CARD_ON_DELIVERY']}"

        );
        final Cart cart = PushApiCartProvider.buildCartRequest();
        final CartResponse cartResponse = new CartResponse(
                Collections.singletonList(defaultOrderItem()),
                Collections.singletonList(option),
                Collections.singletonList(PaymentMethod.YANDEX)
        );
        final CartContext cartContext = new CartContext(123L);

        postprocessor.process(cart, cartResponse, cartContext);

        assertThat(cartResponse.getDeliveryOptions(), hasSize(1));
        assertThat(cartResponse.getDeliveryOptions().get(0).getDeliveryServiceId(), is(99L));
    }

    @Test
    public void testNoDeliveryServiceIdSubstitutionForCrossborder() throws Exception {
        DeliveryResponse option = JsonTestUtil.deserialize(deserializer,
                "{'type':'DELIVERY'," +
                        " 'id': '12345'," +
                        " 'shopDeliveryId': '54321'," +
                        " 'hash': 'vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='," +
                        " 'deliveryOptionId': '774_DELIVERY_234_20-05-2013_noreserve_23-05-2013_54321_24_99_SHOP" +
                        "'," +
                        " 'price': 2345," +
                        " 'dates': {" +
                        "   'toDate': '23-05-2013'," +
                        "   'fromDate': '20-05-2013'" +
                        "  }," +
                        " 'outlets':[{'id': 3456}, {'id': 4567}, {'id': 5678}, {'code': 'Ny-Ny'}]," +
                        " 'outletIds':[3456, 4567, 5678]," +
                        " 'serviceName': '234'," +
                        " 'deliveryServiceId': 155," +
                        " 'deliveryPartnerType': 'SHOP', " +
                        " 'paymentMethods': ['CASH_ON_DELIVERY', 'CARD_ON_DELIVERY']}"

        );
        final Cart cart = PushApiCartProvider.buildCartRequest();
        cart.setCrossborder(true);
        final CartResponse cartResponse = new CartResponse(
                Collections.singletonList(defaultOrderItem()),
                Collections.singletonList(option),
                Collections.singletonList(PaymentMethod.YANDEX)
        );
        final CartContext cartContext = new CartContext(123L);

        postprocessor.process(cart, cartResponse, cartContext);

        assertThat(cartResponse.getDeliveryOptions(), hasSize(1));
        assertThat(cartResponse.getDeliveryOptions().get(0).getDeliveryServiceId(), is(155L));
    }

    private void checkMergeDifferentDeliveryOptions(String delivery1Src, String delivery2Src)
            throws Exception {
        DeliveryResponse delivery1 = JsonTestUtil.deserialize(deserializer, delivery1Src);
        DeliveryResponse delivery2 = JsonTestUtil.deserialize(deserializer, delivery2Src);

        DeliveryResponse delivery1Expected = JsonTestUtil.deserialize(deserializer, delivery1Src);
        DeliveryResponse delivery2Expected = JsonTestUtil.deserialize(deserializer, delivery2Src);
        delivery1Expected.setDeliveryOptionId(DeliveryUtil.buildDeliveryOptionId(774, delivery1Expected));
        delivery2Expected.setDeliveryOptionId(DeliveryUtil.buildDeliveryOptionId(774, delivery2Expected));

        List<DeliveryResponse> actual = postprocessor.mergeDeliveryOptions(774,
                Arrays.asList(delivery1, delivery2));

        assertEquals(Arrays.asList(delivery1Expected, delivery2Expected), actual);
    }


    @Test
    public void testMergeDeliveryOptionsNoOutlets() throws Exception {
        String delivery1Src = "{'type':'DELIVERY'," +
                " 'id': '12345'," +
                " 'shopDeliveryId': '54321'," +
                " 'hash': 'vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='," +
                " 'price': 2345," +
                " 'dates': {" +
                "   'toDate': '23-05-2013'," +
                "   'fromDate': '20-05-2013'" +
                "  }," +
                " 'serviceName': '234'," +
                " 'paymentMethods': ['CASH_ON_DELIVERY']}";

        DeliveryResponse delivery1 = JsonTestUtil.deserialize(deserializer, delivery1Src);

        DeliveryResponse delivery1Expected = JsonTestUtil.deserialize(deserializer, delivery1Src);
        delivery1Expected.setDeliveryOptionId(DeliveryUtil.buildDeliveryOptionId(
                774,
                delivery1Expected)
        );

        List<DeliveryResponse> actual = postprocessor.mergeDeliveryOptions(
                774,
                Collections.singletonList(delivery1)
        );

        assertEquals(Collections.singletonList(delivery1Expected), actual);

    }

    @Test
    public void testMergeDeliveryOptionsEmpty() throws Exception {
        List<DeliveryResponse> actual = postprocessor.mergeDeliveryOptions(
                774,
                Collections.emptyList()
        );
        assertEquals(null, actual);
    }

    @Test
    public void testMergeDeliveryOptionsNull() throws Exception {
        List<DeliveryResponse> actual = postprocessor.mergeDeliveryOptions(774, null);
        assertEquals(null, actual);
    }
}
