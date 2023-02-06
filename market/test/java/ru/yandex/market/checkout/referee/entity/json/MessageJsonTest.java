package ru.yandex.market.checkout.referee.entity.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.checkout.entity.Message;
import ru.yandex.market.checkout.entity.structures.PagedMessages;
import ru.yandex.market.checkout.referee.entity.AbstractJsonHandlerTest;
import ru.yandex.market.checkout.referee.entity.CheckoutRefereeHelper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author kukabara
 */
public class MessageJsonTest extends AbstractJsonHandlerTest {

    @Test
    public void testMessage() throws Exception {
        Message expectedObj = CheckoutRefereeHelper.getMessage();
        String objectString = CheckoutRefereeHelper.getJsonMessage();

        String json = write(expectedObj);
        JSONAssert.assertEquals(objectString, json, true);

        InputStream is = new ByteArrayInputStream(json.getBytes());
        Message obj = read(Message.class, is);
        assertEquals(expectedObj, obj);
    }

    @Test
    public void testPagedMessages() throws Exception {
        PagedMessages expectedObj = CheckoutRefereeHelper.getPagedMessages();
        String objectString = CheckoutRefereeHelper.getJsonPagedMessages();

        String json = write(expectedObj);
        JSONAssert.assertEquals(objectString, json, true);

        InputStream is = new ByteArrayInputStream(json.getBytes());
        PagedMessages obj = read(PagedMessages.class, is);
        assertArrayEquals(expectedObj.getItems().toArray(), obj.getItems().toArray());
    }
}
