package ru.yandex.common.framework.core.servantletchecker.response;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author agorbunov @ Oct 25, 2010
 */
public class NodeContainmentMatcher {
    private Node expected;
    private Node actual;

    public NodeContainmentMatcher(Node expected, Node actual) {
        this.expected = expected;
        this.actual = actual;
    }

    public String getDifference() {
        String childDifference = getChildDifference();
        if (!childDifference.isEmpty()) {
            return childDifference;
        }
        String textDifference = getTextDifference();
        if (!textDifference.isEmpty()) {
            return textDifference;
        }
        String attributeDifference = getAttributeDifference();
        if (!attributeDifference.isEmpty()) {
            return attributeDifference;
        }
        return "";
    }

    private String getChildDifference() {
        if (!expected.hasChildNodes()) {
            return "";
        }
        NodeList expectedChildNodes = expected.getChildNodes();
        NodeList actualChildNodes = actual.getChildNodes();
        for (int i = 0; i < expectedChildNodes.getLength(); i++) {
            Node expectedChild = expectedChildNodes.item(i);
            if (expectedChild.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Node actualChild = findNodeByExample(actualChildNodes, expectedChild);
            if (actualChild == null) {
                actualChild = findNodeByName(actualChildNodes, expectedChild);
            }
            if (actualChild == null) {
                return "Missing element: " + getPath(expectedChild);
            }
            String grandChildDifference = new NodeContainmentMatcher(expectedChild, actualChild).getDifference();
            if (!grandChildDifference.isEmpty()) {
                return grandChildDifference;
            }
        }
        return "";
    }

    private Node findNodeByExample(NodeList nodeList, Node example) {
        for (int j = 0; j < nodeList.getLength(); j++) {
            Node candidate = nodeList.item(j);
            if (example.getNodeName().equals(candidate.getNodeName()) &&
                    isFullyEqual(example, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isFullyEqual(Node example, Node candidate) {
        return new NodeContainmentMatcher(example, candidate).getDifference().isEmpty();
    }

    private Node findNodeByName(NodeList nodeList, Node name) {
        return findNodeByName(nodeList, name.getNodeName());
    }

    static Node findNodeByName(NodeList nodeList, String name) {
        for (int j = 0; j < nodeList.getLength(); j++) {
            Node candidate = nodeList.item(j);
            if (candidate.getNodeName().equals(name)) {
                return candidate;
            }
        }
        return null;
    }

    private String getTextDifference() {
        return new TextContainmentMatcher(expected, actual).getDifference();
    }

    private String getAttributeDifference() {
        return new AttributeContainmentMatcher(expected, actual).getDifference();
    }

    static String getPath(Node node) {
        String path = "";
        while (true) {
            path = "/" + node.getNodeName() + path;
            node = node.getParentNode();
            if (node == null || node.getNodeName().equals("#document")) {
                return path;
            }
        }
    }
}
