package ru.yandex.market.logistics.werewolf.client;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

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
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.werewolf.client.configuration.TestClientConfiguration;

@ExtendWith({
    SpringExtension.class,
    SoftAssertionsExtension.class,
})
@SpringBootTest(classes = TestClientConfiguration.class)
@TestPropertySource("classpath:integration-test.properties")
@ParametersAreNonnullByDefault
public class AbstractClientTest {
    @InjectSoftAssertions
    protected SoftAssertions softly;

    @Value("${ww.api.url}")
    protected String host;

    @Autowired
    protected HttpTemplate httpTemplate;

    @Autowired
    protected RestTemplate clientRestTemplate;

    @Autowired
    protected WwClient wwClient;

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
    ResponseActions prepareMockRequest(
        MockRequestUtils.MockRequest request
    ) {
        return MockRequestUtils.prepareRequest(request, mock, host);
    }

    // Возможно, не хватает такого метода в IntegrationTestUtils
    @Nonnull
    protected byte[] readFileIntoByteArray(String relativePath) {
        try (InputStream is = ClassLoader.getSystemResourceAsStream(relativePath)) {
            if (is == null) {
                throw new RuntimeException("Error during reading file " + relativePath);
            }
            return IOUtils.toByteArray(is);
        } catch (IOException e) {
            throw new RuntimeException("Error during reading file " + relativePath, e);
        }
    }
}
