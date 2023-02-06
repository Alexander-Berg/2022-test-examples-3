package ru.yandex.market.logistic.api.utils;

import java.util.Collection;
import java.util.HashSet;

import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class ParsingXmlTest<T> extends ParsingTest<T> {
    private final Collection<String> nodesToSkip = new HashSet<>();

    protected ParsingXmlTest(Class<T> type, String fileName) {
        super(type, fileName);
    }

    protected void addNodeNameToSkip(String nodeName) {
        nodesToSkip.add(nodeName);
    }

    @Override
    protected final void checkRawStrings(String expected, String actual) {
        Diff diff = DiffBuilder.compare(expected)
            .withTest(actual)
            .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
            .withNodeFilter(node -> !nodesToSkip.contains(node.getNodeName()))//return false to skip
            .ignoreWhitespace()
            .ignoreComments()
            .checkForSimilar()
            .build();

        assertThat(diff.hasDifferences())
            .as(diff.toString())
            .isFalse();
    }
}
