package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BooleanHolderJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws Exception {
        String json = "{ \"value\": true }";
        BooleanHolder holder = read(BooleanHolder.class, json);

        Assertions.assertTrue(holder.getValue());
    }

    @Test
    public void serialize() throws Exception {
        BooleanHolder booleanHolder = new BooleanHolder(true);

        String json = write(booleanHolder);

        checkJson(json, "$.value", true);
    }
}
