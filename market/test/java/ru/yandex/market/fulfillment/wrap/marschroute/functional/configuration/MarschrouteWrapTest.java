package ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {MarschrouteTestConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource("classpath:application.properties")
public abstract class MarschrouteWrapTest extends BaseIntegrationTest {

    @Value("${fulfillment.marschroute.api.url}")
    protected String apiUrl;

    @Value("${fulfillment.marschroute.api.key}")
    protected String apiKey;

    @Autowired
    @Qualifier("deliveryMapper")
    protected XmlMapper deliveryMapper;

    @Autowired
    @Qualifier("fulfillmentMapper")
    protected XmlMapper fulfillmentMapper;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    @Qualifier("marschrouteRestTemplate")
    protected RestTemplate restTemplate;

    protected String createMarschrouteApiUrl(String... segments) {
        return UriComponentsBuilder.fromHttpUrl(apiUrl)
                .pathSegment(apiKey)
                .pathSegment(segments)
                .toUriString();
    }
}

