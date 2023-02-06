package ru.yandex.market.logistic.gateway.client;

import java.io.IOException;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.client.config.ClientTestConfig;
import ru.yandex.market.logistic.gateway.common.model.payload.ObjectMapperFactory;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;

import static java.lang.ClassLoader.getSystemResourceAsStream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    ClientTestConfig.class,
})
@TestPropertySource("classpath:application-integration-test.properties")
public abstract class AbstractRestTest {

    protected final ObjectMapper objectMapper = ObjectMapperFactory.createInternalObjectMapper();

    protected final SoftAssertions assertions = new SoftAssertions();

    @Value("${lgw.api.host}")
    protected String uri;

    @Autowired
    protected HttpTemplate httpTemplate;

    protected MockRestServiceServer mock;

    @Before
    public void setUp() {
        mock = MockRestServiceServer.createServer(((HttpTemplateImpl) httpTemplate).getRestTemplate());
    }

    @After
    public void tearDown() {
        mock.verify();
        assertions.assertAll();
    }

    protected String getFileContent(String filename) throws IOException {
        return IOUtils.toString(getSystemResourceAsStream(filename),
            Charset.forName("UTF-8"));
    }
}
