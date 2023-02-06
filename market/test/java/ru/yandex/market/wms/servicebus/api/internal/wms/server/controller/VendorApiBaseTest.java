package ru.yandex.market.wms.servicebus.api.internal.wms.server.controller;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.vendor.model.schaefer.response.WcsResponse;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class VendorApiBaseTest extends IntegrationTest {

    protected static final int WITHOUT_RETRY_COUNT = 1;
    protected static final int DEFAULT_RETRY_COUNT = 3;
    protected static final int DELETE_RETRY_COUNT = 3;

    protected static MockWebServer mockBackEnd;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void setUpMockBackEnd() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start(IntegrationTest.MOCK_WEB_SERVER_PORT);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    protected void mockVendorBackEndResponse(HttpStatus httpStatus, String fileName, Class returnType, int retryCount)
            throws JsonProcessingException {
        Object response = mapper.readValue(getFileContent(fileName), returnType);
        String responseBody = mapper.writeValueAsString(response);

        IntStream.range(0, retryCount).forEach(i -> {
                mockBackEnd.enqueue(new MockResponse()
                        .setResponseCode(httpStatus.value())
                        .setBody(responseBody)
                        .addHeader("Content-Type", MediaType.APPLICATION_JSON));
        });
    }

    protected void mockVendorBackEndResponse(HttpStatus httpStatus, String response, int retryCount) {
        IntStream.range(0, retryCount).forEach(i -> mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(httpStatus.value())
                .setBody(response)
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)));
    }

    protected void mockVendorBackEndResponse(Map<HttpStatus, WcsResponse> responses) {
        responses.forEach((key, value) -> {
            try {
                mockBackEnd.enqueue(new MockResponse()
                        .setResponseCode(key.value())
                        .setBody(mapper.writeValueAsString(value))
                        .addHeader("Content-Type", MediaType.APPLICATION_JSON));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }

    protected void mockVendorBackEndEmptyResponse(HttpStatus httpStatus) {
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(httpStatus.value())
                .addHeader("Content-Type", MediaType.APPLICATION_JSON));
    }

    protected void assertVendorRequest(HttpMethod httpMethod, String urlPath, String fileName, int retryCount)
            throws InterruptedException {
        for (int i = 0; i < retryCount; i++) {
            RecordedRequest recordedRequest = mockBackEnd.takeRequest(5, TimeUnit.SECONDS);

            if (recordedRequest == null) {
                assertions.fail("Request not found");
            }

            assertSoftly(assertions -> {
                assertions.assertThat(recordedRequest).isNotNull();
                assertions.assertThat(recordedRequest.getMethod()).isEqualTo(httpMethod.name());
                assertions.assertThat(recordedRequest.getPath()).isEqualTo(urlPath);
                assertions.assertThat(recordedRequest.getHeader("Content-Type"))
                        .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
                assertions.assertThat(recordedRequest.getBody().readUtf8()).isEqualTo(getFileContent(fileName));
            });
        }
    }
}
