package ru.yandex.market.checkout.util.matching;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.collections4.iterators.NodeListIterator;
import org.hamcrest.Condition;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static javax.xml.xpath.XPathConstants.NODESET;
import static org.hamcrest.Condition.matched;
import static org.hamcrest.Condition.notMatched;

/**
 * @author mkasumov
 */
public class XPathMatcher<R> extends TypeSafeDiagnosingMatcher<Node> {

    public static final NamespaceContext NO_NAMESPACE_CONTEXT = null;

    private final Matcher<R> valueMatcher;
    private final XPathExpression compiledXPath;
    private final String xpathString;
    private final QName evaluationMode;

    private XPathMatcher(String xPathExpression, NamespaceContext namespaceContext, Matcher<R> valueMatcher,
                         QName mode) {
        this.compiledXPath = compiledXPath(xPathExpression, namespaceContext);
        this.xpathString = xPathExpression;
        this.valueMatcher = valueMatcher;
        this.evaluationMode = mode;
    }

    private static Iterable<Node> nodeListToIterable(NodeList nodeList) {
        return () -> new NodeListIterator(nodeList);
    }

    private static XPathExpression compiledXPath(String xPathExpression, NamespaceContext namespaceContext) {
        try {
            final XPath xPath = XPathFactory.newInstance().newXPath();
            if (namespaceContext != null) {
                xPath.setNamespaceContext(namespaceContext);
            }
            return xPath.compile(xPathExpression);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Invalid XPath : " + xPathExpression, e);
        }
    }

    public static Builder xPath(String xPath) {
        return new Builder(xPath);
    }

    @Override
    public boolean matchesSafely(Node item, Description mismatch) {
        return evaluated(item, mismatch)
                .and(nodeExists())
                .matching(valueMatcher);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("an XML document with XPath ").appendText(xpathString);
        if (valueMatcher != null) {
            description.appendText(" ").appendDescriptionOf(valueMatcher);
        }
    }

    private Condition<R> evaluated(Node item, Description mismatch) {
        try {
            return matched(evaluateXPathValue(item), mismatch);
        } catch (XPathExpressionException e) {
            mismatch.appendText(e.getMessage());
        }
        return notMatched();
    }

    private R evaluateXPathValue(Node item) throws XPathExpressionException {
        Object value = compiledXPath.evaluate(item, evaluationMode);
        if (evaluationMode == NODESET) {
            value = nodeListToIterable((NodeList) value);
        }
        //noinspection unchecked
        return (R) value;
    }

    private Condition.Step<R, R> nodeExists() {
        return (value, mismatch) -> {
            if (value == null) {
                mismatch.appendText("xpath returned no results.");
                return notMatched();
            }
            return matched(value, mismatch);
        };
    }

    public static class Builder {

        private final String xPath;

        private Builder(String xPath) {
            this.xPath = xPath;
        }

        public XPathMatcher<? super String> hasValue(Matcher<? super String> valueMatcher) {
            return new XPathMatcher<>(xPath, NO_NAMESPACE_CONTEXT, valueMatcher, XPathConstants.STRING);
        }

        public XPathMatcher<? super Node> hasNode(Matcher<? super Node> nodeMatcher) {
            return new XPathMatcher<>(xPath, NO_NAMESPACE_CONTEXT, nodeMatcher, XPathConstants.NODE);
        }

        public XPathMatcher<Iterable<? super Node>> hasNodes(Matcher<Iterable<? super Node>> nodeListMatcher) {
            return new XPathMatcher<>(xPath, NO_NAMESPACE_CONTEXT, nodeListMatcher, XPathConstants.NODESET);
        }
    }
}
