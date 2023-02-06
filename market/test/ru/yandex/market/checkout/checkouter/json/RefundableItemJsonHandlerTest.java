package ru.yandex.market.checkout.checkouter.json;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.pay.RefundableItem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RefundableItemJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serializeInner() throws Exception {
        RefundableItem refundableItem = EntityHelper.getRefundableItem();

        String json = write(refundableItem);
        System.out.println(json);

        checkJson(json, "$." + Names.OfferItem.FEED_ID, 123);
        checkJson(json, "$." + Names.OfferItem.OFFER_ID, "345");
        checkJson(json, "$." + Names.OfferItem.WARE_MD5, "wareMd5");
        checkJson(json, "$." + Names.OfferItem.FEED_CATEGORY_ID, "567");
        checkJson(json, "$." + Names.OfferItem.CATEGORY_ID, 789);
        checkJson(json, "$." + Names.OfferItem.OFFER_NAME, "offerName");
        checkJson(json, "$." + Names.OfferItem.PRICE, 9.87);
        checkJson(json, "$." + Names.OfferItem.COUNT, 765);
        checkJson(json, "$." + Names.OrderItem.MODEL_ID, 654);
        checkJson(json, "$." + Names.OrderItem.DESCRIPTION, "description");
        checkJson(json, "$." + Names.OrderItem.PICTURES, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.OrderItem.PICTURES, hasSize(1));
        checkJson(json, "$." + Names.OrderItem.BUYER_PRICE, 4.32);
        checkJson(json, "$." + Names.RefundableItem.REFUNDABLE_COUNT, 321);
        checkJson(json, "$." + Names.OrderItem.QUANTITY, BigDecimal.valueOf(765));
        checkJson(json, "$." + Names.OrderItem.QUANT_PRICE, BigDecimal.valueOf(4.32));
        checkJson(json, "$." + Names.RefundableItem.REFUNDABLE_QUANTITY, BigDecimal.valueOf(321));
    }

    @Test
    public void deserializeInner() throws Exception {
        String json = "{\"feedId\":123,\"offerId\":\"345\",\"wareMd5\":\"wareMd5\",\"feedCategoryId\":\"567\"," +
                "\"categoryId\":789,\"offerName\":\"offerName\",\"price\":9.87,\"count\":765,\"modelId\":654," +
                "\"description\":\"description\",\"pictures\":[{\"url\":\"//avatars.mds.yandex" +
                ".net/get-marketpictesting/478213/market_cpEGvnjlGcUKC7OdzSrryw/50x50\",\"width\":12,\"height\":34," +
                "\"containerWidth\":78,\"containerHeight\":56}],\"buyerPrice\":4.32,\"refundableCount\":321," +
                "\"quantity\":765,\"quantPrice\":4.32,\"refundableQuantity\":321}";
        RefundableItem refundableItem = read(RefundableItem.class, json);

        assertEquals(123L, refundableItem.getFeedId().longValue());
        assertEquals("345", refundableItem.getOfferId());
        assertEquals("wareMd5", refundableItem.getWareMd5());
        assertEquals("567", refundableItem.getFeedCategoryId());
        assertEquals(789, refundableItem.getCategoryId().intValue());
        assertEquals("offerName", refundableItem.getOfferName());
        assertEquals(new BigDecimal("9.87"), refundableItem.getPrice());
        assertEquals(765, refundableItem.getCount().intValue());
        assertEquals(654, refundableItem.getModelId().intValue());
        assertEquals("description", refundableItem.getDescription());
        assertNotNull(refundableItem.getPictures());
        assertThat(refundableItem.getPictures(), hasSize(1));
        assertEquals(new BigDecimal("4.32"), refundableItem.getBuyerPrice());
        assertEquals(321, refundableItem.getRefundableCount());
        assertThat(refundableItem.getQuantity(), comparesEqualTo(BigDecimal.valueOf(765)));
        assertThat(refundableItem.getQuantPrice(), comparesEqualTo(BigDecimal.valueOf(4.32)));
        assertThat(refundableItem.getRefundableQuantity(), comparesEqualTo(BigDecimal.valueOf(321)));
    }
}
