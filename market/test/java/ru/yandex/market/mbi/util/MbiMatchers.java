package ru.yandex.market.mbi.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONException;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.NodeMatcher;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
@ParametersAreNonnullByDefault
public final class MbiMatchers {
    private MbiMatchers() {
        throw new UnsupportedOperationException("Shouldn't be instantiated");
    }

    @Nonnull
    public static Matcher<String> jsonPropertyMatches(String propertyName, Matcher<String> resultMatcher) {
        return new JsonResultProperyMatcher(propertyName, resultMatcher);
    }

    @Nonnull
    public static Matcher<String> jsonPropertyEquals(String propertyName, String json) {
        return jsonPropertyMatches(propertyName, jsonEquals(json));
    }

    @Nonnull
    public static Matcher<String> jsonEquals(String expectedAsString) {
        return new JsonEqualsMatcher(expectedAsString);
    }

    @Nonnull
    public static Matcher<String> jsonEquals(String expected, List<Customization> customizations) {
        return new JsonEqualsMatcher(expected, customizations);
    }

    @Nonnull
    public static Matcher<String> jsonPath(String expression, String expected) {
        return new JsonPathMatcher(expression, expected);
    }

    @Nonnull
    public static Matcher<String> jsonPath(String expression, Matcher<String> matcher) {
        return new JsonPathMatcher(expression, matcher);
    }

    @Nonnull
    public static Matcher<String> xmlEquals(String expectedAsString) {
        return new XmlEqualsMatcher(expectedAsString);
    }

    @Nonnull
    public static Matcher<String> xmlEquals(String expectedAsString, NodeMatcher nodeMatcher) {
        return new XmlEqualsMatcher(expectedAsString, nodeMatcher);
    }

    @Nonnull
    public static Matcher<String> xmlEquals(String expectedAsString,
                                            Set<String> ignoredAttributes) {
        return new XmlEqualsMatcher(expectedAsString, ignoredAttributes);
    }

    @Nonnull
    public static Matcher<String> xmlEquals(String expectedAsString, NodeMatcher nodeMatcher,
                                            Set<String> ignoredAttributes) {
        return new XmlEqualsMatcher(expectedAsString, nodeMatcher, ignoredAttributes);
    }

    @Nonnull
    public static Matcher<String> jsonArrayEquals(String... expectedAsStringValues) {
        return jsonEquals(Arrays.stream(expectedAsStringValues).collect(Collectors.joining(", ", "[", "]")));
    }

    public static <T, R> Matcher<T> transformedBy(
            Function<? super T, ? extends R> transformation,
            Matcher<R> resultMatcher
    ) {
        return new CanBeTransformedMatcher<>(resultMatcher, transformation);
    }

    public static <T, R> Matcher<T> transformedBy(
            Function<? super T, ? extends R> transformation,
            Matcher<R> resultMatcher,
            @Nonnull String description
    ) {
        return new CanBeTransformedMatcher<>(resultMatcher, transformation, description);
    }

    public static <T, R> Matcher<T> transformedBy(Function<? super T, ? extends R> transformation, R value) {
        return transformedBy(transformation, Matchers.is(value));
    }

    public static <T, R> Matcher<T> transformedBy(
            Function<? super T, ? extends R> transformation,
            R value,
            String description
    ) {
        return transformedBy(transformation, Matchers.is(value), description);
    }

    public static <T> Matcher<T> satisfy(Predicate<? super T> predicate) {
        return transformedBy(predicate::test, Matchers.equalTo(true));
    }

    public static <K, V> Matcher<Map.Entry<K, V>> isEntry(
            Matcher<? super K> keyMatcher,
            Matcher<? super V> valueMatcher
    ) {
        return Matchers.allOf(
                transformedBy(Map.Entry::getKey, keyMatcher),
                transformedBy(Map.Entry::getValue, valueMatcher)
        );
    }

    public static <T> Matcher<Optional<T>> isPresent(Matcher<T> valueMatcher) {
        return Matchers.allOf(
                satisfy(Optional::isPresent),
                transformedBy(Optional::get, valueMatcher)
        );
    }

    public static <T> Matcher<Optional<T>> isPresent(T value) {
        return Matchers.allOf(
                satisfy(Optional::isPresent),
                transformedBy(Optional::get, value)
        );
    }

    public static Matcher<OptionalInt> isIntPresent(int value) {
        return Matchers.allOf(
                satisfy(OptionalInt::isPresent),
                transformedBy(OptionalInt::getAsInt, value)
        );
    }

