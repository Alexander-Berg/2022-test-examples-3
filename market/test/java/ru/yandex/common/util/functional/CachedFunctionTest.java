package ru.yandex.common.util.functional;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Gleb Smirnov <gsmir@yandex-team.ru>
 */
public class CachedFunctionTest {

    @Test
    public void testReplaceValue() {
        CachedFunction<String, Integer> cache = new CachedFunction<String, Integer>() {
            @Override
            protected Integer reallyApply(String arg) {
                return Integer.parseInt(arg);
            }
        };

        Assert.assertEquals(Integer.valueOf(10), cache.apply("10"));

        cache.replaceValue("10", 11);

        Assert.assertEquals(Integer.valueOf(11), cache.apply("10"));
    }
}
