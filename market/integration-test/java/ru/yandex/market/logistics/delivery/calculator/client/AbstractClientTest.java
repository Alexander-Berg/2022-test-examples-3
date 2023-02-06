package ru.yandex.market.logistics.delivery.calculator.client;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistics.delivery.calculator.client.configuration.TestClientConfig;

import static java.lang.ClassLoader.getSystemResourceAsStream;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestClientConfig.class)
@TestPropertySource("classpath:integration-test.properties")
public abstract class AbstractClientTest {

    @RegisterExtension
    JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @Value("${delivery-calculator.api.url}")
    protected String uri;

    @Autowired
    protected MockRestServiceServer mock;

    @AfterEach
    public void tearDown() {
        mock.verify();
    }

    @SneakyThrows
    protected String getFileContent(String filename) {
        return IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(filename)), StandardCharsets.UTF_8);
    }
}
