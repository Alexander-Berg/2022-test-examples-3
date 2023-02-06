package ru.yandex.market.api.internal.filters;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.junit.Assert;
import ru.yandex.market.api.internal.guruass.FilterControls.FilterControl;
import ru.yandex.market.api.internal.guruass.FilterControls.FilterControlValue;
import ru.yandex.market.api.util.CriterionTestUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Методы проверки для значений контролов фильтров
 * Created by vivg on 20.03.17.
 */
public class FilterControlsAssert {

    private static final Comparator<FilterControlValue> CONTROL_VALUE_COMPARATOR = Ordering
        .from(Comparator.<FilterControlValue, String>comparing(v -> Objects.nonNull(v.getId()) ? v.getId() : ""))
        .compound(Comparator.comparing(FilterControlValue::getName));

    /**
     * Проверка значений в {@link FilterControlValue}
     */
    public static void assertEquals(FilterControlValue v1, FilterControlValue v2) {
        if (v1 == v2) {
            return;
        } else if (null == v1 || null == v2) {
            Assert.fail();
        }
        Assert.assertEquals(v1.getId(), v2.getId());
        Assert.assertEquals(v1.getName(), v2.getName());
        Assert.assertEquals(v1.getGroup(), v2.getGroup());
        Assert.assertEquals(v1.getSelected(), v2.getSelected());
        CriterionTestUtil.assertCriterionEquals(v1.getCriteria(), v2.getCriteria());
    }

    /**
     * Проверка коллекций {@link FilterControlValue}
     */
    public static void assertEquals(Collection<FilterControlValue> expected, Collection<FilterControlValue> actual) {
        List<FilterControlValue> exp = Lists.newArrayList(expected);
        Collections.sort(exp, CONTROL_VALUE_COMPARATOR);

        List<FilterControlValue> act = Lists.newArrayList(actual);
        Collections.sort(act, CONTROL_VALUE_COMPARATOR);

        assertElementsEqual(exp.iterator(), act.iterator(), FilterControlsAssert::assertEquals);
    }

    /**
     * Проверка параметров {@link FilterControl}, за исключением {@link FilterControl#getValues()}
     */
    public static void assertEquals(FilterControl c1, FilterControl c2) {
        if (c1 == c2) {
            return;
        } else if (null == c1 || null == c2) {
            Assert.fail();
        }
        Assert.assertEquals(c1.getId(), c2.getId());
        Assert.assertEquals(c1.getName(), c2.getName());
        Assert.assertEquals(c1.getType(), c2.getType());
    }

    private static <U, V> void assertElementsEqual(Iterator<U> iterator1,
                                                   Iterator<V> iterator2,
                                                   BiConsumer<? super U, ? super V> equalsAsserter) {
        while (iterator1.hasNext()) {
            Assert.assertTrue(iterator2.hasNext());
            equalsAsserter.accept(iterator1.next(), iterator2.next());
        }
        Assert.assertFalse(iterator2.hasNext());
    }
}
