package ru.yandex.market.checkout.pushapi.client.json.order;

import org.junit.Test;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;

import java.math.BigDecimal;

public class OrderItemJsonSerializerTest {
    
    private OrderItemJsonSerializer serializer = new OrderItemJsonSerializer();

    @Test
    public void testSerializeCartRequest() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new OrderItem() {{
                setFeedId(1234l);
                setOfferId("2345");
                setFeedCategoryId("Камеры");
                setOfferName("OfferName");
                setCount(5);
            }},
            "{'feedId': 1234," +
                "'offerId': '2345'," +
                "'feedCategoryId': 'Камеры'," +
                "'offerName': 'OfferName'," +
                "'count': 5}"
        );
    }

    @Test
    public void testSerializeAcceptRequest() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new OrderItem() {{
                setFeedId(1234l);
                setOfferId("2345");
                setFeedCategoryId("Камеры");
                setOfferName("OfferName");
                setPrice(new BigDecimal("4567"));
                setCount(5);
                setDelivery(false);
            }},
            "{'feedId': 1234," +
                "'offerId': '2345'," +
                "'feedCategoryId': 'Камеры'," +
                "'offerName': 'OfferName'," +
                "'price': 4567," +
                "'count': 5," +
                "'delivery': false}"
        );
    }

    @Test
    public void testSerializeEmpty() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new OrderItem(),
            "{}"
        );
    }
}
