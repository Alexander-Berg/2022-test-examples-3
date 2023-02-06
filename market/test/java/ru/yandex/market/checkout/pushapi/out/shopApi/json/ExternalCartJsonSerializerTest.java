package ru.yandex.market.checkout.pushapi.out.shopApi.json;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.common.json.JsonWriter;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;
import ru.yandex.market.checkout.pushapi.out.shopApi.json.order.BuyerJsonSerializer;
import ru.yandex.market.checkout.pushapi.out.shopApi.json.order.ExternalCartItemJsonSerializer;
import ru.yandex.market.checkout.pushapi.providers.ShopOrderProvider;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCart;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCartItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

//TODO пераработать тест на адекатную проверку MARKETCHECKOUT-14893
public class ExternalCartJsonSerializerTest {

    private DeliveryWithRegion deliveryWithRegion = mock(DeliveryWithRegion.class);
    private ExternalCartItem item1;
    private ExternalCartItem item2;

    private ExternalCartJsonSerializer serializer = new ExternalCartJsonSerializer();

    @BeforeEach
    public void setUp() throws Exception {
        item1 = new ExternalCartItem() {{
            setOfferId("item1");
            setFeedId(1L);
        }};
        item2 = new ExternalCartItem() {{
            setOfferId("item2");
            setFeedId(1L);
        }};
        serializer.setDeliveryWithRegionJsonSerializer(
                new DeliveryWithRegionJsonSerializer() {
                    @Override
                    public void serialize(DeliveryWithRegion value, JsonWriter writer) throws IOException {
                        assertEquals(deliveryWithRegion, value);
                        writer.setValue("delivery");
                    }
                }
        );
        serializer.setExternalCartItemJsonSerializer(
                new ExternalCartItemJsonSerializer(null) {
                    @Override
                    public void serialize(ExternalCartItem value, JsonWriter generator) throws IOException {
                        if (item1 == value) {
                            generator.setValue(value.getOfferId());
                        } else if (item2 == value) {
                            generator.setValue(value.getOfferId());
                        } else {
                            fail();
                        }
                    }
                }
        );
        serializer.setBuyerJsonSerializer(new BuyerJsonSerializer());
    }

    @Test
    public void testSerializeWithAddress() throws Exception {
        final ExternalCart externalCart = new ExternalCart(
                1L,
                deliveryWithRegion,
                Currency.RUR,
                Currency.USD,
                Arrays.asList(item1, item2));
        JsonTestUtil.assertJsonSerialize(
                serializer, externalCart,
                "{'cart': {'businessId': 1, 'currency': 'RUR'," +
                        "'deliveryCurrency': 'USD'," +
                        "'items': ['item1', 'item2']," +
                        "'delivery': 'delivery'}}"
        );
    }

    @Test
    public void testSerializeWithBuyer() throws Exception {
        final ExternalCart externalCart = new ExternalCart(
                1L,
                deliveryWithRegion,
                Currency.RUR,
                Currency.USD,
                Arrays.asList(item1, item2));
        externalCart.setBuyer(ShopOrderProvider.prepareSberIdBuyer());
        JsonTestUtil.assertJsonSerialize(
                serializer, externalCart,
                "{'cart': {'businessId': 1, 'currency': 'RUR'," +
                        "'deliveryCurrency': 'USD'," +
                        "'buyer': {" +
                        "    'id': '1234567890'," +
                        "    'lastName': 'Tolstoy'," +
                        "    'firstName': 'Leo'," +
                        "    'middleName': 'Nikolaevich'," +
                        "    'phone': '+71234567891'," +
                        "    'email': 'a@b.com'," +
                        "    'uid': 2305843009213693951" +
                        "}," +
                        "'items': ['item1', 'item2']," +
                        "'delivery': 'delivery'}}"
        );
    }

    @Test
    public void testSerializeEmpty() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new ExternalCart(),
                "{'cart': {}}"
        );
    }
}
