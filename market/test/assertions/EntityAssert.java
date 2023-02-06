package ru.yandex.market.jmf.entity.test.assertions;

import java.util.Objects;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.assertj.core.api.AbstractAssert;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.AttributeNotFoundException;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;

public class EntityAssert<T extends Entity> extends AbstractAssert<EntityAssert<T>, T> {

    private final Multimap<String, Object> mismatchValues = HashMultimap.create();
    private final boolean detailed;
    private boolean error = false;

    public EntityAssert(T actual, boolean detailed) {
        super(actual, EntityAssert.class);
        this.detailed = detailed;
    }

    public static <T extends Entity> EntityAssert<T> assertThat(T actual) {
        return assertThat(actual, true);
    }

    public static <T extends Entity> EntityAssert<T> assertThat(T actual, boolean detailed) {
        return new EntityAssert<>(actual, detailed);
    }

    /**
     * Сущность содержит все пары {@code expectedKeyValuePairs}
     *
     * @param expectedKeyValuePairs пары (код атрибута -> значение)
     *                              где значением может быть как фактическое значение
     *                              так и {@link Matcher}
     */
    public EntityAssert<T> hasAttributes(Object... expectedKeyValuePairs) {
        Preconditions.checkArgument(
                0 == (expectedKeyValuePairs.length & 1),
                "The number of function parameters must be even"
        );
        Multimap<String, Object> expectedValues = HashMultimap.create();
        for (int i = 0; i < expectedKeyValuePairs.length; i += 2) {
            expectedValues.put((String) expectedKeyValuePairs[i], expectedKeyValuePairs[i + 1]);
        }
        return hasAttributes(expectedValues);
    }

    private EntityAssert<T> hasAttributes(Multimap<String, Object> expectedValues) {
        expectedValues.forEach(this::hasAttribute);
        if (error) {
            if (detailed) {
                failWithMessage("""
                        Expected entity to have attributes, but it doesn't. Diff:
                        %s
                        """, EntityAssertDiffFormatter.formatDiff(expectedValues, mismatchValues));
            } else {
                failWithMessage(
                        "Expected entity to have attributes (%s) but have (%s)",
                        expectedValues,
                        mismatchValues
                );
            }
        }
        return this;
    }

    private EntityAssert<T> hasAttribute(String code, Object expectedValue) {
        Object actualValue;
        try {
            actualValue = actual.getAttribute(code);
        } catch (AttributeNotFoundException e) {
            error = true;
            mismatchValues.put(code, String.format("{not found in %s}", actual.getFqn()));
            return this;
        }
        if (expectedValue instanceof Matcher) {
            return match(code, (Matcher<?>) expectedValue, actualValue);
        }
        if (!equals(actualValue, expectedValue)) {
            error = true;
            mismatchValues.put(code, actualValue);
        }
        return this;
    }

    private EntityAssert<T> match(String code, Matcher<?> matcher, Object actualValue) {
        if (!matcher.matches(actualValue)) {
            error = true;
            mismatchValues.put(code, getMatcherDescription(matcher, actualValue));
        }
        return this;
    }

    private Description getMatcherDescription(Matcher<?> matcher, Object actualValue) {
        StringDescription description = new StringDescription();
        description.appendText("{");
        matcher.describeTo(description);
        description.appendText(" but ");
        matcher.describeMismatch(actualValue, description);
        description.appendText("}");
        return description;
    }

    private boolean equals(Object actual, Object expected) {
        return Objects.equals(actual, expected)
                || equalsByPattern(actual, expected)
                || equalsByGid(actual, expected)
                || equalsByNaturalId(actual, expected);
    }

    private boolean equalsByGid(Object actual, Object expected) {
        return actual instanceof Entity
                && expected instanceof String
                && Objects.equals(((Entity) actual).getGid(), expected);
    }

    private boolean equalsByNaturalId(Object actual, Object expected) {
        if (actual instanceof Entity entity) {
            return entity.getMetaclass().getAttributes().stream()
                    .filter(Attribute::isNaturalId)
                    .map(entity::getAttribute)
                    .anyMatch(obj -> Objects.equals(expected, obj));
        }
        return false;
    }

    private boolean equalsByPattern(Object actual, Object expected) {
        if (actual instanceof String && expected instanceof Pattern) {
            return ((Pattern) expected).matcher((String) actual).matches();
        }
        return false;
    }

    public Multimap<String, Object> getMismatchValues() {
        return mismatchValues;
    }
}
