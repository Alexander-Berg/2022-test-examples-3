package ru.yandex.direct.core.entity.conversionsourcetype.service.validation;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.conversionsourcetype.model.ConversionSourceType;
import ru.yandex.direct.core.entity.conversionsourcetype.service.validadation.ConversionSourceTypeValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.utils.TextConstants;
import ru.yandex.direct.validation.defect.StringDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.unableToDelete;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ConversionSourceTypeValidationServiceTest {
    @Autowired
    private Steps steps;

    @Autowired
    private ConversionSourceTypeValidationService validationService;

    @Test
    public void addValidationSuccess() {

        ConversionSourceType conversionSourceType = steps.conversionSourceTypeSteps().getDefaultConversionSourceType();

        ValidationResult<List<ConversionSourceType>, Defect> result =
                validationService.validateAdd(singletonList(conversionSourceType));

        assertThat(result.flattenErrors()).isEmpty();
    }

    @Test
    public void addValidationSuccessWithEn() {

        ConversionSourceType conversionSourceType = steps.conversionSourceTypeSteps().getDefaultConversionSourceTypeWithEn();

        ValidationResult<List<ConversionSourceType>, Defect> result =
                validationService.validateAdd(singletonList(conversionSourceType));

        assertThat(result.flattenErrors()).isEmpty();
    }

    @Test
    public void addValidationSuccessWithEnPunctuationAndSeparators() {
        ConversionSourceType conversionSourceType = steps.conversionSourceTypeSteps().getDefaultConversionSourceType()
                .withNameEn("Correct" + TextConstants.PUNCTUATION)
                .withDescriptionEn("Correct Eng Text With Space" + TextConstants.SPACE_CHARS);
        ValidationResult<List<ConversionSourceType>, Defect> result =
                validationService.validateAdd(singletonList(conversionSourceType));
        assertThat(result.flattenErrors()).isEmpty();
    }

    @Test
    public void addValidationWithInvalidName() {

        ConversionSourceType conversionSourceTypeWithInvalidName = steps.conversionSourceTypeSteps()
                .getConversionSourceTypeWithInvalidName();

        ValidationResult<List<ConversionSourceType>, Defect> result = validationService
                .validateAdd(singletonList(conversionSourceTypeWithInvalidName));

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(path(index(0), field("name")),
                maxStringLength(ConversionSourceTypeValidationService.MAX_NAME_LENGTH)))));
    }

    @Test
    public void addValidationWithInvalidNameEn() {

        ConversionSourceType conversionSourceTypeWithInvalidName = steps.conversionSourceTypeSteps()
                .getConversionSourceTypeWithInvalidNameEn();

        ValidationResult<List<ConversionSourceType>, Defect> result = validationService
                .validateAdd(singletonList(conversionSourceTypeWithInvalidName));

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(path(index(0), field("nameEn")),
                maxStringLength(ConversionSourceTypeValidationService.MAX_NAME_LENGTH)))));
    }

    @Test
    public void addValidationWithInvalidNameEnWithCyrillic() {

        ConversionSourceType conversionSourceTypeWithInvalidName = steps.conversionSourceTypeSteps()
                .getConversionSourceTypeWithInvalidNameEnCyrillic();

        ValidationResult<List<ConversionSourceType>, Defect> result = validationService
                .validateAdd(singletonList(conversionSourceTypeWithInvalidName));

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(path(index(0), field("nameEn")),
                StringDefects.admissibleChars()))));
    }

    @Test
    public void addValidationWithInvalidDescriptionEn() {

        ConversionSourceType conversionSourceTypeWithInvalidName = steps.conversionSourceTypeSteps()
                .getConversionSourceTypeWithInvalidDescriptionEn();

        ValidationResult<List<ConversionSourceType>, Defect> result = validationService
                .validateAdd(singletonList(conversionSourceTypeWithInvalidName));

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(path(index(0), field("descriptionEn")),
                maxStringLength(ConversionSourceTypeValidationService.MAX_DESCRIPTION_LENGTH)))));
    }

    @Test
    public void addValidationWithInvalidDescriptionEnWithCyrillic() {

        ConversionSourceType conversionSourceTypeWithInvalidName = steps.conversionSourceTypeSteps()
                .getConversionSourceTypeWithInvalidDescriptionEnWithCyrillic();

        ValidationResult<List<ConversionSourceType>, Defect> result = validationService
                .validateAdd(singletonList(conversionSourceTypeWithInvalidName));

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(path(index(0), field("descriptionEn")),
                StringDefects.admissibleChars()))));
    }

    @Test
    public void addValidationWithBrokenActivationUrl() {

        ConversionSourceType conversionSourceTypeWithBrokenActivationUrl = steps.conversionSourceTypeSteps()
                .getConversionSourceTypeWithBrokenActivationUrl();

        ValidationResult<List<ConversionSourceType>, Defect> result = validationService
                .validateAdd(singletonList(conversionSourceTypeWithBrokenActivationUrl));

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(path(index(0),
                field("activationUrl")), invalidValue()))));
    }

    @Test
    public void updateValidationSuccess() {

        ConversionSourceType conversionSourceType = steps.conversionSourceTypeSteps().getDefaultConversionSourceType();

        ValidationResult<List<ConversionSourceType>, Defect> result =
                validationService.validateUpdate(singletonList(conversionSourceType));

        assertThat(result.flattenErrors()).isEmpty();
    }

    @Test
    public void updateValidationWithInvalidDescription() {

        ConversionSourceType conversionSourceTypeWithInvalidDescription =
                steps.conversionSourceTypeSteps().getConversionSourceTypeWithInvalidDescription();

        ValidationResult<List<ConversionSourceType>, Defect> result = validationService
                .validateUpdate(singletonList(conversionSourceTypeWithInvalidDescription));

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(path(index(0), field("description")),
                maxStringLength(ConversionSourceTypeValidationService.MAX_DESCRIPTION_LENGTH)))));
    }

    @Test
    public void updateValidationWithBrokenIconUrl() {

        ConversionSourceType conversionSourceTypeWithBrokenIconUrl =
                steps.conversionSourceTypeSteps().getConversionSourceTypeWithBrokenIconUrl();

        ValidationResult<List<ConversionSourceType>, Defect> result = validationService
                .validateUpdate(singletonList(conversionSourceTypeWithBrokenIconUrl));

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(path(index(0), field("iconUrl")),
                invalidValue()))));
    }

    @Test
    public void updateValidationWhenNotIsEditable() {

        ConversionSourceType notEditableConversionSourceType =
                steps.conversionSourceTypeSteps().getNotEditableConversionSourceType();

        ValidationResult<List<ConversionSourceType>, Defect> result = validationService
                .validateUpdate(singletonList(notEditableConversionSourceType));

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(path(index(0), field("isEditable")),
                invalidValue()))));
    }

    @Test
    public void removeValidationSuccess() {

        ConversionSourceType conversionSourceType = steps.conversionSourceTypeSteps().addDefaultConversionSourceType();

        ValidationResult<List<ConversionSourceType>, Defect> result = validationService
                .validateRemove(singletonList(conversionSourceType.getId()));

        assertThat(result.flattenErrors()).isEmpty();
    }

    @Test
    public void removeValidationWhenNotExist() {

        ConversionSourceType conversionSourceType = steps.conversionSourceTypeSteps().getDefaultConversionSourceType();

        ValidationResult<List<ConversionSourceType>, Defect> result = validationService
                .validateRemove(singletonList(conversionSourceType.getId()));

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(path(), unableToDelete()))));
    }

    @Test
    public void removeValidationWhenNotIsEditable() {

        ConversionSourceType notEditableConversionSourceType =
                steps.conversionSourceTypeSteps().addNotEditableConversionSourceType();

        ValidationResult<List<ConversionSourceType>, Defect> result = validationService
                .validateRemove(singletonList(notEditableConversionSourceType.getId()));

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(path(index(0), field("isEditable")),
                invalidValue()))));
    }
}
