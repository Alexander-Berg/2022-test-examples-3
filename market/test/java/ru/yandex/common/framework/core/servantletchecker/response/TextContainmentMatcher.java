package ru.yandex.common.framework.core.servantletchecker.response;

import org.w3c.dom.Node;

import static ru.yandex.common.framework.core.servantletchecker.response.NodeContainmentMatcher.findNodeByName;
import static ru.yandex.common.framework.core.servantletchecker.response.NodeContainmentMatcher.getPath;

/**
 * @author agorbunov @ Dec 3, 2010
 */
public class TextContainmentMatcher {
    private Node expected;
    private Node actual;

    public TextContainmentMatcher(Node expected, Node actual) {
        this.expected = expected;
        this.actual = actual;
    }

    public String getDifference() {
        Node expectedTextElement = findNodeByName(expected.getChildNodes(), "#text");
        Node actualTextElement = findNodeByName(actual.getChildNodes(), "#text");
        String expectedText = (expectedTextElement != null) ? expectedTextElement.getTextContent() : "";
        String actualText = (actualTextElement != null) ? actualTextElement.getTextContent() : "";
        if (!expectedText.trim().equals(actualText.trim())) {
            return "Element " + getPath(expected) + " expected:<" + expectedText + "> but was:<" + actualText + ">";
        }
        return "";
    }
}
