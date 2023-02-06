package ru.yandex.market.mbo.gwt.models;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;

import java.util.List;

/**
 * @author Stas Lyahnovich conterouz@yandex-team.ru 02.02.12 15:37
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TreeTest {

    private class N {
        int id;
        String name;

        private N(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private TreeNode<N> tree() {
        TreeNode<N> root = new TreeNode<N>(new N(0, "ROOT"));
        TreeNode<N> a1 = new TreeNode<N>(new N(1, "A1"));
        root.addChild(a1);
        TreeNode<N> a2 = new TreeNode<N>(new N(2, "A2"));
        a1.addChild(a2);
        TreeNode<N> a3 = new TreeNode<N>(new N(3, "A3"));
        a2.addChild(a3);
        TreeNode<N> b1 = new TreeNode<N>(new N(4, "B1"));
        root.addChild(b1);
        TreeNode<N> b2 = new TreeNode<N>(new N(5, "B2"));
        b1.addChild(b2);
        TreeNode<N> b3 = new TreeNode<N>(new N(6, "B3"));
        b1.addChild(b3);
        return root;

    }

    @Test
    public void testSearch() {
        TreeNode<N> n = tree().find(data -> data.id == 3);
        Assert.assertEquals("A3", n.getData().name);

        TreeNode<N> n2 = tree().find(data -> data.id == 6);
        Assert.assertEquals("B3", n2.getData().name);
    }

    @Test
    public void testList() {

        TreeNode<N> tree = tree();
        TreeNode<N> n = tree.find(data -> data.id == 3);
        List<? extends TreeNode<N>> lst = n.pathTo(tree);
        Assert.assertEquals(4, lst.size());
        Assert.assertEquals("A3", lst.get(0).getData().name);
        Assert.assertEquals("A2", lst.get(1).getData().name);
        Assert.assertEquals("A1", lst.get(2).getData().name);
        Assert.assertEquals("ROOT", lst.get(3).getData().name);
    }
}
