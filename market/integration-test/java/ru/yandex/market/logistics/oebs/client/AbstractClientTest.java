package ru.yandex.market.logistics.oebs.client;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;

import ru.yandex.market.logistics.oebs.client.configuration.TestClientConfiguration;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith({
    SpringExtension.class,
    SoftAssertionsExtension.class,
})
@SpringBootTest(classes = TestClientConfiguration.class)
@TestPropertySource("classpath:integration-test.properties")
public abstract class AbstractClientTest {

    @Value("${oebs.api.url}")
    protected String uri;
    @Autowired
    protected MockRestServiceServer mock;
    @InjectSoftAssertions
    SoftAssertions softly;

    @AfterEach
    public void tearDown() {
        mock.verify();
    }

    @SneakyThrows
    protected String getFileContent(String filename) {
        return IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(filename)), StandardCharsets.UTF_8);
    }

    protected void prepareMockRequest(String path, String requestFile, String responseFile) {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON)
            .body(getFileContent(responseFile));

        mock.expect(requestTo(uri + path))
            .andExpect(header("Authorization", "OAuth oebs.api.token"))
            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
            .andExpect(content().json(getFileContent(requestFile), true))
            .andRespond(taskResponseCreator);
    }
}
