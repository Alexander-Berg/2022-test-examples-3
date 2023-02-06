package ru.yandex.market.checkout.checkouter.pay;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;

public class MetaDataPhoneSanitizerTest {

    @Test
    public void testPhoneSanitization() {
        Map<String, String> inputAndExpectationMap = new HashMap<String, String>() {{
            put("123", "+7123");
            put("+7234", "+7234");
            put("7123", "+7123");
            put("8123", "+7123");
            put("(8)123", "+7123");
            put(" + 7 123", "+7123");
            put("+7 (495) 644 42 99", "+74956444299");
            put("+7 495-602-96-97  ", "+74956029697");
            put("7 (495) 660-51-66 (6043)", "+749566051666043");
            put("7 (495) 660-51-66 (6043) +79151111111", null);
        }};

        inputAndExpectationMap.entrySet().forEach(stringStringEntry -> {
            String input = stringStringEntry.getKey();
            String expected = stringStringEntry.getValue();
            Assertions.assertEquals(expected, ShopMetaData.sanitizePhone(input), "Phone conversion incorrect");
        });
    }
}
