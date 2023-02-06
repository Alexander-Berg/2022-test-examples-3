package ru.yandex.market.delivery.trust.client;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.web.client.RestTemplate;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfiguration.class)
@TestPropertySource("classpath:integration-test.properties")
abstract class AbstractTest {

    @RegisterExtension
    protected JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @Autowired
    protected TrustClient trustClient;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${trust.api.url}")
    private String host;

    private MockRestServiceServer mock;

    @BeforeEach
    public void setUp() {
        mock = MockRestServiceServer.createServer(restTemplate);
    }

    @AfterEach
    public void tearDown() {
        mock.verify();
    }

    protected ResponseActions expectRequestTo(HttpMethod method, String path) {
        return mock.expect(requestTo(host + "/trust-payments/v2/" + path))
            .andExpect(method(method));
    }

    protected RequestMatcher serviceToken(String token) {
        return header("X-Service-Token", token);
    }

    protected RequestMatcher jsonRequest(String path) {
        return content().json(resourceContent(path), true);
    }

    @SneakyThrows
    private String resourceContent(String path) {
        InputStream resourceStream = Objects.requireNonNull(getSystemResourceAsStream(path));
        return IOUtils.toString(resourceStream, StandardCharsets.UTF_8);
    }

    protected DefaultResponseCreator jsonResponse(String path) {
        return withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(resourceContent(path));
    }

}
