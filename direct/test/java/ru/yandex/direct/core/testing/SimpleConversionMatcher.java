package ru.yandex.direct.core.testing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import one.util.streamex.StreamEx;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import ru.yandex.direct.utils.JsonUtils;

import static ru.yandex.direct.core.testing.CloneTestUtil.fillIgnoring;
import static ru.yandex.direct.core.testing.CloneTestUtil.get;
import static ru.yandex.direct.core.testing.CloneTestUtil.getAllFields;

/**
 * Заполняет поля исходного объекта рандомными значениями,
 * конвертирует объект с помощью переданной функции конвертации,
 * затем проверяет, что в полученном объекте встречаются значения из исходного объекта.
 * <p>
 * Умеет заполнять только поля примитивных типов, остальные должны быть явно проигнорированы.
 */
public class SimpleConversionMatcher<I, O, F extends Function<I, O>> extends BaseMatcher<F> {

    private final I inputItem;
    private final Set<String> ignoredFields;
    private final Function<O, Object> resultItemGetter;

    private List<Object> expectedValues;
    private List<Object> unexistingValues;

    public SimpleConversionMatcher(I inputItem, Set<String> ignoredFields,
                                   Function<O, Object> resultItemGetter) {
        this.inputItem = inputItem;
        this.ignoredFields = ignoredFields;
        this.resultItemGetter = resultItemGetter;
    }

    public static <I, O, F extends Function<I, O>> SimpleConversionMatcher<I, O, F> converts(
            I inputItem, Set<String> ignoredFields, Function<O, Object> resultItemGetter) {
        return new SimpleConversionMatcher<>(inputItem, ignoredFields, resultItemGetter);
    }

    public static <I, O, F extends Function<I, O>> SimpleConversionMatcher<I, O, F> converts(
            I inputItem, Set<String> ignoredFields) {
        return new SimpleConversionMatcher<>(inputItem, ignoredFields, o -> o);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean matches(Object function) {
        expectedValues = fillIgnoring(inputItem, ignoredFields);
        O convertedItem = ((Function<I, O>) function).apply(inputItem);
        Object resultItem = resultItemGetter.apply(convertedItem);
        unexistingValues = getUnexistingValues(resultItem, expectedValues);
        return unexistingValues.isEmpty();
    }

    @Override
    public void describeMismatch(Object item, Description mismatchDescription) {
        mismatchDescription.appendText("result object doesn't contain next values:")
                .appendText(JsonUtils.toJson(unexistingValues));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("result object contains values after conversion:")
                .appendText(JsonUtils.toJson(expectedValues));
    }

    private static List<Object> getUnexistingValues(Object object, List<Object> expectedValues) {
        List<Object> expectedValuesInternal = new ArrayList<>(expectedValues);
        List<Object> actualValues = StreamEx.of(getAllFields(object.getClass()))
                .map(field -> get(field, object))
                .toList();
        List<Object> unexistingValues = new ArrayList<>(expectedValuesInternal.size());

        outer:
        for (Object expectedValue : expectedValuesInternal) {
            for (Iterator<Object> iterator = actualValues.iterator(); iterator.hasNext(); ) {
                Object actualValue = iterator.next();
                if (actualValue == expectedValue) {
                    iterator.remove();
                    continue outer;
                }
            }
            unexistingValues.add(expectedValue);
        }

        return unexistingValues;
    }
}
