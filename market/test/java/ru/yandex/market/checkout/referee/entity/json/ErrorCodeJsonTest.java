package ru.yandex.market.checkout.referee.entity.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.referee.entity.AbstractJsonHandlerTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author kukabara
 */
public class ErrorCodeJsonTest extends AbstractJsonHandlerTest {

    @Test
    public void test() throws Exception {
        ErrorCodeException expectedObj = new ErrorCodeException("SOME_CODE", "Длинное сообщение об ошибке", 500);
        String json = write(expectedObj);

        String objectString = "{\"status\":500,\"code\":\"SOME_CODE\",\"message\":\"Длинное сообщение об ошибке\"}";
        JSONAssert.assertEquals(objectString, json, true);

        InputStream is = new ByteArrayInputStream(json.getBytes());
        ErrorCodeException obj = read(ErrorCodeException.class, is);
        assertEquals(expectedObj.getMessage(), obj.getMessage());
        assertEquals(expectedObj.getStatusCode(), obj.getStatusCode());
        assertEquals(expectedObj.getCode(), obj.getCode());
    }

}
