package ru.yandex.market.delivery.partnerapimock.util;

import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

public final class XmlMatcher {

    private XmlMatcher() {
        throw new UnsupportedOperationException();
    }

    public static boolean isXmlsEqual(String xml1, String xml2) {
        Diff d = DiffBuilder.compare(xml1)
            .withTest(xml2)
            .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
            .withComparisonListeners()
            .ignoreWhitespace()
            .ignoreComments()
            .checkForSimilar()
            .build();

        return !d.hasDifferences();
    }
}
