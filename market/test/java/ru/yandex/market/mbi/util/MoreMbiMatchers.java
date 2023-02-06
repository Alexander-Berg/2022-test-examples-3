package ru.yandex.market.mbi.util;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.springframework.http.ResponseEntity;

@ParametersAreNonnullByDefault
public final class MoreMbiMatchers {
    private MoreMbiMatchers() {
        throw new UnsupportedOperationException("Shouldn't be instantiated");
    }

    @Nonnull
    public static <T> Matcher<ResponseEntity<T>> responseBodyMatches(Matcher<T> bodyMatcher) {
        return new ResponseEntityBodyMatcher<>(bodyMatcher);
    }

    @Nonnull
    public static Matcher<String> jsonPropertyMatches(String propertyName, Matcher<String> resultMatcher) {
        return MbiMatchers.jsonPropertyMatches(propertyName, resultMatcher);
    }

    @Nonnull
    public static Matcher<String> jsonPropertyEquals(String propertyName, String json) {
        return MbiMatchers.jsonPropertyEquals(propertyName, json);
    }

    @Nonnull
    public static Matcher<String> jsonEquals(String expectedAsString) {
        return MbiMatchers.jsonEquals(expectedAsString);
    }

    @Nonnull
    public static Matcher<String> xmlEquals(String expectedAsString) {
        return MbiMatchers.xmlEquals(expectedAsString);
    }

    @Nonnull
    public static Matcher<String> jsonArrayEquals(String... expectedAsStringValues) {
        return MbiMatchers.jsonArrayEquals(expectedAsStringValues);
    }

    private static class ResponseEntityBodyMatcher<T> extends TypeSafeMatcher<ResponseEntity<T>> {
        private final Matcher<T> bodyMatcher;

        public ResponseEntityBodyMatcher(Matcher<T> bodyMatcher) {
            this.bodyMatcher = bodyMatcher;
        }

        @Override
        protected boolean matchesSafely(@Nonnull ResponseEntity<T> item) {
            return bodyMatcher.matches(item.getBody());
        }

        @Override
        protected void describeMismatchSafely(ResponseEntity<T> response, Description mismatchDescription) {
            super.describeMismatchSafely(response, mismatchDescription);
            mismatchDescription.appendText("\nresponse body differs:\n");
            mismatchDescription.appendText("\nexpected to be\n");
            bodyMatcher.describeTo(mismatchDescription);
            mismatchDescription.appendText(",\nbut:\n");
            bodyMatcher.describeMismatch(response.getBody(), mismatchDescription);
        }

        @Override
        public void describeTo(@Nonnull Description description) {
            description.appendText("Response body matches ");
            bodyMatcher.describeTo(description);
        }
    }
}
