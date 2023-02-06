package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.text.ParseException;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;

public class ReceiptItemJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void testSerialize() throws IOException, ParseException {
        ReceiptItem receiptItem = new ReceiptItem();

        String json = write(receiptItem);

        checkJson(json, "$.deliveryId", (Object) null);
        checkJson(json, "$.itemId", (Object) null);
        checkJson(json, "$.itemTitle", (Object) null);
        checkJson(json, "$.price", (Object) null);
        checkJson(json, "$.amount", (Object) null);
    }
}
