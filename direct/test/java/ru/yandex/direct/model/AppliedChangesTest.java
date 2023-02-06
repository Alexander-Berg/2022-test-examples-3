package ru.yandex.direct.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

public class AppliedChangesTest {

    private static final Long ID = 1L;
    private static final String OLD_NAME = "old name";
    private static final String NEW_NAME = "new name";
    private static final String NEW_NAME_2 = "new name 2";

    private static final BigDecimal OLD_PRICE = BigDecimal.ONE;
    private static final BigDecimal NEW_PRICE = BigDecimal.TEN;

    // для поля name определяет функцию эквивалентности: оба значения равны null или длины строк равны.
    private static final Map<ModelProperty<? super TestClass, ?>, BiFunction<Object, Object, Boolean>>
            CUSTOM_EQUALS_MAP =
            ImmutableMap.of(TestClass.NAME,
                    (name1, name2) -> (name1 == null && name2 == null) ||
                            (name1 instanceof String && name2 instanceof String
                                    && ((String) name1).length() == ((String) name2).length()));

    // getOldValue

    @Test
    public void getOldValue_NothingIsModified_ReturnsCurrentValue() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);

        String name = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(name, is(OLD_NAME));
    }

    @Test
    public void getOldValue_OtherPropIsModified_ReturnsCurrentValue() {
        TestClass t = new TestClass(ID, OLD_NAME, OLD_PRICE);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.PRICE, NEW_PRICE);

        String name = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(name, is(OLD_NAME));
    }

    @Test
    public void getOldValue_PropIsModified_ReturnsOldValue() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);

        String name = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(name, is(OLD_NAME));
    }

    @Test
    public void getOldValue_PropIsModifiedTwice_ReturnsOldValue() {
        TestClass t = new TestClass(ID, OLD_NAME, OLD_PRICE);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);
        appliedChanges.modify(TestClass.NAME, NEW_NAME_2);

        String name = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(name, is(OLD_NAME));
    }

    @Test
    public void getOldValue_PropIsModifiedFromNull_ReturnsNull() {
        TestClass t = new TestClass(ID, null);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);

        String name = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(name, nullValue());
    }

    @Test
    public void getOldValue_PropIsModifiedFromNullTwice_ReturnsNull() {
        TestClass t = new TestClass(ID, null);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);
        appliedChanges.modify(TestClass.NAME, NEW_NAME_2);

        String name = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(name, nullValue());
    }

    @Test
    public void getOldValue_PropIsModifiedToNull_ReturnsOldValue() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, null);

        String name = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(name, is(OLD_NAME));
    }

    @Test
    public void getOldValue_TwoPropAreModified_BothReturnsOldValues() {
        TestClass t = new TestClass(ID, OLD_NAME, OLD_PRICE);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);
        appliedChanges.modify(TestClass.PRICE, NEW_PRICE);

        String name = appliedChanges.getOldValue(TestClass.NAME);
        BigDecimal price = appliedChanges.getOldValue(TestClass.PRICE);
        assertThat(name, is(OLD_NAME));
        assertThat(price, is(OLD_PRICE));
    }

    @Test
    public void getOldValue_PropIsModifiedOnceWithInitialValue_ReturnsCurrentValue() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        //noinspection RedundantStringConstructorCall
        appliedChanges.modify(TestClass.NAME, new String(OLD_NAME));

        String name = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(name, sameInstance(OLD_NAME));
    }

    @Test
    public void getOldValue_PropIsModifiedAndThenModifiedToInitialValue_ReturnsInitialValue() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);
        //noinspection RedundantStringConstructorCall
        appliedChanges.modify(TestClass.NAME, new String(OLD_NAME));

        String name = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(name, sameInstance(OLD_NAME));
    }

    @Test // проверяем, что первоначальные значения не затираются при повторном изменении
    public void getOldValue_PropIsModifiedTwiceAndThenModifiedToInitialValue_ReturnsInitialValue() {
        String oldName = "old name";
        String newName1 = "new name 1";
        String newName2 = "new name 2";
        TestClass t = new TestClass(ID, oldName);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, newName1);
        appliedChanges.modify(TestClass.NAME, newName2);
        //noinspection RedundantStringConstructorCall
        appliedChanges.modify(TestClass.NAME, new String(OLD_NAME));

        String name = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(name, sameInstance(oldName));
    }

    // getOldValue for BigDecimal

    @Test
    public void getOldValue_BigDecimalPropIsNotModified_ReturnsCurrentValue() {
        BigDecimal oldPrice = BigDecimal.TEN.setScale(2, RoundingMode.DOWN);
        BigDecimal newPrice = BigDecimal.TEN.setScale(1, RoundingMode.DOWN);
        TestClass t = new TestClass(ID, OLD_NAME, oldPrice);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.PRICE, newPrice);

        BigDecimal name = appliedChanges.getOldValue(TestClass.PRICE);
        assertThat(name, sameInstance(oldPrice));
    }

    @Test
    public void getOldValue_BigDecimalPropIsModified_ReturnsOldValue() {
        TestClass t = new TestClass(ID, OLD_NAME, OLD_PRICE);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.PRICE, NEW_PRICE);

        BigDecimal name = appliedChanges.getOldValue(TestClass.PRICE);
        assertThat(name, sameInstance(OLD_PRICE));
    }

    @Test
    public void getOldValue_BigDecimalPropIsModifiedFromNull_ReturnsNull() {
        TestClass t = new TestClass(ID, OLD_NAME, null);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.PRICE, NEW_PRICE);

        BigDecimal name = appliedChanges.getOldValue(TestClass.PRICE);
        assertThat(name, nullValue());
    }

    @Test
    public void getOldValue_BigDecimalPropIsModifiedToNull_ReturnsOldValue() {
        TestClass t = new TestClass(ID, OLD_NAME, OLD_PRICE);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.PRICE, null);

        BigDecimal name = appliedChanges.getOldValue(TestClass.PRICE);
        assertThat(name, sameInstance(OLD_PRICE));
    }

    @Test
    public void getOldValue_BigDecimalPropIsModifiedAndThenModifiedToValueSameAsInitial_ReturnsInitialValue() {
        BigDecimal oldPrice = BigDecimal.TEN.setScale(2, RoundingMode.DOWN);
        BigDecimal newPrice = BigDecimal.ONE.setScale(1, RoundingMode.DOWN);
        BigDecimal newPrice2AsOld = BigDecimal.TEN.setScale(1, RoundingMode.DOWN);
        TestClass t = new TestClass(ID, OLD_NAME, oldPrice);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.PRICE, newPrice);
        appliedChanges.modify(TestClass.PRICE, newPrice2AsOld);

        BigDecimal name = appliedChanges.getOldValue(TestClass.PRICE);
        assertThat(name, sameInstance(oldPrice));
    }

    // getOldValue for custom equals function

    @Test
    public void getOldValue_PropIsNotModifiedByCustomEquals_ReturnsCurrentValue() {
        String oldName = "old";
        String newName = "new";
        TestClass t = new TestClass(ID, oldName);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t, emptySet(), CUSTOM_EQUALS_MAP);
        appliedChanges.modify(TestClass.NAME, newName);

        String name = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(name, is(oldName));
    }

    @Test
    public void getOldValue_PropIsModifiedByCustomEquals_ReturnsOldValue() {
        String oldName = "old";
        String newName = "new1";
        TestClass t = new TestClass(ID, oldName);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t, emptySet(), CUSTOM_EQUALS_MAP);
        appliedChanges.modify(TestClass.NAME, newName);

        String name = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(name, is(oldName));
    }

    @Test
    public void getOldValue_PropIsModifiedTwiceByCustomEquals_ReturnsInitialValue() {
        String oldName = "old";
        String newName = "new1";
        String newName2 = "new123";
        TestClass t = new TestClass(ID, oldName);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t, emptySet(), CUSTOM_EQUALS_MAP);
        appliedChanges.modify(TestClass.NAME, newName);
        appliedChanges.modify(TestClass.NAME, newName2);

        String name = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(name, is(oldName));
    }

    @Test // проверяем, что повторные изменения не перетирают исходное значение
    public void getOldValue_PropIsModifiedTwiceAndThenModifiedToValueSameAsInitialByCustomEquals_ReturnsInitialValue() {
        String oldName = "old name";
        String newName1 = "new name 1";
        String newName2 = "new name 123";
        String newName3SameAsOld = "new name";
        TestClass t = new TestClass(ID, oldName);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t, emptySet(), CUSTOM_EQUALS_MAP);
        appliedChanges.modify(TestClass.NAME, newName1);
        appliedChanges.modify(TestClass.NAME, newName2);
        appliedChanges.modify(TestClass.NAME, newName3SameAsOld);

        String name = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(name, sameInstance(oldName));
    }

    // getNewValue

    @Test
    public void getNewValue_NothingIsModified_ReturnsCurrentValue() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);

        String name = appliedChanges.getNewValue(TestClass.NAME);
        assertThat(name, is(OLD_NAME));
    }

    @Test
    public void getNewValue_OtherPropIsModified_ReturnsCurrentValue() {
        TestClass t = new TestClass(ID, OLD_NAME, OLD_PRICE);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.PRICE, NEW_PRICE);

        String name = appliedChanges.getNewValue(TestClass.NAME);
        assertThat(name, is(OLD_NAME));
    }

    @Test
    public void getNewValue_PropIsModified_ReturnsNewValue() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);

        String name = appliedChanges.getNewValue(TestClass.NAME);
        assertThat(name, is(NEW_NAME));
    }

    @Test
    public void getNewValue_PropIsModifiedFromNull_ReturnsNewValue() {
        TestClass t = new TestClass(ID, null);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);

        String name = appliedChanges.getNewValue(TestClass.NAME);
        assertThat(name, is(NEW_NAME));
    }

    @Test
    public void getNewValue_PropIsModifiedToNull_ReturnsNull() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, null);

        String name = appliedChanges.getNewValue(TestClass.NAME);
        assertThat(name, nullValue());
    }

    @Test
    public void getNewValue_TwoPropAreModified_BothReturnsNewValues() {
        TestClass t = new TestClass(ID, OLD_NAME, OLD_PRICE);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);
        appliedChanges.modify(TestClass.PRICE, NEW_PRICE);

        String name = appliedChanges.getNewValue(TestClass.NAME);
        BigDecimal price = appliedChanges.getNewValue(TestClass.PRICE);
        assertThat(name, is(NEW_NAME));
        assertThat(price, is(NEW_PRICE));
    }

    @Test
    public void getNewValue_PropIsModifiedTwice_ReturnsLatestValue() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);
        appliedChanges.modify(TestClass.NAME, NEW_NAME_2);

        String name = appliedChanges.getNewValue(TestClass.NAME);
        assertThat(name, is(NEW_NAME_2));
    }

    @Test
    public void getNewValue_PropIsModifiedOnceWithInitialValue_ReturnsInitialValue() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        //noinspection RedundantStringConstructorCall
        appliedChanges.modify(TestClass.NAME, new String(OLD_NAME));

        String name = appliedChanges.getNewValue(TestClass.NAME);
        assertThat(name, sameInstance(OLD_NAME));
    }

    @Test
    public void getNewValue_PropIsModifiedAndThenModifiedWithInitialValue_ReturnsInitialValue() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);
        //noinspection RedundantStringConstructorCall
        appliedChanges.modify(TestClass.NAME, new String(OLD_NAME));

        String name = appliedChanges.getNewValue(TestClass.NAME);
        assertThat(name, sameInstance(OLD_NAME));
    }

    // getNewValue for BigDecimal

    @Test
    public void getNewValue_BigDecimalPropIsNotModifiedByCustomEquals_ReturnsCurrentValue() {
        BigDecimal oldPrice = BigDecimal.TEN.setScale(2, RoundingMode.DOWN);
        BigDecimal newPrice = BigDecimal.TEN.setScale(1, RoundingMode.DOWN);
        TestClass t = new TestClass(ID, OLD_NAME, oldPrice);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.PRICE, newPrice);

        BigDecimal name = appliedChanges.getNewValue(TestClass.PRICE);
        assertThat(name, sameInstance(oldPrice));
    }

    @Test
    public void getNewValue_BigDecimalPropIsModified_ReturnsNewValue() {
        TestClass t = new TestClass(ID, OLD_NAME, OLD_PRICE);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.PRICE, NEW_PRICE);

        BigDecimal name = appliedChanges.getNewValue(TestClass.PRICE);
        assertThat(name, is(NEW_PRICE));
    }

    @Test
    public void getNewValue_BigDecimalPropIsModifiedFromNull_ReturnsNewValue() {
        TestClass t = new TestClass(ID, OLD_NAME, null);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.PRICE, NEW_PRICE);

        BigDecimal name = appliedChanges.getNewValue(TestClass.PRICE);
        assertThat(name, is(NEW_PRICE));
    }

    @Test
    public void getNewValue_BigDecimalPropIsModifiedToNull_ReturnsNull() {
        TestClass t = new TestClass(ID, OLD_NAME, OLD_PRICE);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.PRICE, null);

        BigDecimal name = appliedChanges.getNewValue(TestClass.PRICE);
        assertThat(name, nullValue());
    }

    @Test
    public void getNewValue_BigDecimalPropIsModifiedAndThenModifiedToValueSameAsInitial_ReturnsInitialValue() {
        BigDecimal oldPrice = BigDecimal.TEN.setScale(2, RoundingMode.DOWN);
        BigDecimal newPrice = BigDecimal.ONE.setScale(1, RoundingMode.DOWN);
        BigDecimal newPrice2AsOld = BigDecimal.TEN.setScale(1, RoundingMode.DOWN);
        TestClass t = new TestClass(ID, OLD_NAME, oldPrice);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.PRICE, newPrice);
        appliedChanges.modify(TestClass.PRICE, newPrice2AsOld);

        BigDecimal name = appliedChanges.getNewValue(TestClass.PRICE);
        assertThat(name, sameInstance(oldPrice));
    }

    // getNewValue for custom equals function

    @Test
    public void getNewValue_PropIsNotModifiedByCustomEquals_ReturnsCurrentValue() {
        String oldName = "old";
        String newName = "new";
        TestClass t = new TestClass(ID, oldName);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t, emptySet(), CUSTOM_EQUALS_MAP);
        appliedChanges.modify(TestClass.NAME, newName);

        String name = appliedChanges.getNewValue(TestClass.NAME);
        assertThat(name, is(oldName));
    }

    @Test
    public void getNewValue_PropIsModifiedByCustomEquals_ReturnsNewValue() {
        String oldName = "old";
        String newName = "new1";
        TestClass t = new TestClass(ID, oldName);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t, emptySet(), CUSTOM_EQUALS_MAP);
        appliedChanges.modify(TestClass.NAME, newName);

        String name = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(name, sameInstance(oldName));
    }

    @Test
    public void getOldValue_PropIsModifiedTwiceByCustomEquals_ReturnsLatestValue() {
        String oldName = "old";
        String newName = "new1";
        String newName2 = "new123";
        TestClass t = new TestClass(ID, oldName);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t, emptySet(), CUSTOM_EQUALS_MAP);
        appliedChanges.modify(TestClass.NAME, newName);
        appliedChanges.modify(TestClass.NAME, newName2);

        String name = appliedChanges.getNewValue(TestClass.NAME);
        assertThat(name, is(newName2));
    }

    @Test
    public void getOldValue_PropIsModifiedThanModifiedToValueSameAsInitialByCustomEquals_ReturnsInitialValue() {
        String oldName = "old";
        String newName = "new1";
        String newName2 = "new";
        TestClass t = new TestClass(ID, oldName);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t, emptySet(), CUSTOM_EQUALS_MAP);
        appliedChanges.modify(TestClass.NAME, newName);
        appliedChanges.modify(TestClass.NAME, newName2);

        String name = appliedChanges.getNewValue(TestClass.NAME);
        assertThat(name, sameInstance(oldName));
    }

    // getActuallyChangedProps

    @Test
    public void getActuallyChangedProps_NothingIsModified_ReturnsEmptySet() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);

        Set<ModelProperty<? super TestClass, ?>> changedProps =
                appliedChanges.getActuallyChangedProps();
        assertThat(changedProps, emptyIterable());
    }

    @Test
    public void getActuallyChangedProps_OnePropIsModifiedWithInitialValue_ReturnsEmptySet() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        //noinspection RedundantStringConstructorCall
        appliedChanges.modify(TestClass.NAME, new String(OLD_NAME));

        Set<ModelProperty<? super TestClass, ?>> changedProps =
                appliedChanges.getActuallyChangedProps();
        assertThat(changedProps, emptyIterable());
    }

    @Test
    public void getActuallyChangedProps_OnePropIsModified_ReturnsOneItem() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);

        Set<ModelProperty<? super TestClass, ?>> changedProps =
                appliedChanges.getActuallyChangedProps();
        assertThat(changedProps, contains(sameInstance(TestClass.NAME)));
    }

    @Test
    public void getActuallyChangedProps_OnePropIsModifiedFromNull_ReturnsOneItem() {
        TestClass t = new TestClass(ID, null);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);

        Set<ModelProperty<? super TestClass, ?>> changedProps =
                appliedChanges.getActuallyChangedProps();
        assertThat(changedProps, contains(sameInstance(TestClass.NAME)));
    }

    @Test
    public void getActuallyChangedProps_OnePropIsModifiedToNull_ReturnsOneItem() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, null);

        Set<ModelProperty<? super TestClass, ?>> changedProps =
                appliedChanges.getActuallyChangedProps();
        assertThat(changedProps, contains(sameInstance(TestClass.NAME)));
    }

    @Test
    public void getActuallyChangedProps_TwoPropsAreModifiedIncludingBigDecimal_ReturnsTwoItems() {
        TestClass t = new TestClass(ID, OLD_NAME, OLD_PRICE);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);
        appliedChanges.modify(TestClass.PRICE, NEW_PRICE);

        Set<ModelProperty<? super TestClass, ?>> changedProps =
                appliedChanges.getActuallyChangedProps();
        assertThat(changedProps,
                containsInAnyOrder(sameInstance(TestClass.NAME), sameInstance(TestClass.PRICE)));
    }

    @Test
    public void getActuallyChangedProps_OnePropIsModifiedAndThenModifiedWithInitialValue_ReturnsEmptySet() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);
        //noinspection RedundantStringConstructorCall
        appliedChanges.modify(TestClass.NAME, new String(OLD_NAME));

        Set<ModelProperty<? super TestClass, ?>> changedProps =
                appliedChanges.getActuallyChangedProps();
        assertThat(changedProps, emptyIterable());
    }

    @Test
    public void getActuallyChangedProps_TwoPropsAreModifiedAndThenOneIsModifiedWithInitialValue_ReturnsOneItem() {
        TestClass t = new TestClass(ID, OLD_NAME, OLD_PRICE);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);
        appliedChanges.modify(TestClass.PRICE, NEW_PRICE);
        //noinspection RedundantStringConstructorCall
        appliedChanges.modify(TestClass.NAME, new String(OLD_NAME));

        Set<ModelProperty<? super TestClass, ?>> changedProps =
                appliedChanges.getActuallyChangedProps();
        assertThat(changedProps, contains(sameInstance(TestClass.PRICE)));
    }

    // getActuallyChangedProps for BigDecimal

    @Test
    public void getActuallyChangedProps_BigDecimalPropIsModifiedWithValueSameAsInitial_ReturnsEmptySet() {
        BigDecimal oldPrice = BigDecimal.TEN.setScale(2, RoundingMode.DOWN);
        BigDecimal newPrice = BigDecimal.TEN.setScale(1, RoundingMode.DOWN);
        TestClass t = new TestClass(ID, OLD_NAME, oldPrice);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.PRICE, newPrice);

        Set<ModelProperty<? super TestClass, ?>> changedProps =
                appliedChanges.getActuallyChangedProps();
        assertThat(changedProps, emptyIterable());
    }

    @Test
    public void getActuallyChangedProps_BigDecimalPropIsModified_ReturnsOneItem() {
        TestClass t = new TestClass(ID, OLD_NAME, OLD_PRICE);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.PRICE, NEW_PRICE);

        Set<ModelProperty<? super TestClass, ?>> changedProps =
                appliedChanges.getActuallyChangedProps();
        assertThat(changedProps, contains(sameInstance(TestClass.PRICE)));
    }

    // getActuallyChangedProps for custom equals

    @Test
    public void getActuallyChangedProps_PropIsNotModifiedByCustomEquals_ReturnsEmptySet() {
        String oldName = "old";
        String newName = "new";
        TestClass t = new TestClass(ID, oldName);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t, emptySet(), CUSTOM_EQUALS_MAP);
        appliedChanges.modify(TestClass.NAME, newName);

        Set<ModelProperty<? super TestClass, ?>> changedProps =
                appliedChanges.getActuallyChangedProps();
        assertThat(changedProps, emptyIterable());
    }

    @Test
    public void getActuallyChangedProps_PropIsModifiedAndThenModifiedToInitialValueByCustomEquals_ReturnsEmptySet() {
        String oldName = "old";
        String newName1 = "new";
        String newName2 = "123";
        TestClass t = new TestClass(ID, oldName);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t, emptySet(), CUSTOM_EQUALS_MAP);
        appliedChanges.modify(TestClass.NAME, newName1);
        appliedChanges.modify(TestClass.NAME, newName2);

        Set<ModelProperty<? super TestClass, ?>> changedProps =
                appliedChanges.getActuallyChangedProps();
        assertThat(changedProps, emptyIterable());
    }

    // changed

    @Test
    public void changed_NothingIsModified_ReturnsFalse() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);

        boolean changed = appliedChanges.changed(TestClass.NAME);
        assertThat(changed, is(false));
    }

    @Test
    public void changed_OtherPropIsModified_ReturnsFalse() {
        TestClass t = new TestClass(ID, OLD_NAME, OLD_PRICE);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.PRICE, NEW_PRICE);

        boolean changed = appliedChanges.changed(TestClass.NAME);
        assertThat(changed, is(false));
    }

    @Test
    public void changed_PropIsModified_ReturnsTrue() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);

        boolean changed = appliedChanges.changed(TestClass.NAME);
        assertThat(changed, is(true));
    }

    @Test
    public void changed_PropIsModifiedFromNull_ReturnsTrue() {
        TestClass t = new TestClass(ID, null);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);

        boolean changed = appliedChanges.changed(TestClass.NAME);
        assertThat(changed, is(true));
    }

    @Test
    public void changed_PropIsModifiedToNull_ReturnsTrue() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, null);

        boolean changed = appliedChanges.changed(TestClass.NAME);
        assertThat(changed, is(true));
    }

    @Test
    public void changed_TwoPropsAreModified_ReturnsTrueForBothProps() {
        TestClass t = new TestClass(ID, OLD_NAME, OLD_PRICE);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, null);
        appliedChanges.modify(TestClass.PRICE, NEW_PRICE);

        boolean nameChanged = appliedChanges.changed(TestClass.NAME);
        boolean priceChanged = appliedChanges.changed(TestClass.PRICE);
        assertThat(nameChanged, is(true));
        assertThat(priceChanged, is(true));
    }

    // deleted

    @Test
    public void deleted_NothingIsModified_ReturnsFalse() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);

        boolean deleted = appliedChanges.deleted(TestClass.NAME);
        assertThat(deleted, is(false));
    }

    @Test
    public void deleted_PropIsModified_ReturnsFalse() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);

        boolean deleted = appliedChanges.deleted(TestClass.NAME);
        assertThat(deleted, is(false));
    }

    @Test
    public void deleted_PropIsModifiedFromNull_ReturnsFalse() {
        TestClass t = new TestClass(ID, null);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);

        boolean deleted = appliedChanges.deleted(TestClass.NAME);
        assertThat(deleted, is(false));
    }

    @Test
    public void deleted_PropIsModifiedToNull_ReturnsTrue() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, null);

        boolean deleted = appliedChanges.deleted(TestClass.NAME);
        assertThat(deleted, is(true));
    }

    // changedAndNotDeleted

    @Test
    public void changedAndNotDeleted_NothingIsModified_ReturnsFalse() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);

        boolean changedAndNotDeleted = appliedChanges.changedAndNotDeleted(TestClass.NAME);
        assertThat(changedAndNotDeleted, is(false));
    }

    @Test
    public void deleted_PropIsModified_ReturnsTrue() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);

        boolean changedAndNotDeleted = appliedChanges.changedAndNotDeleted(TestClass.NAME);
        assertThat(changedAndNotDeleted, is(true));
    }

    @Test
    public void changedAndNotDeleted_PropIsModifiedFromNull_ReturnsTrue() {
        TestClass t = new TestClass(ID, null);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);

        boolean changedAndNotDeleted = appliedChanges.changedAndNotDeleted(TestClass.NAME);
        assertThat(changedAndNotDeleted, is(true));
    }

    @Test
    public void changedAndNotDeleted_PropIsModifiedToNull_ReturnsFalse() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, null);

        boolean changedAndNotDeleted = appliedChanges.changedAndNotDeleted(TestClass.NAME);
        assertThat(changedAndNotDeleted, is(false));
    }


    // tests with ModelChanges

    @Test
    public void noChangedPropertiesIfNoModifications() {
        TestClass t = new TestClass(1L, "old name");
        ModelChanges<TestClass> changes = testClassModelChanges();
        changes.processNotNull("old name", TestClass.NAME);

        AppliedChanges<TestClass> appliedChanges = changes.applyTo(t);

        Set<ModelProperty<? super TestClass, ?>> actual = appliedChanges.getActuallyChangedProps();
        assertThat(actual, empty());
    }

    @Test
    public void noChangedPropertiesIfNoModificationsWithBigDecimalProperties() {
        BigDecimal priceScaleOne = BigDecimal.valueOf(6, 1);
        BigDecimal priceScaleTwo = BigDecimal.valueOf(60, 2);

        // prerequisites: цены не равны по equals, но равны по compareTo
        checkState(!priceScaleOne.equals(priceScaleTwo));
        checkState(priceScaleOne.compareTo(priceScaleTwo) == 0);

        TestClass t = new TestClass(1L, "name", priceScaleOne);

        ModelChanges<TestClass> changes = testClassModelChanges();
        changes.processNotNull(priceScaleTwo, TestClass.PRICE);

        AppliedChanges<TestClass> appliedChanges = changes.applyTo(t);

        Collection<ModelProperty<? super TestClass, ?>> actual = appliedChanges.getActuallyChangedProps();
        assertThat(actual, empty());
    }

    @Test
    public void valueIsModifiedAfterChangeAppliance() {
        TestClass t = new TestClass(1L, "old name");
        ModelChanges<TestClass> changes = testClassModelChanges();
        changes.processNotNull("new name", TestClass.NAME);

        AppliedChanges<TestClass> appliedChanges = changes.applyTo(t);

        String actual = TestClass.NAME.get(appliedChanges.getModel());
        assertThat(actual, is("new name"));
    }

    @Test
    public void oldValueIsAvailableAfterChangeAppliance() {
        TestClass t = new TestClass(1L, "old name");
        ModelChanges<TestClass> changes = testClassModelChanges();
        changes.processNotNull("new name", TestClass.NAME);

        AppliedChanges<TestClass> appliedChanges = changes.applyTo(t);

        String actual = appliedChanges.getOldValue(TestClass.NAME);
        assertThat(actual, is("old name"));
    }

    // getPropertiesForUpdate

    @Test
    public void sensitivePropertyIsPassedToUpdate() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t, singleton(TestClass.NAME));
        appliedChanges.modify(TestClass.NAME, OLD_NAME);
        assertThat(appliedChanges.getPropertiesForUpdate(), contains(TestClass.NAME));
    }

    @Test
    public void notSensitivePropertyPassedToUpdate() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);
        assertThat(appliedChanges.getPropertiesForUpdate(), contains(TestClass.NAME));
    }

    @Test
    public void notSensitivePropertyPassedToUpdateAndReverted() {
        TestClass t = new TestClass(ID, OLD_NAME);
        AppliedChanges<TestClass> appliedChanges = new AppliedChanges<>(t);
        appliedChanges.modify(TestClass.NAME, NEW_NAME);
        appliedChanges.modify(TestClass.NAME, OLD_NAME);
        assertThat(appliedChanges.getPropertiesForUpdate(), empty());
    }

    private static ModelChanges<TestClass> testClassModelChanges() {
        return new ModelChanges<>(ID, TestClass.class);
    }
}
