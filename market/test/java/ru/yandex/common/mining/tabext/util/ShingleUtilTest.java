package ru.yandex.common.mining.tabext.util;

import junit.framework.TestCase;
import static ru.yandex.common.mining.tabext.util.ShingleUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Date: 09.01.2007
 * Time: 2:14:18
 *
 * @author nmalevanny@yandex-team.ru
 */
public class ShingleUtilTest extends TestCase {
    public void testBuildShingles() {
        List<String> word = Arrays.asList("a", "b", "c", "d", "e");
        {
            final List<List<String>> shingles =
                    buildShingles(word, 3, 1);
            assertEquals(3, shingles.size());
            assertEquals("c.d.e", joinWithDot(shingles.get(shingles.size() - 1)));
        }
        {
            final List<List<String>> shingles = buildShingles(word,
                    4, 1);
            assertEquals(2, shingles.size());
            assertEquals("a.b.c.d", joinWithDot(shingles.get(0)));
            assertEquals("b.c.d.e", joinWithDot(shingles.get(shingles.size() - 1)));
        }
        {
            final List<List<String>> shingles = buildShingles(word,
                    3, 2);
            assertEquals(2, shingles.size());
            assertEquals("c.d.e", joinWithDot(shingles.get(shingles.size() - 1)));
        }
        {
            final List<List<String>> shingles = buildShingles(word,
                    3, 3);
            assertEquals(2, shingles.size());
            assertEquals("d.e", joinWithDot(shingles.get(shingles.size() - 1)));
        }
        {
            final List<List<String>> shingles = buildShingles(word,
                    5, 1);
            assertEquals(1, shingles.size());
            assertEquals("a.b.c.d.e",
                    joinWithDot(shingles.get(shingles.size() - 1)));
        }
    }

    public void testCountDiff() {
        List<String> word1 = Arrays.asList("a", "b", "c", "d", "e", "f");
        List<String> word2 = Arrays.asList("e", "d", "c", "b", "a");
        List<String> word3 = Arrays.asList("a", "b", "c", "d", "e", "t");
        List<String> word4 = Arrays.asList("a", "a", "a", "a");
        List<String> word5 = Arrays.asList("a", "a", "a", "a", "a", "b");
        assertEquals(1d, countWordDiff(word1, word2, 3, 1), 0.0001d);
        assertEquals(1d, countWordDiff(null, word2, 3, 1), 0.0001d);
        assertEquals(1d, countWordDiff(Collections.<String>emptyList(), word2, 3, 1), 0.0001d);
        assertEquals(0.25d, countWordDiff(word1, word3, 3, 1), 0.0001d);
        assertEquals(0.25d, countWordDiff(word3, word1, 3, 1), 0.0001d);
        assertEquals(0d, countWordDiff(word1, new ArrayList<String>(word1), 3, 1), 0.0001d);
        assertEquals(0.25d, countWordDiff(word4, word5, 3, 1));
    }

    public void testCountWeight() {
        List<String> word = Arrays.asList("a", "b", "c", "a", "b", "c", "a", "b", "c");
        final List<List<String>> list = buildShingles(word, 3, 1);
        assertEquals(3, ShingleUtil.countWeight(list, 2));
        assertEquals(7, ShingleUtil.countWeight(list, 1));
        assertEquals(0, ShingleUtil.countWeight(list, 3));
    }
}
