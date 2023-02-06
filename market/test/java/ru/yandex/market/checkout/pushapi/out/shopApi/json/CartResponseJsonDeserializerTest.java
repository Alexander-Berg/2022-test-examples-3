package ru.yandex.market.checkout.pushapi.out.shopApi.json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemBuilder;
import ru.yandex.market.checkout.common.json.JsonReader;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;
import ru.yandex.market.checkout.pushapi.out.shopApi.json.order.OrderItemJsonDeserializer;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.CASH_ON_DELIVERY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.YANDEX_MONEY;

public class CartResponseJsonDeserializerTest {

    private DeliveryResponse delivery1 = mock(DeliveryResponse.class);
    private DeliveryResponse delivery2 = mock(DeliveryResponse.class);
    private OrderItem item1 = (new OrderItemBuilder()).withOfferId("item1").withFeedId(1L).build();
    private OrderItem item2 = (new OrderItemBuilder()).withOfferId("item2").withFeedId(1L).build();

    private CartResponseJsonDeserializer deserializer = new CartResponseJsonDeserializer();

    @BeforeEach
    public void setUp() throws Exception {
        deserializer.setDeliveryResponseJsonDeserializer(
                new DeliveryResponseJsonDeserializer() {
                    @Override
                    public DeliveryResponse deserialize(JsonReader jsonParser) throws IOException {
                        switch (jsonParser.getString("val")) {
                            case "delivery1":
                                return delivery1;
                            case "delivery2":
                                return delivery2;
                            default:
                                fail();
                                return null;
                        }
                    }
                }
        );
        deserializer.setOrderItemJsonDeserializer(
                new OrderItemJsonDeserializer() {
                    @Override
                    public OrderItem deserialize(JsonReader jsonParser) throws IOException {
                        switch (jsonParser.getString("val")) {
                            case "item1":
                                return item1;
                            case "item2":
                                return item2;
                            default:
                                fail();
                                return null;
                        }
                    }
                }
        );

    }

    @Test
    public void testDeserialize() throws Exception {
        final CartResponse actual = JsonTestUtil.deserialize(
                deserializer,
                "{'cart': { 'deliveryCurrency': 'USD', 'items': [{'val': 'item1'}, {'val': 'item2'}], 'deliveryOptions': [{'val': 'delivery1'}, {'val': 'delivery2'}]," +
                        "'paymentMethods': ['CASH_ON_DELIVERY', 'YANDEX_MONEY']}}"
        );

        assertEquals(Currency.USD, actual.getDeliveryCurrency());
        assertEquals(Arrays.asList(item1, item2), actual.getItems());
        assertEquals(Arrays.asList(delivery1, delivery2), actual.getDeliveryOptions());
        assertEquals(Arrays.asList(CASH_ON_DELIVERY, YANDEX_MONEY), actual.getPaymentMethods());
    }

    @Test
    public void testDeserializeEmpty() throws Exception {
        JsonTestUtil.deserialize(
                deserializer,
                "{'cart': {}}"
        );

    }
}
