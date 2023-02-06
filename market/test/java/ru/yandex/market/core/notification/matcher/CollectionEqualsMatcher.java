package ru.yandex.market.core.notification.matcher;

import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;

/**
 * {@link ArgumentMatcher} для проверки что две коллекции поэлементно совпадают.
 * Не учитывается порядок элементов.
 *
 * @author Vladislav Bauer
 */
public class CollectionEqualsMatcher<T> extends BaseMatcher<Collection<T>> {

    private final Collection<T> expected;

    private CollectionEqualsMatcher(final Collection<T> expected) {
        this.expected = expected;
    }

    /**
     * Создает матчер, который матчится, если проверяемая коллекция логически эквивалентна предоставляемой коллекции
     * <code>operand</code>.
     * <br/>
     * Пример:
     * <pre>
     * assertThat(Arrays.asList(1, 2, 3), equalToCollection(Stream.of(1, 2, 3).collect(Collectors.toSet())));
     * </pre>
     */
    public static <T> CollectionEqualsMatcher<T> equalToCollection(final Collection<T> operand) {
        return new CollectionEqualsMatcher<>(operand);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final Object argument) {
        return argument instanceof Collection
                && CollectionUtils.isEqualCollection(expected, (Collection) argument);
    }

    @Override
    public void describeTo(Description description) {
    }

}
