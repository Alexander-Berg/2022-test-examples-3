package ru.yandex.market.clab.api;

import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DifferenceEvaluator;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 06.12.2018
 */
public class SkipExpectedDifferences implements DifferenceEvaluator {
    private static final String ANY = "any";
    private static final Pattern PLACEHOLDER = Pattern.compile("~~~([\\w\\d-]+)~~~");

    private final Map<String, Object> values = new HashMap<>();

    public SkipExpectedDifferences() {
    }

    @Override
    public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
        if (outcome != ComparisonResult.DIFFERENT) {
            return outcome;
        }
        switch (comparison.getType()) {
            case ATTR_VALUE:
            case TEXT_VALUE:

                String controlValue = (String) comparison.getControlDetails().getValue();
                Object testValue = comparison.getTestDetails().getValue();
                Matcher matcher = PLACEHOLDER.matcher(controlValue);
                if (matcher.matches()) {

                    String placeholderName = matcher.group(1);
                    if (!ANY.equals(placeholderName)) {
                        values.compute(placeholderName, (key, oldValue) -> {
                            if (oldValue != null) {
                                throw new IllegalStateException(
                                    "duplicate placeholder name '" + key + "'. Use uniq or '" + ANY + "'");
                            }
                            return testValue;
                        });
                    }
                    return ComparisonResult.EQUAL;
                }
                break;
        }
        return outcome;
    }

    public Map<String, Object> getValues() {
        return values;
    }
}
