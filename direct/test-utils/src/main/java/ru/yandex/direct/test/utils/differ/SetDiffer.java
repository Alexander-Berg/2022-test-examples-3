package ru.yandex.direct.test.utils.differ;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.autotests.irt.testutils.beandiffer2.Diff;
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanField;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.differ.AbstractDiffer;
import ru.yandex.autotests.irt.testutils.beandiffer2.differ.ListDiffer;

public class SetDiffer extends AbstractDiffer {
    public SetDiffer(BeanField field, CompareStrategy compareStrategy) {
        super(field, compareStrategy);
    }

    public SetDiffer() {
    }

    @Override
    public List<Diff> compare(Object actual, Object expected) {
        List<Object> actualList = setToSortedList((Set<?>) actual);
        List<Object> expectedList = setToSortedList((Set<?>) expected);
        return new ListDiffer(getField(), getCompareStrategy()).compare(actualList, expectedList);
    }

    private static List<Object> setToSortedList(Set<?> set) {
        return set.stream().sorted().collect(Collectors.toList());
    }
}
