package ru.yandex.market.promoboss.api.v2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException;
import ru.yandex.market.promoboss.AbstractFunctionalTest;
import ru.yandex.mj.generated.client.self_client.api.HistoryApiClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PromoHistoryApiTest extends AbstractFunctionalTest {

    @Autowired
    private HistoryApiClient historyApiClient;

    @Test
    void getHistory_invalidPageSize() {
        Exception e = assertThrows(Exception.class,
                () -> historyApiClient.getHistory("promo_id", 0, 1).schedule().join());

        assertEquals(HttpStatus.BAD_REQUEST.value(),
                ((CommonRetrofitHttpExecutionException) e.getCause()).getHttpCode());
    }

    @Test
    void getHistory_invalidPageNumber() {
        Exception e = assertThrows(Exception.class,
                () -> historyApiClient.getHistory("promo_id", 1, 0).schedule().join());

        assertEquals(HttpStatus.BAD_REQUEST.value(),
                ((CommonRetrofitHttpExecutionException) e.getCause()).getHttpCode());
    }
}
