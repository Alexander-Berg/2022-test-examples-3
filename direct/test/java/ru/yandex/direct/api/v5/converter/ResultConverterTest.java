package ru.yandex.direct.api.v5.converter;

import java.util.List;

import com.yandex.direct.api.v5.general.ActionResult;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.api.v5.common.validation.DefaultApiPresentations;
import ru.yandex.direct.api.v5.common.validation.DefectPresentationService;
import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.api.v5.result.ApiResult;
import ru.yandex.direct.api.v5.result.ApiResultState;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.api.v5.validation.DefectTypes;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.defect.StringDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.api.v5.entity.adgroups.AdGroupDefectTypes.adGroupNameCantBeEmpty;
import static ru.yandex.direct.api.v5.entity.adgroups.AdGroupsEndpoint.ADD_PATH_CONVERTER;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class ResultConverterTest {
    private static final int SUCCESSFUL_COUNT = 10;
    private static final int ERROR_COUNT = 7;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private ResultConverter resultConverter;

    @Before
    public void before() {
        DefectPresentationService defectPresentationService =
                new DefectPresentationService(DefaultApiPresentations.HOLDER);
        this.resultConverter = new ResultConverter(mock(TranslationService.class), defectPresentationService);
    }

    @Test
    public void toMassResult_checkCounts() {
        List<Object> items = singletonList(new Object());
        ValidationResult<List<Object>, Defect> validationResult = new ValidationResult<>(items);
        MassResult<Object> massResult = MassResult.successfulMassAction(items, validationResult);
        massResult.withCounts(SUCCESSFUL_COUNT, ERROR_COUNT);
        ApiMassResult<Object> apiMassResult = resultConverter.toApiMassResult(massResult);

        softly.assertThat(apiMassResult.getSuccessfulCount()).isEqualTo(SUCCESSFUL_COUNT);
        softly.assertThat(apiMassResult.getErrorCount()).isEqualTo(ERROR_COUNT);
    }

    @Test
    public void convertValidationResult_convertEmpty() {
        List<Object> items = singletonList(new Object());
        ValidationResult<List<Object>, Defect> validationResult = new ValidationResult<>(items);
        ValidationResult<List<Object>, DefectType> converted =
                resultConverter.convertValidationResult(validationResult);
        softly.assertThat(converted.flattenErrors()).isEmpty();
        softly.assertThat(converted.getValue()).isEqualTo(items);
    }

    @Test
    public void convertValidationResult_oneLevel() {
        List<Object> items = singletonList(new Object());
        ValidationResult<List<Object>, Defect> validationResult = new ValidationResult<>(items);
        validationResult.addError(CommonDefects.invalidValue());
        validationResult.addWarning(StringDefects.notEmptyString());
        ValidationResult<List<Object>, DefectType> converted =
                resultConverter.convertValidationResult(validationResult);
        assertThat(converted.flattenErrors()).is(matchedBy(
                contains(validationError(path(), DefectTypes.invalidValue()))));
        assertThat(converted.flattenWarnings()).is(matchedBy(
                contains(validationError(path(), DefectTypes.emptyValue()))));
    }

    @Test
    public void convertValidationResult_nested() {
        List<Object> items = singletonList(new Object());
        ValidationResult<List<Object>, Defect> vr = new ValidationResult<>(items);
        vr.addError(CommonDefects.invalidValue());

        final String nestedFieldName = "xxx";
        ValidationResult<Object, Defect> subVr =
                vr.getOrCreateSubValidationResult(field(nestedFieldName), null);
        subVr.addError(StringDefects.notEmptyString());
        subVr.addWarning(CommonDefects.invalidValue());

        ValidationResult<List<Object>, DefectType> converted =
                resultConverter.convertValidationResult(vr);

        assertThat(converted).is(matchedBy(
                hasDefectWith(validationError(path(), DefectTypes.invalidValue()))));
        assertThat(converted).is(matchedBy(
                hasDefectWith(validationError(path(field(nestedFieldName)), DefectTypes.emptyValue()))));
        assertThat(converted.flattenWarnings()).is(matchedBy(
                contains(validationError(path(field(nestedFieldName)), DefectTypes.invalidValue()))));
    }

    @Test
    public void toActionResults_WithError() {
        List<DefectInfo<DefectType>> errors =
                singletonList(new DefectInfo<>(path(field("name")), "", adGroupNameCantBeEmpty()));

        List<ApiResult<Long>> results = singletonList(new ApiResult<>(null, errors, null, ApiResultState.BROKEN));
        ApiResult<List<ApiResult<Long>>> apiResult
                = new ApiResult<>(results, null, null, ApiResultState.SUCCESSFUL);
        List<ActionResult> convertedResults = resultConverter.toActionResults(apiResult, ADD_PATH_CONVERTER);

        assertThat(convertedResults.get(0).getErrors().get(0).getCode())
                .isEqualTo(errors.get(0).getDefect().getCode())
                .as("Конвертер должен установить правильную ошибку");
    }

    @Test
    public void toActionResults_WithWarning() {
        List<DefectInfo<DefectType>> warnings =
                singletonList(new DefectInfo<>(path(field("name")), "", adGroupNameCantBeEmpty()));

        List<ApiResult<Long>> results = singletonList(new ApiResult<>(null, null, warnings, ApiResultState.SUCCESSFUL));
        ApiResult<List<ApiResult<Long>>> apiResult
                = new ApiResult<>(results, null, null, ApiResultState.SUCCESSFUL);
        List<ActionResult> convertedResults = resultConverter.toActionResults(apiResult, ADD_PATH_CONVERTER);

        assertThat(convertedResults.get(0).getWarnings().get(0).getCode().intValue())
                .isEqualTo(warnings.get(0).getDefect().getCode())
                .as("Конвертер должен установить правильный ворнинг");
    }
}
