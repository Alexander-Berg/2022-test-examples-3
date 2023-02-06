package ru.yandex.market.logistics.datacamp.client;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistics.datacamp.client.configuration.DataCampTestConfiguration;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;

@ExtendWith({
    SpringExtension.class,
    SoftAssertionsExtension.class,
})
@SpringBootTest(classes = DataCampTestConfiguration.class)
public class AbstractDataCampClientTest {

    @Autowired
    protected DataCampClient dataCampClient;

    @Autowired
    protected HttpTemplate dataCampHttpClient;

    @InjectSoftAssertions
    protected SoftAssertions softly;

    protected MockRestServiceServer mock;

    @Value("${data-camp.url}")
    protected String url;

    @BeforeEach
    public void setUp() {
        mock = MockRestServiceServer.createServer(((HttpTemplateImpl) dataCampHttpClient).getRestTemplate());
    }

    @AfterEach
    public void tearDown() {
        mock.verify();
    }
}
