package ru.yandex.direct.core.entity.image.service.validation;

import org.junit.Test;

import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class SaveImageFromUrlValidationServiceTest {
    private SaveImageFromUrlValidationService saveImageFromUrlValidationService =
            new SaveImageFromUrlValidationService();

    @Test
    public void validate_UrlInvalid_HasError() {
        ValidationResult<String, Defect> vr = saveImageFromUrlValidationService.validate("url");
        assertThat(vr)
                .is(matchedBy(
                        hasDefectDefinitionWith(validationError(path(), CommonDefects.invalidValue()))));
    }
}
