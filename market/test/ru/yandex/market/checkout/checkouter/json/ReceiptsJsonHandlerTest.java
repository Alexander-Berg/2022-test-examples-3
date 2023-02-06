package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.receipt.Receipts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class ReceiptsJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws IOException, ParseException {
        Receipts receipts = new Receipts(Collections.singletonList(EntityHelper.getReceipt()));

        String json = write(receipts);

        checkJson(json, "$." + Names.Receipt.RECEIPTS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Receipt.RECEIPTS, hasSize(1));
    }

    @Test
    public void deserialize() throws IOException {
        String json = "{\n" +
                "  \"receipts\": [\n" +
                "    {\n" +
                "      \"id\": 123,\n" +
                "      \"type\": \"INCOME\",\n" +
                "      \"paymentId\": 456,\n" +
                "      \"refundId\": 789,\n" +
                "      \"status\": \"NEW\",\n" +
                "      \"createdAt\": \"21-12-5490 08:31:51\",\n" +
                "      \"updatedAt\": \"11-12-9011 14:03:42\",\n" +
                "      \"statusUpdatedAt\": \"29-11-12532 19:35:33\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Receipts receipts = read(Receipts.class, json);

        Assertions.assertNotNull(receipts.getContent());
        assertThat(receipts.getContent(), hasSize(1));
    }
}
