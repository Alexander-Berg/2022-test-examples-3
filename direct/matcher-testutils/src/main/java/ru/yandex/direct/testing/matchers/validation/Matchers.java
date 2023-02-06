package ru.yandex.direct.testing.matchers.validation;

import java.util.List;
import java.util.function.Function;

import one.util.streamex.StreamEx;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectId;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

public class Matchers {
    public static Matcher<DefectInfo<Defect>> validationError(Path expectedPath,
                                                              Defect expectedDefectType) {
        return new TypeSafeDiagnosingMatcher<DefectInfo<Defect>>() {
            @Override
            protected boolean matchesSafely(DefectInfo<Defect> item, Description mismatchDescription) {
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

    public static Matcher<DefectInfo<Defect>> matchesWith(Defect defect) {
        return new TypeSafeDiagnosingMatcher<DefectInfo<Defect>>() {
            @Override
            protected boolean matchesSafely(DefectInfo<Defect> item, Description mismatchDescription) {
                if (item.getDefect().equals(defect)) {
                    return true;
                }
                mismatchDescription.appendText("error defect ")
                        .appendValue(item.getDefect().toString());
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("error defect ")
                        .appendValue(defect.toString());
            }
        };
    }


    public static Matcher<DefectInfo<Defect>> validationError(DefectId defectId) {
        return new TypeSafeDiagnosingMatcher<DefectInfo<Defect>>() {
            @Override
            protected boolean matchesSafely(DefectInfo<Defect> item, Description mismatchDescription) {
                if (item.getDefect().defectId().equals(defectId)) {
                    return true;
                }
                mismatchDescription.appendText("error defect ")
                        .appendValue(item.getDefect().defectId());
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("error defect ")
                        .appendValue(defectId);
            }
        };
    }

    public static Matcher<DefectInfo<Defect>> validationError(Path expectedPath, DefectId defectId) {
        return new TypeSafeDiagnosingMatcher<DefectInfo<Defect>>() {
            @Override
            protected boolean matchesSafely(DefectInfo<Defect> item, Description mismatchDescription) {
                if (item.getPath().equals(expectedPath) && item.getDefect().defectId().equals(defectId)) {
                    return true;
                }
                mismatchDescription.appendText("error with path ")
                        .appendValue(item.getPath())
                        .appendText(" and error defect ")
                        .appendValue(item.getDefect().defectId());
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("error with path ")
                        .appendValue(expectedPath)
                        .appendText(" and error defect ")
                        .appendValue(defectId);
            }
        };
    }

    public static Matcher<DefectInfo<Defect>> anyValidationErrorOnPathStartsWith(Path expectedPath) {
        return new TypeSafeDiagnosingMatcher<DefectInfo<Defect>>() {
            @Override
            protected boolean matchesSafely(DefectInfo<Defect> item, Description mismatchDescription) {
                if (item.getPath().startsWith(expectedPath)) {
                    return true;
                }
                mismatchDescription.appendText("any error with path starts with").appendValue(item.getPath());
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("error with path starts with").appendValue(expectedPath);
            }
        };
    }

    public static Matcher<DefectInfo<Defect>> anyValidationErrorOnPath(Path expectedPath) {
        return new TypeSafeDiagnosingMatcher<DefectInfo<Defect>>() {
            @Override
            protected boolean matchesSafely(DefectInfo<Defect> item, Description mismatchDescription) {
                if (item.getPath().equals(expectedPath)) {
                    return true;
                }
                mismatchDescription.appendText("any error with path ").appendValue(item.getPath());
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("error with path ").appendValue(expectedPath);
            }
        };
    }

    private static <T> Matcher<ValidationResult<T, Defect>> hasDefectWithDefinition(
            Matcher<DefectInfo<Defect>> matcher,
            Function<ValidationResult<T, Defect>, Iterable<DefectInfo<Defect>>> defectsSelector) {
        return new TypeSafeDiagnosingMatcher<ValidationResult<T, Defect>>() {

            @Override
            protected boolean matchesSafely(ValidationResult<T, Defect> item,
                                            Description mismatchDescription) {
                boolean isPastFirst = false;
                for (DefectInfo<Defect> defectDefinition : defectsSelector.apply(item)) {
                    if (matcher.matches(defectDefinition)) {
                        return true;
                    }

                    if (isPastFirst) {
                        mismatchDescription.appendText(",");
                    }
                    matcher.describeMismatch(defectDefinition, mismatchDescription);
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

    /**
     * Проверяет что результат содержит error, удовлетворяющую заданному matcher-у
     */
    public static <T> Matcher<ValidationResult<T, Defect>> hasDefectWithDefinition(
            Matcher<DefectInfo<Defect>> matcher) {
        return hasDefectWithDefinition(matcher, ValidationResult::flattenErrors);
    }

    /**
     * Проверяет что результат содержит error или warning, удовлетворяющий заданному matcher-у
     */
    public static <T> Matcher<ValidationResult<T, Defect>> hasDefectDefinitionWith(
            Matcher<DefectInfo<Defect>> matcher) {
        return hasDefectWithDefinition(
                matcher,
                item -> StreamEx.of(item.flattenErrors()).append(item.flattenWarnings()));
    }

    private static <T> Matcher<ValidationResult<T, Defect>> hasNoDefectsDefinitions(
            Function<ValidationResult<T, Defect>, List<DefectInfo<Defect>>> defectsSelector) {
        return new TypeSafeDiagnosingMatcher<ValidationResult<T, Defect>>() {
            @Override
            protected boolean matchesSafely(ValidationResult<T, Defect> item,
                                            Description mismatchDescription) {
                List<DefectInfo<Defect>> defects = defectsSelector.apply(item);
                if (defects.isEmpty()) {
                    return true;
                }
                describeMismatch(defects, mismatchDescription);
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("ItemValidation has no defects");
            }
        };
    }

    /**
     * Проверяет, что в результате нет error-ов (но могут быть warning-ги)
     */
    public static <T> Matcher<ValidationResult<T, Defect>> hasNoDefectsDefinitions() {
        return hasNoDefectsDefinitions(ValidationResult::flattenErrors);
    }

    /**
     * Проверяет, что в результате нет error-ов (но могут быть warning-ги)
     */
    public static <T> Matcher<ValidationResult<T, Defect>> hasNoErrors() {
        return hasNoDefectsDefinitions();
    }

    /**
     * Проверяет, что в результате нет ни error-ов, ни warning-ов
     */
    public static <T> Matcher<ValidationResult<T, Defect>> hasNoErrorsAndWarnings() {
        return hasNoDefectsDefinitions(
                item -> StreamEx.of(item.flattenErrors()).append(item.flattenWarnings()).toList());
    }

    public static <T> Matcher<ValidationResult<T, Defect>> hasNoWarnings() {
        return hasNoDefectsDefinitions(
                item -> StreamEx.of(item.flattenWarnings()).toList());
    }

    /**
     * Проверяет, что в результате есть warning, удовлетворяющий заданному matcher-у
     */
    public static <T> Matcher<ValidationResult<T, Defect>> hasWarningWithDefinition(
            Matcher<DefectInfo<Defect>> matcher) {
        return hasDefectWithDefinition(matcher, ValidationResult::flattenWarnings);
    }
}
