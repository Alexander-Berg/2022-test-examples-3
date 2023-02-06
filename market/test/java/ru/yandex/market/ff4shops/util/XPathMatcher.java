package ru.yandex.market.ff4shops.util;

import java.io.StringReader;

import javax.annotation.Nonnull;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;
import org.xml.sax.InputSource;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class XPathMatcher extends TypeSafeMatcher<String> {

    private static ThreadLocal<XPathFactory> factory = ThreadLocal.withInitial(XPathFactory::newDefaultInstance);

    private final String actualXPath;
    private final String expected;
    private final Matcher<String> valueMatcher;


    public XPathMatcher(String actualXPath, String expected, Matcher<String> valueMatcher) {
        this.actualXPath = actualXPath;
        this.expected = expected;
        this.valueMatcher = valueMatcher;
    }

    public XPathMatcher(String actualXPath, String expected) {
        this(actualXPath, expected, new IsEqual<>(expected));
    }

    @Override
    protected boolean matchesSafely(String item) {
        String computedActual = evaluate(actualXPath, item);
        return valueMatcher.matches(computedActual);
    }

    @Override
    protected void describeMismatchSafely(String valueAsString, Description mismatchDescription) {
        mismatchDescription.appendText("was ");
        mismatchDescription.appendValue(evaluate(actualXPath, valueAsString));
    }

    @Override
    public void describeTo(@Nonnull Description description) {
        description.appendText("Value for xpath '")
                .appendText(actualXPath)
                .appendText("' matches: ").appendText(expected);
    }

    private String evaluate(String xpath, String source) {
        try {
            return factory.get().newXPath().compile(xpath).evaluate(new InputSource(new StringReader(source)));
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }
}
