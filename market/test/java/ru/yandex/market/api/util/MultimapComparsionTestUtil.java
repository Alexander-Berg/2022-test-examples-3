package ru.yandex.market.api.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.junit.Assert;

import java.util.Collections;
import java.util.List;

/**
 * Утилиты для сравнения мультимап в тестинге
 * @author dimkarp93
 */
public class MultimapComparsionTestUtil {
    public static void assertMapEquals(Multimap<String, String> expected, Multimap<String, String> actual) {
        List<String> expKeys = Lists.newArrayList(expected.keySet());
        Collections.sort(expKeys);

        List<String> actKeys = Lists.newArrayList(actual.keySet());
        Collections.sort(actKeys);

        Assert.assertTrue(Iterables.elementsEqual(expKeys, actKeys));

        for (String key: expKeys) {
            List<String> expValues = Lists.newArrayList(expected.get(key));
            Collections.sort(expValues);

            List<String> actValues = Lists.newArrayList(actual.get(key));
            Collections.sort(actValues);

            Assert.assertTrue(Iterables.elementsEqual(expKeys, actKeys));
        }
    }

}
