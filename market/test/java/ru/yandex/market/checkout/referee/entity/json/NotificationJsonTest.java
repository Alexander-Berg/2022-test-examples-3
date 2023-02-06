package ru.yandex.market.checkout.referee.entity.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.checkout.entity.Note;
import ru.yandex.market.checkout.entity.structures.NotificationChunk;
import ru.yandex.market.checkout.referee.entity.AbstractJsonHandlerTest;
import ru.yandex.market.checkout.referee.entity.CheckoutRefereeHelper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author kukabara
 */
public class NotificationJsonTest extends AbstractJsonHandlerTest {

    @Test
    public void testNote() throws Exception {
        Note expectedObj = CheckoutRefereeHelper.getNote();
        String objectString = CheckoutRefereeHelper.getJsonNote();
        String json = write(expectedObj);
        JSONAssert.assertEquals(objectString, json, true);

        InputStream is = new ByteArrayInputStream(json.getBytes());
        Note obj = read(Note.class, is);
        assertEquals(expectedObj, obj);
    }

    @Test
    public void testNotificationChunk() throws Exception {
        NotificationChunk expectedObj = CheckoutRefereeHelper.getNotificationChunk();
        String objectString = CheckoutRefereeHelper.getJsonNotificationChunk();
        String json = write(expectedObj);
        JSONAssert.assertEquals(objectString, json, true);

        InputStream is = new ByteArrayInputStream(json.getBytes());
        NotificationChunk obj = read(NotificationChunk.class, is);

        assertArrayEquals(expectedObj.getNotes().toArray(), obj.getNotes().toArray());
    }
}