    public static Matcher<OptionalDouble> isDoublePresent(double value) {
        return Matchers.allOf(
                satisfy(OptionalDouble::isPresent),
                transformedBy(OptionalDouble::getAsDouble, value)
        );
    }

    public static <T> Matcher<T> instanceOf(Class<? extends T> klass) {
        return Matchers.instanceOf(klass);
    }

    @SuppressWarnings("unchecked")
    public static <T, U extends T> Matcher<T> instanceOf(Class<U> klass, Matcher<U> matcher) {
        return Matchers.allOf(Matchers.instanceOf(klass), transformedBy(instance -> (U) instance, matcher));
    }

    public static <T> AllOfBuilder<T> newAllOfBuilder() {
        return new AllOfBuilder<>();
    }

    @SuppressWarnings("unchecked")
    public static <T> Matcher<Collection<T>> isEmptyCollection() {
        return EmptyCollectionMatcher.INSTANCE;
    }

    public static class AllOfBuilder<T> {
        private final List<Matcher<? super T>> matchers = new ArrayList<>();

        public AllOfBuilder<T> add(Matcher<? super T> matcher) {
            matchers.add(matcher);
            return this;
        }

        public <R> AllOfBuilder<T> add(Function<? super T, ? extends R> transformation, Matcher<? super R> matcher) {
            return add(transformation, matcher, CanBeTransformedMatcher.NO_DESCRIPTION);
        }

        public <R> AllOfBuilder<T> add(
                Function<? super T, ? extends R> transformation,
                Matcher<? super R> matcher,
                String description
        ) {
            return add(transformedBy(transformation, matcher, description));
        }

        public <R> AllOfBuilder<T> add(Function<? super T, ? extends R> transformation, R value) {
            return add(transformation, value, CanBeTransformedMatcher.NO_DESCRIPTION);
        }

        public <R> AllOfBuilder<T> add(Function<? super T, ? extends R> transformation, R value, String description) {
            if (value instanceof BigDecimal) {
                // BigDecimal's equals is somewhat "broken", 0.0 is not equal to 0.00, but 0.0 compares equal to 0.00
                return add(transformation, (Matcher<R>) Matchers.comparesEqualTo((BigDecimal) value), description);
            }
            return add(transformation, Matchers.is(value), description);
        }

        public Matcher<T> build() {
            return Matchers.allOf(matchers);
        }
    }

    private static class JsonResultProperyMatcher extends TypeSafeMatcher<String> {
        private static final ObjectReader JSON_READER = createObjectReader();
        private final String propertyName;
        private final Matcher<String> resultMatcher;

        JsonResultProperyMatcher(String propertyName, Matcher<String> resultMatcher) {
            this.propertyName = propertyName;
            this.resultMatcher = resultMatcher;
        }

        private static ObjectReader createObjectReader() {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.reader();
        }

        @Override
        protected boolean matchesSafely(@Nonnull String fullResponse) {
            return resultMatcher.matches(propertyValue(fullResponse));
        }

        private String propertyValue(@Nonnull String fullResponse) {
            try {
                ObjectNode fullResponseObject = (ObjectNode) JSON_READER.readTree(fullResponse);
                return fullResponseObject.get(propertyName).toString();
            } catch (IOException ex) {
                throw new UncheckedIOException("Unexpected IO error", ex);
            }
        }

        @Override
        protected void describeMismatchSafely(String fullResponse, Description mismatchDescription) {
            super.describeMismatchSafely(fullResponse, mismatchDescription);
            mismatchDescription.appendText("\n" + propertyName + " differs:\n");
            mismatchDescription.appendText("\nexpected to be\n");
            resultMatcher.describeTo(mismatchDescription);
            mismatchDescription.appendText(",\nbut:\n");
            resultMatcher.describeMismatch(propertyValue(fullResponse), mismatchDescription);
        }

        @Override
        public void describeTo(@Nonnull Description description) {
            description.appendText("\"").appendText(propertyName).appendText("\" property matches ");
            resultMatcher.describeTo(description);
        }
    }

    private static class JsonEqualsMatcher extends TypeSafeMatcher<String> {
        private final String expectedAsString;
        private final JSONComparator comparator;

        JsonEqualsMatcher(String expectedAsString) {
            this.expectedAsString = expectedAsString;
            this.comparator = new MbiCustomComparator(JSONCompareMode.STRICT);
        }

        JsonEqualsMatcher(String expectedAsString, List<Customization> customizations) {
            this.expectedAsString = expectedAsString;
            this.comparator = new CustomComparator(
                    JSONCompareMode.LENIENT,
                    customizations.toArray(new Customization[0])
            );
        }

