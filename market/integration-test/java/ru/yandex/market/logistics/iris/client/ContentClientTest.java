package ru.yandex.market.logistics.iris.client;

import java.util.List;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.logistics.iris.client.api.ContentClient;
import ru.yandex.market.logistics.iris.client.api.ContentClientImpl;
import ru.yandex.market.logistics.iris.client.model.request.ContentSyncRequest;
import ru.yandex.market.logistics.iris.client.model.response.ContentSyncResponse;
import ru.yandex.market.logistics.iris.client.utils.TestHttpTemplateImpl;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class ContentClientTest extends AbstractClientTest {
    private final ContentClient client =
            new ContentClientImpl(new TestHttpTemplateImpl(uri, restTemplate));

    @Test
    public void onSuccessPost() {
        ResponseCreator responseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(extractFileContent("fixtures/content/sync/response_ok.json"));

        UriComponents requestUri = UriComponentsBuilder.fromHttpUrl(uri)
                .path("content")
                .pathSegment("sync")
                .build();

        mockServer.expect(requestTo(requestUri.toUriString()))
                .andExpect(content().json(extractFileContent("fixtures/content/sync/request_ok.json"),
                        false))
                .andExpect(method(HttpMethod.POST))
                .andRespond(responseCreator);


        ContentSyncRequest request = ContentSyncRequest.of(List.of(
                ItemIdentifier.of("partner_id_123", "partner_sku_123")
        ));

        ContentSyncResponse response = new ContentSyncResponse(List.of(
                new ContentSyncResponse.ContentSyncItemResult(
                        "partner_id_123", "partner_sku_123", true, true, null)
        ));

        assertEquals(response, client.postSync(request));

        mockServer.verify();
    }

}
