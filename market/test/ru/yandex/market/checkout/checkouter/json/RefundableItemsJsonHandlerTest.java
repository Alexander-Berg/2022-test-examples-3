package ru.yandex.market.checkout.checkouter.json;

import java.util.Collections;

import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.pay.RefundableItems;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class RefundableItemsJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws Exception {
        String json = "{\"items\":[{\"wareMd5\":\"wareMd5\",\"offerName\":\"offerName\",\"categoryId\":789," +
                "\"feedCategoryId\":\"567\",\"price\":9.87,\"count\":765,\"modelId\":654," +
                "\"description\":\"description\",\"pictures\":[{\"url\":\"//avatars.mds.yandex" +
                ".net/get-marketpictesting/478213/market_cpEGvnjlGcUKC7OdzSrryw/50x50\",\"width\":12,\"height\":34," +
                "\"containerWidth\":78,\"containerHeight\":56}],\"buyerPrice\":4.32,\"refundableCount\":321," +
                "\"feedId\":123,\"offerId\":\"345\"}],\"delivery\":{\"type\":\"DELIVERY\"," +
                "\"serviceName\":\"serviceName\",\"price\":12.34,\"buyerPrice\":34.56,\"refundable\":true}," +
                "\"canRefundAmount\":true}\n";

        RefundableItems refundableItems = read(RefundableItems.class, json);

        Assertions.assertNotNull(refundableItems.getItems());
        assertThat(refundableItems.getItems(), hasSize(1));
        Assertions.assertNotNull(refundableItems.getDelivery());
        Assertions.assertTrue(refundableItems.canRefundAmount());
    }

    @Test
    public void serialize() throws Exception {
        RefundableItems refundableItems = new RefundableItems();
        refundableItems.setItems(Collections.singletonList(EntityHelper.getRefundableItem()));
        refundableItems.setCanRefundAmount(true);
        refundableItems.setDelivery(EntityHelper.getRefundableDelivery());

        String json = write(refundableItems);
        System.out.println(json);

        checkJson(json, "$." + Names.RefundableItems.ITEMS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.RefundableItems.ITEMS, hasSize(1));
        checkJson(json, "$." + Names.RefundableItems.DELIVERY, JsonPathExpectationsHelper::assertValueIsMap);
        checkJsonMatcher(json, "$." + Names.RefundableItems.DELIVERY, IsNull.notNullValue());
        checkJson(json, "$." + Names.RefundableItems.CAN_REFUND_AMOUNT, true);
    }
}
