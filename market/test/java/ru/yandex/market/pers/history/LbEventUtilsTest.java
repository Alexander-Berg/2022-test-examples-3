package ru.yandex.market.pers.history;

import org.junit.Test;
import java.util.Date;

import static org.junit.Assert.*;

public class LbEventUtilsTest {

    private void checkSerialization(Item item, String expected) {
        String result = LbEventUtils.serializeItem(item);
        assertEquals(expected, result);
    }

    @Test
    public void testJsonSerialization() {
        Item item = new Item();
        item.setDate(new Date(1234567890l));

        checkSerialization(item, "{\"timestamp\":1234567890}");

        item.setUserId(987l);
        item.setUuid("u876");
        item.setYandexUid("y765");
        item.setResourceId(135l);
        item.setHid(357l);
        item.setRgb(RGB.green);
        item.setMarketSku("246");
        item.setNid(579l);
        item.setScreen(Screen.OTHER);
        item.setType(Type.MODEL);

        checkSerialization(item, "{\"timestamp\":1234567890,\"puid\":987,\"uuid\":\"u876\",\"yandexuid\":\"y765\",\"rgb\":\"green\",\"screen\":\"OTHER\",\"resourceType\":\"MODEL\",\"resourceId\":135,\"sku\":\"246\",\"hid\":357,\"nid\":579}");
    }
}
