package ru.yandex.market.partner.mvc.controller.order;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.Mockito.when;

/**
 * Проверка возврата подстатусов для заказов с 60,70,80 чекпоинтом
 */
public class OrdersControllerV2ReturnSubstatusesTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2ReturnSubstatusesTest.before.csv")
    void testOrderReturns() {

        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "v2/campaigns/2/orders/returnSubstatuses?order_ids=1,2,3,4,5");


        //language=json
        String expected = "{\"1\":\"RETURN_TRANSMITTED_FULFILMENT\",\"2\":\"RETURN_ARRIVED\",\"3\":\"RETURN_PREPARING\",\"4\":\"RETURN_PREPARING\"}";

        JsonTestUtil.assertEquals(response, expected);
    }
}
