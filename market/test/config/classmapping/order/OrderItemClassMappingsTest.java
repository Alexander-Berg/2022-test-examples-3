package config.classmapping.order;

import config.classmapping.BaseClassMappingsTest;
import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.checkouter.order.ItemParameterBuilder;
import ru.yandex.market.checkout.checkouter.order.OrderItem;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OrderItemClassMappingsTest extends BaseClassMappingsTest {
    @Test
    public void testDeserializeCartRequest() throws Exception {
        final OrderItem actual = deserialize(OrderItem.class,
                "<item feed-id='1234'" +
                        "      offer-id='2345'" +
                        "      feed-category-id='3456'" +
                        "      offer-name='OfferName'" +
                        "      count='5' />"
        );

        assertEquals(1234, actual.getFeedId().longValue());
        assertEquals("2345", actual.getOfferId());
        assertEquals("3456", actual.getFeedCategoryId());
        assertEquals("OfferName", actual.getOfferName());
        assertEquals(5, actual.getCount().intValue());
    }

    @Test
    public void testDeserializeAcceptRequest() throws Exception {
        final OrderItem actual = deserialize(OrderItem.class,
                "<item feed-id='1234'" +
                        "      offer-id='2345'" +
                        "      feed-category-id='3456'" +
                        "      offer-name='OfferName'" +
                        "      price='4567'" +
                        "      count='5'" +
                        "      delivery='true'/>"
        );

        assertEquals(1234, actual.getFeedId().longValue());
        assertEquals("2345", actual.getOfferId());
        assertEquals("3456", actual.getFeedCategoryId());
        assertEquals("OfferName", actual.getOfferName());
        assertEquals(new BigDecimal(4567), actual.getPrice());
        assertEquals(5, actual.getCount().intValue());
        assertEquals(true, actual.getDelivery());
    }

    @Test
    public void testDeserializeEmpty() throws Exception {
        final OrderItem actual = deserialize(OrderItem.class,
                "<item />"
        );

        assertNotNull(actual);
    }

    @Test
    public void toStringTest() {
        OrderItem orderItem = new OrderItem();
        orderItem.setKind2Parameters(
                Arrays.asList(
                        null,
                        (new ItemParameterBuilder()).build()
                )
        );
        String string = orderItem.toString();
        assertNotNull(string);
    }
}
