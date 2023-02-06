package ru.yandex.direct.api.v5.ws;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import ru.yandex.direct.core.TranslatableException;

/**
 * Сравнивает {@link TranslatableException}
 */
public class TranslatableExceptionMatcher extends TypeSafeMatcher<TranslatableException> {
    private TranslatableException exc;

    public TranslatableExceptionMatcher(TranslatableException exc) {
        this.exc = exc;
    }

    @Override
    protected boolean matchesSafely(TranslatableException item) {
        return exc.getCode() == item.getCode() && exc.getShortMessage().equals(item.getShortMessage())
                && exc.getDetailedMessage() != null && exc.getDetailedMessage().equals(item.getDetailedMessage());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("expected ").appendValue(exc.toString());
    }
}
