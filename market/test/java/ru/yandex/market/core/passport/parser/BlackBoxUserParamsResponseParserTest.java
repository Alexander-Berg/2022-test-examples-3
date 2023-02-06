package ru.yandex.market.core.passport.parser;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.passport.paser.BlackBoxUserParamsResponseParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BlackBoxUserParamsResponseParserTest {

    @Test
    void testParsing() throws IOException {
        BlackBoxUserParamsResponseParser tested = new BlackBoxUserParamsResponseParser();

        Map<String, String> result = tested.parse(getClass().getResourceAsStream("userInfo.response.xml"));

        assertNotNull(result);
        assertEquals(6, result.size());
        assertEquals("1090807280", result.get("uid"));
        assertEquals("2020-05-21 17:37:50", result.get("reg_date"));
        assertEquals("1", result.get("sex"));
        assertEquals("aleksfes-beru-3", result.get("login"));
        assertEquals("false", result.get("hosted"));
        assertEquals("Pupkin Vasily", result.get("fio"));
    }
}
