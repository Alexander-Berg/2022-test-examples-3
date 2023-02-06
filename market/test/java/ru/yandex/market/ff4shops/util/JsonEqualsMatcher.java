package ru.yandex.market.ff4shops.util;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class JsonEqualsMatcher extends TypeSafeMatcher<String> {
    private final String expected;
    private final Set<String> ignoredElements;
    private final CustomComparator customComparator;

    public JsonEqualsMatcher(String expected, Set<String> ignoredElements, CustomComparator customComparator) {
        this.expected = expected;
        this.ignoredElements = ignoredElements;
        if (customComparator != null) {
            this.customComparator = customComparator;
        } else {
            Customization[] customizations = ignoredElements.stream()
                    .map(elem -> new Customization(elem, (el1, el2) -> true))
                    .collect(Collectors.toList())
                    .toArray(new Customization[ignoredElements.size()]);
            this.customComparator = new CustomComparator(JSONCompareMode.LENIENT, customizations);
        }
    }

    public JsonEqualsMatcher(String expected) {
        this(expected, Collections.emptySet(), null);
    }

    public JsonEqualsMatcher(String expected, CustomComparator customComparator) {
        this(expected, Collections.emptySet(), customComparator);
    }

    public JsonEqualsMatcher(String expected, Set<String> ignoredElements) {
        this(expected, ignoredElements, null);
    }

    @Override
    protected boolean matchesSafely(String actual) {
        JSONCompareResult result = JSONCompare.compareJSON(expected, actual, customComparator);
        return result.passed();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Value matches:\n````\n").appendText(expected);
        description.appendText("\n````\nup to json invariants");
    }

    @Override
    protected void describeMismatchSafely(String actual, Description mismatchDescription) {
        super.describeMismatchSafely(actual, mismatchDescription);

        JSONCompareResult result = JSONCompare.compareJSON(expected, actual, customComparator);

        mismatchDescription.appendText("\nDifference is:\n````\n");
        mismatchDescription.appendText(result.getMessage());
        mismatchDescription.appendText("\n````\n");
    }
}
