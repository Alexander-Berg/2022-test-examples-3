package ru.yandex.market.checkout.pushapi.out.shopApi.xml;

import java.math.BigDecimal;
import java.util.Arrays;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.ItemParameterBuilder;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemBuilder;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.pushapi.client.xml.order.ItemParameterXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.ItemPromoXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.OrderItemXmlSerializer;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrderItem;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.checkouter.order.promo.ItemPromo.createWithSubsidy;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition.marketCouponPromo;
import static ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil.sameXmlAs;
import static ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil.serialize;

public class OrderItemXmlSerializerTest {

    private OrderItemBuilder orderItemBuilder;
    private OrderItemXmlSerializer pushSerializer;
    private ShopOrderItemShopXmlSerializer shopSerializer;

    @BeforeEach
    public void setUp() {
        pushSerializer = new OrderItemXmlSerializer();
        pushSerializer.setItemParameterXmlSerializer(new ItemParameterXmlSerializer());
        pushSerializer.setPromoXmlSerializer(new ItemPromoXmlSerializer());

        shopSerializer = new ShopOrderItemShopXmlSerializer(
                new ItemPromoXmlSerializer()
        );

        orderItemBuilder = new OrderItemBuilder()
                .withId(5432L)
                .withFeedId(1234L)
                .withOfferId("2345")
                .withCategoryId(3456)
                .withFeedCategoryId("Камеры")
                .withOfferName("OfferName")
                .withCount(5)
                .withPrice(new BigDecimal(4567))
                .withSubsidy(250)
                .withDelivery(true)
                .withQuantityLimits(1, 1)
                .withVat(VatType.NO_VAT)
                .withSellerInn("1234")
                .withKind2Parameters(
                        Arrays.asList(
                                (new ItemParameterBuilder()).withName("Ширина").build(),
                                (new ItemParameterBuilder()).withName("Высота").build()
                        )
                )
                .withPromo(
                        Sets.newHashSet(
                                createWithSubsidy(marketCouponPromo(), new BigDecimal("299.9"))
                        )
                );
    }

    @Test
    public void testPushApiXmlSerializer() throws Exception {
        OrderItem item = orderItemBuilder.build();
        item.getPrices().setBuyerPriceBeforeDiscount(BigDecimal.valueOf(4569));
        String actual = serialize(pushSerializer, item);
        assertThat(
                actual,
                is(sameXmlAs("<item id='5432'" +
                        "      feed-id='1234'" +
                        "      offer-id='2345'" +
                        "      category-id='3456'" +
                        "      feed-category-id='Камеры'" +
                        "      offer-name='OfferName'" +
                        "      count='5'" +
                        "      price='4567'" +
                        "      buyer-price-before-discount='4569'" +
                        "      delivery='true'" +
                        "      subsidy='250'" +
                        "      seller-inn='1234'" +
                        "      vat='NO_VAT'>" +
                        "      <quantity-limits min='1' step='1'/>" +
                        "      <kind2Parameters>" +
                        "           <item-parameter type='number' sub-type='' name='Ширина' " +
                        "               value='20' unit='м' code=''/>" +
                        "           <item-parameter type='number' sub-type='' name='Высота' " +
                        "               value='20' unit='м' code=''/>" +
                        "      </kind2Parameters>" +
                        "    <promos> " +
                        "        <promo type='MARKET_COUPON' subsidy='299.9'/> " +
                        "    </promos> " +
                        "      </item>"))
        );
    }


    @Test
    public void testShopXmlSerializer() throws Exception {
        String lineEndingEscaped = ", ";
        String actual = serialize(shopSerializer, new ShopOrderItem() {{
            setFeedId(1234L);
            setOfferId("2345");
            setFeedCategoryId("Камеры");
            setOfferName("OfferName");
            setPrice(new BigDecimal("4567"));
            setCount(5);
            setDelivery(true);
            setId(5432L);
            setFeedId(1234L);
            setOfferId("2345");
            setCategoryId(3456);
            setFeedCategoryId("Камеры");
            setOfferName("OfferName");
            setCount(5);
            setPrice(new BigDecimal(4567));
            setBuyerPriceBeforeDiscount(new BigDecimal(4569));
            setSubsidy(new BigDecimal(250));
            setDelivery(true);
            setVat(VatType.NO_VAT);
            setKind2ParametersString("Ширина: 20 м, Высота: 20 м");
            setPromos(
                    Sets.newHashSet(
                            createWithSubsidy(marketCouponPromo(), new BigDecimal("299.9"))
                    )
            );
        }});
        assertThat(
                actual,
                is(sameXmlAs(
                        "<item id='5432'" +
                                "      feed-id='1234'" +
                                "      offer-id='2345'" +
                                "      category-id='3456'" +
                                "      feed-category-id='Камеры'" +
                                "      offer-name='OfferName'" +
                                "      price='4567'" +
                                "      buyer-price-before-discount='4569'" +
                                "      subsidy='250'" +
                                "      delivery='true'" +
                                "      vat='NO_VAT'" +
                                "      count='5'" +
                                "      params='Ширина: 20 м" + lineEndingEscaped + "Высота: 20 м'>" +
                                "      <promos> " +
                                "          <promo type='MARKET_COUPON' subsidy='299.9'/> " +
                                "      </promos> " +
                                "      </item>"))
        );
    }


    @Test
    public void testPushApiEmptyXmlSerializer() throws Exception {
        String actual = serialize(pushSerializer, new OrderItemBuilder()
                .withCount(null)
                .withDelivery(null)
                .withFeedCategoryId(null)
                .withFeedId(null)
                .withOfferId(null)
                .withOfferName(null)
                .withPrice(null)
                .build());
        assertThat(
                actual,
                is(sameXmlAs(
                        "<item subsidy='0'/>"))
        );
    }

}
