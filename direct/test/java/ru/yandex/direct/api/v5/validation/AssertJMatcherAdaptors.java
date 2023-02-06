package ru.yandex.direct.api.v5.validation;

import org.assertj.core.api.Condition;
import org.hamcrest.Matcher;

import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class AssertJMatcherAdaptors {
    public static <T> Condition<ValidationResult<T, DefectType>> hasNoDefects() {
        return matchedBy(Matchers.hasNoDefects());
    }

    public static <T> Condition<ValidationResult<T, DefectType>> hasDefectWith(
            Matcher<DefectInfo<DefectType>> matcher) {
        return matchedBy(Matchers.hasDefectWith(matcher));
    }

    public static <T> Condition<ValidationResult<T, DefectType>> hasOnlyWarningDefectWith(
            Matcher<DefectInfo<DefectType>> matcher) {
        return matchedBy(Matchers.hasOnlyWarningDefectWith(matcher));
    }
}
