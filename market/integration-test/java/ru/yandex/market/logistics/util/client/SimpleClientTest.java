package ru.yandex.market.logistics.util.client;

import java.io.IOException;

import javax.annotation.Nullable;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;

import ru.yandex.market.logistics.test.integration.matchers.JsonMatcher;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.logistics.util.client.model.CreateTestResourceRequest;
import ru.yandex.market.logistics.util.client.model.ResourceId;
import ru.yandex.market.logistics.util.client.model.TestResourceResponse;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * @author avetokhin 2019-03-12.
 */
class SimpleClientTest extends AbstractClientTest {

    private static final String TEST_SERVICE_TICKET = "test-service-ticket";
    private static final String TEST_USER_TICKET = "test-user-ticket";

    @Autowired
    private SimpleClient simpleClient;

    @DisplayName("тест на GET запрос")
    @Test
    void get() throws IOException {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON)
            .body(getFileContent("response/resource_response.json"));
        prepareMockRequest(taskResponseCreator, "/test/get?id=10", null, APPLICATION_JSON);

        final TestResourceResponse resource = simpleClient.getResource(10L);

        verifyResource(resource);
    }

    @DisplayName("тест на POST запрос с результатом")
    @Test
    void postWithResponse() throws IOException {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON)
            .body(getFileContent("response/resource_response.json"));
        prepareMockRequest(taskResponseCreator, "/test/create", "request/resource_request.json", APPLICATION_JSON);

        final TestResourceResponse resource = simpleClient.createResource(new CreateTestResourceRequest("test name"));
        verifyResource(resource);
    }

    @DisplayName("тест на POST запрос с пустым ответом")
    @Test
    void postVoid() throws IOException {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON);
        prepareMockRequest(taskResponseCreator, "/test/sync", "request/sync_request.json", APPLICATION_JSON);

        simpleClient.syncResource(new ResourceId("yandex-id", "partner-id"));
    }

    @DisplayName("тест на POST запрос с исключением")
    @Test
    void postException() throws IOException {
        ResponseCreator taskResponseCreator = withStatus(BAD_REQUEST)
            .contentType(APPLICATION_JSON)
            .body("Some error");
        prepareMockRequest(taskResponseCreator, "/test/sync", "request/sync_request.json", APPLICATION_JSON);

        assertions.assertThatThrownBy(
            () -> simpleClient.syncResource(new ResourceId("yandex-id", "partner-id"))
        )
            .isInstanceOf(HttpTemplateException.class)
            .hasMessage("Http request exception: status <400>, response body <Some error>.");
    }

    @DisplayName("запрос управляет типом контента, если не задан HttpTemplateImpl#mediaType")
    @Test
    void requestContentType() throws IOException {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON)
            .body("[]");

        prepareMockRequest(taskResponseCreator, "/test/text", null, TEXT_PLAIN);
        simpleClient.syncText("test");
    }

    @DisplayName("запрос посылает множество значений заголовка")
    @Test
    void multiValuedHeaders() throws IOException {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON)
            .body("[]");

        MultiValuedMap<String, String> headers = prepareHeaders();
        headers.put(HttpHeaders.ACCEPT, "text/plain");
        headers.put(HttpHeaders.ACCEPT, "application/json");

        prepareMockRequest(taskResponseCreator, "/test/text2", null, TEXT_PLAIN, headers);
        simpleClient.getText();
    }

    @DisplayName("запрос с пустым путем")
    @Test
    void emptyPath() throws IOException {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(TEXT_PLAIN)
            .body("[]");

        prepareMockRequest(taskResponseCreator, "", null, null, prepareHeaders());
        simpleClient.getRoot();
    }

    private MultiValuedMap<String, String> prepareHeaders() {
        MultiValuedMap<String, String> headers = new ArrayListValuedHashMap<>();
        headers.put(HttpTemplate.SERVICE_TICKET_HEADER, TEST_SERVICE_TICKET);
        headers.put(HttpTemplate.USER_TICKET_HEADER, TEST_USER_TICKET);
        return headers;
    }

    private void prepareMockRequest(
        final ResponseCreator responseCreator,
        final String path,
        @Nullable final String requestFile,
        @Nullable final MediaType contentType
    ) throws IOException {
        prepareMockRequest(
            responseCreator,
            path,
            requestFile,
            contentType,
            prepareHeaders()
        );
    }

    private void prepareMockRequest(
        final ResponseCreator responseCreator,
        final String path,
        @Nullable final String requestFile,
        @Nullable final MediaType contentType,
        final MultiValuedMap<String, String> headers
        ) throws IOException {
        final ResponseActions responseActions = mock.expect(requestTo(uri + path));

        if (contentType != null) {
            responseActions.andExpect(content().contentType(contentType));
        } else {
            responseActions.andExpect(request ->
                assertTrue("Content type is set", request.getHeaders().getContentType() == null)
            );
        }

        headers.keys()
            .forEach(h -> {
                responseActions.andExpect(header(h, headers.get(h).toArray(new String[0])));
            });

        if (requestFile != null) {
            responseActions.andExpect(content().string(new JsonMatcher(getFileContent(requestFile))));
        }
        responseActions.andRespond(responseCreator);
    }

    private void verifyResource(final TestResourceResponse resource) {
        assertions.assertThat(resource).isNotNull();
        assertions.assertThat(resource.getName()).isEqualTo("test name");
        assertions.assertThat(resource.getResourceId()).isNotNull();
        assertions.assertThat(resource.getResourceId().getYandexId()).isEqualTo("yandex-id");
        assertions.assertThat(resource.getResourceId().getPartnerId()).isEqualTo("partner-id");
    }

}
