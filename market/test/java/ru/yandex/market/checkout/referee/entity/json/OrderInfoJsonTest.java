package ru.yandex.market.checkout.referee.entity.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.checkout.entity.OrderInfo;
import ru.yandex.market.checkout.referee.entity.AbstractJsonHandlerTest;
import ru.yandex.market.checkout.referee.entity.CheckoutRefereeHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author kukabara
 */
public class OrderInfoJsonTest extends AbstractJsonHandlerTest {

    @Test
    public void test() throws Exception {
        OrderInfo expectedObj = CheckoutRefereeHelper.getOrderInfo();
        String objectString = CheckoutRefereeHelper.getJsonOrderInfo();

        String json = write(expectedObj);
        JSONAssert.assertEquals(objectString, json, true);

        InputStream is = new ByteArrayInputStream(json.getBytes());
        OrderInfo obj = read(OrderInfo.class, is);
        assertEquals(obj, expectedObj);
    }
}
