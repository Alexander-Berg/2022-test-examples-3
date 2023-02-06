package ru.yandex.direct.core.entity.banner.service;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
public class ValidationResultAssertions {

    public static <B extends OldBanner> void assertSingleBannerError(
            ValidationResult<List<B>, Defect> validationResult,
            Defect expectedDefect) {
        assertThat(validationResult,
                hasDefectDefinitionWith(validationError(path(index(0)), expectedDefect)));
    }

    public static <B extends OldBanner> void assertSingleBannerError(
            ValidationResult<List<B>, Defect> validationResult,
            String field, Defect expectedDefect) {
        assertThat(validationResult,
                hasDefectDefinitionWith(validationError(path(index(0), field(field)), expectedDefect)));
    }

    public static <B extends OldBanner> void assertSingleBannerModelChangesError(
            ValidationResult<List<ModelChanges<B>>, Defect> validationResult,
            String field, Defect expectedDefect) {
        assertThat(validationResult,
                hasDefectDefinitionWith(validationError(path(index(0), field(field)), expectedDefect)));
    }

    public static <B extends OldBanner> void assertBannerError(
            ValidationResult<B, Defect> validationResult,
            String field, Defect expectedDefect) {
        assertThat(validationResult,
                hasDefectDefinitionWith(validationError(path(field(field)), expectedDefect)));
    }

    public static <T> void checkElementError(ValidationResult<List<T>, Defect> validationResult,
                                             Path path, Defect defect) {
        MatcherAssert.assertThat("результат валидации не должен содержать ошибок уровня операции",
                validationResult.hasErrors(),
                is(false));
        MatcherAssert.assertThat("результат валидации должен содержать ошибку уровня элемента",
                validationResult,
                hasDefectDefinitionWith(validationError(path, defect)));
    }

}
