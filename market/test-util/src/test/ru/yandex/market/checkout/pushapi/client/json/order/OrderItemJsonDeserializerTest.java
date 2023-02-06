package ru.yandex.market.checkout.pushapi.client.json.order;

import org.junit.Test;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OrderItemJsonDeserializerTest {
    
    private OrderItemJsonDeserializer deserializer = new OrderItemJsonDeserializer();

    @Test
    public void testDeserializeCartRequest() throws Exception {
        final OrderItem actual = JsonTestUtil.deserialize(
            deserializer,
            "{'feedId': 1234," +
                "'offerId': '2345'," +
                "'feedCategoryId': '3456'," +
                "'offerName': 'OfferName'," +
                "'count': 5," +
                "'delivery': false}"
        );
        
        assertEquals(1234, actual.getFeedId().longValue());
        assertEquals("2345", actual.getOfferId());
        assertEquals("3456", actual.getFeedCategoryId());
        assertEquals("OfferName", actual.getOfferName());
        assertEquals(5, actual.getCount().intValue());
        assertEquals(false, actual.getDelivery());
    }

    @Test
    public void testDeserializeCartResponse() throws Exception {
        final OrderItem actual = JsonTestUtil.deserialize(
            deserializer,
            "{'feedId': 1234," +
                "'offerId': '2345'," +
                "'price': 3456," +
                "'count': 5}"
        );

        assertEquals(1234, actual.getFeedId().longValue());
        assertEquals("2345", actual.getOfferId());
        assertEquals(new BigDecimal(3456), actual.getPrice());
        assertEquals(5, actual.getCount().intValue());
    }

    @Test
    public void testDeserializeAcceptRequest() throws Exception {
        final OrderItem actual = JsonTestUtil.deserialize(
            deserializer,
            "{'feedId': 1234," +
                "'offerId': '2345'," +
                "'feedCategoryId': '3456'," +
                "'offerName': 'OfferName'," +
                "'price': 3456," +
                "'count': 5}"
        );

        assertEquals(1234, actual.getFeedId().longValue());
        assertEquals("2345", actual.getOfferId());
        assertEquals("3456", actual.getFeedCategoryId());
        assertEquals("OfferName", actual.getOfferName());
        assertEquals(new BigDecimal(3456), actual.getPrice());
        assertEquals(5, actual.getCount().intValue());
    }

    @Test
    public void testDeserializeEmpty() throws Exception {
        final OrderItem actual = JsonTestUtil.deserialize(
            deserializer,
            "{}"
        );

        assertNotNull(actual);
    }
}
