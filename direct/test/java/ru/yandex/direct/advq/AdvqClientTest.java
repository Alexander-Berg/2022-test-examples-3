package ru.yandex.direct.advq;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.advq.checkminhits.CheckMinHitsItem;
import ru.yandex.direct.advq.search.AdvqRequestKeyword;
import ru.yandex.direct.asynchttp.FetcherSettings;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class AdvqClientTest {
    private AdvqClientSettings settings;
    private AdvqClient client;

    @Before
    public void setUp() {
        settings = new AdvqClientSettings()
                .withHost("back-test.advq.yandex.ru")
                .withCheckMinHitsPath("/advq/check_min_hits")
                .withCheckMinHitsChunkSize(5)
                .withSearchPath("/advq/search")
                .withSearchChunkSize(5)
                .withFetcherSettings(new FetcherSettings());
        client = new AdvqClient(settings, new DefaultAsyncHttpClient());
    }

    @Test
    @Ignore
    public void checkImpressionsTest() throws TimeoutException, InterruptedException {
        AdvqClientSettings settings = new AdvqClientSettings()
                .withHost("back-test.advq.yandex.ru")
                .withCheckMinHitsPath("/advq/check_min_hits")
                .withCheckMinHitsChunkSize(10)
                .withFetcherSettings(new FetcherSettings());
        AdvqClient client = new AdvqClient(settings, new DefaultAsyncHttpClient());
        Map<String, Map<String, CheckMinHitsItem>> result = client.checkMinHits(
                asList("купить носки", "сапоги", "брюки клеш", "audi rs3", "формула 1 билет", "формула 1 россия"),
                asList(0L, -1L),
                asList("all", "tablet")).getResults();
        assertThat(result.size(), equalTo(2));
    }

    @Test
    @Ignore
    public void searchSmokeTest() throws TimeoutException, InterruptedException {
        Map<SearchRequest, Map<AdvqRequestKeyword, SearchKeywordResult>> results = client.search(
                asList(SearchRequest.fromPhrases(asList("купить сапоги", "купить носки"), singletonList(1L)),
                        //.withDevices(asList(Device.ALL, Device.DESKTOP)),
                        SearchRequest.fromPhrases(asList("продать сапоги", "продать носки"), singletonList(1L))
                        //.withDevices(asList(Device.ALL, Device.DESKTOP))
                ));
        assertThat(results.size(), equalTo(2));
    }

    @Test
    @Ignore
    public void searchSmokeTest_withTimeout() throws TimeoutException, InterruptedException {
        // отправляем тяжёлый для разбора запрос и устанавливаем небольшой таймаут. Ожидаем, что будет 504-ая ошибка
        StringBuilder sb = new StringBuilder("купить iphone");
        for (int i = 0; i < 10_000; i++) {
            sb.append(" -(\"android\")");
        }
        String veryLongPhrase = sb.toString();
        AdvqRequestKeyword veryLongKeyword = new AdvqRequestKeyword(veryLongPhrase);
        // Если поставить очень маленький таймаут, то ADVQ не успеет ответить за тот таймаут, что мы ждём (вдвое больший)
        FetcherSettings newFetcherSettings =
                settings.getFetcherSettings().withRequestTimeout(Duration.ofMillis(2_000L));
        AdvqClientSettings newSettings = settings.withFetcherSettings(newFetcherSettings);

        AdvqClient client = new AdvqClient(newSettings, new DefaultAsyncHttpClient());

        SearchRequest searchRequest = new SearchRequest(singletonList(veryLongKeyword), singletonList(1L));
        Map<SearchRequest, Map<AdvqRequestKeyword, SearchKeywordResult>> results =
                client.search(singletonList(searchRequest));
        assertThat(results.size(), equalTo(1));
        assertThat(results.get(searchRequest).get(veryLongKeyword).hasErrors(), equalTo(true));
        //noinspection ConstantConditions
        assertThat(results.get(searchRequest).get(veryLongKeyword).getErrors().get(0).getMessage(),
                containsString("statusCode=504"));
    }

}
