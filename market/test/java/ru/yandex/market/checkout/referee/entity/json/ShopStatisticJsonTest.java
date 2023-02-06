package ru.yandex.market.checkout.referee.entity.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.checkout.entity.ShopStatistic;
import ru.yandex.market.checkout.referee.entity.AbstractJsonHandlerTest;
import ru.yandex.market.checkout.referee.entity.CheckoutRefereeHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author kukabara
 */
public class ShopStatisticJsonTest extends AbstractJsonHandlerTest {

    @Test
    public void test() throws Exception {
        ShopStatistic expectedObj = CheckoutRefereeHelper.getShopStatistic();
        String objectString = CheckoutRefereeHelper.getJsonShopStatistic();
        String json = write(expectedObj);
        JSONAssert.assertEquals(objectString, json, true);

        InputStream is = new ByteArrayInputStream(json.getBytes());
        ShopStatistic obj = read(ShopStatistic.class, is);

        assertEquals(expectedObj, obj);
    }
}
