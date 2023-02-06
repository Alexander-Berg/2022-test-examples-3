package ru.yandex.common.framework.core.servantletchecker.response;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import static ru.yandex.common.framework.core.servantletchecker.response.NodeContainmentMatcher.getPath;

/**
 * @author agorbunov @ Oct 26, 2010
 */
public class AttributeContainmentMatcher {
    private NamedNodeMap expectedAttributes;
    private NamedNodeMap actualAttributes;
    private String elementPath;

    public AttributeContainmentMatcher(Node expected, Node actual) {
        this.expectedAttributes = expected.getAttributes();
        this.actualAttributes = actual.getAttributes();
        this.elementPath = getPath(expected);
    }

    public String getDifference() {
        if (expectedAttributes == null) {
            return "";
        }
        for (int i = 0; i < expectedAttributes.getLength(); i++) {
            Node expectedAttribute = expectedAttributes.item(i);
            Node actualAttribute = actualAttributes.getNamedItem(expectedAttribute.getNodeName());
            String path = elementPath + "/@" + expectedAttribute.getNodeName();
            if (actualAttribute == null) {
                return "Missing attribute: " + path;
            }
            String expectedText = expectedAttribute.getTextContent();
            String actualText = actualAttribute.getTextContent();
            if (!expectedText.equals(actualText)) {
                return "Attribute " + path + " expected:<" + expectedText + "> but was:<" + actualText + ">";
            }
        }
        return "";
    }
}
