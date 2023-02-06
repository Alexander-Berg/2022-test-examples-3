package ru.yandex.market.ff4shops.client;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.ff4shops.api.model.CourierDto;
import ru.yandex.market.notifier.ff4shops.client.FF4ShopsClient;
import ru.yandex.market.notifier.ff4shops.client.FF4ShopsClientImpl;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class FF4ShopsClientTest {
    protected String uri = "localhost";
    protected FF4ShopsClient client;
    protected MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        RestTemplate ff4ShopsRestTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(ff4ShopsRestTemplate);
        client = new FF4ShopsClientImpl(uri, ff4ShopsRestTemplate, new ObjectMapper());
    }

    @AfterEach
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    @DisplayName("Получение информации о курьере")
    void getRemovalPermissions() throws IOException {
        long orderId = 456;

        String body = IOUtils.toString(getClass().getResourceAsStream("/json/get_courier_info.json"),
                StandardCharsets.UTF_8);

        mockServer.expect(requestTo(startsWith(uri + "/orders/" + orderId + "/courier")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(OK)
                                .body(body)
                                .contentType(APPLICATION_JSON)
                );

        CourierDto courierInfo = client.getCourier(orderId);
        assertEquals("Иван", courierInfo.getFirstName());
        assertEquals("Иванов", courierInfo.getLastName());
        assertEquals("https://go.yandex/route/6ea161f870ba6574d3bd9bdd19e1e9d8?lang=ru", courierInfo.getUrl());
        assertEquals("+79991234567", courierInfo.getPhoneNumber());
        assertEquals("123QWEasd", courierInfo.getElectronicAcceptanceCertificateCode());
    }

}
