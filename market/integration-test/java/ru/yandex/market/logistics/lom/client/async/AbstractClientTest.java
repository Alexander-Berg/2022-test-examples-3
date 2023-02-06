package ru.yandex.market.logistics.lom.client.async;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.util.client.HttpTemplate;

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
}
