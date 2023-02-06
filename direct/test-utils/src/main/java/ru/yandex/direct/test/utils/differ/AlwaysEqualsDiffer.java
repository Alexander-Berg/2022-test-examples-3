package ru.yandex.direct.test.utils.differ;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.autotests.irt.testutils.beandiffer2.Diff;
import ru.yandex.autotests.irt.testutils.beandiffer2.differ.AbstractDiffer;

@ParametersAreNonnullByDefault
public class AlwaysEqualsDiffer extends AbstractDiffer {
    @Override
    public List<Diff> compare(Object actual, Object expected) {
        return new ArrayList<>();
    }
}
