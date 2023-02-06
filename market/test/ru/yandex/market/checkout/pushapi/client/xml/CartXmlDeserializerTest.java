package ru.yandex.market.checkout.pushapi.client.xml;

import java.util.Arrays;
import java.util.HashMap;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartItem;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.order.CartItemXmlDeserializer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class CartXmlDeserializerTest {

    private Delivery delivery = mock(Delivery.class);
    private CartItem item1 = mock(CartItem.class);
    private CartItem item2 = mock(CartItem.class);

    private CartXmlDeserializer deserializer = new CartXmlDeserializer();

    @BeforeEach
    public void setUp() throws Exception {
        deserializer.setCartItemXmlDeserializer(
                XmlTestUtil.createDeserializerMock(
                        CartItemXmlDeserializer.class,
                        new HashMap<String, CartItem>() {{
                            put("<item>item1</item>", item1);
                            put("<item>item2</item>", item2);
                        }}
                )
        );
        deserializer.setDeliveryXmlDeserializer(
                XmlTestUtil.createDeserializerMock(
                        DeliveryXmlDeserializer.class,
                        new HashMap<String, Delivery>() {{
                            put("<delivery>delivery market-branded='false'</delivery>", delivery);
                        }}
                )
        );
    }

    @Test
    public void testParseWithRegion() throws Exception {
        final Cart actual = XmlTestUtil.deserialize(
                deserializer,
                "<cart currency='RUR'>" +
                        "        <delivery>delivery market-branded='false'</delivery>" +
                        "        <items>" +
                        "            <item>item1</item>" +
                        "            <item>item2</item>" +
                        "        </items>" +
                        "    </cart>"
        );

        assertEquals(delivery, actual.getDelivery());
        assertEquals(Arrays.asList(item1, item2), actual.getItems());
        assertEquals(Currency.RUR, actual.getCurrency());
    }

    @Test
    public void testParseEmpty() throws Exception {
        final Cart actual = XmlTestUtil.deserialize(
                deserializer,
                "<cart />"
        );

        assertNotNull(actual);
    }

    @Test
    public void testComplex() throws Exception {
        final Cart actual = XmlTestUtil.deserialize(
                deserializer,
                "<cart currency='RUR' tax-system='OSN'>" +
                        "   <delivery vat='VAT_10_110' region-id='213' market-branded='false'>" +
                        "       <shop-address street='Котиков' country='Страна' postcode='105120' city='Тест' " +
                        "subway='Шоколадное' house='01' block='592' floor='87'/>" +
                        "   </delivery>" +
                        "   <items>" +
                        "       <item feed-id='200307709' offer-id='8894802' category-id='7811912' " +
                        "feed-category-id='97' offer-name='Бюстгальтер для беременных и кормящих Medela Eva' " +
                        "vat='VAT_18' count='1'><quantity-limits min='1' step='1'/>" +
                        "           <kind2Parameters>" +
                        "               <item-parameter type='enum' sub-type='color' name='Цвет' value='белый' " +
                        "code='#FFFFFF'/><item-parameter type='enum' sub-type='size' name='Обхват под " +
                        "грудью'><units><unit-value values='60,36' shop-values='60,36' unit-id='EN' " +
                        "default-unit='false'/><unit-value values='60,65,70,75,80' unit-id='EU' " +
                        "default-unit='false'/><unit-value values='60,65,70,75,80' unit-id='INT' " +
                        "default-unit='false'/><unit-value values='60,65,70,75,80' unit-id='RU' " +
                        "default-unit='true'/></units></item-parameter>" +
                        "               <item-parameter type='enum' sub-type='size' name='Чашка'><units><unit-value " +
                        "values='AA,A,B,C,D' unit-id='EU' default-unit='false'/><unit-value values='AA,A,B,C,D' " +
                        "unit-id='INT' default-unit='false'/><unit-value values='AA,A,B,C,D' unit-id='RU' " +
                        "default-unit='true'/><unit-value values='AA,A,B,C,D' shop-values='AA,A,B,C,D' unit-id='UK' " +
                        "default-unit='false'/></units></item-parameter>" +
                        "           </kind2Parameters>" +
                        "       </item>" +
                        "   </items>" +
                        "</cart>"
        );
        assertNotNull(actual);
        assertEquals(1, actual.getItems().size());

    }

    @Test
    public void testParseHasCertificate() throws Exception {
        final Cart actual = XmlTestUtil.deserialize(
                deserializer,
                "<cart currency='RUR' hasCertificate='true'>" +
                        "</cart>"
        );

        assertTrue(actual.hasCertificate());
    }

    @Test
    public void testParseExperiments() throws Exception {
        Cart actual = XmlTestUtil.deserialize(
                deserializer,
                "<cart experiments='experiments'></cart>"
        );

        assertThat(actual.getExperiments(), CoreMatchers.equalTo("experiments"));
    }
}
