package ru.yandex.market.sberlog.utils;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashSet;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 16.04.19
 */
@Ignore
public class SpringUtilsTest {

    @Test
    public void splitToSet() {
        String example = "1,2,3";

        HashSet<String> expectedHashSet = new HashSet<>();
        expectedHashSet.add("1");
        expectedHashSet.add("2");
        expectedHashSet.add("3");

        Assert.assertEquals(expectedHashSet, SpringUtils.splitToSet(example));
    }
}
