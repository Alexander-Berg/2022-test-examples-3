package ru.yandex.market.delivery.mdbapp.components.lgw.client;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.delivery.mdbapp.IntegrationTest;
import ru.yandex.market.delivery.mdbapp.configuration.TvmMockConfiguration;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.sc.internal.client.ScIntClient;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.util.client.HttpTemplate.SERVICE_TICKET_HEADER;

@RunWith(SpringJUnit4ClassRunner.class)
@MockBean({
    LMSClient.class,
    PechkinHttpClient.class,
    ScIntClient.class,
})
@IntegrationTest
@DirtiesContext
public class LgwDeliveryClientTest {

    @Autowired
    @Qualifier("lgwDeliveryClient")
    private DeliveryClient lgwDeliveryClient;

    @Autowired
    private RestTemplate lgwRestTemplate;

    @Test
    public void checkTvmHeadersExist() throws Exception {

        MockRestServiceServer mock = MockRestServiceServer.createServer(lgwRestTemplate);

        ResourceId orderId = ResourceId.builder().setYandexId("12345").setPartnerId("ABC12345").build();
        Partner partner = new Partner(654L);
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON)
            .body("{}");

        mock.expect(requestTo("http://localhost:8080/delivery/getOrdersDeliveryDate"))
            .andExpect(header(SERVICE_TICKET_HEADER, TvmMockConfiguration.TEST_TVM_TICKET))
            .andRespond(taskResponseCreator);

        lgwDeliveryClient.getOrdersDeliveryDate(Collections.singletonList(orderId), partner);
        mock.verify();
    }
}
