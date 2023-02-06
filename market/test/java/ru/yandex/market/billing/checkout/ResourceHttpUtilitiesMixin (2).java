package ru.yandex.market.billing.checkout;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.spring.MockClientHttpRequestFactory;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public interface ResourceHttpUtilitiesMixin extends ResourceUtilities {

    default void mockClientWithResource(String filename) throws IOException {
        getRestTemplate().setRequestFactory(
                new MockClientHttpRequestFactory(
                        new InputStreamResource(getResourceAsInputStream(filename))
                ));
    }

    default void mockAnswerForAnyRequest(String filePath, int expectedCount) throws IOException {
        MockRestServiceServer.createServer(getRestTemplate())
                .expect(ExpectedCount.times(expectedCount), method(HttpMethod.GET))
                .andExpect(anything())
                .andRespond(
                        withSuccess()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(resourceAsString(filePath))
                );
    }

    default void mockAnswerForAnyRequest(String filePath) throws IOException {
        mockAnswerForAnyRequest(filePath, 1);
    }

    private String resourceAsString(String path) throws IOException {
        try (InputStream resource = getClass().getResourceAsStream(getResourcePrefix() + path)) {
            return IOUtils.toString(resource, StandardCharsets.UTF_8);
        }
    }

    RestTemplate getRestTemplate();
}
