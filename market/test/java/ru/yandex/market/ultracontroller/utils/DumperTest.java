package ru.yandex.market.ultracontroller.utils;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("MagicNumber")
public class DumperTest {
    @Test
    public void dump() throws Exception {
        Assert.assertEquals(
            "Test string, %09infa 100%25!" +
                "%22|Описание: Артикул %E2%84%96%20311661! 1-2-3 %E0%AE%B8",
            Dumper.dump(("Test string, \tinfa 100%!" +
                "\"|Описание: Артикул № 311661! 1-2-3 " + (char) 3000).getBytes("UTF-8"))
        );
    }
}
