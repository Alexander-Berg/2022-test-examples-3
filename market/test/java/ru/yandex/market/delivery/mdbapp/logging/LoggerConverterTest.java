package ru.yandex.market.delivery.mdbapp.logging;

import java.util.LinkedHashMap;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.logistics.logging.converter.LoggerConverter;

import static ru.yandex.market.delivery.mdbapp.components.storage.domain.type.OrderToShipStatus.CANCELLED;
import static ru.yandex.market.delivery.mdbapp.components.storage.domain.type.OrderToShipStatus.CREATED;

public class LoggerConverterTest {

    @Test
    public void testConvertTskv() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("foo", 2984L);
        map.put("zun", "Wosche");
        map.put("bar", "Bljad");
        Assert.assertEquals(
            "Unexpected result from LoggerConverter",
            "foo=2984\tzun=Wosche\tbar=Bljad",
            LoggerConverter.convertToTskv(map)
        );
    }

    @Test
    public void testConvertJson() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("string", "ABC123");
        map.put("long", 777L);
        map.put("enum1", CREATED);
        map.put("enum2", CANCELLED);
        Assert.assertEquals(
            "Unexpected result from LoggerConverter",
            "{\"string\":\"ABC123\",\"long\":777,\"enum1\":\"CREATED\",\"enum2\":\"CANCELLED\"}",
            LoggerConverter.convertToJson(map)
        );

    }
}
