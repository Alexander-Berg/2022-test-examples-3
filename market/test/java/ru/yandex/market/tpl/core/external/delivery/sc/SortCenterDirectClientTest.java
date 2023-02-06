package ru.yandex.market.tpl.core.external.delivery.sc;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.tpl.common.util.exception.TplExternalException;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.domain.order.batch.OrderBatchesScDto;
import ru.yandex.market.tpl.core.external.delivery.sc.dto.ScRoutingResult;
import ru.yandex.market.tpl.core.external.delivery.sc.parser.ScResponseParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class SortCenterDirectClientTest {

    public static final String EXTERNAL_ORDER_ID = "external_order_id";
    public static final String ROUTE_COURIER_ID = "route_courier_id";
    public static final String CORRECT_SC_RESULT_RESPONSE = EXTERNAL_ORDER_ID + "," + ROUTE_COURIER_ID;
    public static final String INCORRECT_SC_RESULT_RESPONSE = "incorrect_data_format";
    public static final String TOKEN_SC = "TOKEN_SC";
    public static final String URL = "http://url.url";

    @Mock
    private RestTemplate mockedRestTemplate;
    @Mock
    private ScResponseParser mockedScResponseParser;

    private SortCenterDirectClient client;

    @BeforeEach
    void setUp() {
        client = new SortCenterDirectClient(mockedRestTemplate, mockedScResponseParser, URL);
    }

    @Test
    void getRoutingResult_whenCorrectResult() {
        //given
        Mockito.doReturn(ResponseEntity.of(Optional.of(CORRECT_SC_RESULT_RESPONSE)))
                .when(mockedRestTemplate).exchange(any(URI.class), any(HttpMethod.class), any(), eq(String.class));

        Mockito.doReturn(Collections.singletonList(new ScRoutingResult.OrderCourier(EXTERNAL_ORDER_ID,
                ROUTE_COURIER_ID)))
                .when(mockedScResponseParser).parseRoutingResultResponse(CORRECT_SC_RESULT_RESPONSE);

        //when
        Optional<ScRoutingResult> routingResult = client.getRoutingResult(LocalDate.now(), TOKEN_SC);

        //then
        assertTrue(routingResult.isPresent());

        List<ScRoutingResult.OrderCourier> orderCouriers = routingResult
                .map(ScRoutingResult::getRoutingOrders)
                .orElseGet(Collections::emptyList);

        assertThat(orderCouriers).hasSize(1);

        assertEquals(orderCouriers.get(0).getCourierId(), ROUTE_COURIER_ID);
        assertEquals(orderCouriers.get(0).getOrderId(), EXTERNAL_ORDER_ID);
    }

    @Test
    void getRoutingResult_whenInCorrectResultFormat_throwsSkipRetry() {
        //given
        Mockito.doReturn(ResponseEntity.of(Optional.of(INCORRECT_SC_RESULT_RESPONSE)))
                .when(mockedRestTemplate).exchange(any(URI.class), any(HttpMethod.class), any(), eq(String.class));

        Mockito.doThrow(TplExternalException.class)
                .when(mockedScResponseParser).parseRoutingResultResponse(INCORRECT_SC_RESULT_RESPONSE);
        //then
        assertThrows(
                TplExternalException.class,
                () -> client.getRoutingResult(LocalDate.now(), TOKEN_SC));
    }

    @Test
    void getRoutingResult_whenNotSuccessStatus_throwsForRetry() {
        //given
        Mockito.doReturn(ResponseEntity.badRequest().build())
                .when(mockedRestTemplate).exchange(any(URI.class), any(HttpMethod.class), any(), eq(String.class));

        //then
        assertThrows(
                TplInvalidActionException.class,
                () -> client.getRoutingResult(LocalDate.now(), TOKEN_SC));
    }

    @Test
    void getBatchRegistryScInfo() {
        String batchRegisterId = "tpl_123";

        client.getBatchRegistry(batchRegisterId);

        String expectedUrl = URL + "/internal/batchRegistry/" + batchRegisterId;
        Mockito.verify(mockedRestTemplate).getForObject(eq(expectedUrl), eq(OrderBatchesScDto.class));
    }
}
