package ru.yandex.http.test;

import java.util.Iterator;

import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultComparisonFormatter;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.ElementSelectors;

import ru.yandex.test.util.Checker;

public class XmlChecker implements Checker {
    private final String expected;

    public XmlChecker(final String expected) {
        this.expected = expected;
    }

    @Override
    public String check(final String value) {
        DefaultComparisonFormatter diffFormatter = new DefaultComparisonFormatter();

        Diff diff = DiffBuilder.compare(Input.fromString(expected))
            .withTest(Input.fromString(value))
            // ignoring order of elements
            .ignoreComments()
            .ignoreWhitespace()
            .build();

        if (!diff.hasDifferences()) {
            return null;
        }

        Iterator<Difference> iterator = diff.getDifferences().iterator();

        StringBuilder diffs = new StringBuilder();
        diffs.append("\n");
        while (iterator.hasNext()) {
            diffs.append(iterator.next().getComparison().toString(diffFormatter));
            diffs.append("\n");
        }

        return diffs.toString();
    }
}
