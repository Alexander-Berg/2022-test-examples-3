package ru.yandex.personal.mail.search.metrics.scraper.services.scraping.systems.gapi;

import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.gmail.Gmail;
import org.junit.jupiter.api.Test;

import ru.yandex.personal.mail.search.metrics.scraper.metrics.basket.BasketQuery;
import ru.yandex.personal.mail.search.metrics.scraper.mocks.GmailMockFactory;
import ru.yandex.personal.mail.search.metrics.scraper.model.mail.search.MailSearchResult;
import ru.yandex.personal.mail.search.metrics.scraper.services.archive.response.json.JsonResponseRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class GApiSearchSystemTest {
    private GmailMockFactory factory = new GmailMockFactory();

    @Test
    void search() throws Exception {
        int serpSize = new Random().nextInt(100) + 10;

        Gmail gmail = factory.mockGmail(serpSize);
        JsonResponseRepository rr = mock(JsonResponseRepository.class);
        GApiSearchSystem service =
                new GApiSearchSystem(gmail, new GApiMessageToMessageSearchSnippetConverter(), rr, new ObjectMapper());
        MailSearchResult result = service.search(new BasketQuery(""));

        assertEquals(serpSize, result.getScrapedData().getSnippets().size());
        verify(gmail.users().messages().list(anyString()).setQ(anyString()), times(1)).execute();
        verify(gmail.users().messages().get(anyString(), anyString()), times(serpSize)).execute();
    }

    @Test
    void emptySearch() throws Exception {
        int serpSize = 0;

        Gmail gmail = factory.mockGmail(serpSize);
        JsonResponseRepository rr = mock(JsonResponseRepository.class);
        GApiSearchSystem service =
                new GApiSearchSystem(gmail, new GApiMessageToMessageSearchSnippetConverter(), rr, new ObjectMapper());
        MailSearchResult result = service.search(new BasketQuery(""));

        assertEquals(0, result.getScrapedData().getSnippets().size());
        verify(gmail.users().messages().list(anyString()).setQ(anyString()), times(1)).execute();
        verify(gmail.users().messages().get(anyString(), anyString()), times(serpSize)).execute();
    }
}
