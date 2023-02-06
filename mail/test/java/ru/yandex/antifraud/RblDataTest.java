package ru.yandex.antifraud;

import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.antifraud.rbl.RblData;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class RblDataTest extends TestBase {
    public RblDataTest() {
        super(false, 0L);
    }
    @Test
    public void parsing() throws Exception {
        {
            final RblData data = new RblData(TypesafeValueContentHandler.parse(
                    Files.readString(resource("rbl-data1.json"))));
            Assert.assertEquals("AM", data.getIsoCountry());
        }
        {
            final JsonMap rawData =
                    TypesafeValueContentHandler.parse(Files.readString(resource("rbl-data2.json"))).asMap();
            final RblData data = new RblData(rawData);
            Assert.assertEquals("Ur", data.getIsoCountry());
            Assert.assertTrue(data.isYandexNet());
            YandexAssert.check(
                    new JsonChecker(Files.readString(resource("rbl-data2.json"))),
                    JsonType.NORMAL.toString(data.toJson()));
        }
    }
}
