package ru.yandex.market.promoboss.api.v2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException;
import ru.yandex.market.promoboss.exception.ApiErrorException;
import ru.yandex.market.promoboss.service.mechanics.PromocodeReservationService;
import ru.yandex.mj.generated.client.self_client.api.PromocodeApiClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

public class PromocodeApiTest extends AbstractApiTest {

    private final static String CODE = "CODE";
    private final static Long START_AT = 1512292530L;
    private final static Long END_AT = 1512292530L;


    @MockBean
    PromocodeReservationService promocodeReservationService;

    @Autowired
    PromocodeApiClient promocodeApiClient;

    @Test
    void testPromocodeCheck_ok() {
        doReturn(true).when(promocodeReservationService).isPromocodeAvailable(CODE, START_AT, END_AT);

        Boolean actualResult = promocodeApiClient.checkPromocodeV2(CODE, START_AT, END_AT).schedule().join();

        Assertions.assertTrue(actualResult);
    }

    @Test
    void testPromocodeCheck_apiError() {
        doThrow(new ApiErrorException("Error response")).when(promocodeReservationService)
                .isPromocodeAvailable(CODE, START_AT, END_AT);

        Exception e = assertThrows(Exception.class,
                () -> promocodeApiClient.checkPromocodeV2(CODE, START_AT, END_AT).schedule().join());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ((CommonRetrofitHttpExecutionException) e.getCause()).getHttpCode());

        Assertions.assertTrue(e.getCause().getMessage().contains("Error response"));
    }
}
