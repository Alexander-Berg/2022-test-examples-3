package ru.yandex.market.logistics.test.integration.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import static ru.yandex.market.logistics.test.integration.utils.ComparsionUtils.compareXml;

public class XmlMatcher extends TypeSafeMatcher<String> {

    private final String expected;

    public XmlMatcher(String expected) {
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(String actual) {
        return !compareXml(expected, actual)
            .hasDifferences();
    }


    @Override
    protected void describeMismatchSafely(String item, Description mismatchDescription) {
        mismatchDescription.appendText(" was :" + item)
            .appendText("Found problems : ").appendValue(compareXml(expected, item));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(" Expected XML is  : " + expected);
    }
}
