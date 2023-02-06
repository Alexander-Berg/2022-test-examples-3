package ru.yandex.market.ff4shops.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.yandex.market.ff4shops.client.configuration.TestClientConfiguration;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;

@ExtendWith({
        SpringExtension.class
})
@SpringBootTest(classes = {
        TestClientConfiguration.class,
        JacksonAutoConfiguration.class,
})
@TestPropertySource("/integration-test.properties")
public class AbstractClientTest {
    @Value("${ff4shops.api.url}")
    protected String uri;

    @Autowired
    protected HttpTemplate httpTemplate;

    @Autowired
    protected FF4ShopsClient client;

    protected MockRestServiceServer mock;

    @BeforeEach
    public void setUp() {
        mock = MockRestServiceServer.createServer(((HttpTemplateImpl) httpTemplate).getRestTemplate());
    }

    @AfterEach
    public void tearDown() {
        mock.verify();
    }
}
