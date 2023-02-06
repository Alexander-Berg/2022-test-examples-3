package ru.yandex.personal.mail.search.metrics.scraper.services.account;

import java.io.IOException;
import java.net.URL;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountPropertiesTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializeTest() throws IOException {
        AccountProperties expected = new AccountProperties();
        expected.setHostUrl("https://mail.example.com");

        URL resourceUrl = Resources.getResource("account/account-properties.json");
        AccountProperties actual = mapper.readValue(resourceUrl, AccountProperties.class);

        assertEquals(expected, actual);
    }
}
