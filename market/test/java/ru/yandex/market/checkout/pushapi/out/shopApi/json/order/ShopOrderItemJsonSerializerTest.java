package ru.yandex.market.checkout.pushapi.out.shopApi.json.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;
import ru.yandex.market.checkout.pushapi.out.shopApi.json.promo.OrderItemPromoJsonSerializer;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrderItem;

import static ru.yandex.market.checkout.checkouter.order.promo.ItemPromo.createWithSubsidy;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition.marketCoinPromo;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition.marketCouponPromo;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition.marketPromocodePromo;

class ShopOrderItemJsonSerializerTest {

    private final ShopOrderItemJsonSerializer serializer = new ShopOrderItemJsonSerializer(
            new OrderItemPromoJsonSerializer(),
            new OrderItemInstanceJsonSerializer()
    );

    @Test
    void testSerializeEmpty() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new ShopOrderItem(),
                "{'subsidy': 0}"
        );
    }

    @Test
    void testSerializeAcceptRequest() throws Exception {
        ShopOrderItem orderItem = getBuildOrderItem();
        orderItem.setDelivery(false);
        JsonTestUtil.assertJsonSerialize(
                serializer,
                orderItem,
                "{'id': 5432," +
                        "'feedId': 1234," +
                        "'offerId': '2345'," +
                        "'feedCategoryId': 'Камеры'," +
                        "'offerName': 'OfferName'," +
                        "'price': 4567," +
                        "'buyerPriceBeforeDiscount': 4569," +
                        "'subsidy': 0," +
                        "'count': 5," +
                        "'delivery': false}"
        );
    }

    @Test
    void testSerializeAcceptRequestWithPrescriptionGuids() throws Exception {
        ShopOrderItem orderItem = getBuildOrderItem();
        orderItem.setDelivery(false);
        orderItem.setPrescriptionGuids(
                Set.of("94c37fed-474f-4b30-bb17-ac4e27fb3845", "94c37fed-474f-4b30-bb17-ac4e27fb3846"));
        JsonTestUtil.assertJsonSerialize(
                serializer,
                orderItem,
                "{'id': 5432," +
                        "'feedId': 1234," +
                        "'offerId': '2345'," +
                        "'feedCategoryId': 'Камеры'," +
                        "'offerName': 'OfferName'," +
                        "'price': 4567," +
                        "'buyerPriceBeforeDiscount': 4569," +
                        "'subsidy': 0," +
                        "'count': 5," +
                        "'delivery': false," +
                        "'guids': [" +
                        "   {'guid':'94c37fed-474f-4b30-bb17-ac4e27fb3845'}," +
                        "   {'guid':'94c37fed-474f-4b30-bb17-ac4e27fb3846'}" +
                        "]}"
        );
    }

    @Test
    void testPromosSerialization() throws Exception {
        ShopOrderItem orderItem = getBuildOrderItem();
        orderItem.setSubsidy(BigDecimal.valueOf(1000));
        orderItem.getPromos().add(createWithSubsidy(marketCouponPromo(), new BigDecimal("250.78")));
        JsonTestUtil.assertJsonSerialize(
                serializer,
                orderItem,
                "{'id': 5432," +
                        "'feedId': 1234," +
                        "'offerId': '2345'," +
                        "'feedCategoryId': 'Камеры'," +
                        "'offerName': 'OfferName'," +
                        "'price': 4567," +
                        "'buyerPriceBeforeDiscount': 4569," +
                        "'subsidy': 1000," +
                        "'count': 5," +
                        "'promos': [{" +
                        "    'type':'MARKET_COUPON'," +
                        "    'subsidy':250.78" +
                        "}]}"
        );
    }

    @Test
    void testPromosPromocodeSerialization() throws Exception {
        ShopOrderItem orderItem = getBuildOrderItem();
        orderItem.setSubsidy(BigDecimal.valueOf(1000));
        orderItem.getPromos().add(createWithSubsidy(marketPromocodePromo("Promo_id", "LPromo_id", 123L),
                new BigDecimal("150.78")));
        JsonTestUtil.assertJsonSerialize(
                serializer,
                orderItem,
                "{'id': 5432," +
                        "'feedId': 1234," +
                        "'offerId': '2345'," +
                        "'feedCategoryId': 'Камеры'," +
                        "'offerName': 'OfferName'," +
                        "'price': 4567," +
                        "'buyerPriceBeforeDiscount': 4569," +
                        "'subsidy': 1000," +
                        "'count': 5," +
                        "'promos': [{" +
                        "    'type':'MARKET_PROMOCODE'," +
                        "    'subsidy':150.78," +
                        "    'marketPromoId':'Promo_id'" +
                        "}]}"
        );
    }

    @Test
    void testPromoMarketCoinSerialization() throws Exception {
        ShopOrderItem orderItem = getBuildOrderItem();
        orderItem.getPromos().add(createWithSubsidy(
                marketCoinPromo("marketPromoId", null, null, 1337L, null, null),
                new BigDecimal("250.78")
        ));

        JsonTestUtil.assertJsonSerialize(
                serializer,
                orderItem,
                "{'id': 5432," +
                        "'feedId': 1234," +
                        "'offerId': '2345'," +
                        "'feedCategoryId': 'Камеры'," +
                        "'offerName': 'OfferName'," +
                        "'price': 4567," +
                        "'buyerPriceBeforeDiscount': 4569," +
                        "'subsidy': 0," +
                        "'count': 5," +
                        "'promos': [{" +
                        "    'type':'MARKET_COIN'," +
                        "    'subsidy':250.78," +
                        "    'marketPromoId': 'marketPromoId'" +
                        "}]}"
        );
    }

    @Test
    void testInstancesSerialization() throws Exception {
        ShopOrderItem orderItem = getBuildOrderItem();
        orderItem.setInstances(List.of(
                createOrderItemInstance("cis1", "cis1Full"),
                createOrderItemInstance("cis2", "cis2Full")
        ));

        JsonTestUtil.assertJsonSerialize(
                serializer,
                orderItem,
                "{'id': 5432," +
                        "'feedId': 1234," +
                        "'offerId': '2345'," +
                        "'feedCategoryId': 'Камеры'," +
                        "'offerName': 'OfferName'," +
                        "'price': 4567," +
                        "'buyerPriceBeforeDiscount': 4569," +
                        "'subsidy': 0," +
                        "'count': 5," +
                        "'instances': [" +
                        "   {" +
                        "    'cis': 'cis1'," +
                        "    'cisFull': 'cis1Full'" +
                        "   }, " +
                        "   {" +
                        "    'cis': 'cis2'," +
                        "    'cisFull': 'cis2Full'" +
                        "   }" +
                        "]}"
        );
    }

    @Nonnull
    private OrderItemInstance createOrderItemInstance(String cis, String cisFull) {
        OrderItemInstance instance = new OrderItemInstance();
        instance.setCis(cis);
        instance.setCisFull(cisFull);
        return instance;
    }

    @Nonnull
    private ShopOrderItem getBuildOrderItem() {
        ShopOrderItem item = new ShopOrderItem();
        item.setId(5432L);
        item.setFeedId(1234L);
        item.setOfferId("2345");
        item.setFeedCategoryId("Камеры");
        item.setOfferName("OfferName");
        item.setCount(5);
        item.setPrice(BigDecimal.valueOf(4567));
        item.setBuyerPriceBeforeDiscount(BigDecimal.valueOf(4569));
        return item;
    }
}
