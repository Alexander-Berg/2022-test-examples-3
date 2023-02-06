package ru.yandex.market.checkout.checkouter.json.items;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.AbstractJsonHandlerTestBase;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.OrderItems;
import ru.yandex.market.checkout.checkouter.order.OrderItemsDiff;
import ru.yandex.market.checkout.checkouter.order.OrderItemsHistory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class OrderItemsHistoryJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        OrderItemsHistory orderItemsHistory = new OrderItemsHistory(
                new OrderItems(Collections.singletonList(EntityHelper.getOrderItem())),
                new OrderItems(Collections.singletonList(EntityHelper.getOrderItem())),
                new OrderItemsDiff(
                        new OrderItems(Collections.singletonList(EntityHelper.getOrderItem())),
                        new OrderItems(Collections.singletonList(EntityHelper.getOrderItem())),
                        new OrderItems(Collections.singletonList(EntityHelper.getOrderItem())),
                        new OrderItems(Collections.singletonList(EntityHelper.getOrderItem()))
                )
        );

        String json = write(orderItemsHistory);
        System.out.println(json);

        checkJson(json, "$.before.items", JsonPathExpectationsHelper::assertValueIsArray);
        checkJson(json, "$.after.items", JsonPathExpectationsHelper::assertValueIsArray);
        checkJson(json, "$.diff", JsonPathExpectationsHelper::assertValueIsMap);
    }

    @Test
    public void deserialize() throws Exception {
        String json = "{\"before\":{\"items\":[{\"wareMd5\":\"456\",\"offerName\":\"offerName\",\"categoryId\":567," +
                "\"price\":67.89,\"subsidy\":5.43,\"count\":789,\"delivery\":true,\"isPromotedByVendor\":true," +
                "\"shopUrl\":\"shopUrl\",\"kind2Params\":[{\"type\":\"type\",\"subType\":\"subType\"," +
                "\"name\":\"name\",\"value\":\"value\",\"unit\":\"unit\",\"code\":\"code\"," +
                "\"specifiedForOffer\":true," +
                "\"units\":[{\"values\":[\"a\",\"b\",\"c\"],\"shopValues\":[\"d\",\"e\",\"f\"],\"unitId\":\"unitId\"," +
                "\"defaultUnit\":true}]}],\"vat\":\"VAT_18\",\"promoKey\":\"promoKey\",\"validationErrors\":" +
                "[{\"type\":\"basic\",\"code\":\"code\",\"severity\":\"ERROR\"}],\"id\":123,\"modelId\":987," +
                "\"description\":\"description\",\"pictures\":[{\"url\":" +
                "\"//avatars.mds.yandex.net/get-marketpictesting/478213/market_cpEGvnjlGcUKC7OdzSrryw/50x50\"," +
                "\"width\":12,\"height\":34,\"containerWidth\":78,\"containerHeight\":56}],\"buyerPrice\":87.6," +
                "\"buyerDiscount\":4.32,\"buyerPriceBeforeDiscount\":3.21,\"buyerSubsidy\":2.10,\"fee\":7.65," +
                "\"feeInt\":765,\"feeSum\":6.54,\"showUid\":\"showUid\",\"realShowUid\":\"realShowUid\"," +
                "\"showInfo\":\"showInfo\",\"fulfilmentShopId\":111,\"sku\":\"sku\",\"shopSku\":\"shopSku\"," +
                "\"changes\":[\"COUNT\"],\"promos\":[{\"type\":\"MARKET_COUPON\",\"subsidy\":5.43}]," +
                "\"offerId\":\"345\",\"feedId\":234}]},\"after\":{\"items\":[{\"wareMd5\":\"456\"," +
                "\"offerName\":\"offerName\",\"categoryId\":567,\"price\":67.89,\"subsidy\":5.43,\"count\":789," +
                "\"delivery\":true,\"availability\":\"ON_DEMAND\",\"isPromotedByVendor\":true," +
                "\"shopUrl\":\"shopUrl\",\"kind2Params\":[{\"type\":\"type\",\"subType\":\"subType\"," +
                "\"name\":\"name\",\"value\":\"value\",\"unit\":\"unit\",\"code\":\"code\"," +
                "\"specifiedForOffer\":true,\"units\":[{\"values\":[\"a\",\"b\",\"c\"],\"shopValues\":[\"d\",\"e\"," +
                "\"f\"],\"unitId\":\"unitId\",\"defaultUnit\":true}]}],\"vat\":\"VAT_18\",\"promoKey\":\"promoKey\"," +
                "\"validationErrors\":[{\"type\":\"basic\",\"code\":\"code\",\"severity\":\"ERROR\"}],\"id\":123," +
                "\"modelId\":987,\"description\":\"description\",\"pictures\":[{\"url\":\"//avatars.mds.yandex" +
                ".net/get-marketpictesting/478213/market_cpEGvnjlGcUKC7OdzSrryw/50x50\",\"width\":12,\"height\":34," +
                "\"containerWidth\":78,\"containerHeight\":56}],\"buyerPrice\":87.6,\"buyerDiscount\":4.32," +
                "\"buyerPriceBeforeDiscount\":3.21,\"buyerSubsidy\":2.10,\"fee\":7.65,\"feeInt\":765,\"feeSum\":6.54," +
                "\"showUid\":\"showUid\",\"realShowUid\":\"realShowUid\",\"showInfo\":\"showInfo\"," +
                "\"fulfilmentShopId\":111,\"sku\":\"sku\",\"shopSku\":\"shopSku\",\"changes\":[\"COUNT\"]," +
                "\"promos\":[{\"type\":\"MARKET_COUPON\",\"subsidy\":5.43}],\"offerId\":\"345\",\"feedId\":234}]}," +
                "\"diff\":{\"changed\":{\"items\":[{\"wareMd5\":\"456\",\"offerName\":\"offerName\"," +
                "\"categoryId\":567,\"price\":67.89,\"subsidy\":5.43,\"count\":789,\"delivery\":true," +
                "\"availability\":\"ON_DEMAND\",\"isPromotedByVendor\":true,\"shopUrl\":\"shopUrl\"," +
                "\"kind2Params\":[{\"type\":\"type\",\"subType\":\"subType\",\"name\":\"name\",\"value\":\"value\"," +
                "\"unit\":\"unit\",\"code\":\"code\",\"specifiedForOffer\":true,\"units\":[{\"values\":[\"a\",\"b\"," +
                "\"c\"],\"shopValues\":[\"d\",\"e\",\"f\"],\"unitId\":\"unitId\",\"defaultUnit\":true}]}]," +
                "\"vat\":\"VAT_18\",\"promoKey\":\"promoKey\",\"validationErrors\":[{\"type\":\"basic\"," +
                "\"code\":\"code\",\"severity\":\"ERROR\"}],\"id\":123,\"modelId\":987," +
                "\"description\":\"description\",\"pictures\":[{\"url\":\"//avatars.mds.yandex" +
                ".net/get-marketpictesting/478213/market_cpEGvnjlGcUKC7OdzSrryw/50x50\",\"width\":12,\"height\":34," +
                "\"containerWidth\":78,\"containerHeight\":56}],\"buyerPrice\":87.6,\"buyerDiscount\":4.32," +
                "\"buyerPriceBeforeDiscount\":3.21,\"buyerSubsidy\":2.10,\"fee\":7.65,\"feeInt\":765,\"feeSum\":6.54," +
                "\"showUid\":\"showUid\",\"realShowUid\":\"realShowUid\",\"showInfo\":\"showInfo\"," +
                "\"fulfilmentShopId\":111,\"sku\":\"sku\",\"shopSku\":\"shopSku\",\"changes\":[\"COUNT\"]," +
                "\"promos\":[{\"type\":\"MARKET_COUPON\",\"subsidy\":5.43}],\"offerId\":\"345\",\"feedId\":234}]}," +
                "\"deleted\":{\"items\":[{\"wareMd5\":\"456\",\"offerName\":\"offerName\",\"categoryId\":567," +
                "\"price\":67.89,\"subsidy\":5.43,\"count\":789,\"delivery\":true,\"availability\":\"ON_DEMAND\"," +
                "\"isPromotedByVendor\":true,\"shopUrl\":\"shopUrl\",\"kind2Params\":[{\"type\":\"type\"," +
                "\"subType\":\"subType\",\"name\":\"name\",\"value\":\"value\",\"unit\":\"unit\",\"code\":\"code\"," +
                "\"specifiedForOffer\":true,\"units\":[{\"values\":[\"a\",\"b\",\"c\"],\"shopValues\":[\"d\",\"e\"," +
                "\"f\"],\"unitId\":\"unitId\",\"defaultUnit\":true}]}],\"vat\":\"VAT_18\",\"promoKey\":\"promoKey\"," +
                "\"validationErrors\":[{\"type\":\"basic\",\"code\":\"code\",\"severity\":\"ERROR\"}],\"id\":123," +
                "\"modelId\":987,\"description\":\"description\",\"pictures\":[{\"url\":\"//avatars.mds.yandex" +
                ".net/get-marketpictesting/478213/market_cpEGvnjlGcUKC7OdzSrryw/50x50\",\"width\":12,\"height\":34," +
                "\"containerWidth\":78,\"containerHeight\":56}],\"buyerPrice\":87.6,\"buyerDiscount\":4.32," +
                "\"buyerPriceBeforeDiscount\":3.21,\"buyerSubsidy\":2.10,\"fee\":7.65,\"feeInt\":765,\"feeSum\":6.54," +
                "\"showUid\":\"showUid\",\"realShowUid\":\"realShowUid\",\"showInfo\":\"showInfo\"," +
                "\"fulfilmentShopId\":111,\"sku\":\"sku\",\"shopSku\":\"shopSku\",\"changes\":[\"COUNT\"]," +
                "\"promos\":[{\"type\":\"MARKET_COUPON\",\"subsidy\":5.43}],\"offerId\":\"345\",\"feedId\":234}]}," +
                "\"added\":{\"items\":[{\"wareMd5\":\"456\",\"offerName\":\"offerName\",\"categoryId\":567," +
                "\"price\":67.89,\"subsidy\":5.43,\"count\":789,\"delivery\":true,\"availability\":\"ON_DEMAND\"," +
                "\"isPromotedByVendor\":true,\"shopUrl\":\"shopUrl\",\"kind2Params\":[{\"type\":\"type\"," +
                "\"subType\":\"subType\",\"name\":\"name\",\"value\":\"value\",\"unit\":\"unit\",\"code\":\"code\"," +
                "\"specifiedForOffer\":true,\"units\":[{\"values\":[\"a\",\"b\",\"c\"],\"shopValues\":[\"d\",\"e\"," +
                "\"f\"],\"unitId\":\"unitId\",\"defaultUnit\":true}]}],\"vat\":\"VAT_18\",\"promoKey\":\"promoKey\"," +
                "\"validationErrors\":[{\"type\":\"basic\",\"code\":\"code\",\"severity\":\"ERROR\"}],\"id\":123," +
                "\"modelId\":987,\"description\":\"description\",\"pictures\":[{\"url\":\"//avatars.mds.yandex" +
                ".net/get-marketpictesting/478213/market_cpEGvnjlGcUKC7OdzSrryw/50x50\",\"width\":12,\"height\":34," +
                "\"containerWidth\":78,\"containerHeight\":56}],\"buyerPrice\":87.6,\"buyerDiscount\":4.32," +
                "\"buyerPriceBeforeDiscount\":3.21,\"buyerSubsidy\":2.10,\"fee\":7.65,\"feeInt\":765,\"feeSum\":6.54," +
                "\"showUid\":\"showUid\",\"realShowUid\":\"realShowUid\",\"showInfo\":\"showInfo\"," +
                "\"fulfilmentShopId\":111,\"sku\":\"sku\",\"shopSku\":\"shopSku\",\"changes\":[\"COUNT\"]," +
                "\"promos\":[{\"type\":\"MARKET_COUPON\",\"subsidy\":5.43}],\"offerId\":\"345\",\"feedId\":234}]}," +
                "\"unchanged\":{\"items\":[{\"wareMd5\":\"456\",\"offerName\":\"offerName\",\"categoryId\":567," +
                "\"price\":67.89,\"subsidy\":5.43,\"count\":789,\"delivery\":true,\"availability\":\"ON_DEMAND\"," +
                "\"isPromotedByVendor\":true,\"shopUrl\":\"shopUrl\",\"kind2Params\":[{\"type\":\"type\"," +
                "\"subType\":\"subType\",\"name\":\"name\",\"value\":\"value\",\"unit\":\"unit\",\"code\":\"code\"," +
                "\"specifiedForOffer\":true,\"units\":[{\"values\":[\"a\",\"b\",\"c\"],\"shopValues\":[\"d\",\"e\"," +
                "\"f\"],\"unitId\":\"unitId\",\"defaultUnit\":true}]}],\"vat\":\"VAT_18\",\"promoKey\":\"promoKey\"," +
                "\"validationErrors\":[{\"type\":\"basic\",\"code\":\"code\",\"severity\":\"ERROR\"}],\"id\":123," +
                "\"modelId\":987,\"description\":\"description\",\"pictures\":[{\"url\":\"//avatars.mds.yandex" +
                ".net/get-marketpictesting/478213/market_cpEGvnjlGcUKC7OdzSrryw/50x50\",\"width\":12,\"height\":34," +
                "\"containerWidth\":78,\"containerHeight\":56}],\"buyerPrice\":87.6,\"buyerDiscount\":4.32," +
                "\"buyerPriceBeforeDiscount\":3.21,\"buyerSubsidy\":2.10,\"fee\":7.65,\"feeInt\":765,\"feeSum\":6.54," +
                "\"showUid\":\"showUid\",\"realShowUid\":\"realShowUid\",\"showInfo\":\"showInfo\"," +
                "\"fulfilmentShopId\":111,\"sku\":\"sku\",\"shopSku\":\"shopSku\",\"changes\":[\"COUNT\"]," +
                "\"promos\":[{\"type\":\"MARKET_COUPON\",\"subsidy\":5.43}],\"offerId\":\"345\",\"feedId\":234}]}}}\n";

        OrderItemsHistory orderItemsHistory = read(OrderItemsHistory.class, json);

        assertThat(orderItemsHistory.getBefore().getContent(), hasSize(1));
        assertThat(orderItemsHistory.getAfter().getContent(), hasSize(1));
        assertThat(orderItemsHistory.getDiff().getAdded().getContent(), hasSize(1));
        assertThat(orderItemsHistory.getDiff().getChanged().getContent(), hasSize(1));
        assertThat(orderItemsHistory.getDiff().getDeleted().getContent(), hasSize(1));
        assertThat(orderItemsHistory.getDiff().getUnchanged().getContent(), hasSize(1));
    }
}
