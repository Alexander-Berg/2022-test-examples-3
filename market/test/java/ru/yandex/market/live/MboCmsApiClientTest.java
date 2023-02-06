package ru.yandex.market.live;

import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.mbo.MboCmsApiClient;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;

import static org.mockito.Mockito.mock;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.and;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withMethod;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withPath;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withQueryParam;

public class MboCmsApiClientTest {
    private HttpClient httpClient = mock(HttpClient.class);
    private MboCmsApiClient mboCmsApiClient = new MboCmsApiClient(
            new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)), "http://localhost12345:8080");

    @Test
    public void testCreateMigration() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/live/cms/create_migration.json",
                and(
                        withQueryParam("userId", MboCmsApiClient.USER_ID),
                        withPath("/migration/new"),
                        withMethod(HttpMethod.POST)));

        long taskId = mboCmsApiClient.createEditFieldsCmsMigration(1234, "field_location",
                "field_value", "description");
        Assertions.assertEquals(100500, taskId);
    }

    @Test
    public void testRunMigration() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/live/cms/run_migration.json",
                and(
                        withPath("/migration/runMigrationSync"),
                        withQueryParam("userId", MboCmsApiClient.USER_ID),
                        withQueryParam("id", 100500),
                        withMethod(HttpMethod.POST)));

        boolean success = mboCmsApiClient.runMigrationTaskSync(100500);
        Assertions.assertTrue(success);

    }
}
