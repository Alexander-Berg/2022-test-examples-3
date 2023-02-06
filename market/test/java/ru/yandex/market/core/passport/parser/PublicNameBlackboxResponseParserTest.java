package ru.yandex.market.core.passport.parser;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.passport.paser.PublicNameBlackboxResponseParser;

import static org.junit.jupiter.api.Assertions.*;

class PublicNameBlackboxResponseParserTest {
    @Test
    public void getPublicNameParserTest() throws IOException {
        PublicNameBlackboxResponseParser parser = new PublicNameBlackboxResponseParser();
        String result = parser.parse(getClass().getResourceAsStream("userName.response.xml"));
        Assertions.assertEquals("Vasily Pupkin", result);
    }
}
