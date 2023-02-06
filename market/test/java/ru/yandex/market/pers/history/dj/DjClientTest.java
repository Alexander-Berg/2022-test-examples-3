package ru.yandex.market.pers.history.dj;

import org.apache.http.client.HttpClient;
import org.junit.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import ru.yandex.market.pers.history.Item;
import ru.yandex.market.pers.history.RGB;
import ru.yandex.market.pers.history.controller.UserIdType;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.pers.history.dj.HttpClientMockUtils.and;
import static ru.yandex.market.pers.history.dj.HttpClientMockUtils.mockResponseWithFile;
import static ru.yandex.market.pers.history.dj.HttpClientMockUtils.withPath;
import static ru.yandex.market.pers.history.dj.HttpClientMockUtils.withQueryParam;

public class DjClientTest {

    public static final String UID = "1";
    private final HttpClient httpClient = mock(HttpClient.class);
    private final DjClient djClient = new DjClient("http://localhost", 12345,
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)));

    @Test
    public void getWhiteHistory() {

        mockResponseWithFile(
            httpClient,
            200,
            "/data/dj/dj_white.json",
            and(
                withPath("/recommend"),
                withQueryParam("puid", UID),
                withQueryParam("experiment", "pers_history_white"))
        );

        List<Item> history = djClient.getHistory(UserIdType.UID, UID, 10, RGB.green);
        assertEquals(5, history.size());
    }

    @Test
    public void getBlueHistory() {

        mockResponseWithFile(
            httpClient,
            200,
            "/data/dj/dj_blue.json",
            and(
                withPath("/recommend"),
                withQueryParam("yandexuid", UID),
                withQueryParam("experiment", "pers_history_blue"),
                withQueryParam("max_count", 10))
        );

        List<Item> history = djClient.getHistory(UserIdType.YANDEXUID, UID, 10, RGB.blue);
        assertEquals(1, history.size());
        assertEquals(761856003L, (long) history.get(0).getResourceId());
        assertEquals(1607873744507L, history.get(0).getDate().getTime());
        assertEquals("101109366729", history.get(0).getMarketSku());
    }

    @Test
    public void getEmptyHistory() {

        mockResponseWithFile(
            httpClient,
            200,
            "/data/dj/dj_empty.json",
            and(
                withPath("/recommend"),
                withQueryParam("uuid", UID),
                withQueryParam("experiment", "pers_history_white"))
        );

        List<Item> history = djClient.getHistory(UserIdType.UUID, UID, 10, RGB.green);
        assertEquals(0, history.size());
    }
}