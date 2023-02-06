package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import steps.utils.TestableClock;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.failover.OrderEventFailoverableService;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.delivery.mdbapp.testutils.MockUtils.prepareMockServer;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@DisplayName("Получение заказа фф из лгв")
class GetOrderTest extends MockContextualTest {

    @Autowired
    private TestableClock clock;

    @Autowired
    @Qualifier("orderEventsPoller12")
    private OrderEventsPoller poller;

    @Autowired
    private OrderEventFailoverableService failoverService;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Autowired
    private FeatureProperties featureProperties;

    @Captor
    private ArgumentCaptor<ResourceId> orderIdArgumentCaptor;

    @Captor
    private ArgumentCaptor<Partner> partnerArgumentCaptor;

    private MockRestServiceServer checkouterMockServer;

    @BeforeEach
    void setUp() throws Exception {
        checkouterMockServer = MockRestServiceServer.createServer(checkouterRestTemplate);
        clock.setFixed(Instant.parse("2019-07-20T00:00:00Z"), ZoneOffset.UTC);

        prepareMockServer(
            checkouterMockServer,
            "/orders/events",
            "/data/events/get_order.json"
        );

        doReturn(Collections.emptySet()).when(failoverService).findFailedOrdersIds();
    }

    @AfterEach
    void tearDown() throws Exception {
        checkouterMockServer.verify();

        verify(failoverService).findFailedOrdersIds();
        if (featureProperties.isLgwGetOrderEventHandlingDisabled()) {
            featureProperties.setLgwGetOrderEventHandlingDisabled(true);
            verify(fulfillmentClient, never()).getOrder(any(), any());
            return;
        }

        verify(fulfillmentClient).getOrder(orderIdArgumentCaptor.capture(), partnerArgumentCaptor.capture());

        softly.assertThat(orderIdArgumentCaptor.getValue())
            .as("Asserting that the orderId was valid")
            .usingRecursiveComparison()
            .isEqualTo(ResourceId.builder().setYandexId("13114275").setPartnerId("EXT133960107").build());
        softly.assertThat(partnerArgumentCaptor.getValue())
            .as("Asserting that the partner was valid")
            .usingRecursiveComparison()
            .isEqualTo(new Partner(145L));
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Успешный ответ на получение заказа")
    void testGetOrderSuccess(boolean lgwGetOrderEventHandlingDisabled) {
        featureProperties.setLgwGetOrderEventHandlingDisabled(lgwGetOrderEventHandlingDisabled);
        poller.poll();

        verify(failoverService, never()).storeError(any(), anyString(), any());
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Получение заказа падает с проверяемым исключением")
    void testGetOrderFailedWithCheckedException(boolean lgwGetOrderEventHandlingDisabled) {
        featureProperties.setLgwGetOrderEventHandlingDisabled(lgwGetOrderEventHandlingDisabled);

        doThrow(GatewayApiException.class).when(fulfillmentClient).getOrder(any(ResourceId.class), any(Partner.class));

        poller.poll();

        if (lgwGetOrderEventHandlingDisabled) {
            verify(failoverService, never()).storeError(any(), anyString(), any());
        } else {
            verify(failoverService).storeError(any(), anyString(), any());
        }
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Получение заказа падает с непроверяемым исключением")
    void testGetOrderFailedWithUncheckedException(boolean lgwGetOrderEventHandlingDisabled) {
        featureProperties.setLgwGetOrderEventHandlingDisabled(lgwGetOrderEventHandlingDisabled);
        doThrow(RuntimeException.class).when(fulfillmentClient).getOrder(any(ResourceId.class), any(Partner.class));

        poller.poll();

        if (lgwGetOrderEventHandlingDisabled) {
            verify(failoverService, never()).storeError(any(), anyString(), any());
        } else {
            verify(failoverService).storeError(any(), anyString(), any());
        }
    }
}
