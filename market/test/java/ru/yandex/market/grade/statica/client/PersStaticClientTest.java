package ru.yandex.market.grade.statica.client;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.test.http.HttpClientMockUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.and;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withMethod;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withPath;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withQueryParam;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 25.03.2021
 */
public class PersStaticClientTest {
    private final HttpClient httpClient = mock(HttpClient.class);
    private final PersStaticClient persStaticClient = new PersStaticClient("http://localhost:1234",
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)));

    @BeforeEach
    public void reset() {
        Mockito.reset(httpClient);
    }

    @Test
    public void testLoadModelsBatched() {
        HttpClientMockUtils.mockResponse(
            httpClient,
            200,
            invocation -> new ByteArrayInputStream(
                "{\"1\":{\"count\":2},\"3\":{\"count\":4}}"
                    .getBytes()),
            and(
                withMethod(HttpMethod.GET),
                withPath("/api/opinion/count/model"),
                withQueryParam("modelId", 1)
            )
        );

        Map<Long, Long> result = persStaticClient.getModelOpinionsCountBulk(List.of(1L, 2L, 3L));
        assertEquals(Map.of(1L, 2L,3L,4L), result);
    }

}
