package ru.yandex.common.util.exception;

import java.util.Arrays;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ExceptionCollectorTest {

    private static final RuntimeException EXCEPTION = new RuntimeException("First exception");
    private static final RuntimeException SUPPRESSED_EXCEPTION = new RuntimeException("Suppressed exception");

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldSuppressMoreThanOneExceptions() {
        expectedException.expect(new SuppressedExceptionMatcher());

        try (ExceptionCollector collector = new ExceptionCollector()) {
            collector.execute(() -> {
                throw EXCEPTION;
            });
            collector.execute(() -> {
                throw SUPPRESSED_EXCEPTION;
            });
            // exception не должен добавиться в коллекцию
            collector.execute(() -> {
                        throw SUPPRESSED_EXCEPTION;
                    },
                    null,
                    e -> "Suppressed exception".equals(e.getMessage()));
        }
    }

    @Test
    public void shouldNotContainMoreThanLimitSuppressedExceptions() {
        final int limit = 10;
        expectedException.expect(new LimitSuppressedExceptionMatcher(limit));
        ExceptionCollector collector = new ExceptionCollector(limit);

        for (int i = 0; i < 2 * limit; ++i) {
            collector.collectException(new RuntimeException());
        }
        collector.close();
    }

    @Test
    public void shouldIgnoreExceptions() {
        try (ExceptionCollector collector = new ExceptionCollector()) {
            collector.execute(() -> {
                        throw new IllegalArgumentException();
                    },
                    null,
                    e -> e instanceof IllegalArgumentException);
        }
    }

    private static final class SuppressedExceptionMatcher extends TypeSafeMatcher<Throwable> {

        private Throwable[] expectedSuppressedExceptions = {SUPPRESSED_EXCEPTION};

        @Override
        protected boolean matchesSafely(Throwable item) {
            return Arrays.equals(item.getSuppressed(), expectedSuppressedExceptions);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("suppressed exceptions ");
            description.appendValue(expectedSuppressedExceptions);
        }

        @Override
        protected void describeMismatchSafely(Throwable item, Description mismatchDescription) {
            mismatchDescription.appendText("was ");
            mismatchDescription.appendValue(item.getSuppressed());
        }
    }

    private static final class LimitSuppressedExceptionMatcher extends TypeSafeMatcher<Throwable> {

        private final int limit;

        private LimitSuppressedExceptionMatcher(int limit) {
            this.limit = limit;
        }

        @Override
        protected boolean matchesSafely(Throwable item) {
            return item.getSuppressed().length <= limit + 1;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("number of suppressed exceptions <= ");
            description.appendValue(limit + 1);
        }

        @Override
        protected void describeMismatchSafely(Throwable item, Description mismatchDescription) {
            mismatchDescription.appendText("was ");
            mismatchDescription.appendValue(item.getSuppressed().length);
        }
    }
}
