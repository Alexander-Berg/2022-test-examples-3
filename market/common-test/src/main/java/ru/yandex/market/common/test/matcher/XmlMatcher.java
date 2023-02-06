package ru.yandex.market.common.test.matcher;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;


/**
 * Матчер сравнивает на соответствие <i>XML</i> строки.
 */
@ParametersAreNonnullByDefault
public class XmlMatcher extends TypeSafeMatcher<String> {

    private final String expected;

    public XmlMatcher(String expected) {
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(String actual) {
        return !compareXml(expected, actual).hasDifferences();
    }


    @Override
    protected void describeMismatchSafely(String item, Description mismatchDescription) {
        mismatchDescription
            .appendText(" was:" + item)
            .appendText("Found problems: ")
            .appendValue(compareXml(expected, item));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(" Expected XML is: " + expected);
    }

    @Nonnull
    private Diff compareXml(String xml1, String xml2) {
        return createDiffBuilder(xml1).withTest(xml2).build();
    }

    @Nonnull
    private static DiffBuilder createDiffBuilder(String expected) {
        return DiffBuilder.compare(expected)
            .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
            .ignoreWhitespace()
            .ignoreComments()
            .checkForSimilar();
    }
}
