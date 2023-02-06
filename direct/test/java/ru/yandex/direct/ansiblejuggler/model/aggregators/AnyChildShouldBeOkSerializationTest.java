package ru.yandex.direct.ansiblejuggler.model.aggregators;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.ansiblejuggler.Util.dumpAsString;
import static ru.yandex.direct.ansiblejuggler.Util.getLocalResource;

public class AnyChildShouldBeOkSerializationTest {
    @Test
    public void test() {
        String expected = getLocalResource("aggregators/AnyChildShouldBeOk.yaml");
        String actual = dumpAsString(new AnyChildShouldBeOk());
        assertEquals(expected, actual);
    }
}
