package ru.yandex.market.cashier.api;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.cashier.AbstractApplicationTest;
import ru.yandex.market.cashier.controllers.SberController;
import ru.yandex.market.cashier.mocks.sber.SberMockConfigurer;
import ru.yandex.market.cashier.providers.SberApiRequestProvider;
import ru.yandex.market.cashier.sber.api.SberbankAPI;
import ru.yandex.market.cashier.sber.api.rest.RegisterOrderRequest;
import ru.yandex.market.cashier.sber.api.rest.SberOrderStatusResponse;
import ru.yandex.market.cashier.sber.api.rest.SberRegisterOrderResponse;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author : poluektov
 * date: 2019-04-16.
 */
public class SberAPITest extends AbstractApplicationTest {
    @Autowired
    private WireMockServer sberMock;
    @Autowired
    private SberMockConfigurer sberMockConfigurer;
    @Autowired
    private SberbankAPI sberbankAPI;
    @Autowired
    private SberController sberController;

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
    public void simpleControllerTest() {
        sberController.registerOrder(1000000L, 12345L);
    }

    private void checkRegisterRequest(List<ServeEvent> event) {
        assertThat(event, hasSize(1));
        assertThat(event.get(0).getRequest().getQueryParams().entrySet(), hasSize(8));
        assertThat(event.get(0).getRequest().getUrl(), allOf(containsString("sbercredit/registerOrder.do")));
    }
}
