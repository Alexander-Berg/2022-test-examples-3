package ru.yandex.direct.api.v5.validation;

import org.assertj.core.api.Condition;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

public class Matchers {
    public static Matcher<DefectInfo<DefectType>> validationError(Path expectedPath,
            DefectType expectedDefectType)
    {
        return new TypeSafeDiagnosingMatcher<>() {
            @Override
            protected boolean matchesSafely(DefectInfo<DefectType> item, Description mismatchDescription) {
                if (item.getPath().equals(expectedPath) && item.getDefect().equals(expectedDefectType)) {
                    return true;
                }
                mismatchDescription.appendText("error with path ")
                        .appendValue(item.getPath())
                        .appendText(" and defect type ")
                        .appendValue(item.getDefect());
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("error with path ")
                        .appendValue(expectedPath)
                        .appendText(" and defect type ")
                        .appendValue(expectedDefectType);
            }
        };
    }

    public static Matcher<DefectInfo<DefectType>> validationError(Path expectedPath, int expectedCode) {
        return new TypeSafeDiagnosingMatcher<>() {
            @Override
            protected boolean matchesSafely(DefectInfo<DefectType> item, Description mismatchDescription) {
                if (item.getPath().equals(expectedPath) && item.getDefect().getCode() == expectedCode) {
                    return true;
                }
                mismatchDescription.appendText("error with path ")
                        .appendValue(item.getPath())
                        .appendText(" and defect code ")
                        .appendValue(item.getDefect().getCode());
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("error with path ")
                        .appendValue(expectedPath)
                        .appendText(" and defect code ")
                        .appendValue(expectedCode);
            }
        };
    }

    public static Matcher<DefectInfo<DefectType>> validationError(int defectCode) {
        return new TypeSafeDiagnosingMatcher<>() {
            @Override
            protected boolean matchesSafely(DefectInfo<DefectType> item, Description mismatchDescription) {
                if (item.getDefect().getCode() == defectCode) {
                    return true;
                }
                mismatchDescription.appendText("error code ")
                        .appendValue(item.getDefect().getCode());
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("error code ")
                        .appendValue(defectCode);
            }
        };
    }

    public static Condition<ValidationResult<?, DefectType>> defectTypeWith(Matcher<DefectInfo<DefectType>> matcher) {
        return new Condition<ValidationResult<?, DefectType>>() {
            @Override
            public boolean matches(ValidationResult<?, DefectType> value) {
                return hasDefectWith(matcher).matches(value);
            }
        };
    }

    public static <T> Matcher<ValidationResult<T, DefectType>> hasDefectWith(Matcher<DefectInfo<DefectType>> matcher) {
        return new TypeSafeDiagnosingMatcher<>() {

            @Override
            protected boolean matchesSafely(ValidationResult<T, DefectType> item, Description mismatchDescription) {
                boolean isPastFirst = false;
                for (DefectInfo<DefectType> defect : item.flattenErrors()) {
                    if (matcher.matches(defect)) {
                        return true;
                    }

                    if (isPastFirst) {
                        mismatchDescription.appendText(",");
                    }
                    matcher.describeMismatch(defect, mismatchDescription);
                    isPastFirst = true;
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("ItemValidation that has defect with ")
                        .appendDescriptionOf(matcher);
            }
        };
    }

    public static <T> Matcher<ValidationResult<T, DefectType>> hasOnlyWarningDefectWith(Matcher<DefectInfo<DefectType>> matcher) {
        return new TypeSafeDiagnosingMatcher<>() {
            @Override
            protected boolean matchesSafely(ValidationResult<T, DefectType> item, Description mismatchDescription) {
                if (!item.flattenErrors().isEmpty()) {
                    describeMismatch(item.flattenErrors(), mismatchDescription);
                    return false;
                }

                boolean isPastFirst = false;
                for (DefectInfo<DefectType> defect : item.flattenWarnings()) {
                    if (matcher.matches(defect)) {
                        return true;
                    }

                    if (isPastFirst) {
                        mismatchDescription.appendText(",");
                    }
                    matcher.describeMismatch(defect, mismatchDescription);
                    isPastFirst = true;
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("ItemValidation that has warning defect with ")
                        .appendDescriptionOf(matcher);
            }
        };
    }

    public static <T> Matcher<ValidationResult<T, DefectType>> hasNoDefects() {
        return new TypeSafeDiagnosingMatcher<>() {
            @Override
            protected boolean matchesSafely(ValidationResult<T, DefectType> item, Description mismatchDescription) {
                if (!item.flattenErrors().isEmpty()) {
                    describeMismatch(item.flattenErrors(), mismatchDescription);
                }
                if (!item.flattenWarnings().isEmpty()) {
                    describeMismatch(item.flattenWarnings(), mismatchDescription);
                }
                return item.flattenErrors().isEmpty() && item.flattenWarnings().isEmpty();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("ItemValidation has no defects");
            }
        };
    }
}
