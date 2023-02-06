package ru.yandex.market.tpl.internal.client;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.tpl.internal.client.configuration.TestConfiguration;

import static java.lang.ClassLoader.getSystemResourceAsStream;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfiguration.class)
@TestPropertySource("classpath:integration-test.properties")
abstract class AbstractTest {

    @RegisterExtension
    protected JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @Autowired
    protected TplInternalClient tplInternalClient;

    protected MockRestServiceServer mock;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${tpl-internal.api.url}")
    private String host;

    @BeforeEach
    public void setUp() {
        mock = MockRestServiceServer.createServer(restTemplate);
    }

    @AfterEach
    public void tearDown() {
        mock.verify();
    }

    @SneakyThrows
    protected String resourceContent(String path) {
        InputStream resourceStream = Objects.requireNonNull(getSystemResourceAsStream(path));
        return IOUtils.toString(resourceStream, StandardCharsets.UTF_8);
    }
}
