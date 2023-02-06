package ru.yandex.market.mbo.gwt.models.visual;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 14.12.2017
 */
@SuppressWarnings("checkstyle:magicnumber")
public class TreeNodeTest {
    private TreeNode<Integer> tree;

    @Before
    public void before() {
        tree = node(1,
                node(11),
                node(12,
                        node(121),
                        node(122,
                                node(1221), node(1222)
                        ),
                        node(123),
                        node(124)
                ),
                node(13)
        );
    }

    @Test
    public void toRoot() {
        TreeNode<Integer> node1222 = tree.find(n -> n == 1222);
        assertThat(getData(node1222.toRoot()), is(Arrays.asList(1222, 122, 12, 1)));
    }

    @Test
    public void toRootSingle() {
        TreeNode<Integer> root = node(15);
        assertThat(root.toRoot(), is(Collections.singletonList(root)));
    }

    @Test
    public void fromRootSingle() {
        TreeNode<Integer> root = node(15);
        assertThat(root.fromRoot(), is(Collections.singletonList(root)));
    }

    @Test
    public void fromRoot() {
        TreeNode<Integer> node1222 = tree.find(n -> n == 1221);
        assertThat(getData(node1222.fromRoot()), is(Arrays.asList(1, 12, 122, 1221)));
    }

    private static <T> List<T> getData(List<? extends TreeNode<T>> nodes) {
        return nodes.stream().map(TreeNode::getData).collect(Collectors.toList());
    }

    @SafeVarargs
    private static <T> TreeNode<T> node(T v, TreeNode<T>... children) {
        TreeNode<T> node = new TreeNode<>(v);
        node.addChildren(Arrays.asList(children));
        return node;
    }
}
