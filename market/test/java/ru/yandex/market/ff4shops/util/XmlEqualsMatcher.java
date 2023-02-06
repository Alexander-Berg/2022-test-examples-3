package ru.yandex.market.ff4shops.util;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.NodeMatcher;

/**
 * @author fbokovikov
 */
public class XmlEqualsMatcher extends TypeSafeMatcher<String> {
    private final String expectedAsString;
    @Nullable
    private final NodeMatcher nodeMatcher;
    /**
     * xml аттрибуты, которые не будут участвовать в сравнении.
     */
    private final Set<String> ignoredElements;

    public XmlEqualsMatcher(String expectedAsString) {
        this(expectedAsString, null, Collections.emptySet());
    }

    public XmlEqualsMatcher(String expectedAsString, @Nullable NodeMatcher nodeMatcher) {
        this(expectedAsString, nodeMatcher, Collections.emptySet());
    }

    public XmlEqualsMatcher(String expectedAsString, @Nonnull Set<String> ignoredAttributes) {
        this(expectedAsString, null, ignoredAttributes);
    }

    public XmlEqualsMatcher(String expectedAsString,
                            @Nullable NodeMatcher nodeMatcher,
                            @Nonnull Set<String> ignoredElements
    ) {
        this.expectedAsString = expectedAsString;
        this.nodeMatcher = nodeMatcher;
        this.ignoredElements = ignoredElements;
    }


    @Override
    protected boolean matchesSafely(@Nonnull String valueAsString) {
        return !createDiff(valueAsString).hasDifferences();
    }

    private Diff createDiff(@Nonnull String valueAsString) {
        DiffBuilder diffBuilder = DiffBuilder.compare(expectedAsString)
                .withTest(valueAsString)
                .ignoreComments()
                .checkForSimilar()
                .ignoreWhitespace()
                .withNodeFilter(node -> !ignoredElements.contains(node.getNodeName()));

        if (nodeMatcher != null) {
            diffBuilder.withNodeMatcher(nodeMatcher);
        }
        return diffBuilder.build();
    }

    @Override
    protected void describeMismatchSafely(String valueAsString, Description mismatchDescription) {
        super.describeMismatchSafely(valueAsString, mismatchDescription);
        mismatchDescription.appendText("\nDifference is:\n````\n");
        mismatchDescription.appendText(createDiff(valueAsString).toString());
        mismatchDescription.appendText("\n````\n");
    }

    @Override
    public void describeTo(@Nonnull Description description) {
        description.appendText("Value matches:\n````\n").appendText(expectedAsString);
        description.appendText("\n````\nup to xml invariants");
    }
}