        @Override
        protected boolean matchesSafely(@Nonnull String valueAsString) {
            JSONCompareResult result = isSameCheckResult(valueAsString);
            return result.passed();
        }

        @Override
        public void describeTo(@Nonnull Description description) {
            description.appendText("Value matches ").appendText(expectedAsString);
            description.appendText(" up to json invariants");
        }

        @Override
        protected void describeMismatchSafely(String item, Description mismatchDescription) {
            super.describeMismatchSafely(item, mismatchDescription);
            JSONCompareResult result = isSameCheckResult(item);
            mismatchDescription.appendText("\nDifferences found:\n");
            mismatchDescription.appendText(result.getMessage());

        }

        private JSONCompareResult isSameCheckResult(@Nonnull String valueAsString) {
            try {
                return JSONCompare.compareJSON(expectedAsString, valueAsString, comparator);
            } catch (JSONException ex) {
                throw new RuntimeException(ex);
            }
        }

        private static class MbiCustomComparator extends DefaultComparator {
            private static final String QUOTED_STRING_REGEX = "\"([^\"]*)\"|'([^']*)'";
            private static final String MBI_MATCHER_MACROS_NAMESPACE_REGEX = "mbiMatchers";
            private static final String IGNORE_PROPERTY_MACROS_REGEX =
                    "\\$\\{\\s*" + MBI_MATCHER_MACROS_NAMESPACE_REGEX + "\\s*.\\s*ignore\\s*}";
            private static final Pattern IGNORE_PROPERTY_MACROS_PATTERN =
                    Pattern.compile(IGNORE_PROPERTY_MACROS_REGEX);
            private static final String ALLOW_DIFFERENT_TIME_ZONE_REGEX =
                    "\\$\\{\\s*" + MBI_MATCHER_MACROS_NAMESPACE_REGEX
                            + "\\s*.\\s*allowDifferentTimeZone\\s*\\(\\s*("
                            + QUOTED_STRING_REGEX + ")\\s*\\)\\s*}";
            private static final Pattern ALLOW_DIFFERENT_TIME_ZONE_MACROS_PATTERN =
                    Pattern.compile(ALLOW_DIFFERENT_TIME_ZONE_REGEX);
            private static final Pattern QUOTED_STRING_PATTERN = Pattern.compile(QUOTED_STRING_REGEX);

            MbiCustomComparator(JSONCompareMode mode) {
                super(mode);
            }

