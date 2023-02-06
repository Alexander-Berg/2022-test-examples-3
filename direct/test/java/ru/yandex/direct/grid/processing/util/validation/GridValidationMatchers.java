package ru.yandex.direct.grid.processing.util.validation;

import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.TypeSafeMatcher;

import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

public class GridValidationMatchers {

    @Factory
    public static Matcher<GdDefect> gridDefect(String expectedPath, Defect expectedDefectType) {
        return new TypeSafeDiagnosingMatcher<>() {

            @Override
            protected boolean matchesSafely(GdDefect item, Description mismatchDescription) {
                if (item.getPath().equals(expectedPath)
                        && item.getCode().equals(expectedDefectType.defectId().getCode())
                        && Objects.equals(item.getParams(), expectedDefectType.params())) {
                    return true;
                }

                mismatchDescription.appendText("path ")
                        .appendValue(item.getPath())
                        .appendText(" and defect ")
                        .appendValue(item.getCode())
                        .appendText(" and params ")
                        .appendValue(item.getParams());
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("path ")
                        .appendValue(expectedPath)
                        .appendText(" and defect ")
                        .appendValue(expectedDefectType);
            }
        };
    }

    @Factory
    public static Matcher<GdDefect> gridDefect(Path expectedPath, Defect expectedDefectType) {
        return gridDefect(expectedPath.toString(), expectedDefectType);
    }

    @Factory
    public static Matcher<GdValidationResult> hasErrorsWith(Matcher<GdDefect> matcher) {
        return new TypeSafeDiagnosingMatcher<>() {

            @Override
            protected boolean matchesSafely(GdValidationResult item, Description mismatchDescription) {
                boolean isPastFirst = false;
                for (GdDefect gridDefect : item.getErrors()) {
                    if (matcher.matches(gridDefect)) {
                        return true;
                    }

                    mismatchDescription.appendText(isPastFirst ? ", " : "has errors with ");
                    matcher.describeMismatch(gridDefect, mismatchDescription);
                    isPastFirst = true;
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("that has errors with ")
                        .appendDescriptionOf(matcher);
            }
        };
    }

    @Factory
    public static Matcher<GridValidationException> hasValidationResult(
            Matcher<GdValidationResult> validationResultMatcher) {
        return new TypeSafeMatcher<>() {

            @Override
            protected boolean matchesSafely(GridValidationException exception) {
                return validationResultMatcher.matches(exception.getValidationResult());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("exception with validationResult ");
                description.appendDescriptionOf(validationResultMatcher);
            }

            @Override
            protected void describeMismatchSafely(GridValidationException exception, Description description) {
                validationResultMatcher.describeMismatch(exception.getValidationResult(), description);
            }
        };
    }
}
