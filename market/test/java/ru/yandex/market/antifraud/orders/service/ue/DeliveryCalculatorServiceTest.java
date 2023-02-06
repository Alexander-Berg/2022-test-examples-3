package ru.yandex.market.antifraud.orders.service.ue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import gumi.builders.UrlBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author dzvyagin
 */
@RunWith(SpringRunner.class)
@RestClientTest(DeliveryCalculatorService.class)
public class DeliveryCalculatorServiceTest {

    public RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private DeliveryCalculatorService deliveryCalculatorService;

    @Before
    public void init() {
        restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(Collections.singletonList(new MappingJackson2HttpMessageConverter(
                AntifraudJsonUtil.OBJECT_MAPPER)));
        mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.reset();
        deliveryCalculatorService = new DeliveryCalculatorService(restTemplate, UrlBuilder.fromString("localhost"));
    }

    @Test
    public void calculateDeliveryCost() throws Exception {
        String jsonPath = getClass().getClassLoader().getResource("delivery_calc.json").getFile();
        String json = new String(Files.readAllBytes(Path.of(jsonPath)));
        DeliveryDataDto deliveryData =
                AntifraudJsonUtil.OBJECT_MAPPER.readValue(json, DeliveryDataDto.class);
        mockServer.expect(requestTo("/forecast/delivery_cost"))
                .andExpect(header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(json))
                .andRespond(withSuccess("[196.0]", MediaType.APPLICATION_JSON_UTF8));
        Optional<BigDecimal> resultO =
                deliveryCalculatorService.calculateDeliveryCost(deliveryData.getParcels().get(0), deliveryData.getTariffs());
        mockServer.verify();
        assertThat(resultO).isPresent();
        assertThat(resultO.get()).isEqualTo(new BigDecimal("196.0"));
    }

}
