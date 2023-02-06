package ru.yandex.market.checkout.checkouter.json;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.pay.RefundItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RefundItemJsonHandlerTest extends AbstractJsonHandlerTestBase {

    public static final String JSON = "{\"itemId\":111,\"feedId\":123,\"offerId\":\"456\",\"count\":1," +
            "\"isDeliveryService\":true,\"quantity\":\"1.1\"}";
    public static final String JSON2 = "{\"feedId\":123,\"offerId\":\"456\",\"count\":1}";

    @Test
    public void deserialize() throws Exception {
        RefundItem refundItem = read(RefundItem.class, JSON);

        assertEquals(111L, refundItem.getItemId().longValue());
        assertEquals(123L, refundItem.getFeedId().longValue());
        assertEquals("456", refundItem.getOfferId());
        assertEquals(1, refundItem.getCount().intValue());
        assertThat(refundItem.getQuantityIfExistsOrCount()).isEqualByComparingTo(BigDecimal.valueOf(1.1));
        assertEquals(true, refundItem.getDeliveryService());

    }

    @Test
    public void deserialize2() throws Exception {
        RefundItem refundItem = read(RefundItem.class, JSON2);

        assertEquals(false, refundItem.getDeliveryService());
    }

    @Test
    public void serialize() throws Exception {
        RefundItem refundItem = EntityHelper.getRefundItem();

        String json = write(refundItem);

        checkJson(json, "$." + Names.RefundItem.ITEM_ID, 111);
        checkJson(json, "$." + Names.RefundItem.FEED_ID, 123);
        checkJson(json, "$." + Names.RefundItem.OFFER_ID, "456");
        checkJson(json, "$." + Names.RefundItem.COUNT, 1);
        checkJson(json, "$." + Names.RefundItem.QUANTITY, "1.1");
        checkJson(json, "$." + Names.RefundItem.IS_DELIVERY_SERVICE, true);
    }

}
