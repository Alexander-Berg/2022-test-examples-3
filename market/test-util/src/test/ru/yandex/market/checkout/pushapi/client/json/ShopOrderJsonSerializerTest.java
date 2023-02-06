package ru.yandex.market.checkout.pushapi.client.json;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.*;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.common.json.JsonWriter;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.client.json.order.OrderItemJsonSerializer;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.checkout.pushapi.client.util.test.PushApiTestHelper.createLongDate;
import static ru.yandex.market.checkout.pushapi.client.util.test.PushApiTestHelper.createOrderItem;

public class ShopOrderJsonSerializerTest {
    
    private OrderItem item1 = createOrderItem("item1", 1l);
    private OrderItem item2 = createOrderItem("item2", 1l);
    private DeliveryWithRegion delivery = mock(DeliveryWithRegion.class);
    
    private ShopOrderJsonSerializer serializer = new ShopOrderJsonSerializer();
    
    @Before
    public void setUp() throws Exception {
        serializer.setItemJsonSerializer(
            new OrderItemJsonSerializer() {
                @Override
                public void serialize(OrderItem value, JsonWriter generator) throws IOException {
                    if(value == item1) {
                        generator.setValue("item1");
                    } else if(value == item2) {
                        generator.setValue("item2");
                    } else {
                        fail();
                    }
                }
            }
        );
        serializer.setDeliveryWithRegionJsonSerializer(
            new DeliveryWithRegionJsonSerializer() {
                @Override
                public void serialize(DeliveryWithRegion value, JsonWriter writer) throws IOException {
                    assertEquals(delivery, value);
                    writer.setValue("delivery");
                }
            }
        );

    }

    @Test
    public void testSerializePostpaid() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new ShopOrder() {{
                setId(1234l);
                setCurrency(Currency.RUR);
                setPaymentType(PaymentType.POSTPAID);
                setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
                setItems(
                    Arrays.asList(
                        item1,
                        item2
                    )
                );
                setDeliveryWithRegion(delivery);
                setNotes("notes-notes-notes");
                setFake(true);
            }},
            "{'order': {" +
                "   'id': 1234," +
                "   'fake': true," +
                "   'currency': 'RUR'," +
                "   'paymentType': 'POSTPAID'," +
                "   'paymentMethod': 'CARD_ON_DELIVERY'," +
                "   'items': ['item1', 'item2']," +
                "   'delivery': 'delivery'," +
                "   'notes': 'notes-notes-notes'" +
                "}}"
        );

    }

    @Test
    public void testSerializePrepaid() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new ShopOrder() {{
                setId(1234l);
                setCurrency(Currency.RUR);
                setPaymentType(PaymentType.PREPAID);
                setItems(
                    Arrays.asList(
                        item1,
                        item2
                    )
                );
                setDeliveryWithRegion(delivery);
                setNotes("notes-notes-notes");
            }},
            "{'order': {" +
                "   'id': 1234," +
                "   'currency': 'RUR'," +
                "   'paymentType': 'PREPAID'," +
                "   'items': ['item1', 'item2']," +
                "   'delivery': 'delivery'," +
                "   'notes': 'notes-notes-notes'" +
                "}}"
        );
    }

    @Test
    public void testSerializeEmpty() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new ShopOrder(),
            "{'order': {}}"
        );
    }

    @Test
    public void testSerializeWithSubstatus() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new ShopOrder() {{
                setId(1234l);
                setStatus(OrderStatus.CANCELLED);
                setSubstatus(OrderSubstatus.SHOP_FAILED);
                setCreationDate(createLongDate("2013-07-06 15:30:40"));
                setCurrency(Currency.RUR);
                setItemsTotal(new BigDecimal("10.53"));
                setTotal(new BigDecimal("11.47"));
                setPaymentType(PaymentType.PREPAID);
                setPaymentMethod(PaymentMethod.SHOP_PREPAID);
                setFake(true);
                setDeliveryWithRegion(delivery);
                setBuyer(new Buyer() {{
                    setId("blah");
                    setLastName("Сидоров");
                    setFirstName("Петя");
                    setMiddleName("Иваныч");
                    setPhone("123456789");
                    setEmail("petya@localhost");
                }});
            }},
            "{'order': {" +
                "   'id': 1234," +
                "   'status': 'CANCELLED'," +
                "   'substatus': 'SHOP_FAILED'," +
                "   'creationDate': '06-07-2013 15:30:40'," +
                "   'currency': 'RUR'," +
                "   'itemsTotal': 10.53," +
                "   'total': 11.47," +
                "   'paymentType': 'PREPAID'," +
                "   'paymentMethod': 'SHOP_PREPAID'," +
                "   'fake': true," +
                "   'delivery': 'delivery'," +
                "   'buyer': {" +
                "       'id': 'blah'," +
                "       'lastName': 'Сидоров'," +
                "       'firstName': 'Петя'," +
                "       'middleName': 'Иваныч'," +
                "       'phone': '123456789'," +
                "       'email': 'petya@localhost'" +
                "   }" +
                "}}"
        );
    }

    @Test
    public void testSerializeWithoutSubstatus() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new ShopOrder() {{
                setStatus(OrderStatus.DELIVERED);
            }},
            "{'order': {'status': 'DELIVERED'}}"
        );
    }
}
