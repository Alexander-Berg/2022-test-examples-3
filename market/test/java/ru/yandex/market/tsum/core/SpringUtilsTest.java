package ru.yandex.market.tsum.core;

import com.google.common.collect.HashMultimap;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 22/02/2017
 */
public class SpringUtilsTest {
    @Test
    public void splitToSet() throws Exception {
        Assert.assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), SpringUtils.splitToSet("a,b , c, a"));
    }

    @Test
    public void splitSetMultimap() throws Exception {
        HashMultimap<String, String> expected = HashMultimap.create();
        expected.put("k1", "v1");
        expected.put("k2", "v1");
        expected.put("k2", "v2");
        expected.put("k3", "vx:x");

        Assert.assertEquals(expected, SpringUtils.splitSetMultimap("k1: v1, k2 : v1 ,k2:v2, k2:v2, k3:vx:x"));
    }

    @Test
    public void splitSetMultimapEscaping() throws Exception {
        HashMultimap<String, String> expected = HashMultimap.create();
        expected.put("k1", "v1");
        expected.put("k2", "v1");
        Assert.assertEquals(expected, SpringUtils.splitSetMultimap("k1|v1, k2 | v1", "|"));
    }
}