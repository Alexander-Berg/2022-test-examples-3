package ru.yandex.market.pers.qa.client.avatarnica;

import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.test.http.HttpClientMockUtils;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author bahus
 * 06.04.2020
 */
class AvatarnicaClientTest {

    private final HttpClient httpClient = mock(HttpClient.class);
    private final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));

    @Test
    void testReturns() {
        AvatarnicaClient avatarnicaClient = new AvatarnicaClient("anything", restTemplate);

        HttpClientMockUtils.mockResponseWithString(httpClient, "{}");
        AvararnicaInfoResponse falseResponse = avatarnicaClient.getInfo("some", "bad", "photo");

        assertFalse(falseResponse != null && falseResponse.isProcessed());

        HttpClientMockUtils.mockResponseWithString(httpClient,
            "{\"processed_by_computer_vision\": false, \"processing\": \"not finished\"}");
        AvararnicaInfoResponse notReadyResponse = avatarnicaClient.getInfo("some", "unprocessed", "photo");

        assertFalse(notReadyResponse.isProcessed());

        HttpClientMockUtils.mockResponseWithString(httpClient,
            "{\"processed_by_computer_vision\": true,\n" +
                "    \"processing\": \"finished\",\n" +
                "    \"NeuralNetClasses\": {\n" +
                "        \"gruesome\": 10,\n" +
                "        \"perversion\": 20,\n" +
                "        \"porno\": 30\n" +
                "    }}");
        AvararnicaInfoResponse goodResponse = avatarnicaClient.getInfo("some", "processed", "photo");

        assertTrue(goodResponse.isProcessed());
        CvResponse cvResponse = goodResponse.getCvResponse();
        assertAll(
            () -> assertEquals(10, cvResponse.getGruesome()),
            () -> assertEquals(20, cvResponse.getPerversion()),
            () -> assertEquals(30, cvResponse.getPorno())
        );
    }
}
