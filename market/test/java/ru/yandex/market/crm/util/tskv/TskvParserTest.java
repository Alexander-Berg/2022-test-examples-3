package ru.yandex.market.crm.util.tskv;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TskvParserTest {

    @Test
    public void testParseEqualSignInValue() {
        String tskvLine =
                "APIKey=23107\t" +
                        "EventValue={\"notification_id\":\"t=857528\"}";

        Map<String, String> res = TskvParser.parse(tskvLine);
        Assertions.assertEquals("23107", res.get("APIKey"));
        Assertions.assertEquals("{\"notification_id\":\"t=857528\"}", res.get("EventValue"));
    }
}
