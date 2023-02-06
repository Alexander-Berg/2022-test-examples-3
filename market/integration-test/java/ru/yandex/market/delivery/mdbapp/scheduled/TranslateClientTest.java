package ru.yandex.market.delivery.mdbapp.scheduled;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import javax.sql.DataSource;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.service.translate.TranslateService;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.TranslateCache;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.TranslateCacheRepository;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

public class TranslateClientTest extends MockContextualTest {
    private static final String BASE_URL = "http://127.0.0.1:1111";
    private static final String SERVICE = "service";

    @Autowired
    @Qualifier("translateRestTemplate")
    private RestTemplate restTemplate;
    @Autowired
    private TranslateService translateService;
    @Autowired
    private TranslateCacheRepository translateCacheRepository;
    @Autowired
    private DataSource dataSource;

    private MockRestServiceServer mockRestServiceServer;

    @Before
    public void setUp() {
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @After
    public void tearDown() {
        mockRestServiceServer.reset();
        translateCacheRepository.deleteAll();
    }

    @Test
    public void shouldUseCorrectIds() throws SQLException {
        String text = "text";
        String languageCode = "en-ru";
        String translation = "текст";

        TranslateCache translateCache = new TranslateCache();
        translateCache.setOriginalText(text);
        translateCache.setLanguageCode(languageCode);
        translateCache.setTranslatedText(translation);

        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("select setval('translate_cache_seq', 2)");
            }

        }

        TranslateCache saved = translateCacheRepository.save(translateCache);
        Assert.assertThat(saved.getId(), Matchers.greaterThan(0L));
    }

    @Test
    public void shouldSendValidTranslateRequest() {
        String text = "text";
        String languageCode = "en-ru";

        mockRestServiceServer.expect(requestTo(CoreMatchers.startsWith(BASE_URL)))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("srv", SERVICE))
            .andExpect(queryParam("text", text))
            .andExpect(queryParam("lang", languageCode))
            .andRespond(
                MockRestResponseCreators.withSuccess("{\"text\": [\"текст\"]}", MediaType.APPLICATION_JSON_UTF8));

        Optional<String> result = translateService.translate(text, languageCode);
        Assert.assertThat(result.get(), is("текст"));
    }

    @Test
    public void shouldReturnTranslationFromCache() {
        String text = "text";
        String languageCode = "en-ru";
        String translation = "текст";

        TranslateCache translateCache = new TranslateCache();
        translateCache.setOriginalText(text);
        translateCache.setLanguageCode(languageCode);
        translateCache.setTranslatedText(translation);

        translateCacheRepository.save(translateCache);

        Optional<String> translate = translateService.translate(text, languageCode);

        Assert.assertThat(translate.get(), is(translation));
    }
}
