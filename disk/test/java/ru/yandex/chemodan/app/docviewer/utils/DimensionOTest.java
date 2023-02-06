package ru.yandex.chemodan.app.docviewer.utils;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class DimensionOTest {

    @Test
    public void testToString() {
        Assert.equals("123x456", DimensionO.cons(123, 456).toString());
        Assert.equals("200x", DimensionO.cons(200).toString(""));
        Assert.equals("?x300", DimensionO.cons(Integer.MAX_VALUE, 300).toString("?"));
        Assert.equals("_x_", DimensionO.cons(Integer.MAX_VALUE, Integer.MAX_VALUE).toString("_"));
    }

}
