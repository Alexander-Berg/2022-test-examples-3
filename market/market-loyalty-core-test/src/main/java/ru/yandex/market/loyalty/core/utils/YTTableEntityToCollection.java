package ru.yandex.market.loyalty.core.utils;

import org.apache.commons.collections4.IteratorUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import ru.yandex.qe.yt.cypress.entities.YTTableEntity;

import static ru.yandex.market.loyalty.lightweight.ExceptionUtils.makeExceptionsUnchecked;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class YTTableEntityToCollection<T, Y extends YTTableEntity<T>> extends TypeSafeDiagnosingMatcher<Y> {
    private final Matcher<Object> valueMatcher;

    public YTTableEntityToCollection(Matcher<? extends Iterable<? extends T>> valueMatcher) {
        this.valueMatcher = nastyGenericsWorkaround(valueMatcher);
    }

    @Override
    public boolean matchesSafely(Y bean, Description mismatch) {
        return makeExceptionsUnchecked(() -> valueMatcher.matches(IteratorUtils.toList(bean.getIterator())));
    }

    @Override
    public void describeTo(Description description) {
        description.appendDescriptionOf(valueMatcher);
    }

    @SuppressWarnings("unchecked")
    private static Matcher<Object> nastyGenericsWorkaround(Matcher<?> valueMatcher) {
        return (Matcher<Object>) valueMatcher;
    }

    public static <T> Matcher<YTTableEntity<T>> ytTableEntityToIterable(Matcher<? extends Iterable<? extends T>> valueMatcher) {
        return new YTTableEntityToCollection<>(valueMatcher);
    }
}
