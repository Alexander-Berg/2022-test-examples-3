package ru.yandex.market.mbo.tms.alice;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.class)
public class AliceModelsServiceImplTest {
    private static final String MOCK_URL = "http://friendship.is/magic";
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private CloseableHttpResponse httpResponse;
    @Mock
    private StatusLine statusLine;
    private AliceModelsService service;

    @Before
    public void setup() {
        service = new AliceModelsServiceImpl(httpClient, MOCK_URL);
    }

    @Test
    public void testHttpErrorThrows() throws IOException {
        mockHttpResult(500, "");
        try {
            service.loadAliceModelInfos();
            fail("Should throw exception.");
        } catch (RuntimeException ex) {
            // check that exception contains URL and http code for debugging purposes
            assertThat(ex.getMessage()).contains(MOCK_URL);
            assertThat(ex.getMessage()).contains("500");
        }
    }

    @Test
    public void testOkResponseWithBrokenJsonThrows() throws IOException {
        String overmindJson = "invalid json data";
        mockHttpResult(200, overmindJson);
        try {
            service.loadAliceModelInfos();
            fail("Should throw exception.");
        } catch (RuntimeException ex) {
            // check that exception contains URL
            assertThat(ex.getMessage()).contains(MOCK_URL);
        }
    }

    @Test
    public void testEmptyJsonDoesNothing() throws IOException {
        mockHttpResult(200, "{}");
        List<AliceModelInfo> modelInfos = service.loadAliceModelInfos();
        assertThat(modelInfos).isEmpty();
    }

    @Test
    public void testEmptyProductIdsDoesNothing() throws IOException {
        mockHttpResult(200, "{\"product_ids\":[]}");
        List<AliceModelInfo> modelInfos = service.loadAliceModelInfos();
        assertThat(modelInfos).isEmpty();
    }

    @Test
    public void testOkJson() throws IOException {
        mockHttpResult(200,
            "{\n" +
                "\"product_ids\":[\n" +
                "{\"id\": 12345,\n\"yandex_smart_home_ecosystem\": true,\n\"tested_with_alice\": false},\n" +
                "{\"id\": 54321,\n\"yandex_smart_home_ecosystem\": true,\n\"tested_with_alice\": true}\n" +
                "]\n" +
                "}");

        AliceModelInfo info1 = getAliceModelInfo(12345L);
        AliceModelInfo info2 = getAliceModelInfo(54321L, true);

        List<AliceModelInfo> modelInfos = service.loadAliceModelInfos();
        assertThat(modelInfos).containsExactlyInAnyOrder(info1, info2);
    }

    @Test
    public void testOkJsonWithStringValues() throws IOException {
        mockHttpResult(200,
            "{\n" +
                "\"product_ids\":[\n" +
                "{\"id\": 12345,\n\"yandex_smart_home_ecosystem\": \"true\",\n\"tested_with_alice\": \"false\"},\n" +
                "{\"id\": 54321,\n\"yandex_smart_home_ecosystem\": \"true\",\n\"tested_with_alice\": \"true\"}\n" +
                "]\n" +
                "}");

        AliceModelInfo info1 = getAliceModelInfo(12345L);
        AliceModelInfo info2 = getAliceModelInfo(54321L, true);

        List<AliceModelInfo> modelInfos = service.loadAliceModelInfos();
        assertThat(modelInfos).containsExactlyInAnyOrder(info1, info2);
    }

    @Test
    public void testExtraFieldsIgnored() throws IOException {
        mockHttpResult(200,
            "{\n" +
                "\"product_ids\":[\n" +
                "{\"id\": 12345,\n\"yandex_smart_home_ecosystem\": true,\n\"tested_with_alice\": false},\n" +
                "{\"id\": 54321,\n\"yandex_smart_home_ecosystem\": true,\n\"tested_with_alice\": true}\n" +
                "],\n" +
                "\"some_else\":[\n" +
                "{\"id\": 12345,\n\"yandex_smart_home_ecosystem\": false,\n\"tested_with_alice\": false},\n" +
                "{\"id\": 54321,\n\"yandex_smart_home_ecosystem\": false,\n\"tested_with_alice\": true}\n" +
                "]\n" +
                "}");

        AliceModelInfo info1 = getAliceModelInfo(12345L);
        AliceModelInfo info2 = getAliceModelInfo(54321L, true);

        List<AliceModelInfo> modelInfos = service.loadAliceModelInfos();
        assertThat(modelInfos).containsExactlyInAnyOrder(info1, info2);
    }

    @Test
    public void testOrderIsPreservedWithUniqueValuesOnly() throws IOException {
        mockHttpResult(200,
            "{\n" +
                "\"product_ids\":[\n" +
                "{\"id\": 12345,\n\"yandex_smart_home_ecosystem\": true,\n\"tested_with_alice\": false},\n" +
                "{\"id\": 23456,\n\"yandex_smart_home_ecosystem\": true,\n\"tested_with_alice\": false},\n" +
                "{\"id\": 34567,\n\"yandex_smart_home_ecosystem\": true,\n\"tested_with_alice\": false},\n" +
                "{\"id\": 45678,\n\"yandex_smart_home_ecosystem\": true,\n\"tested_with_alice\": false}\n" +
                "]\n" +
                "}");

        AliceModelInfo info1 = getAliceModelInfo(12345L);
        AliceModelInfo info2 = getAliceModelInfo(23456L);
        AliceModelInfo info3 = getAliceModelInfo(34567L);
        AliceModelInfo info4 = getAliceModelInfo(45678L);

        List<AliceModelInfo> modelInfos = service.loadAliceModelInfos();
        assertThat(modelInfos).containsExactly(info1, info2, info3, info4);
    }

    private void mockHttpResult(int code, String payload) throws IOException {
        when(httpResponse.getEntity()).thenReturn(
            new StringEntity(payload, ContentType.APPLICATION_JSON.withCharset("UTF-8")));
        when(statusLine.getStatusCode()).thenReturn(code);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
    }

    private AliceModelInfo getAliceModelInfo(long id) {
        return getAliceModelInfo(id, false);
    }

    private AliceModelInfo getAliceModelInfo(long id, boolean worksWithAlice) {
        return new AliceModelInfo(id, true, worksWithAlice);
    }
}
