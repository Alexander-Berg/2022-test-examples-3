package ru.yandex.direct.test.utils.differ;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ru.yandex.autotests.irt.testutils.beandiffer2.Diff;
import ru.yandex.autotests.irt.testutils.beandiffer2.differ.AbstractDiffer;

public class BigDecimalDiffer extends AbstractDiffer {

    @Override
    public List<Diff> compare(Object actual, Object expected) {
        List<Diff> result = new ArrayList<>();

        if (actual == null && expected == null) {
            return result;
        }

        if (((BigDecimal) actual).compareTo((BigDecimal) expected) != 0) {
            result.add(Diff.changed(getField(), actual, expected));
        }

        return result;
    }
}
