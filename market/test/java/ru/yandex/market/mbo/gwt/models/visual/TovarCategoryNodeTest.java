package ru.yandex.market.mbo.gwt.models.visual;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

/**
 * Created by ayratgdl on 13.10.15.
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TovarCategoryNodeTest {

    @Test
    public void testFlatten() throws Exception {
        TovarCategoryNode t0 = new TovarCategoryNode(new TovarCategory(0));
        TovarCategoryNode t10 = (TovarCategoryNode) t0.addChild(new TovarCategoryNode(new TovarCategory(1)));
        TovarCategoryNode t11 = (TovarCategoryNode) t0.addChild(new TovarCategoryNode(new TovarCategory(2)));
        TovarCategoryNode t211 = (TovarCategoryNode) t11.addChild(new TovarCategoryNode(new TovarCategory(3)));

        assertEquals(new HashSet<>(Arrays.asList(t0, t10, t11, t211)), new HashSet<>(t0.flatten()));
    }

    @Test
    public void testListExtendedLeafNodes() throws Exception {
        TovarCategoryNode root = new TovarCategoryNode(new TovarCategory(0));
        TovarCategoryNode leafNode = (TovarCategoryNode) root.addChild(new TovarCategoryNode(new TovarCategory(1)));

        TovarCategory notLeafButPublished = new TovarCategory(2);
        notLeafButPublished.setPublished(true);
        TovarCategoryNode notLeaf = (TovarCategoryNode) root.addChild(new TovarCategoryNode(notLeafButPublished));

        TovarCategory leafButNotPublished = new TovarCategory(3);
        leafButNotPublished.setPublished(false);
        notLeaf.addChild(new TovarCategoryNode(leafButNotPublished));

        assertEquals(new HashSet<>(Arrays.asList(leafNode, notLeaf)),
                     new HashSet<>(root.listFakeLeafNodes(v -> true)));
    }
}
