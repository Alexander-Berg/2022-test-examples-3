package ru.yandex.market.logistics.werewolf.client;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

/**
 * Конфигурирует {@link MockRestServiceServer} в соответствии со спецификацией из {@link MockRequest}.
 */
@UtilityClass
@ParametersAreNonnullByDefault
public class MockRequestUtils {
    @Nonnull
    public ResponseActions prepareRequest(
        MockRequest request,
        MockRestServiceServer mockRestServiceServer,
        String host
    ) {
        ResponseCreator responseCreator = withStatus(request.responseStatus)
            .contentType(request.responseContentType)
            .body(extractFileContent(request.responseContentPath));

        String path = "/" + String.join("/", request.requestPath);
        ResponseActions expect = mockRestServiceServer.expect(requestTo(startsWith(host + path)))
            .andExpect(method(request.requestMethod))
            .andExpect(content().contentType(request.requestContentType));

        if (request.requestContentPath != null) {
            expect.andExpect(content().json(extractFileContent(request.requestContentPath)));
        }

        if (!request.headers.isEmpty()) {
            request.headers.forEach(
                (name, values) -> expect.andExpect(header(name, values.toArray(String[]::new)))
            );
        }

        expect.andRespond(responseCreator);
        return expect;
    }

    @Value
    @Builder
    public static class MockRequest {
        @Builder.Default
        HttpStatus responseStatus = HttpStatus.OK;
        MediaType responseContentType;
        String responseContentPath;
        @Singular("path")
        List<String> requestPath;
        String requestContentPath;
        @Builder.Default
        MediaType requestContentType = MediaType.APPLICATION_JSON;
        HttpMethod requestMethod;
        @Singular
        Map<String, List<String>> headers;
    }
}
