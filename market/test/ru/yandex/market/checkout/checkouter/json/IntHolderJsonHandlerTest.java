package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IntHolderJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws Exception {
        String json = "{ \"value\" : 135135 } ";

        IntHolder holder = read(IntHolder.class, json);

        Assertions.assertEquals(135135, holder.getValue());
    }

    @Test
    public void serialize() throws Exception {
        IntHolder holder = new IntHolder(135135);

        String json = write(holder);
        checkJson(json, "$.value", 135135);
    }

}
