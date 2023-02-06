package ru.yandex.market.tpl.core.external.delivery.sc.parser;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.common.util.exception.TplExternalException;
import ru.yandex.market.tpl.core.external.delivery.sc.dto.ScRoutingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ScResponseCsvParserImplTest {

    public static final String EXTERNAL_ORDER_ID = "external_order_id";
    public static final String ROUTE_COURIER_ID = "route_courier_id";
    public static final String SC_RESULT_RESPONSE = EXTERNAL_ORDER_ID + "," + ROUTE_COURIER_ID;

    private final ScResponseParser scResponseParser = new ScResponseCsvParserImpl();

    @Test
    void parse_whenCorrectData() {
        //when
        List<ScRoutingResult.OrderCourier> orderCouriers =
                scResponseParser.parseRoutingResultResponse(SC_RESULT_RESPONSE);

        //then
        assertNotNull(orderCouriers);
        assertThat(orderCouriers).hasSize(1);

        assertEquals(orderCouriers.get(0).getCourierId(), ROUTE_COURIER_ID);
        assertEquals(orderCouriers.get(0).getOrderId(), EXTERNAL_ORDER_ID);
    }

    @Test
    void parse_whenInCorrectData() {
        //when
        assertThrows(
                TplExternalException.class,
                () -> scResponseParser.parseRoutingResultResponse("incorrect_data_format"));
    }
}
