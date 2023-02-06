package ru.yandex.market.checkout.checkouter.sberbank;

import java.util.List;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.sberbank.model.RegisterOrderRequest;
import ru.yandex.market.checkout.checkouter.sberbank.model.SberOrderStatusResponse;
import ru.yandex.market.checkout.checkouter.sberbank.model.SberRegisterOrderResponse;
import ru.yandex.market.checkout.checkouter.sberbank.model.SberResponse;
import ru.yandex.market.checkout.providers.SberApiRequestProvider;
import ru.yandex.market.checkout.util.sberbank.SberMockConfigurer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.util.sberbank.SberMockConfigurer.PAYMENT_REST_URL;
import static ru.yandex.market.checkout.util.sberbank.SberMockConfigurer.SBERCREDIT_URL;

/**
 * @author : poluektov
 * date: 2019-04-16.
 */
public class SberbankRestAPITest extends AbstractServicesTestBase {

    @Autowired
    private WireMockServer sberMock;
    @Autowired
    private SberMockConfigurer sberMockConfigurer;
    @Autowired
    private SberbankAPI sberbankAPI;

    @BeforeEach
    public void initMock() {
        sberMock.resetRequests();
        sberMockConfigurer.mockWholeSber();
    }

    //TODO: PROKACHAI MENYA;
    @Test
    public void createOrderTest() {
        RegisterOrderRequest request = SberApiRequestProvider.getDefaultRegisterOrderRequest();
        SberRegisterOrderResponse response = sberbankAPI.registerOrder(request);
        checkRegisterRequest(sberMock.getServeEvents().getRequests());
        assertThat(response.getOrderId(), notNullValue());
    }

    @Test
    public void getOrderStatusTest() {
        SberOrderStatusResponse response = sberbankAPI.getOrderStatusExtended(1234L);
        assertThat(response, notNullValue());
        assertThat(response.getOrderBundle(), notNullValue());
        assertThat(response.getOrderNumber(), notNullValue());
        assertThat(response.getAttributes(), notNullValue());
        assertThat(response.getAttributes(), hasSize(1));
    }

    @Test
    public void refundOrderTest() {
        SberResponse response = sberbankAPI.makeFullRefund("tipichniyMdOrderId", 100000L);
        checkRefundRequest(sberMock.getServeEvents().getRequests());
    }

    private void checkRegisterRequest(List<ServeEvent> event) {
        assertThat(event, hasSize(1));
        assertThat(event.get(0).getRequest().getQueryParams().entrySet(), hasSize(2));
        assertThat(event.get(0).getRequest().getUrl(), containsString(SBERCREDIT_URL + "/register.do"));
    }

    private void checkRefundRequest(List<ServeEvent> events) {
        assertThat(events, hasSize(1));
        ServeEvent event = events.get(0);
        assertThat(event.getRequest().getUrl(), containsString(PAYMENT_REST_URL + "/refund.do"));
        Map<String, QueryParameter> params = event.getRequest().getQueryParams();
        assertThat(params.entrySet(), hasSize(4));
        assertThat(params, hasKey("orderId"));
        assertThat(params, hasKey("amount"));
    }
}