            @Override
            public void compareValues(String prefix, Object expectedValue, Object actualValue, JSONCompareResult result)
                    throws JSONException {
                if (expectedValue instanceof String) {
                    String expectedString = (String) expectedValue;
                    if (IGNORE_PROPERTY_MACROS_PATTERN.matcher(expectedString).matches()) {
                        return;
                    }
                    java.util.regex.Matcher matcher = ALLOW_DIFFERENT_TIME_ZONE_MACROS_PATTERN.matcher(expectedString);
                    if (matcher.matches()) {
                        if (!(actualValue instanceof String)) {
                            result.fail("Expecting actual value to be string, but was: " + actualValue.getClass());
                            result.fail(prefix, expectedValue, actualValue);
                            return;
                        }
                        String actualString = (String) actualValue;
                        String quotedString = matcher.group(1);
                        java.util.regex.Matcher quotesMatcher = QUOTED_STRING_PATTERN.matcher(quotedString);
                        if (!quotesMatcher.matches()) {
                            throw new IllegalStateException(
                                    String.format("Expected %s to match quotedString", quotedString)
                            );
                        }
                        String expectedDateTimeString = IntStream.range(1, quotesMatcher.groupCount() + 1)
                                .mapToObj(quotesMatcher::group)
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException(
                                        String.format("Expected %s to match some variant of quoted string",
                                                quotedString)
                                ));
                        OffsetDateTime expectedDateTime;
                        OffsetDateTime actualDateTime;
                        try {
                            expectedDateTime = OffsetDateTime.parse(expectedDateTimeString);
                        } catch (DateTimeParseException e) {
                            result.fail("Expected " + expectedDateTimeString + " to be date time value");
                            result.fail(prefix, expectedValue, actualValue);
                            return;
                        }
                        try {
                            actualDateTime = OffsetDateTime.parse(actualString);
                        } catch (DateTimeParseException e) {
                            result.fail("Expected " + actualString + " to be date time value");
                            result.fail(prefix, expectedValue, actualValue);
                            return;
                        }
                        Instant expectedInstant = expectedDateTime.toInstant();
                        Instant actualInstant = actualDateTime.toInstant();
                        if (!expectedInstant.equals(actualInstant)) {
                            result.fail(prefix, expectedInstant.toString(), actualInstant.toString());
                        }
                        return;
                    }
                }
                super.compareValues(prefix, expectedValue, actualValue, result);
            }
        }
    }

    private static class XmlEqualsMatcher extends TypeSafeMatcher<String> {
        private final String expectedAsString;
        @Nullable
        private final NodeMatcher nodeMatcher;
        /**
         * xml аттрибуты, которые не будут участвовать в сравнении.
         */
        private final Set<String> ignoredAttributes;

        XmlEqualsMatcher(String expectedAsString) {
            this(expectedAsString, null, Collections.emptySet());
        }

        XmlEqualsMatcher(String expectedAsString, @Nullable NodeMatcher nodeMatcher) {
            this(expectedAsString, nodeMatcher, Collections.emptySet());
        }

        XmlEqualsMatcher(String expectedAsString, @Nonnull Set<String> ignoredAttributes) {
            this(expectedAsString, null, ignoredAttributes);
        }

        XmlEqualsMatcher(String expectedAsString,
                                @Nullable NodeMatcher nodeMatcher,
                                @Nonnull Set<String> ignoredAttributes
        ) {
            this.expectedAsString = expectedAsString;
            this.nodeMatcher = nodeMatcher;
            this.ignoredAttributes = ignoredAttributes;
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
                    .withAttributeFilter(attr -> !ignoredAttributes.contains(attr.getName()));

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

    private static class CanBeTransformedMatcher<T, R> extends TypeSafeMatcher<T> {
        private static final String NO_DESCRIPTION = "";
        private final Matcher<R> resultMatcher;
        private final String description;
        private final Function<? super T, ? extends R> transformation;

        CanBeTransformedMatcher(Matcher<R> resultMatcher, Function<? super T, ? extends R> transformation) {
            this(resultMatcher, transformation, NO_DESCRIPTION);
        }

        CanBeTransformedMatcher(
                Matcher<R> resultMatcher,
                Function<? super T, ? extends R> transformation,
                @Nonnull String description
        ) {
            this.resultMatcher = resultMatcher;
            this.transformation = transformation;
            this.description = description;
        }

        @Override
        protected boolean matchesSafely(@Nonnull T value) {
            return resultMatcher.matches(transformation.apply(value));
        }

        @Override
        protected void describeMismatchSafely(T value, Description mismatchDescription) {
            super.describeMismatchSafely(value, mismatchDescription);
            mismatchDescription.appendText("\ntransformation result differs:\n");
            if (NO_DESCRIPTION.equals(description)) {
                mismatchDescription.appendText("\nexpected to be\n");
            } else {
                mismatchDescription.appendText("\n" + "\"" + description + "\" expected to be\n");
            }
            resultMatcher.describeTo(mismatchDescription);
            mismatchDescription.appendText(",\nbut:\n");
            resultMatcher.describeMismatch(transformation.apply(value), mismatchDescription);
        }

        @Override
        public void describeTo(@Nonnull Description description) {
            description.appendText("transformation result matches ");
            resultMatcher.describeTo(description);
        }
    }

    private static class EmptyCollectionMatcher<T> extends TypeSafeMatcher<Collection<T>> {
        @SuppressWarnings("rawtypes")
        private static final EmptyCollectionMatcher INSTANCE = new EmptyCollectionMatcher();

        @Override
        public void describeTo(Description description) {
            description.appendText("Collection should be empty");
        }

        @Override
        protected boolean matchesSafely(Collection<T> item) {
            return item.isEmpty();
        }
    }

    private static class JsonPathMatcher extends TypeSafeMatcher<String> {

        private final JsonPathExpectationsHelper helper;
        private final String expression;
        private final Matcher<String> matcher;

        private JsonPathMatcher(String expression, String expectedValue) {
            this(expression, Matchers.equalTo(expectedValue));
        }

        private JsonPathMatcher(String expression, Matcher<String> matcher) {
            this.helper = new JsonPathExpectationsHelper(expression);
            this.matcher = matcher;
            this.expression = expression;
        }

        @Override
        protected boolean matchesSafely(String item) {
            return matcher.matches(helper.evaluateJsonPath(item, String.class));
        }

        @Override
        protected void describeMismatchSafely(String item, Description mismatchDescription) {
            matcher.describeMismatch(helper.evaluateJsonPath(item, String.class), mismatchDescription);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Value at path ")
                    .appendText(expression)
                    .appendText(" should match ")
                    .appendDescriptionOf(matcher);
        }
    }
}
