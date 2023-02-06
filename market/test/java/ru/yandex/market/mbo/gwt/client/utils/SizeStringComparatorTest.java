package ru.yandex.market.mbo.gwt.client.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author dmserebr
 * @date 23.03.18
 */
public class SizeStringComparatorTest {
    @Test
    public void testSizeSorting() {
        List<String> sizeOptions = Arrays.asList("XL", "10", "12", "46", "48/50", "2XL", "11.5", "UNI", "14", "27-29");
        sizeOptions.sort(SizeStringComparator.INSTANCE);
        Assert.assertArrayEquals(new String[] {"2XL", "10", "11.5", "12", "14", "27-29", "46", "48/50", "UNI", "XL"},
            sizeOptions.toArray());
    }
}
