package ru.yandex.market.logistics.iris.client;

import org.assertj.core.util.Lists;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static ru.yandex.market.logistics.iris.client.configuration.IrisClientModule.getConstructedMapper;

public abstract class AbstractClientTest extends BaseIntegrationTest {

    protected final String uri = "http://localhost:80";
    protected final RestTemplate restTemplate = createTestRestTemplate();
    protected final MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);

    private RestTemplate createTestRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        MappingJackson2HttpMessageConverter jsonMessageConverter = new MappingJackson2HttpMessageConverter();
        jsonMessageConverter.setObjectMapper(getConstructedMapper());
        restTemplate.setMessageConverters(Lists.newArrayList(
            jsonMessageConverter
        ));
        return restTemplate;
    }
}
