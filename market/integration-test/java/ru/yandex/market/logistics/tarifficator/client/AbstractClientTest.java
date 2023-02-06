package ru.yandex.market.logistics.tarifficator.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.tarifficator.client.configuration.TestClientConfig;
import ru.yandex.market.logistics.util.client.HttpTemplate;

import static java.lang.ClassLoader.getSystemResourceAsStream;

@ExtendWith({
    SpringExtension.class,
    SoftAssertionsExtension.class,
})
@SpringBootTest(classes = {
    TestClientConfig.class
})
@TestPropertySource("classpath:integration-test.properties")
public abstract class AbstractClientTest {

    @InjectSoftAssertions
    protected SoftAssertions softly;

    @Value("${tarifficator.api.host}")
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

    protected String getFileContent(String filename) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(filename)), StandardCharsets.UTF_8);
    }
}
