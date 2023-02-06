package ru.yandex.direct.excel.processing.utils;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;

import ru.yandex.autotests.irt.testutils.beandiffer2.Diff;
import ru.yandex.autotests.irt.testutils.beandiffer2.differ.AbstractDiffer;

/**
 * Диффер проверяет, что если значения actual != expected, то значения actual является валидным id
 * Ожидаемый тип даннных: String или Number
 */
@ParametersAreNonnullByDefault
public class ChangeableObjectIdDiffer extends AbstractDiffer {

    @Override
    public List<Diff> compare(@Nullable Object actual, @Nullable Object expected) {
        List<Diff> result = new ArrayList<>();

        if (actual == null && expected == null) {
            return result;
        }

        if (actual instanceof String && expected instanceof String
                && StringUtils.equals((String) actual, (String) expected)) {
            return result;
        }

        if (actual == null || !isValidId(actual)) {
            result.add(Diff.changed(getField(), actual, expected));
        }

        return result;
    }

    private static boolean isValidId(Object object) {
        long value = 0;
        if (object instanceof String) {
            try {
                value = Long.parseLong((String) object);
            } catch (NumberFormatException e) {
                //ignore
            }
        } else if (object instanceof Number) {
            value = ((Number) object).longValue();
        }

        return value > 0;
    }
}
