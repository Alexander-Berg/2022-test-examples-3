package ru.yandex.market.pers.basket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.list.BasketClientParams;
import ru.yandex.market.pers.list.model.v2.enums.ReferenceType;
import ru.yandex.market.util.FormatUtils;
import ru.yandex.passport.tvmauth.TvmClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.and;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.mockResponseWithFile;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withMethod;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withPath;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withQueryParam;

public class CollectionsClientTest {
    private final String url = "http://localhost";
    private final HttpClient httpClient = mock(HttpClient.class);
    private final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    private final Long id = 1L;
    private final String source_name = "1";
    private final String source_version = "2";
    private final String ui = "touch";
    private final CollectionsClient collectionsClient = new CollectionsClient(url, restTemplate);
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testCreateCollectionsCard() throws IOException, JSONException {
        mockResponseWithFile(
                httpClient,
                HttpStatus.SC_OK,
                "/data/collections_card_creation_response.json",
                and(
                    withMethod(HttpMethod.POST),
                    withPath("/api/v1.0/cards"),
                    withQueryParam("user_uid", id),
                    withQueryParam("source_name", source_name),
                    withQueryParam("save_to_market", 0)
                ));

        String json = IOUtils.toString(
                CollectionsClientTest.class
                        .getResourceAsStream("/data/collections_request_create.json"), StandardCharsets.UTF_8
        );
        BasketCollectionsRequestDto requestDto = FormatUtils.fromJson(json, BasketCollectionsRequestDto.class);
        BasketCollectionsDtoResponse response = collectionsClient.createCollectionsCard(id, requestDto, source_name);

        assertNotNull(response);

        String expected = IOUtils.toString(
                CollectionsClientTest.class.getResourceAsStream("/data/collections_card_creation_response.json")
                , StandardCharsets.UTF_8);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        String actual = mapper.writeValueAsString(response);

        JSONAssert.assertEquals(expected, actual, new CustomComparator(JSONCompareMode.NON_EXTENSIBLE));
    }

    @Test
    public void testDeleteCard() throws IOException, JSONException {
        final var referenceType =  ReferenceType.PRODUCT;
        final var referenceId = "111";
        mockResponseWithFile(
                httpClient,
                HttpStatus.SC_OK,
                "/data/collections_card_delete_response.json",
                and(
                    withMethod(HttpMethod.DELETE),
                    withPath("/api/v1.0/cards"),
                    withQueryParam("type", "by_market_id"),
                    withQueryParam(BasketClientParams.REFERENCE_TYPE, referenceType.getName().toUpperCase()),
                    withQueryParam(BasketClientParams.REFERENCE_ID, referenceId),
                    withQueryParam("user_uid", id),
                    withQueryParam("source_name", source_name)
                ));
        var response = collectionsClient.deleteCardFromCollections(
                referenceType, referenceId, id, source_name);
        assertEquals(response.length, 1);
        String expected = IOUtils.toString(
                CollectionsClientTest.class.getResourceAsStream("/data/collections_card_creation_response.json")
                , StandardCharsets.UTF_8);

        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        assertNotNull(response);
        JSONAssert.assertEquals(expected, mapper.writeValueAsString(response[0]), JSONCompareMode.NON_EXTENSIBLE);
    }
}
