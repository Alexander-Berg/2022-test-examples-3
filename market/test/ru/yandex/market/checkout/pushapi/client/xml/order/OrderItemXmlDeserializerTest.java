package ru.yandex.market.checkout.pushapi.client.xml.order;

import java.math.BigDecimal;
import java.util.Arrays;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderItemXmlDeserializerTest {

    private OrderItemXmlDeserializer deserializer = new OrderItemXmlDeserializer();

    @Test
    void testDeserializeCartRequest() throws Exception {
        deserializer.setItemParameterXmlDeserializer(new ItemParameterXmlDeserializer());
        final OrderItem actual = XmlTestUtil.deserialize(
                deserializer,
                "<item feed-id='1234'" +
                        "      offer-id='2345'" +
                        "      feed-category-id='3456'" +
                        "      offer-name='OfferName'" +
                        "      params='Длина: 20 м, Длина: 20 м'" +
                        "      external-feed-id='123456'" +
                        "      at-supplier-warehouse='true'" +
                        "      count='5' >" +
                        "      <kind2Parameters>" +
                        "           <item-parameter type='number' sub-type='' name='Ширина' " +
                        "               value='20' unit='м' code=''/>" +
                        "           <item-parameter type='number' sub-type='' name='Высота' " +
                        "               value='20' unit='м' code=''/>" +
                        "      </kind2Parameters>" +
                        "      </item>"
        );

        assertEquals(1234, actual.getFeedId().longValue());
        assertEquals("2345", actual.getOfferId());
        assertEquals("3456", actual.getFeedCategoryId());
        assertEquals("OfferName", actual.getOfferName());
        assertEquals(5, actual.getCount().intValue());
        assertEquals("Длина: 20 м, Длина: 20 м", actual.getKind2ParametersString());
        assertEquals(2, actual.getKind2Parameters().size());
        assertEquals(123456L, (long) actual.getExternalFeedId());
        assertTrue(actual.getAtSupplierWarehouse());
    }

    @Test
    void testDeserializeAcceptRequest() throws Exception {
        final OrderItem actual = XmlTestUtil.deserialize(
                deserializer,
                "<item feed-id='1234'" +
                        "      offer-id='2345'" +
                        "      feed-category-id='3456'" +
                        "      offer-name='OfferName'" +
                        "      price='4567'" +
                        "      buyer-price-before-discount='4569'" +
                        "      params='Длина: 20 м, Длина: 20 м'" +
                        "      external-feed-id='123456'" +
                        "      at-supplier-warehouse='true'" +
                        "      count='5'" +
                        "      delivery='true'/>"
        );

        assertEquals(1234, actual.getFeedId().longValue());
        assertEquals("2345", actual.getOfferId());
        assertEquals("3456", actual.getFeedCategoryId());
        assertEquals("OfferName", actual.getOfferName());
        assertEquals(new BigDecimal(4567), actual.getPrice());
        assertEquals(new BigDecimal(4569), actual.getPrices().getBuyerPriceBeforeDiscount());
        assertEquals(5, actual.getCount().intValue());
        assertEquals(true, actual.getDelivery());
        assertEquals("Длина: 20 м, Длина: 20 м", actual.getKind2ParametersString());
        assertEquals(123456L, (long) actual.getExternalFeedId());
        assertTrue(actual.getAtSupplierWarehouse());
    }

    @Test
    void testDeserializeEmpty() throws Exception {
        final OrderItem actual = XmlTestUtil.deserialize(
                deserializer,
                "<item />"
        );

        assertNotNull(actual);
    }

    @Test
    void testDeserializeComplex() throws Exception {
        ItemParameterXmlDeserializer itemParameterD = new ItemParameterXmlDeserializer();
        itemParameterD.setUnitValueXmlDeserializer(new UnitValueXmlDeserializer());
        deserializer.setItemParameterXmlDeserializer(itemParameterD);
        final OrderItem actual = XmlTestUtil.deserialize(
                deserializer,
                "<item id='5432' feed-id='200307709' offer-id='8894802' category-id='7811912' feed-category-id='97' offer-name='Бюстгальтер для беременных и кормящих Medela Eva' vat='VAT_18' count='1'>" +
                        "   <quantity-limits min='1' step='1'/>" +
                        "   <kind2Parameters>" +
                        "       <item-parameter type='enum' sub-type='color' name='Цвет' value='белый' code='#FFFFFF'/>" +
                        "       <item-parameter type='enum' sub-type='size' name='Обхват под грудью'>" +
                        "           <units>" +
                        "               <unit-value values='60,36' shop-values='60,36' unit-id='EN' default-unit='false'/>" +
                        "               <unit-value values='60,65,70,75,80' unit-id='EU' default-unit='false'/>" +
                        "               <unit-value values='60,65,70,75,80' unit-id='INT' default-unit='false'/>" +
                        "               <unit-value values='60,65,70,75,80' unit-id='RU' default-unit='true'/>" +
                        "           </units>" +
                        "       </item-parameter>" +
                        "       <item-parameter type='enum' sub-type='size' name='Чашка'>" +
                        "           <units>" +
                        "               <unit-value values='AA,A,B,C,D' unit-id='EU' default-unit='false'/>" +
                        "               <unit-value values='AA,A,B,C,D' unit-id='INT' default-unit='false'/>" +
                        "               <unit-value values='AA,A,B,C,D' unit-id='RU' default-unit='true'/>" +
                        "               <unit-value values='AA,A,B,C,D' shop-values='AA,A,B,C,D' unit-id='UK' default-unit='false'/>" +
                        "           </units>" +
                        "       </item-parameter>" +
                        "   </kind2Parameters>" +
                        "</item>"
        );

        assertEquals(5432, actual.getId().longValue());
        assertEquals(200307709, actual.getFeedId().longValue());
        assertEquals(3, actual.getKind2Parameters().size());
        assertNull(actual.getKind2Parameters().get(0).getUnits());
        assertEquals(4, actual.getKind2Parameters().get(1).getUnits().size());
        assertEquals(
                Arrays.asList("60", "65", "70", "75", "80"),
                actual.getKind2Parameters().get(1).getUnits().get(1).getValues()
        );
        assertEquals(4, actual.getKind2Parameters().get(2).getUnits().size());
        assertEquals(
                Arrays.asList("AA", "A", "B", "C", "D"),
                actual.getKind2Parameters().get(2).getUnits().get(1).getValues()
        );
    }

    @Test
    void testDeserializePromo() throws Exception {
        final OrderItem actual = XmlTestUtil.deserialize(
                deserializer,
                "<item feed-id='1234'   " +
                        "              offer-id='2345'   " +
                        "              category-id='3456'   " +
                        "              feed-category-id='Камеры'   " +
                        "              offer-name='OfferName'   " +
                        "              subsidy='234'   " +
                        "              count='5'   " +
                        "              price='4567.55'   " +
                        "> " +
                        "    <promos> " +
                        "        <promo type=\"MARKET_COUPON\" subsidy=\"299.9\"/> " +
                        "    </promos> " +
                        "</item>"
        );

        assertEquals(1234, actual.getFeedId().longValue());
        assertEquals("2345", actual.getOfferId());
        assertEquals("Камеры", actual.getFeedCategoryId());
        assertEquals("OfferName", actual.getOfferName());
        assertEquals(5, actual.getCount().intValue());
        assertEquals(4567.55, actual.getPrice().doubleValue(), 0.0001);
        assertEquals(1, actual.getPromos().size());
        assertEquals(new BigDecimal("234"), actual.getPrices().getSubsidy());

        ItemPromo itemPromo1 = actual.getPromos().stream().filter(promo -> promo.getType() == PromoType.MARKET_COUPON).findAny().orElse(null);
        assertNotNull(itemPromo1);
        assertEquals("MARKET_COUPON", itemPromo1.getType().getCode());
        assertEquals(299.9, itemPromo1.getSubsidy().floatValue(), 0.001);
        assertNull(itemPromo1.getPromoDefinition().getMarketPromoId());
    }

    @Test
    void testDeserializeCoinPromo() throws Exception {
        OrderItem actual = XmlTestUtil.deserialize(
                deserializer,
                "<item><promos><promo type='MARKET_COIN' subsidy='399'/></promos></item>"
        );

        ItemPromo promo = Iterables.getOnlyElement(actual.getPromos());
        assertThat(promo.getType(), is(PromoType.MARKET_COIN));
        assertThat(promo.getSubsidy(), equalTo(new BigDecimal("399")));
    }

    @Test
    void testDeserializeWithUnknownPromo() throws Exception {
        final OrderItem actual = XmlTestUtil.deserialize(
                deserializer,
                "<item><promo type='unknown' value='100'/></item>"
        );
        assertThat(actual.getPromos(), anyOf(empty(), nullValue()));
    }

    @Test
    void testDeserializeSellerInn() throws Exception {
        OrderItem actual = XmlTestUtil.deserialize(
                deserializer,
                "<item seller-inn='1234'/>"
        );

        String sellerInn = actual.getSellerInn();
        assertThat(sellerInn, is("1234"));
    }
}
