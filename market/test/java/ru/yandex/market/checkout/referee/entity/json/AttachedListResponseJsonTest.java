package ru.yandex.market.checkout.referee.entity.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.checkout.entity.structures.AttachedListResponse;
import ru.yandex.market.checkout.referee.entity.AbstractJsonHandlerTest;
import ru.yandex.market.checkout.referee.entity.CheckoutRefereeHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author kukabara
 */
public class AttachedListResponseJsonTest extends AbstractJsonHandlerTest {

    @Test
    public void test() throws Exception {
        AttachedListResponse expectedObj = CheckoutRefereeHelper.getAttachedListResponse();
        String objectString = CheckoutRefereeHelper.getJsonAttachedListResponse();

        String json = write(expectedObj);
        JSONAssert.assertEquals(objectString, json, true);

        InputStream is = new ByteArrayInputStream(json.getBytes());
        AttachedListResponse obj = read(AttachedListResponse.class, is);
        assertEquals(expectedObj, obj);
    }
}
