package ru.yandex.direct.validation.constraint;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class UniqueItemsConstraintTest {
    @Test
    public void apply_Zero() {
        ImmutableList<String> uniqueLabels = ImmutableList.of();
        UniqueItemsConstraint<String, String, Boolean> uniqueItemsConstraint = new UniqueItemsConstraint<>(
                s -> s, () -> true
        );
        assertThat("в пустом списке не найдено дублирующих элементов",
                uniqueItemsConstraint.apply(uniqueLabels), not(hasValue(true)));
    }

    @Test
    public void apply_Unique() {
        ImmutableList<String> uniqueLabels = ImmutableList.of(
                "red",
                "orange",
                "yellow",
                "green",
                "blue",
                "purple");
        UniqueItemsConstraint<String, String, Boolean> uniqueItemsConstraint = new UniqueItemsConstraint<>(
                s -> s, () -> true
        );
        assertThat("в списке уникальных элементов нет дубликатов",
                uniqueItemsConstraint.apply(uniqueLabels), not(hasValue(true)));
    }

    @Test
    public void apply_NonUnique() {
        ImmutableList<String> nonUniqueLabels = ImmutableList.of(
                "red",
                "red",
                "orange",
                "yellow",
                "green",
                "blue",
                "purple");
        UniqueItemsConstraint<String, String, Boolean> uniqueItemsConstraint = new UniqueItemsConstraint<>(
                s -> s, () -> true
        );
        assertThat("в списке неуникальных элементов нашёлся дубликат",
                uniqueItemsConstraint.apply(nonUniqueLabels), hasEntry(1, true));
    }
}
