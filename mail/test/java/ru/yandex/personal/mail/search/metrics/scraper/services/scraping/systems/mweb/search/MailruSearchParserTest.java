package ru.yandex.personal.mail.search.metrics.scraper.services.scraping.systems.mweb.search;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ru.yandex.personal.mail.search.metrics.scraper.model.mail.search.MailSearchMessageSnippet;
import ru.yandex.personal.mail.search.metrics.scraper.model.mail.search.MailSearchScrapedData;
import ru.yandex.personal.mail.search.metrics.scraper.utils.DeserializableMailSearchScrapedData;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MailruSearchParserTest {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeAll
    static void setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
    }

    @Test
    void parseHtml() throws IOException {
        URL url = Resources.getResource("serp/mailru/serp.html");
        String html = Resources.toString(url, Charset.defaultCharset());

        MailruSearchParser parser = new MailruSearchParser();
        MailSearchScrapedData parsed = parser.parse(html);

        URL expectedUrl = Resources.getResource("serp/mailru/parsed_serp.json");
        MailSearchScrapedData expected = mapper.readValue(expectedUrl, DeserializableMailSearchScrapedData.class);

        assertEquals(expected.getFoundAllAverageSize(), parsed.getFoundAllAverageSize());
        assertEquals(expected.getSnippets().size(), parsed.getSnippets().size());
        for (int i = 0; i < Math.min(expected.getSnippets().size(), parsed.getSnippets().size()); i++) {
            MailSearchMessageSnippet expectedSnippet = expected.getSnippets().get(i);
            MailSearchMessageSnippet actualSnippet = parsed.getSnippets().get(i);
            assertEquals(expectedSnippet.getSubject(), actualSnippet.getSubject());
            assertEquals(expectedSnippet.getSnippet(), actualSnippet.getSnippet());
            assertEquals(expectedSnippet.getSender(), actualSnippet.getSender());
            ZonedDateTime expectedDate = expectedSnippet.getDate().atZone(ZoneId.systemDefault());
            ZonedDateTime actualDate = actualSnippet.getDate().atZone(ZoneId.systemDefault());
            assertEquals(expectedDate.getMonth(), actualDate.getMonth());
            assertEquals(expectedDate.getDayOfMonth(), actualDate.getDayOfMonth());
            assertEquals(expectedDate.getHour(), actualDate.getHour());
            assertEquals(expectedDate.getMinute(), actualDate.getMinute());
            assertEquals(expectedDate.getSecond(), actualDate.getSecond());
        }
    }
}
