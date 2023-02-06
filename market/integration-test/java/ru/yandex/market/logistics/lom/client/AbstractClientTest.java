package ru.yandex.market.logistics.lom.client;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.lom.client.configuration.TestClientConfiguration;
import ru.yandex.market.logistics.util.client.HttpTemplate;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@ExtendWith({
    SpringExtension.class,
    SoftAssertionsExtension.class,
})
@SpringBootTest(classes = {
    TestClientConfiguration.class,
})
@TestPropertySource("classpath:integration-test.properties")
@ParametersAreNonnullByDefault
public class AbstractClientTest {
    @InjectSoftAssertions
    protected SoftAssertions softly;

    @Value("${lom.api.url}")
    protected String uri;

    @Autowired
    protected HttpTemplate httpTemplate;

    @Autowired
    protected RestTemplate clientRestTemplate;

    protected MockRestServiceServer mock;

    @BeforeEach
    public void setUp() {
        mock = MockRestServiceServer.createServer(clientRestTemplate);
    }

    @AfterEach
    public void tearDown() {
        mock.verify();
    }

    @Nonnull
    ResponseActions prepareMockRequest(HttpMethod method, String urlPath, String filePath) {
        return prepareMockRequest(method, urlPath, "request/" + filePath, "response/" + filePath);
    }

    @Nonnull
    ResponseActions prepareMockRequest(
        HttpMethod method,
        String path,
        @Nullable String requestPath,
        String responsePath
    ) {
        return prepareMockRequest(HttpStatus.OK, method, path, requestPath, responsePath);
    }

    @Nonnull
    ResponseActions prepareMockRequest(
        HttpStatus status,
        HttpMethod method,
        String path,
        @Nullable String requestPath,
        String responsePath
    ) {
        return prepareMockRequest(
            status,
            method,
            path,
            requestPath,
            responsePath,
            Map.of()
        );
    }

    @Nonnull
    ResponseActions prepareMockRequest(
        HttpStatus status,
        HttpMethod method,
        String path,
        @Nullable String requestPath,
        String responsePath,
        Map<String, String> queryParams
    ) {
        ResponseCreator responseCreator = withStatus(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(extractFileContent(responsePath));

        ResponseActions expect = mock.expect(requestTo(startsWith(uri + path)))
            .andExpect(method(method));
        if (requestPath != null) {
            expect.andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(extractFileContent(requestPath)));
        }
        queryParams.forEach((key, value) -> expect.andExpect(queryParam(key, value)));
        expect.andRespond(responseCreator);
        return expect;
    }
}
