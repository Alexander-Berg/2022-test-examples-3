package ru.yandex.market.api.partner.controllers.order;

import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.util.checkouter.CheckouterMockHelper;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DbUnitDataSet(before = "OrderControllerTestOnlyEstimatedDelivery.before.csv")
class OrderControllerOnlyEstimationDeliveryTest extends FunctionalTest implements ResourceUtilitiesMixin {

    private static final long DBS_PARTNER_ID = 1L;
    private static final long DBS_CAMPAIGN_ID = 1001L;

    @Value("${market.checkouter.client.url}")
    private String checkouterUrl;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Autowired
    private CheckouterAPI checkouterClient;

    private CheckouterMockHelper checkouterMockHelper;

    @BeforeEach
    void setUp() {
        checkouterMockHelper = new CheckouterMockHelper(checkouterRestTemplate, checkouterUrl);
    }

    static Stream<Arguments> onlyEstimatedDeliveryData() {
        return Stream.of(
                Arguments.of("", EnumSet.of(OrderStatus.DELIVERY, OrderStatus.PROCESSING), null),
                Arguments.of("&status=DELIVERY", EnumSet.of(OrderStatus.DELIVERY), null),
                Arguments.of("&status=PROCESSING", EnumSet.of(OrderStatus.PROCESSING), null),
                Arguments.of("&status=DELIVERY&substatus=DELIVERY_SERVICE_RECEIVED",
                        EnumSet.of(OrderStatus.DELIVERY), EnumSet.of(OrderSubstatus.DELIVERY_SERVICE_RECEIVED))
        );
    }

    @ParameterizedTest
    @MethodSource("onlyEstimatedDeliveryData")
    void testGetOrdersWithOnlyEstimationDelivery(String additionalParams, EnumSet<OrderStatus> expectedStatuses,
                                                 EnumSet<OrderSubstatus> expectedSubStatuses) {
        String meaninglessResponse = "{\"pager\":{}, \"orders\":[]}";

        checkouterMockHelper.mockGetOrders(DBS_PARTNER_ID)
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(meaninglessResponse));

        String url = String.format("%s/campaigns/%s/orders.json?%s", urlBasePrefix, DBS_CAMPAIGN_ID,
                "onlyEstimatedDelivery=true" + additionalParams);

        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(url, HttpMethod.GET, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(checkouterClient).getOrders(any(),
                argThat(request -> request.hasEstimatedDelivery != null && request.hasEstimatedDelivery &&
                        Objects.equals(expectedStatuses, request.statuses) &&
                        Objects.equals(expectedSubStatuses, request.substatuses)));
    }

    @Test
    void testInconsistentParams() {
        String meaninglessResponse = "{\"pager\":{}, \"orders\":[]}";

        checkouterMockHelper.mockGetOrders(DBS_PARTNER_ID)
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(meaninglessResponse));

        String url = String.format("%s/campaigns/%s/orders.json?%s", urlBasePrefix, DBS_CAMPAIGN_ID,
                "onlyEstimatedDelivery=true&status=PICKUP");

        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(url, HttpMethod.GET, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(checkouterClient);
        Assertions.assertNotNull(response.getBody());
        JsonTestUtil.assertEquals(resourceAsString("asserts/empty_orders.json"), response.getBody());
    }

    static Stream<Arguments> notOnlyEstimatedDeliveryData() {
        return Stream.of(
                Arguments.of("", null, null),
                Arguments.of("onlyEstimatedDelivery=false", null, null),
                Arguments.of("onlyEstimatedDelivery=false&status=DELIVERY&substatus=DELIVERY_SERVICE_RECEIVED",
                        EnumSet.of(OrderStatus.DELIVERY), EnumSet.of(OrderSubstatus.DELIVERY_SERVICE_RECEIVED))
        );
    }

    @ParameterizedTest
    @MethodSource("notOnlyEstimatedDeliveryData")
    void testGetOrdersWithoutOnlyEstimationDelivery(String queryParams, EnumSet<OrderStatus> expectedStatuses,
                                                    EnumSet<OrderSubstatus> expectedSubstatuses) {
        String meaninglessResponse = "{\"pager\":{}, \"orders\":[]}";

        checkouterMockHelper.mockGetOrders(DBS_PARTNER_ID)
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(meaninglessResponse));

        String url = String.format("%s/campaigns/%s/orders.json?%s", urlBasePrefix, DBS_CAMPAIGN_ID, queryParams);

        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(url, HttpMethod.GET, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(checkouterClient).getOrders(any(),
                argThat(request -> request.hasEstimatedDelivery == null &&
                        Objects.equals(expectedStatuses, request.statuses) &&
                        Objects.equals(expectedSubstatuses, request.substatuses)));
    }
}
