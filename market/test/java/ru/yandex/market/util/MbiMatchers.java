package ru.yandex.market.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.NodeMatcher;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
@ParametersAreNonnullByDefault
public class MbiMatchers {
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
                MbiMatchers.transformedBy(Map.Entry::getKey, keyMatcher),
                MbiMatchers.transformedBy(Map.Entry::getValue, valueMatcher)
        );
    }

    public static <T> Matcher<Optional<T>> isPresent(Matcher<T> valueMatcher) {
        return Matchers.allOf(
                MbiMatchers.satisfy(Optional::isPresent),
                MbiMatchers.transformedBy(Optional::get, valueMatcher)
        );
    }

    public static <T> Matcher<Optional<T>> isPresent(T value) {
        return Matchers.allOf(
                MbiMatchers.satisfy(Optional::isPresent),
                MbiMatchers.transformedBy(Optional::get, value)
        );
    }

    public static Matcher<OptionalInt> isIntPresent(int value) {
        return Matchers.allOf(
                MbiMatchers.satisfy(OptionalInt::isPresent),
                MbiMatchers.transformedBy(OptionalInt::getAsInt, value)
        );
    }

    public static Matcher<OptionalLong> isLongPresent(long value) {
        return Matchers.allOf(
                MbiMatchers.satisfy(OptionalLong::isPresent),
                MbiMatchers.transformedBy(OptionalLong::getAsLong, value)
        );
    }

    public static Matcher<OptionalDouble> isDoublePresent(double value) {
        return Matchers.allOf(
                MbiMatchers.satisfy(OptionalDouble::isPresent),
                MbiMatchers.transformedBy(OptionalDouble::getAsDouble, value)
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

    public static class AllOfBuilder<T> {
        private final List<Matcher<? super T>> matchers = new ArrayList<>();

        public AllOfBuilder<T> add(Matcher<? super T> matcher) {
            matchers.add(matcher);
            return this;
        }

        public <R> AllOfBuilder<T> add(Function<? super T, ? extends R> transformation, Matcher<? super R> matcher) {
            matchers.add(MbiMatchers.transformedBy(transformation, matcher));
            return this;
        }

        public <R> AllOfBuilder<T> add(
                Function<? super T, ? extends R> transformation,
                Matcher<? super R> matcher,
                String description
        ) {
            matchers.add(MbiMatchers.transformedBy(transformation, matcher, description));
            return this;
        }

        public <R> AllOfBuilder<T> add(Function<? super T, ? extends R> transformation, R value) {
            matchers.add(MbiMatchers.transformedBy(transformation, value));
            return this;
        }

        public <R> AllOfBuilder<T> add(Function<? super T, ? extends R> transformation, R value, String description) {
            matchers.add(MbiMatchers.transformedBy(transformation, value, description));
            return this;
        }

        public Matcher<T> build() {
            return Matchers.allOf(matchers);
        }
    }

    private static class JsonResultProperyMatcher extends TypeSafeMatcher<String> {
        private final String propertyName;
        private final Matcher<String> resultMatcher;
        private static final ObjectReader JSON_READER = createObjectReader();

        JsonResultProperyMatcher(String propertyName, Matcher<String> resultMatcher) {
            this.propertyName = propertyName;
            this.resultMatcher = resultMatcher;
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

        private static ObjectReader createObjectReader() {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.reader();
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

        JsonEqualsMatcher(String expectedAsString) {
            this.expectedAsString = expectedAsString;
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
                return JSONCompare.compareJSON(expectedAsString, valueAsString, JSONCompareMode.STRICT);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null; //bad case very very bad FIXME!
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
}
