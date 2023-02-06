package ru.yandex.direct.test.utils.differ;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import ru.yandex.autotests.irt.testutils.beandiffer2.Diff;
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanField;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.differ.AbstractDiffer;

/**
 * Сравнивает даты с точностью до секунды (допустима разница в 1 секунду)
 */
public class LocalDateSecondsPrecisionDiffer extends AbstractDiffer {
    public LocalDateSecondsPrecisionDiffer(BeanField field, CompareStrategy strategy) {
        super(field, strategy);
    }

    public LocalDateSecondsPrecisionDiffer() {
    }

    @Override
    public List<Diff> compare(Object actual, Object expected) {
        List<Diff> result = new ArrayList<>();

        if (actual == null && expected == null) {
            return result;
        }

        if (actual == null || expected == null) {
            result.add(Diff.changed(getField(), actual, expected));
            return result;
        }
        LocalDateTime actualDateTime = ((LocalDateTime) actual).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime expectedDateTime = ((LocalDateTime) expected).truncatedTo(ChronoUnit.SECONDS);
        // При записи в базу наносекунды округляются, могут изменить секунду на 1
        if (Math.abs(ChronoUnit.SECONDS.between(actualDateTime, expectedDateTime)) > 1) {
            result.add(Diff.changed(getField(), actual, expected));
        }

        return result;
    }
}
