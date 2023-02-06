package ru.yandex.market.core.passport.parser;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.passport.model.EmailInfo;
import ru.yandex.market.core.passport.paser.EmailBlackboxResponseParser;

public class EmailBlackboxResponseParserTest {
    @Test
    public void getEmailsBlackBoxTest() throws IOException {
        EmailBlackboxResponseParser parser = new EmailBlackboxResponseParser();
        List<EmailInfo> result = parser.parse(getClass().getResourceAsStream("userEmails.response.xml"));
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("pupkin@yandex.ru", result.get(0).getEmail());
    }
}
