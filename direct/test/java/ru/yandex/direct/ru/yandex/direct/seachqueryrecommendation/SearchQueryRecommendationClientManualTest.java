package ru.yandex.direct.ru.yandex.direct.seachqueryrecommendation;

import java.io.IOException;
import java.util.List;

import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.searchqueryrecommendation.SearchQueryRecommendationApiUtil;
import ru.yandex.direct.searchqueryrecommendation.SearchQueryRecommendationClient;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("Тест для ручного запуска")
public class SearchQueryRecommendationClientManualTest {

    private SearchQueryRecommendationClient client;

    @Before
    public void before() {
        DefaultAsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder()
                .setConnectTimeout(30 * 1000)
                .setReadTimeout(5 * 1000)
                .setMaxConnections(100)
                .build();

        client = new SearchQueryRecommendationClient(
                "http://search-query-recom.advmachine.yandex.net",
                new ParallelFetcherFactory(new DefaultAsyncHttpClient(config), new FetcherSettings()));
    }

    @Test
    public void test() throws IOException {
        String title = "Маркетплейс Беру - большой ассортимент товаров из";
        String url = "https://beru.ru";
        String body = "Беру - маркетплейс от Яндекса, поможет вам найти и купить подходящий товар по выгодной цене " +
                "среди большого ассортимента товаров быстро и качественно!";
        List<Long> regionIds = List.of(Region.MOSCOW_REGION_ID);
        var request = SearchQueryRecommendationApiUtil.buildRequest(url, title, body, regionIds);
        var searchQueryRecommendations = client.getSearchQueryRecommendations(request);

        System.err.println(JsonUtils.toJson(searchQueryRecommendations));

        assertThat(searchQueryRecommendations).isNotNull();
        assertThat(searchQueryRecommendations.getCandidatesList()).isNotEmpty();
    }

}
