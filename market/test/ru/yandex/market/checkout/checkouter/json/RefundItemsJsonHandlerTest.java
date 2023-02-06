package ru.yandex.market.checkout.checkouter.json;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.pay.RefundItems;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class RefundItemsJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws Exception {
        String json = "{ \"items\": [" + RefundItemJsonHandlerTest.JSON + "] }";
        RefundItems items = read(RefundItems.class, json);
        assertThat(items.getItems(), hasSize(1));
    }

    @Test
    public void serialize() throws Exception {
        RefundItems refundItems = new RefundItems(Collections.singletonList(EntityHelper.getRefundItem()));

        String json = write(refundItems);

        checkJson(json, "$." + Names.RefundableItems.ITEMS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.RefundableItems.ITEMS, hasSize(1));
    }
}
