package ru.yandex.market.crm.core.services.messages;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.services.external.ClckClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TextMessageTransformerTest {
    @Mock
    private ClckClient clckClient;

    private TextMessageTransformer textMessageTransformer;

    @Before
    public void setUp() {
        textMessageTransformer = new TextMessageTransformer(clckClient);
    }

    /**
     * Ссылки в тексте корректно сокращаются, при этом сокращенная ссылка получена от сервиса сокращателя ссылок
     */
    @Test
    public void testShorteningUrlsInText() {
        var shortUrl = "https://ya.cc/m/ABCDEF";
        when(clckClient.getShortLink(anyString())).thenReturn(shortUrl);

        assertEquals(
                "some text and url=(ya.cc/m/ABCDEF)",
                textMessageTransformer.shortTextUrls("some text and url=(http://www.yandex.ru)")
        );
        assertEquals("url: ya.cc/m/ABCDEF", textMessageTransformer.shortTextUrls("url: http://www.yandex.ru?a=123&b=abcdef"));
        assertEquals("url: ya.cc/m/ABCDEF.", textMessageTransformer.shortTextUrls("url: https://ffff.rarar.com."));
        assertEquals("ya.cc/m/ABCDEF! GOGOGO", textMessageTransformer.shortTextUrls("https://www.rarar.com! GOGOGO"));
    }

    /**
     * Все одинаковые ссылки в тексте заменяются одним сокращенным url'ом
     */
    @Test
    public void testShorteningMultipleSimilarUrlsInText() {
        var url = "http://www.yandex.ru?a=123&b=abcdef";
        var shortUrl = "ya.cc/m/ABC";
        when(clckClient.getShortLink(url)).thenReturn("https://" + shortUrl);

        var text = String.format("some text and url1=%s, url2=%s", url, url);
        var expectedText = String.format("some text and url1=%s, url2=%s", shortUrl, shortUrl);
        assertEquals(expectedText, textMessageTransformer.shortTextUrls(text));
    }

    /**
     * Если в тексте несколько различных ссылок, все они будут заменены на сокращенный вариант
     */
    @Test
    public void testShorteningMultipleDifferentUrlsInText() {
        var url1 = "http://www.yandex.ru?a=123";
        var url2 = "https://ffff.rarar.com";
        var shortUrl1 = "ya.cc/m/ABC";
        var shortUrl2 = "ya.cc/m/DEF";
        when(clckClient.getShortLink(url1)).thenReturn("https://" + shortUrl1);
        when(clckClient.getShortLink(url2)).thenReturn("http://" + shortUrl2);

        var text = String.format("some text and url1=%s, url2=%s", url1, url2);
        var expectedText = String.format("some text and url1=%s, url2=%s", shortUrl1, shortUrl2);
        assertEquals(expectedText, textMessageTransformer.shortTextUrls(text));
    }

    /**
     * При сокращении ссылок в тексте, если в нем нет ссылок, то текст не будет изменен
     */
    @Test
    public void testUrlsShorteningDontChangeTextWithoutUrls() {
        var text = "some text without url.run!";
        assertEquals(text, textMessageTransformer.shortTextUrls(text));
        assertEquals(text, textMessageTransformer.shortTextUrls("some text without url.run!"));
    }
}
