package ru.yandex.market.mbo.cms.core.models;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class NodeBlockTest {

    private static final String TEST_PLACEHOLDER = "TEST_PLACEHOLDER";

    @Test(expected = UnsupportedOperationException.class)
    public void testGetWidgetValues() {
        Node node = new Node();
        Node innerNode = new Node();
        node.addWidgetValue(TEST_PLACEHOLDER, innerNode);
        Assert.assertEquals(node, innerNode.getParentNode());
        List<Node> widgetValues = node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER);
        widgetValues.add(innerNode);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetWidgets() {
        Node node = new Node();
        Node innerNode = new Node();
        node.addWidgetValue(TEST_PLACEHOLDER, innerNode);
        Assert.assertEquals(node, innerNode.getParentNode());
        Map<String, List<Node>> widgets = node.getParametersBlock().getWidgets();
        widgets.put(TEST_PLACEHOLDER, Collections.singletonList(innerNode));
    }

    @Test
    public void testSetWidgetValue() {
        Node node = new Node();
        Node oldInnerNode1 = new Node();
        Node oldInnerNode2 = new Node();
        Node newInnerNode = new Node();

        node.getParametersBlock().addWidgetValue(TEST_PLACEHOLDER, oldInnerNode1);
        node.getParametersBlock().addWidgetValue(TEST_PLACEHOLDER, oldInnerNode2);
        Assert.assertEquals(node, oldInnerNode1.getParentNode());
        Assert.assertEquals(node, oldInnerNode2.getParentNode());
        Assert.assertEquals(2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
        Assert.assertEquals(oldInnerNode1, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(0));
        Assert.assertEquals(oldInnerNode2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(1));

        node.getParametersBlock().setWidgetValue(TEST_PLACEHOLDER, newInnerNode);
        Assert.assertNull(oldInnerNode1.getParentNode());
        Assert.assertNull(oldInnerNode2.getParentNode());
        Assert.assertEquals(node, newInnerNode.getParentNode());
        Assert.assertEquals(1, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
        Assert.assertEquals(newInnerNode, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(0));
    }

    @Test
    public void testSetWidgetValues() {
        Node node = new Node();
        Node oldInnerNode1 = new Node();
        Node oldInnerNode2 = new Node();
        Node newInnerNode1 = new Node();
        Node newInnerNode2 = new Node();
        List<Node> newNodes = new ArrayList<>();
        newNodes.add(newInnerNode1);
        newNodes.add(newInnerNode2);

        node.getParametersBlock().addWidgetValue(TEST_PLACEHOLDER, oldInnerNode1);
        node.getParametersBlock().addWidgetValue(TEST_PLACEHOLDER, oldInnerNode2);
        Assert.assertEquals(node, oldInnerNode1.getParentNode());
        Assert.assertEquals(node, oldInnerNode2.getParentNode());
        Assert.assertEquals(2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
        Assert.assertEquals(oldInnerNode1, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(0));
        Assert.assertEquals(oldInnerNode2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(1));

        node.getParametersBlock().setWidgetValues(TEST_PLACEHOLDER, newNodes);
        Assert.assertNull(oldInnerNode1.getParentNode());
        Assert.assertNull(oldInnerNode2.getParentNode());
        Assert.assertEquals(node, newInnerNode1.getParentNode());
        Assert.assertEquals(node, newInnerNode2.getParentNode());
        Assert.assertEquals(2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
        Assert.assertEquals(newInnerNode1, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(0));
        Assert.assertEquals(newInnerNode2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(1));
    }

    @Test
    public void testAddWidgetValue() {
        Node node = new Node();
        Node newInnerNode1 = new Node();
        Node newInnerNode2 = new Node();

        node.getParametersBlock().addWidgetValue(TEST_PLACEHOLDER, newInnerNode1);
        Assert.assertEquals(node, newInnerNode1.getParentNode());
        Assert.assertEquals(1, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
        Assert.assertEquals(newInnerNode1, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(0));

        node.getParametersBlock().addWidgetValue(TEST_PLACEHOLDER, newInnerNode2);
        Assert.assertEquals(node, newInnerNode2.getParentNode());
        Assert.assertEquals(2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
        Assert.assertEquals(newInnerNode2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(1));

    }

    @Test
    public void testMoveChildWidget() {
        Node node = new Node();
        Node newInnerNode1 = new Node();
        Node newInnerNode2 = new Node();

        node.getParametersBlock().addWidgetValue(TEST_PLACEHOLDER, newInnerNode1);
        node.getParametersBlock().addWidgetValue(TEST_PLACEHOLDER, newInnerNode2);
        Assert.assertEquals(node, newInnerNode1.getParentNode());
        Assert.assertEquals(node, newInnerNode2.getParentNode());
        Assert.assertEquals(2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
        Assert.assertEquals(newInnerNode1, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(0));
        Assert.assertEquals(newInnerNode2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(1));

        node.getParametersBlock().moveChildWidget(TEST_PLACEHOLDER, newInnerNode1, false);
        Assert.assertEquals(node, newInnerNode1.getParentNode());
        Assert.assertEquals(node, newInnerNode2.getParentNode());
        Assert.assertEquals(2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
        Assert.assertEquals(newInnerNode2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(0));
        Assert.assertEquals(newInnerNode1, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(1));

        node.getParametersBlock().moveChildWidget(TEST_PLACEHOLDER, newInnerNode1, true);
        Assert.assertEquals(node, newInnerNode1.getParentNode());
        Assert.assertEquals(node, newInnerNode2.getParentNode());
        Assert.assertEquals(2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
        Assert.assertEquals(newInnerNode1, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(0));
        Assert.assertEquals(newInnerNode2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(1));
    }

    @Test
    public void testRemoveWidgets() {
        Node node = new Node();
        Node newInnerNode1 = new Node();
        Node newInnerNode2 = new Node();

        node.getParametersBlock().addWidgetValue(TEST_PLACEHOLDER, newInnerNode1);
        node.getParametersBlock().addWidgetValue(TEST_PLACEHOLDER, newInnerNode2);
        Assert.assertEquals(node, newInnerNode1.getParentNode());
        Assert.assertEquals(node, newInnerNode2.getParentNode());
        Assert.assertEquals(2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
        Assert.assertEquals(newInnerNode1, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(0));
        Assert.assertEquals(newInnerNode2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(1));

        node.getParametersBlock().removeWidgets(TEST_PLACEHOLDER);
        Assert.assertNull(newInnerNode1.getParentNode());
        Assert.assertNull(newInnerNode2.getParentNode());
        Assert.assertEquals(0, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
    }

    @Test
    public void testRemoveChildWidget() {
        Node node = new Node();
        Node newInnerNode1 = new Node();
        Node newInnerNode2 = new Node();

        node.getParametersBlock().addWidgetValue(TEST_PLACEHOLDER, newInnerNode1);
        node.getParametersBlock().addWidgetValue(TEST_PLACEHOLDER, newInnerNode2);
        Assert.assertEquals(node, newInnerNode1.getParentNode());
        Assert.assertEquals(node, newInnerNode2.getParentNode());
        Assert.assertEquals(2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
        Assert.assertEquals(newInnerNode1, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(0));
        Assert.assertEquals(newInnerNode2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(1));

        node.getParametersBlock().removeChildWidget(TEST_PLACEHOLDER, newInnerNode1);
        Assert.assertNull(newInnerNode1.getParentNode());
        Assert.assertEquals(node, newInnerNode2.getParentNode());
        Assert.assertEquals(1, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
        Assert.assertEquals(newInnerNode2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(0));

        node.getParametersBlock().removeChildWidget(TEST_PLACEHOLDER, newInnerNode2);
        Assert.assertNull(newInnerNode1.getParentNode());
        Assert.assertNull(newInnerNode2.getParentNode());
        Assert.assertEquals(0, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
    }

    @Test
    public void testClearWidgets() {
        Node node = new Node();
        Node newInnerNode1 = new Node();
        Node newInnerNode2 = new Node();

        node.getParametersBlock().addWidgetValue(TEST_PLACEHOLDER, newInnerNode1);
        node.getParametersBlock().addWidgetValue(TEST_PLACEHOLDER, newInnerNode2);
        Assert.assertEquals(node, newInnerNode1.getParentNode());
        Assert.assertEquals(node, newInnerNode2.getParentNode());
        Assert.assertEquals(2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
        Assert.assertEquals(newInnerNode1, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(0));
        Assert.assertEquals(newInnerNode2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(1));

        node.getParametersBlock().clearWidgets(TEST_PLACEHOLDER);
        Assert.assertNull(newInnerNode1.getParentNode());
        Assert.assertNull(newInnerNode2.getParentNode());
        Assert.assertNotNull(node.getParametersBlock().getWidgets().get(TEST_PLACEHOLDER));
        Assert.assertEquals(0, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
    }

    @Test
    public void testSetNodeBlockForNode() {
        Node node = new Node();

        NodeBlock nodeBlock = new NodeBlock();
        Node newInnerNode1 = new Node();
        Node newInnerNode2 = new Node();

        nodeBlock.addWidgetValue(TEST_PLACEHOLDER, newInnerNode1);
        nodeBlock.addWidgetValue(TEST_PLACEHOLDER, newInnerNode2);
        Assert.assertNull(newInnerNode1.getParentNode());
        Assert.assertNull(newInnerNode2.getParentNode());

        node.setParametersBlock(nodeBlock);
        Assert.assertEquals(node, newInnerNode1.getParentNode());
        Assert.assertEquals(node, newInnerNode2.getParentNode());
        Assert.assertEquals(2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).size());
        Assert.assertEquals(newInnerNode1, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(0));
        Assert.assertEquals(newInnerNode2, node.getParametersBlock().getWidgetValues(TEST_PLACEHOLDER).get(1));
    }
}
