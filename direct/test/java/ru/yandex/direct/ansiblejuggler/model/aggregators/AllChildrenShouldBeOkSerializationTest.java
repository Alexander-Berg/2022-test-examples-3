package ru.yandex.direct.ansiblejuggler.model.aggregators;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.ansiblejuggler.Util.dumpAsString;
import static ru.yandex.direct.ansiblejuggler.Util.getLocalResource;

public class AllChildrenShouldBeOkSerializationTest {
    @Test
    public void test() {
        String expected = getLocalResource("aggregators/AllChildrenShouldBeOk.yaml");
        String actual = dumpAsString(new AllChildrenShouldBeOk());
        assertEquals(expected, actual);
    }
}
