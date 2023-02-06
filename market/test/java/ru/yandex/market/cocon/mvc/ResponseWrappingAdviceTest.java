package ru.yandex.market.cocon.mvc;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.cocon.FunctionalTest;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.spring.RestTemplateFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class ResponseWrappingAdviceTest extends FunctionalTest {

    private static final RestTemplate REST_TEMPLATE = RestTemplateFactory.createRestTemplate();

    private static ObjectMapper mapper = new ObjectMapper();

    @Test
    void defaultWrapper() throws IOException {
        String url = baseUrl() + "/cabinet/supplier/config";
        String response = FunctionalTestHelper.get(url).getBody();
        assertNotNull(mapper.readTree(response).get("result"));
    }

    @Test
    void jsonWrapper() throws IOException, URISyntaxException {
        String url = baseUrl() + "/cabinet/supplier/config";
        String response = REST_TEMPLATE.exchange(url, HttpMethod.GET,
                RequestEntity.get(new URI(url))
                        .accept(MediaType.APPLICATION_JSON)
                        .build(),
                String.class).getBody();
        assertNotNull(mapper.readTree(response).get("result"));
    }

}
