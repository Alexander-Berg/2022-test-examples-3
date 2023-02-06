package ru.yandex.market.mbo.cms.core.models;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.Test;

public class NodeTest {
    @Test
    public void testCollectAllNodesRootFalse() {
        Predicate<Node> predicate = node -> !node.isHidden();

        List<Node> r1 = new ArrayList<>();
        Node n1 = makeNode();
        n1.setHidden(true);
        n1.collectAllNodes(r1, predicate);
        Assert.assertEquals(Collections.emptyList(), r1);
    }

    @Test
    public void testCollectAllNodesRootTrue() {
        Predicate<Node> predicate = node -> !node.isHidden();

        List<Node> r2 = new ArrayList<>();
        Node n2 = makeNode();
        r2.add(n2);
        n2.setHidden(false);
        n2.collectAllNodes(r2, predicate);
    }

    @Test
    public void testCollectAllNodesAllTrue() {
        Predicate<Node> predicate = node -> !node.isHidden();

        List<Node> r1 = new ArrayList<>();
        Node n1 = makeNode();
        Node n11 = makeNode();
        Node n12 = makeNode();
        Node n111 = makeNode();
        Node n112 = makeNode();
        Node n121 = makeNode();
        Node n122 = makeNode();
        n1.getParametersBlock().addWidgetValues("p", Arrays.asList(n11, n12));
        n11.getParametersBlock().addWidgetValues("p", Arrays.asList(n111, n112));
        n12.getParametersBlock().addWidgetValues("p", Arrays.asList(n121, n122));
        r1.add(n1);
        r1.add(n11);
        r1.add(n12);
        r1.add(n111);
        r1.add(n112);
        r1.add(n121);
        r1.add(n122);

        n1.collectAllNodes(r1, predicate);
    }

    @Test
    public void testCollectAllNodesSomeFalse() {
        Predicate<Node> predicate = node -> !node.isHidden();

        List<Node> r1 = new ArrayList<>();
        Node n1 = makeNode();
        Node n11 = makeNode();
        n11.setHidden(true);
        Node n12 = makeNode();
        Node n111 = makeNode();
        Node n112 = makeNode();
        Node n121 = makeNode();
        n121.setHidden(true);
        Node n122 = makeNode();
        n1.getParametersBlock().addWidgetValues("p", Arrays.asList(n11, n12));
        n11.getParametersBlock().addWidgetValues("p", Arrays.asList(n111, n112));
        n12.getParametersBlock().addWidgetValues("p", Arrays.asList(n121, n122));
        r1.add(n1);
        r1.add(n112);
        r1.add(n122);

        n1.collectAllNodes(r1, predicate);
    }

    private Node makeNode() {
        Node result = new Node(new NodeType(), 0, 0);
        NodeBlock nodeBlock = new NodeBlock();
        result.setParametersBlock(nodeBlock);
        return result;
    }
}
