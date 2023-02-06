package ru.yandex.market.ir.nirvana.common;

import org.junit.Test;
import ru.yandex.bolts.collection.Option;

import java.util.OptionalDouble;

import static org.junit.Assert.assertEquals;

public class UtilTest {
    public static final double SOME_DOUBLE = 3.14;

    @Test
    public void toPositiveDouble() throws Exception {
        assertEquals(OptionalDouble.of(SOME_DOUBLE), Util.toPositiveDouble(Option.of("3.14")));
        assertEquals(OptionalDouble.empty(), Util.toPositiveDouble(Option.of("-")));
    }

    @Test
    public void justName() {
        assertEquals("c", Util.justName("//a/b/c"));
        assertEquals("c", Util.justName("c"));
    }

    @Test
    public void concat() {
        assertEquals("//a/b/c/d/e", Util.concat('/', "//a", "b/", "c", "/d/", "/e"));
    }
}
