package ru.yandex.market.tpl.core.external.xiva;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.tpl.core.domain.push_carrier.notification.model.PushCarrierPayload;
import ru.yandex.market.tpl.core.external.xiva.model.PushCarrierEvent;
import ru.yandex.market.tpl.core.external.xiva.model.PushCarrierSendRequest;

class XivaSendTvmClientTest {

    private XivaSendTvmClient xivaSendTvmClient;

    private MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate(
                List.of(
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter()
                )
        );
        mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);

        PartnerCarrierXivaProperties xivaProperties = new PartnerCarrierXivaProperties();
        xivaProperties.setUrl("https://push.yandex.ru");
        xivaProperties.setService("market-tpl");

        xivaSendTvmClient = new XivaSendTvmClient(
                restTemplate,
                new XivaUriFactory(xivaProperties)
        );
    }

    @Test
    void shouldSend() {
        mockRestServiceServer
                .expect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.queryParam("user", "123"))
                .andExpect(MockRestRequestMatchers.queryParam("service", "market-tpl"))
                .andExpect(MockRestRequestMatchers.queryParam("event", "DRIVER_LOST"))
                .andExpect(MockRestRequestMatchers.queryParam("ttl", "300"))
                .andRespond(MockRestResponseCreators.withSuccess("123", MediaType.TEXT_PLAIN));
        ;

        xivaSendTvmClient.send(PushCarrierSendRequest.builder()
                .xivaUserId("123")
                .event(PushCarrierEvent.DRIVER_LOST)
                .payload(PushCarrierPayload.builder()
                        .title("title")
                        .body("body")
                        .deepLink("carrier://reportDriverStatus?force=true")
                        .build())
                .build()
        );

        mockRestServiceServer.verify();
    }

}
