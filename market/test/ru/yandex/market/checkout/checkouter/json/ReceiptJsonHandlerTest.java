package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class ReceiptJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws Exception {
        String json = "{\n" +
                "  \"id\": 123,\n" +
                "  \"type\": \"INCOME\",\n" +
                "  \"paymentId\": 456,\n" +
                "  \"refundId\": 789,\n" +
                "  \"status\": \"NEW\",\n" +
                "  \"createdAt\": \"21-12-5490 08:31:51\",\n" +
                "  \"updatedAt\": \"11-12-9011 14:03:42\",\n" +
                "  \"statusUpdatedAt\": \"29-11-12532 19:35:33\",\n" +
                "  \"items\":[{\n" +
                "       \"orderId\":1,\n" +
                "       \"receiptId\":123,\n" +
                "       \"itemId\":2,\n" +
                "       \"itemServiceId\":4,\n" +
                "       \"deliveryId\":3,\n" +
                "       \"itemTitle\":\"top item\",\n" +
                "       \"count\":666,\n" +
                "       \"price\":322,\n" +
                "       \"amount\":123456\n" +
                "   }]\n" +
                "}";

        Receipt receipt = read(Receipt.class, json);

        Assertions.assertEquals(123, receipt.getId());
        Assertions.assertEquals(ReceiptType.INCOME, receipt.getType());
        Assertions.assertEquals(456, receipt.getPaymentId().longValue());
        Assertions.assertEquals(789, receipt.getRefundId().longValue());
        Assertions.assertEquals(ReceiptStatus.NEW, receipt.getStatus());
        Assertions.assertEquals(EntityHelper.CREATED_AT.toInstant(), receipt.getCreatedAt());
        Assertions.assertEquals(EntityHelper.UPDATED_AT.toInstant(), receipt.getUpdatedAt());
        Assertions.assertEquals(EntityHelper.STATUS_UPDATED_AT.toInstant(), receipt.getStatusUpdatedAt());
        Assertions.assertEquals(EntityHelper.getReceiptItem(), receipt.getItems().get(0));
    }

    @Test
    public void serialize() throws Exception {
        Receipt receipt = EntityHelper.getReceipt();

        String json = write(receipt);
        checkJson(json, "$." + Names.Receipt.ID, 123);
        checkJson(json, "$." + Names.Receipt.TYPE, ReceiptType.INCOME.name());
        checkJson(json, "$." + Names.Receipt.PAYMENT_ID, 456);
        checkJson(json, "$." + Names.Receipt.REFUND_ID, 789);
        checkJson(json, "$." + Names.Receipt.STATUS, ReceiptStatus.NEW.name());
        checkJson(json, "$." + Names.Receipt.CREATED_AT, "21-12-5490 08:31:51");
        checkJson(json, "$." + Names.Receipt.UPDATED_AT, "11-12-9011 14:03:42");
        checkJson(json, "$." + Names.Receipt.STATUS_UPDATED_AT, "29-11-12532 19:35:33");
        checkJson(json, "$." + Names.Receipt.ITEMS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Receipt.ITEMS, hasSize(1));
    }

}
