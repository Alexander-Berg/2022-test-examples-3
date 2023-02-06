package ru.yandex.market.tpl.common.dsm.client;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.tpl.common.dsm.client.api.CourierApi;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "external.market-delivery-staff-manager.url=http://dev/null",
        "external.market-delivery-staff-manager.tvmServiceId=123",
        "external.market-delivery-staff-manager.connectTimeoutMillis=100",
        "external.market-delivery-staff-manager.readTimeoutMillis=100",
        "external.market-delivery-staff-manager.maxConnTotal=1",
})
@AutoConfigureMockRestServiceServer
@RestClientTest(CourierApi.class)
@ContextConfiguration(classes = MarketDeliveryStaffManagerClientConfiguration.class)
public class RestTemplateResponseErrorHandlerTest {

    @Autowired
    private RestTemplate marketDeliveryStaffManagerRestTemplate;

    @Autowired
    private CourierApi courierApi;

    @MockBean
    private TvmClient tvmClient;

    @Test
    public void testExceptionHandler() {
        String errorBody = "Very very very very very bad request";
        MockRestServiceServer mockServer =
                MockRestServiceServer.bindTo(marketDeliveryStaffManagerRestTemplate).build();
        mockServer
                .expect(ExpectedCount.once(), requestTo("http://dev/null/couriers/123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body(errorBody));

        MarketDeliveryStaffManagerException exception = assertThrows(
                MarketDeliveryStaffManagerException.class,
                () -> courierApi.couriersIdGet("123")
        );

        Assertions.assertThat(exception.getMessage()).isEqualTo(HttpStatus.BAD_REQUEST + " " + errorBody);
    }
}
