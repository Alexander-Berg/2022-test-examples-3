package ru.yandex.market.api.internal.cataloger;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * Created by fettsery on 17.09.18.
 */
public class Nid2HidParserTest {
    @Test
    public void shouldParseNavigationTrees() {
        Int2IntMap result = new Nid2HidParser().parse(
            ResourceHelpers.getResource("navigation-trees.xml")
        );

        Assert.assertEquals(14, result.size());

        Assert.assertEquals(91461, result.get(20322));
        Assert.assertEquals(7812139, result.get(20409));
        Assert.assertEquals(91498, result.get(20546));
        Assert.assertEquals(90401, result.get(20001));
    }

    @Test
    public void shouldNotParseEmptyHid() {
        Int2IntMap result = new Nid2HidParser().parse(
            ResourceHelpers.getResource("navigation-trees.xml")
        );

        Assert.assertFalse(result.containsKey(20012));
    }

    @Test
    public void shouldNotParseEmptyNid() {
        Int2IntMap result = new Nid2HidParser().parse(
            ResourceHelpers.getResource("navigation-trees.xml")
        );

        Assert.assertFalse(result.containsValue(91500));
    }

    @Test
    public void shouldParseWithParentEmptyHid() {
        Int2IntMap result = new Nid2HidParser().parse(
            ResourceHelpers.getResource("navigation-trees.xml")
        );

        Assert.assertTrue(result.containsKey(20010));
    }

    @Test
    public void shouldParseWithParentEmptyNid() {
        Int2IntMap result = new Nid2HidParser().parse(
            ResourceHelpers.getResource("navigation-trees.xml")
        );

        Assert.assertTrue(result.containsKey(20547));
    }
}
