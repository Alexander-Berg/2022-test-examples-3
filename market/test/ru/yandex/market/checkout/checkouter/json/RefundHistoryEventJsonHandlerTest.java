package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.event.RefundHistoryEvent;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.pay.PaymentHistoryEventType;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.pay.RefundSubstatus;

public class RefundHistoryEventJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        RefundHistoryEvent event = EntityHelper.getRefundHistoryEvent();

        String json = write(event);
        System.out.println(json);

        checkJson(json, "$." + Names.RefundHistoryEvent.ID, 123);
        checkJson(json, "$." + Names.RefundHistoryEvent.TYPE, PaymentHistoryEventType.CREATE.name());
        checkJson(json, "$." + Names.RefundHistoryEvent.AUTHOR, JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, "$." + Names.RefundHistoryEvent.STATUS, RefundStatus.SUCCESS.name());
        checkJson(json, "$." + Names.RefundHistoryEvent.SUBSTATUS, RefundSubstatus.REFUND_EXPIRED.name());
        checkJson(json, "$." + Names.RefundHistoryEvent.UPDATE_DATE, "21-12-5490 08:31:51");
        checkJson(json, "$." + Names.RefundHistoryEvent.EXPIRY_DATE, "11-12-9011 14:03:42");
        checkJson(json, "$." + Names.RefundHistoryEvent.REFUND, JsonPathExpectationsHelper::assertValueIsMap);
    }

    @Test
    public void deserialize() throws Exception {
        String json = "{\n" +
                "  \"id\": 123,\n" +
                "  \"type\": \"CREATE\",\n" +
                "  \"author\": {\n" +
                "    \"role\": \"USER\",\n" +
                "    \"id\": 345,\n" +
                "    \"uid\": 345\n" +
                "  },\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"substatus\": \"REFUND_EXPIRED\",\n" +
                "  \"updateDate\": \"21-12-5490 08:31:51\",\n" +
                "  \"expiryDate\": \"11-12-9011 14:03:42\",\n" +
                "  \"refund\": {\n" +
                "    \"id\": 123,\n" +
                "    \"orderId\": 456,\n" +
                "    \"paymentId\": 789,\n" +
                "    \"trustRefundId\": \"trustRefundId\",\n" +
                "    \"hasReceipt\": true,\n" +
                "    \"currency\": \"RUR\",\n" +
                "    \"amount\": 12.34,\n" +
                "    \"orderRemainder\": 56.78,\n" +
                "    \"comment\": \"comment\",\n" +
                "    \"status\": \"SUCCESS\",\n" +
                "    \"substatus\": \"REFUND_FAILED\",\n" +
                "    \"createdBy\": 987,\n" +
                "    \"createdByRole\": \"SHOP\",\n" +
                "    \"shopManagerId\": 654,\n" +
                "    \"creationDate\": \"21-12-5490 08:31:51\",\n" +
                "    \"updateDate\": \"11-12-9011 14:03:42\",\n" +
                "    \"statusUpdateDate\": \"17-02-107599 00:55:33\",\n" +
                "    \"statusExpiryDate\": \"02-11-142808 08:14:04\",\n" +
                "    \"reason\": \"ORDER_CHANGED\",\n" +
                "    \"fake\": true\n" +
                "  }\n" +
                "}";

        RefundHistoryEvent refundHistoryEvent = read(RefundHistoryEvent.class, json);

        Assertions.assertEquals(123L, refundHistoryEvent.getId().longValue());
        Assertions.assertEquals(PaymentHistoryEventType.CREATE, refundHistoryEvent.getType());
        Assertions.assertNotNull(refundHistoryEvent.getAuthor());
        Assertions.assertEquals(RefundStatus.SUCCESS, refundHistoryEvent.getStatus());
        Assertions.assertEquals(RefundSubstatus.REFUND_EXPIRED, refundHistoryEvent.getSubstatus());
        Assertions.assertEquals(EntityHelper.UPDATE_DATE2, refundHistoryEvent.getUpdateDate());
        Assertions.assertEquals(EntityHelper.EXPIRY_DATE2, refundHistoryEvent.getExpiryDate());
        Assertions.assertNotNull(refundHistoryEvent.getRefund());
    }
}
