package ru.yandex.chemodan.bazinga;

import lombok.AllArgsConstructor;
import org.junit.Test;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.http.YandexCloudRequestIdHolder;

import static org.junit.Assert.assertEquals;

/**
 * @author friendlyevil
 */
public class YcridTaskParametersConverterTest {

    @AllArgsConstructor
    static class TestClass extends YcridTaskParameters {
        String someField;
        int intValue;
    }

    @Test
    public void convertWithoutYcrid() {
        YandexCloudRequestIdHolder.set("ycridValue");
        TestClass testClass = new TestClass("someValue", 15);
        MapF<String, Object> parameters = new YcridTaskParametersConverter().convert(testClass).getMap();

        assertEquals(parameters.getOrThrow("someField"), "someValue");
        assertEquals(parameters.getOrThrow("intValue"), 15);
        assertEquals(parameters.getO("ycrid"), Option.empty());
    }
}
